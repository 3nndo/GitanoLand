package com.endo.gitanoland.mission;

import com.endo.gitanoland.Config;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Misiones de los aldeanos normales: cada uno (de las profesiones que lo permiten) te pide una vez unos objetos
 * difíciles de conseguir y te paga en Kirkys. Las profesiones de trueque fácil (palos, trigo...) no aparecen en
 * el JSON y por tanto no dan Kirkys. Config: config/gitanoland/missions.json (si no existe, el del mod).
 */
public final class Missions {
    private static final String DONE = "gitanoland_mission_done";
    private static Map<ResourceLocation, List<Mission>> cache;

    private Missions() {}

    public record Mission(Item item, int count, int reward) {}

    /** La misión de este aldeano, o null si su profesión no da (o ya la completó, según "includeDone"). */
    public static Mission of(Villager v) {
        if (v.isBaby()) return null;
        ResourceLocation prof = BuiltInRegistries.VILLAGER_PROFESSION.getKey(v.getVillagerData().getProfession());
        List<Mission> list = table().get(prof);
        if (list == null || list.isEmpty()) return null;
        long h = v.getUUID().getMostSignificantBits() ^ v.getUUID().getLeastSignificantBits();
        return list.get((int) Math.floorMod(h, (long) list.size()));
    }

    public static boolean isDone(Villager v) {
        return v.getPersistentData().getBoolean(DONE);
    }

    public static void markDone(Villager v) {
        CompoundTag t = v.getPersistentData();
        t.putBoolean(DONE, true);
    }

    /** Kirkys que paga la misión a este jugador: tope normal, o el de Héroe del Pueblo escalado en proporción. */
    public static int reward(Mission m, Player p) {
        int max = Config.MISSION_MAX_KIRKYS.get();
        int base = Math.min(max, Math.max(1, m.reward()));
        if (!p.hasEffect(MobEffects.HERO_OF_THE_VILLAGE)) return base;
        int heroMax = Math.max(max, Config.MISSION_MAX_KIRKYS_HERO.get());
        return Math.min(heroMax, (int) Math.round(base * (heroMax / (double) max)));
    }

    // ------------------------------------------------------------------ carga

    private static synchronized Map<ResourceLocation, List<Mission>> table() {
        if (cache == null) cache = load();
        return cache;
    }

    private static Map<ResourceLocation, List<Mission>> load() {
        Map<ResourceLocation, List<Mission>> out = new HashMap<>();
        try (Reader r = open()) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            JsonObject ms = root.getAsJsonObject("missions");
            for (Map.Entry<String, JsonElement> e : ms.entrySet()) {
                List<Mission> list = new ArrayList<>();
                for (JsonElement me : e.getValue().getAsJsonArray()) {
                    JsonObject o = me.getAsJsonObject();
                    ResourceLocation id = ResourceLocation.parse(o.get("item").getAsString());
                    Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
                    if (item == null) {
                        GitanoLand.LOGGER.warn("[GitanoLand] Misión con item desconocido: {}", id);
                        continue;
                    }
                    list.add(new Mission(item, Math.max(1, o.get("count").getAsInt()), o.get("reward").getAsInt()));
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
