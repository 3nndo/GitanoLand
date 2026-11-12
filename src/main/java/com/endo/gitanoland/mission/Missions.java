package com.endo.gitanoland.mission;

import com.endo.gitanoland.GitanoLand;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Tratos especiales de los aldeanos normales: uno por nivel, añadido a su lista de comercio normal. Piden objetos
 * difíciles y pagan Kirkys. Las profesiones de trueque fácil (palos, trigo...) no salen en el JSON y no dan Kirkys.
 * Config: config/gitanoland/missions.json (si no existe, el del mod).
 */
public final class Missions {
    public static final int LEVELS = 5;
    public static final int STACK = 64;
    private static final String K_LEVEL = "gitanoland_lvl";
    private static final String K_IDX = "gitanoland_special_idx";
    private static final String K_USED = "gitanoland_special_used";
    private static Map<ResourceLocation, List<Special>> cache;

    private Missions() {}

    /** Trato especial de un nivel (reward..rewardMax: el valor concreto sale del UUID del aldeano). */
    public record Special(Item item, int count, int reward, int rewardMax) {}

    public static Special of(Villager v, int level) {
        if (v.isBaby() || level < 1 || level > LEVELS) return null;
        ResourceLocation prof = BuiltInRegistries.VILLAGER_PROFESSION.getKey(v.getVillagerData().getProfession());
        List<Special> list = table().get(prof);
        if (list == null || list.size() < level) return null;
        return list.get(level - 1);
    }

    public static boolean hasMissions(Villager v) {
        return of(v, 1) != null;
    }

    private static long hash(Villager v) {
        return v.getUUID().getMostSignificantBits() ^ v.getUUID().getLeastSignificantBits();
    }

    /** Kirkys totales que paga el trato especial de este aldeano. */
    public static int reward(Villager v, Special s) {
        if (s.rewardMax() <= s.reward()) return s.reward();
        return s.reward() + (int) Math.floorMod(hash(v), (long) (s.rewardMax() - s.reward() + 1));
    }

    private static RandomSource rng(Villager v, int level) {
        return RandomSource.create(hash(v) * 31L + level);
    }

    /** Añade a la lista del aldeano los tratos de los niveles que aún no tenía y recalcula los usos de los especiales. */
    public static void ensure(Villager v, Player player) {
        MerchantOffers offers = v.getOffers();
        CompoundTag t = v.getPersistentData();
        int done = t.getInt(K_LEVEL);
        int cur = Math.min(LEVELS, v.getVillagerData().getLevel());
        int[] idx = specialIdx(t);
        for (int l = done + 1; l <= cur; l++) {
            Special s = of(v, l);
            if (s == null) continue;
            RandomSource r = rng(v, l);
            // 1) trueque normal con Kirkys (2 a 5), copiado de uno de los tratos que ya tiene
            MerchantOffer reg = regularKirky(offers, r);
            if (reg != null) offers.add(reg);
            // 2) trato especial: sube un nivel entero la primera vez (el de nivel 4, hasta Maestro)
            int xp = maxXp(l);
            ItemStack result = new ItemStack(GitanoLand.KIRKY.get(), Math.min(STACK, reward(v, s)));
            offers.add(new MerchantOffer(new ItemCost(s.item(), s.count()), Optional.empty(), result, 0, 3, xp, 0f, 0));
            idx[l - 1] = offers.size() - 1;
        }
        if (cur > done) t.putInt(K_LEVEL, cur);
        t.putIntArray(K_IDX, idx);
        // Los especiales no se reabastecen: usos permitidos = 2 (3 si le caes muy bien) menos los gastados.
        int[] used = usedArr(t);
        boolean liked = v.getPlayerReputation(player) >= 20;
        for (int l = 1; l <= LEVELS; l++) {
            int i = idx[l - 1];
            if (i < 0 || i >= offers.size()) continue;
            MerchantOffer o = offers.get(i);
            int remaining = Math.max(0, (liked ? 3 : 2) - used[l - 1]);
            o.resetUses();
            for (int k = 0; k < 3 - remaining; k++) o.increaseUses();
        }
    }

    /** Experiencia de aldeano que hace falta para pasar del nivel l al siguiente. */
    public static int maxXp(int level) {
        return net.minecraft.world.entity.npc.VillagerData.getMaxXpPerLevel(level);
    }

    private static MerchantOffer regularKirky(MerchantOffers offers, RandomSource r) {
        List<MerchantOffer> pool = new ArrayList<>();
        for (MerchantOffer o : offers) {
            if (o.getCostA().is(GitanoLand.KIRKY.get()) || o.getResult().is(GitanoLand.KIRKY.get())) continue;
            pool.add(o);
        }
        if (pool.isEmpty()) return null;
        MerchantOffer base = pool.get(r.nextInt(pool.size()));
        int kirkys = 2 + r.nextInt(4); // 2 a 5
        if (base.getResult().is(Items.EMERALD)) {
            // el aldeano compra el objeto y paga en Kirkys en vez de esmeraldas
            Optional<ItemCost> b = base.getItemCostB();
            return new MerchantOffer(base.getItemCostA(), b, new ItemStack(GitanoLand.KIRKY.get(), kirkys), 0,
                    base.getMaxUses(), base.getXp(), base.getPriceMultiplier(), 0);
        }
        // el aldeano vende el objeto y cobra en Kirkys
        return new MerchantOffer(new ItemCost(GitanoLand.KIRKY.get(), kirkys), Optional.empty(), base.getResult().copy(), 0,
                base.getMaxUses(), base.getXp(), base.getPriceMultiplier(), 0);
    }

    /** Nivel (1..5) del trato especial al que corresponde esta oferta, o -1. */
    public static int levelOf(Villager v, MerchantOffer o) {
        int[] idx = specialIdx(v.getPersistentData());
        MerchantOffers offers = v.getOffers();
        for (int l = 1; l <= LEVELS; l++) {
            int i = idx[l - 1];
            if (i >= 0 && i < offers.size() && offers.get(i) == o) return l;
        }
        return -1;
    }

    /** Suma un uso al trato especial del nivel y devuelve cuántos lleva. */
    public static int addUse(Villager v, int level) {
        CompoundTag t = v.getPersistentData();
        int[] used = usedArr(t);
        used[level - 1]++;
        t.putIntArray(K_USED, used);
        return used[level - 1];
    }

    private static int[] specialIdx(CompoundTag t) {
        int[] a = t.getIntArray(K_IDX);
        if (a.length == LEVELS) return a;
        int[] n = new int[LEVELS];
        Arrays.fill(n, -1);
        System.arraycopy(a, 0, n, 0, Math.min(a.length, LEVELS));
        return n;
    }

    private static int[] usedArr(CompoundTag t) {
        int[] a = t.getIntArray(K_USED);
        return a.length == LEVELS ? a : new int[LEVELS];
    }

    // ------------------------------------------------------------------ carga

    private static synchronized Map<ResourceLocation, List<Special>> table() {
        if (cache == null) cache = load();
        return cache;
    }

    private static Map<ResourceLocation, List<Special>> load() {
        Map<ResourceLocation, List<Special>> out = new HashMap<>();
        try (Reader r = open()) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            JsonObject ms = root.getAsJsonObject("missions");
            for (Map.Entry<String, JsonElement> e : ms.entrySet()) {
                List<Special> list = new ArrayList<>();
                for (JsonElement me : e.getValue().getAsJsonObject().getAsJsonArray("levels")) {
                    JsonObject o = me.getAsJsonObject();
                    ResourceLocation id = ResourceLocation.parse(o.get("item").getAsString());
                    Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
                    if (item == null) {
                        GitanoLand.LOGGER.warn("[GitanoLand] Trato especial con item desconocido: {}", id);
                        list.clear();
                        break;
                    }
                    int reward = Math.max(1, o.get("reward").getAsInt());
                    int max = o.has("rewardMax") ? o.get("rewardMax").getAsInt() : reward;
                    list.add(new Special(item, Math.max(1, o.get("count").getAsInt()), reward, max));
                }
                out.put(ResourceLocation.parse(e.getKey()), list);
            }
        } catch (Exception ex) {
            GitanoLand.LOGGER.error("[GitanoLand] No se pudo leer missions.json: {}", ex.toString());
        }
        return out;
    }

    private static Reader open() throws Exception {
        Path cfg = FMLPaths.CONFIGDIR.get().resolve("gitanoland").resolve("missions.json");
        if (Files.exists(cfg)) return Files.newBufferedReader(cfg, StandardCharsets.UTF_8);
        InputStream in = Missions.class.getResourceAsStream("/data/gitanoland/missions.json");
        if (in == null) throw new java.io.IOException("falta missions.json en el mod");
        return new InputStreamReader(in, StandardCharsets.UTF_8);
    }
}
