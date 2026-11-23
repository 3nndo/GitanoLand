package com.endo.gitanoland.safe;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/** Que maquina o aldeano esta enlazado a que cofre-almacen. Clave de maquina: "dimension|posLong"; de aldeano: "e|uuid". */
public class WarehouseLinks extends SavedData {
    private static final String NAME = "gitanoland_warehouses";

    /** Cofre enlazado y su dueno (para avisos y para impedir que otro jugador se lo quite). */
    public record Link(GlobalPos chest, String ownerUuid, String ownerName) {}

    private final Map<String, Link> links = new HashMap<>();

    public static WarehouseLinks get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(WarehouseLinks::new, WarehouseLinks::load, null), NAME);
    }

    public static String key(Level level, BlockPos pos) {
        return level.dimension().location() + "|" + pos.asLong();
    }

    public static String entityKey(java.util.UUID entity) {
        return "e|" + entity;
    }

    @Nullable
    public Link linkOf(String key) {
        return links.get(key);
    }

    public void link(String key, Link link) {
        links.put(key, link);
        setDirty();
    }

    public void unlink(String key) {
        if (links.remove(key) != null) setDirty();
    }

    public static WarehouseLinks load(CompoundTag tag, HolderLookup.Provider provider) {
        WarehouseLinks s = new WarehouseLinks();
        ListTag ls = tag.getList("links", Tag.TAG_COMPOUND);
        for (int i = 0; i < ls.size(); i++) {
            CompoundTag t = ls.getCompound(i);
            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(t.getString("dim")));
            s.links.put(t.getString("key"), new Link(GlobalPos.of(dim, BlockPos.of(t.getLong("pos"))), t.getString("ownerUuid"), t.getString("ownerName")));
        }
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag ls = new ListTag();
        for (Map.Entry<String, Link> e : links.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putString("key", e.getKey());
            t.putString("dim", e.getValue().chest().dimension().location().toString());
            t.putLong("pos", e.getValue().chest().pos().asLong());
            t.putString("ownerUuid", e.getValue().ownerUuid());
            t.putString("ownerName", e.getValue().ownerName());
            ls.add(t);
        }
        tag.put("links", ls);
        return tag;
    }
}
