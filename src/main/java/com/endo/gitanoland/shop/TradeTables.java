package com.endo.gitanoland.shop;

import com.endo.gitanoland.Config;
import com.endo.gitanoland.GitanoLand;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.util.DefaultGunAttachments;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.micaxs.smokeleaf.strain.StrainData;
import net.micaxs.smokeleaf.strain.StrainItemFactory;
import net.micaxs.smokeleaf.strain.StrainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Tablas de tratos en JSON (config/gitanoland/trades/<nombre>.json; si no existe se copia la de por defecto del mod).
 * Los items de TaCZ se buscan por nombre dentro de los ids que tenga cargados: ver "find" en los JSON.
 */
public final class TradeTables {
    private TradeTables() {}

    public record CostDef(ResourceLocation item, int count) {}

    public record Def(String type, int kirkys, double base, List<CostDef> extra, List<String> find,
                      ResourceLocation id, int count, int xp, int variants) {}

    public record Level(int level, List<Def> trades, double luckyChance, int luckyReplace, List<Def> lucky) {}

    /** Especialidad de un aldeano: qué vende (cada aldeano recibe una según su UUID, con "weight" como rareza). */
    public record Specialty(String id, int weight, List<Level> levels) {}

    public record Table(List<Level> levels, List<Specialty> specialties, List<Def> stock, double thcPerPoint,
                        double cbdPerPoint, double maxMultiplier, double priceMultiplier) {}

    public record Result(List<ShopOffer> offers, List<String> missing) {}

    // ------------------------------------------------------------------ carga

    public static Table load(String name) {
        Path file = FMLPaths.CONFIGDIR.get().resolve("gitanoland").resolve("trades").resolve(name + ".json");
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                try (InputStream in = TradeTables.class.getResourceAsStream("/data/gitanoland/trades/" + name + ".json")) {
                    if (in == null) throw new IOException("falta el JSON por defecto: " + name);
                    Files.copy(in, file);
                }
            }
            try (Reader r = Files.newBufferedReader(file)) {
                return withPriceMultiplier(parse(JsonParser.parseReader(r).getAsJsonObject()), priceMultiplier(name));
            }
        } catch (Exception e) {
            GitanoLand.LOGGER.error("[GitanoLand] No se pudo leer {}: {}", file, e.toString());
            return new Table(List.of(), List.of(), List.of(), 0.01, 0.005, 3.0, 1.0);
        }
    }

    private static Table withPriceMultiplier(Table t, double mult) {
        return new Table(t.levels(), t.specialties(), t.stock(), t.thcPerPoint(), t.cbdPerPoint(), t.maxMultiplier(), mult);
    }

    /** Multiplicador de precios de la tienda (config del servidor). Si la config aún no está cargada (cliente), el valor por defecto. */
    private static double priceMultiplier(String shop) {
        try {
            switch (shop) {
                case "gunsmith": return Config.GUNSMITH_PRICE_MULTIPLIER.get();
                case "ammo_maker": return Config.AMMO_PRICE_MULTIPLIER.get();
                default: return 1.0;
            }
        } catch (Exception e) {
            return shop.equals("gunsmith") ? 1.03 : 1.0;
        }
    }

    private static int price(int kirkys, double mult) {
        if (kirkys <= 0 || mult == 1.0) return kirkys;
        return Math.max(1, (int) Math.round(kirkys * mult));
    }

    private static Table parse(JsonObject root) {
        List<Level> levels = root.has("levels") ? parseLevels(root.getAsJsonArray("levels")) : new ArrayList<>();
        List<Specialty> specialties = new ArrayList<>();
        if (root.has("specialties")) {
            for (JsonElement se : root.getAsJsonArray("specialties")) {
                JsonObject so = se.getAsJsonObject();
                specialties.add(new Specialty(so.get("id").getAsString(), Math.max(1, (int) dbl(so, "weight", 1)),
                        so.has("levels") ? parseLevels(so.getAsJsonArray("levels")) : new ArrayList<>()));
            }
        }
        List<Def> stock = defs(root, "stock");
        JsonObject price = root.has("price") ? root.getAsJsonObject("price") : new JsonObject();
        return new Table(levels, specialties, stock, dbl(price, "thc_per_point", 0.01), dbl(price, "cbd_per_point", 0.005),
                dbl(price, "max_multiplier", 3.0), 1.0);
    }

    private static List<Level> parseLevels(JsonArray arr) {
        List<Level> levels = new ArrayList<>();
        for (JsonElement le : arr) {
            JsonObject lo = le.getAsJsonObject();
            List<Def> trades = defs(lo, "trades");
            double chance = 0;
            int replace = 1;
            List<Def> lucky = List.of();
            if (lo.has("lucky")) {
                JsonObject lk = lo.getAsJsonObject("lucky");
                chance = dbl(lk, "chance", 0);
                replace = (int) dbl(lk, "replace", 1);
                lucky = defs(lk, "offers");
            }
            levels.add(new Level((int) dbl(lo, "level", 1), trades, chance, replace, lucky));
        }
        return levels;
    }

    private static List<Def> defs(JsonObject o, String key) {
        List<Def> out = new ArrayList<>();
        if (!o.has(key)) return out;
        for (JsonElement e : o.getAsJsonArray(key)) {
            JsonObject d = e.getAsJsonObject();
            List<CostDef> extra = new ArrayList<>();
            if (d.has("extra")) {
                for (JsonElement ce : d.getAsJsonArray("extra")) {
                    JsonObject c = ce.getAsJsonObject();
                    extra.add(new CostDef(ResourceLocation.parse(c.get("item").getAsString()), (int) dbl(c, "count", 1)));
                }
            }
            List<String> find = new ArrayList<>();
            if (d.has("find")) {
                JsonElement f = d.get("find");
                if (f.isJsonArray()) for (JsonElement fe : (JsonArray) f) find.add(fe.getAsString());
                else find.add(f.getAsString());
            }
            ResourceLocation id = d.has("id") ? ResourceLocation.parse(d.get("id").getAsString()) : null;
            out.add(new Def(d.has("type") ? d.get("type").getAsString() : "item", (int) dbl(d, "kirkys", 0),
                    dbl(d, "base", 0), extra, find, id, (int) dbl(d, "count", 1), (int) dbl(d, "xp", -1),
                    (int) dbl(d, "variants", 1)));
        }
        return out;
    }

    private static double dbl(JsonObject o, String k, double def) {
        return o.has(k) ? o.get(k).getAsDouble() : def;
    }

    // ------------------------------------------------------------------ construcción

    /** Especialidad de un aldeano (estable por semilla; la rareza sale de "weight"). null si la tienda no tiene. */
    public static Specialty specialty(Table t, long seed) {
        if (t.specialties().isEmpty()) return null;
        int total = 0;
        for (Specialty s : t.specialties()) total += s.weight();
        int roll = new Random(seed * 0x9E3779B97F4A7C15L + 0x5DEECE66DL).nextInt(total);
        for (Specialty s : t.specialties()) {
            roll -= s.weight();
            if (roll < 0) return s;
        }
        return t.specialties().get(0);
    }

    /** Ofertas de un aldeano: todos los niveles hasta el actual. Lo "lucky" es estable por aldeano (semilla). */
    public static Result buildLeveled(Table t, int villagerLevel, long seed) {
        return buildLeveledFor(t, specialty(t, seed), villagerLevel, seed);
    }

    /** Igual, pero con una especialidad concreta (para REI/JEI). Los "levels" sueltos del JSON son comunes a todas. */
    public static Result buildLeveledFor(Table t, Specialty spec, int villagerLevel, long seed) {
        List<ShopOffer> offers = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        List<Level> all = new ArrayList<>(t.levels());
        if (spec != null) all.addAll(spec.levels());
        all.sort(Comparator.comparingInt(Level::level));
        for (Level lv : all) {
            if (lv.level() > villagerLevel) continue;
            List<Def> defs = new ArrayList<>(lv.trades());
            if (lv.luckyChance() > 0 && !lv.lucky().isEmpty() && !defs.isEmpty()) {
                Random r = new Random(seed ^ (lv.level() * 0x9E3779B97F4A7C15L));
                if (r.nextDouble() < lv.luckyChance()) {
                    for (int k = 0; k < Math.max(1, lv.luckyReplace()); k++) {
                        defs.set(r.nextInt(defs.size()), lv.lucky().get(r.nextInt(lv.lucky().size())));
                    }
                }
            }
            for (Def d : defs) {
                ItemStack stack = makeStack(d);
                if (stack.isEmpty()) {
                    missing.add(label(d));
                    continue;
                }
                addOffer(offers, missing, d, stack, price(d.kirkys(), t.priceMultiplier()),
                        d.xp() >= 0 ? d.xp() : 10 * lv.level());
            }
        }
        return new Result(offers, missing);
    }

    /** Stock del WeedsGuy: cepas aleatorias pero estables durante un día (semilla). */
    public static Result buildStock(Table t, long seed) {
        List<ShopOffer> offers = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        List<String> strains = new ArrayList<>(StrainRegistry.ids());
        strains.sort(Comparator.naturalOrder());
        Random r = new Random(seed);
        for (Def d : t.stock()) {
            if (!d.type().equals("strain")) {
                ItemStack stack = makeStack(d);
                if (stack.isEmpty()) missing.add(label(d));
                else addOffer(offers, missing, d, stack, d.kirkys(), d.xp() >= 0 ? d.xp() : 0);
                continue;
            }
            Item item = d.id() == null ? null : BuiltInRegistries.ITEM.getOptional(d.id()).orElse(null);
            if (item == null || strains.isEmpty()) {
                missing.add(label(d));
                continue;
            }
            for (int v = 0; v < Math.max(1, d.variants()); v++) {
                String sid = strains.get(r.nextInt(strains.size()));
                ItemStack stack = StrainItemFactory.create(item, sid);
                if (stack.isEmpty()) continue;
                stack.setCount(Math.max(1, d.count()));
                StrainData sd = StrainRegistry.getRequired(sid);
                double mult = Math.min(t.maxMultiplier(), 1.0 + sd.thc() * t.thcPerPoint() + sd.cbd() * t.cbdPerPoint());
                int price = (int) Math.max(1, Math.round(d.base() * mult));
                addOffer(offers, missing, d, stack, price, 0);
            }
        }
        return new Result(offers, missing);
    }

    private static void addOffer(List<ShopOffer> offers, List<String> missing, Def d, ItemStack stack, int kirkys, int xp) {
        List<ShopOffer.Cost> extras = new ArrayList<>();
        for (CostDef c : d.extra()) {
            Item it = BuiltInRegistries.ITEM.getOptional(c.item()).orElse(null);
            if (it == null) {
                missing.add(c.item().toString());
                return;
            }
            extras.add(new ShopOffer.Cost(it, c.count()));
        }
        offers.add(new ShopOffer(kirkys, extras, stack, xp));
    }

    private static String label(Def d) {
        if (!d.find().isEmpty()) return d.type() + ":" + d.find().get(0);
        return d.type() + ":" + d.id();
    }

    // ------------------------------------------------------------------ resolución de items

    private static ItemStack makeStack(Def d) {
        switch (d.type()) {
            case "ammo": {
                ResourceLocation id = pick(ids(TimelessAPI.getAllCommonAmmoIndex()), d);
                return id == null ? ItemStack.EMPTY
                        : AmmoItemBuilder.create().setId(id).setCount(Math.max(1, d.count())).build();
            }
            case "tacz": {
                ResourceLocation gun = pick(ids(TimelessAPI.getAllCommonGunIndex()), d);
                if (gun != null) return buildGun(gun);
                ResourceLocation att = pick(ids(TimelessAPI.getAllCommonAttachmentIndex()), d);
                return att == null ? ItemStack.EMPTY
                        : AttachmentItemBuilder.create().setId(att).setCount(1).build();
            }
            default: {
                Item item = d.id() == null ? null : BuiltInRegistries.ITEM.getOptional(d.id()).orElse(null);
                return item == null ? ItemStack.EMPTY : new ItemStack(item, Math.max(1, d.count()));
            }
        }
    }

    private static ItemStack buildGun(ResourceLocation id) {
        CommonGunIndex idx = TimelessAPI.getCommonGunIndex(id).orElse(null);
        if (idx == null) return ItemStack.EMPTY;
        GunItemBuilder b = GunItemBuilder.create().setId(id).setCount(1).setAmmoCount(0).setAmmoInBarrel(false)
                .putAllVirtualAttachment(DefaultGunAttachments.defaults(id));
        List<FireMode> modes = idx.getGunData().getFireModeSet();
        if (!modes.isEmpty()) b.setFireMode(modes.get(0));
        return b.build();
    }

    private static <T> List<ResourceLocation> ids(Set<Map.Entry<ResourceLocation, T>> set) {
        List<ResourceLocation> out = new ArrayList<>();
        for (Map.Entry<ResourceLocation, T> e : set) out.add(e.getKey());
        return out;
    }

    private static ResourceLocation pick(Collection<ResourceLocation> ids, Def d) {
        if (d.id() != null && ids.contains(d.id())) return d.id();
        for (String q : d.find()) {
            ResourceLocation r = match(ids, q);
            if (r != null) return r;
        }
        return null;
    }

    private static String compact(String s) {
        return s.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    /**
     * Busca por nombre: primero igualdad exacta (ignorando guiones y espacios); si no, ids que contengan TODAS las
     * palabras de la búsqueda, ganando el más corto. Si hay empate, se descarta para no entregar un item equivocado.
     */
    static ResourceLocation match(Collection<ResourceLocation> ids, String query) {
        String[] tokens = query.toLowerCase().split("[^a-z0-9]+");
        List<String> toks = new ArrayList<>();
        for (String t : tokens) if (!t.isEmpty()) toks.add(t);
        if (toks.isEmpty()) return null;
        String qc = String.join("", toks);

        List<ResourceLocation> exact = new ArrayList<>();
        for (ResourceLocation id : ids) if (compact(id.getPath()).equals(qc)) exact.add(id);
        if (exact.size() == 1) return exact.get(0);

        List<ResourceLocation> cands = new ArrayList<>();
        for (ResourceLocation id : ids) {
            String c = compact(id.getPath());
            boolean all = true;
            for (String t : toks) if (!c.contains(t)) { all = false; break; }
            if (all) cands.add(id);
        }
        if (cands.isEmpty()) return null;
        cands.sort(Comparator.comparingInt((ResourceLocation id) -> id.getPath().length()));
        if (cands.size() > 1 && cands.get(0).getPath().length() == cands.get(1).getPath().length()) return null;
        return cands.get(0);
    }
}
