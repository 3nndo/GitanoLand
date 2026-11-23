package com.endo.gitanoland.event;

import com.endo.gitanoland.GitanoLand;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.micaxs.smokeleaf.component.ModDataComponentTypes;
import net.micaxs.smokeleaf.strain.StrainData;
import net.micaxs.smokeleaf.strain.StrainItemFactory;
import net.micaxs.smokeleaf.strain.StrainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

/**
 * Aldeanos normales que compran weed (Smokeleaf) a cambio de Kirkys. Solo algunos aldeanos son compradores (según su UUID).
 * El precio depende del THC y del CBD de la cepa y de su rareza (ver config/gitanoland/trades/villager_weed.json).
 * Cada comprador pide una cepa concreta: hay que llevarle ESA cepa.
 */
@EventBusSubscriber(modid = GitanoLand.MODID)
public final class VillagerWeedBuyers {
    private VillagerWeedBuyers() {}

    private record Rarity(String name, double minScore, double mult) {}

    private record Want(String item, int level, int count, double base, int maxUses, int xp) {}

    private record Cfg(boolean enabled, double buyerChance, double thcPerPoint, double cbdPerPoint, double maxMultiplier,
                       int maxKirkys, List<Rarity> rarities, List<Want> wants, List<String> skipProfessions,
                       List<String> strongEffects, List<String> mildEffects) {}

    private static Cfg cfg;

    private static synchronized Cfg cfg() {
        if (cfg == null) cfg = load();
        return cfg;
    }

    private static Cfg load() {
        Path file = FMLPaths.CONFIGDIR.get().resolve("gitanoland").resolve("trades").resolve("villager_weed.json");
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                try (InputStream in = VillagerWeedBuyers.class.getResourceAsStream("/data/gitanoland/trades/villager_weed.json")) {
                    if (in == null) throw new IOException("falta el JSON por defecto villager_weed.json");
                    Files.copy(in, file);
                }
            }
            try (Reader r = Files.newBufferedReader(file)) {
                return parse(JsonParser.parseReader(r).getAsJsonObject());
            }
        } catch (Exception e) {
            GitanoLand.LOGGER.error("[GitanoLand] No se pudo leer {}: {}. Los aldeanos NO comprarán weed.", file, e.toString());
            return new Cfg(false, 0, 0, 0, 1, 64, List.of(), List.of(), List.of(), List.of(), List.of());
        }
    }

    private static Cfg parse(JsonObject o) {
        JsonObject price = o.getAsJsonObject("price");
        List<Rarity> rar = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray("rarities")) {
            JsonObject r = e.getAsJsonObject();
            rar.add(new Rarity(r.get("name").getAsString(), r.get("min_score").getAsDouble(), r.get("multiplier").getAsDouble()));
        }
        rar.sort(Comparator.comparingDouble(Rarity::minScore));
        List<Want> wants = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray("wants")) {
            JsonObject w = e.getAsJsonObject();
            wants.add(new Want(w.get("item").getAsString(), w.get("level").getAsInt(), w.get("count").getAsInt(),
                    w.get("base").getAsDouble(), w.get("max_uses").getAsInt(), w.get("xp").getAsInt()));
        }
        return new Cfg(o.get("enabled").getAsBoolean(), o.get("buyer_chance").getAsDouble(),
                price.get("thc_per_point").getAsDouble(), price.get("cbd_per_point").getAsDouble(),
                price.get("max_multiplier").getAsDouble(), o.get("max_kirkys_per_offer").getAsInt(), rar, wants,
                strings(o.getAsJsonArray("skip_professions")), strings(o.getAsJsonObject("rarity_score").getAsJsonArray("strong_effects")),
                strings(o.getAsJsonObject("rarity_score").getAsJsonArray("mild_effects")));
    }

    private static List<String> strings(JsonArray a) {
        List<String> l = new ArrayList<>();
        for (JsonElement e : a) l.add(e.getAsString());
        return l;
    }

    // ------------------------------------------------------------------ rareza y precio

    /** Puntuación de rareza de una cepa: efectos valiosos, potencia, híbrido... */
    static double rarityScore(StrainData sd, Cfg c) {
        double s = 0;
        int n = 0;
        for (ResourceLocation fx : sd.effects()) {
            String id = fx.toString();
            if (c.strongEffects().contains(id)) s += 2;
            else if (c.mildEffects().contains(id)) s += 1;
            if (n++ > 0) s += 1; // cada efecto extra
        }
        int total = sd.thc() + sd.cbd();
        if (total >= 40) s += 3;
        else if (total >= 33) s += 2;
        else if (total >= 28) s += 1;
        if (sd.amplifier() > 1) s += sd.amplifier() - 1;
        if (sd.durationTicks() >= 230) s += 1;
        if (sd.baseStrain1() != null && !sd.baseStrain1().isEmpty()) s += 1; // cruzada: cuesta más conseguirla
        return s;
    }

    static Rarity rarityOf(double score, Cfg c) {
        Rarity best = c.rarities().get(0);
        for (Rarity r : c.rarities()) if (score >= r.minScore()) best = r;
        return best;
    }

    static int priceFor(Want w, StrainData sd, Cfg c) {
        double potency = Math.min(c.maxMultiplier(), 1.0 + sd.thc() * c.thcPerPoint() + sd.cbd() * c.cbdPerPoint());
        double rarity = rarityOf(rarityScore(sd, c), c).mult();
        long kirkys = Math.round(w.base() * w.count() * potency * rarity);
        return (int) Math.max(1, Math.min(c.maxKirkys(), kirkys));
    }

    // ------------------------------------------------------------------ tratos

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTrades(VillagerTradesEvent event) {
        Cfg c = cfg();
        if (!c.enabled() || c.rarities().isEmpty()) return;
        VillagerProfession p = event.getType();
        ResourceLocation key = BuiltInRegistries.VILLAGER_PROFESSION.getKey(p);
        if (key == null || c.skipProfessions().contains(key.toString())) return;
        for (Want w : c.wants()) {
            event.getTrades().computeIfAbsent(w.level(), k -> new ArrayList<>()).add(new Buy(w));
        }
    }

    private record Buy(Want w) implements ItemListing {
        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource random) {
            Cfg c = cfg();
            // solo algunos aldeanos compran: se decide por el UUID, así es siempre el mismo comprador
            long h = trader.getUUID().getLeastSignificantBits() ^ (trader.getUUID().getMostSignificantBits() * 31);
            double roll = ((h >>> 1) % 10000) / 10000.0;
            if (roll >= c.buyerChance()) return null;
            ResourceLocation id = ResourceLocation.parse(w.item());
            var item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
            if (item == null) return null;
            List<String> strains = new ArrayList<>(StrainRegistry.ids());
            if (strains.isEmpty()) return null;
            strains.sort(Comparator.naturalOrder());
            String sid = pickStrain(strains, c, random);
            StrainData sd = StrainRegistry.getRequired(sid);
            // el aldeano pide ESA cepa (por STRAIN_ID); lo demás del objeto da igual
            ItemCost cost = new ItemCost(item, w.count()).withComponents(b -> b.expect(ModDataComponentTypes.STRAIN_ID.get(), sid));
            ItemStack pay = new ItemStack(GitanoLand.KIRKY.get(), priceFor(w, sd, c));
            return new MerchantOffer(cost, Optional.empty(), pay, 0, w.maxUses(), w.xp(), 0.05f);
        }

        /** Las cepas raras salen menos: peso = 1 / multiplicador de rareza. */
        private static String pickStrain(List<String> strains, Cfg c, RandomSource random) {
            double[] weight = new double[strains.size()];
            double sum = 0;
            for (int i = 0; i < weight.length; i++) {
                StrainData sd = StrainRegistry.getRequired(strains.get(i));
                weight[i] = 1.0 / Math.max(0.1, rarityOf(rarityScore(sd, c), c).mult());
                sum += weight[i];
            }
            double r = random.nextDouble() * sum;
            for (int i = 0; i < weight.length; i++) {
                r -= weight[i];
                if (r <= 0) return strains.get(i);
            }
            return strains.get(strains.size() - 1);
        }
    }

    /** Para pruebas y /gitanoland: texto con el precio de una cepa. */
    public static String describe(String strainId, String itemId) {
        Cfg c = cfg();
        StrainData sd = StrainRegistry.getRequired(strainId);
        Want w = c.wants().stream().filter(x -> x.item().equals(itemId)).findFirst().orElse(c.wants().get(0));
        double score = rarityScore(sd, c);
        return strainId + ": THC " + sd.thc() + ", CBD " + sd.cbd() + ", rareza " + rarityOf(score, c).name() + " (puntos " + score
                + ") -> " + priceFor(w, sd, c) + " Kirkys por " + w.count() + " x " + w.item();
    }
}
