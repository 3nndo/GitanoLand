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
    /** Dueno de cada maquina del casino (clave de maquina): quien la reclama primero con la Mano del Crupier o la Llave. */
    public record Owner(String uuid, String name) {}
    private final Map<String, Owner> owners = new HashMap<>();

    /** Dueno de la maquina: el que la reclamo, o el dueno de su cofre enlazado. null = sin dueno todavia. */
    @Nullable
    public Owner ownerOf(String key) {
        Owner o = owners.get(key);
        if (o != null) return o;
        Link l = links.get(key);
        return l == null ? null : new Owner(l.ownerUuid(), l.ownerName());
    }

    public void claim(String key, Owner owner) {
        owners.put(key, owner);
        setDirty();
    }

    public void release(String key) {
        boolean a = owners.remove(key) != null;
        boolean b = links.remove(key) != null;
        if (a || b) setDirty();
    }


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
        ListTag os = tag.getList("owners", Tag.TAG_COMPOUND);
        for (int i = 0; i < os.size(); i++) {
            CompoundTag t = os.getCompound(i);
            s.owners.put(t.getString("key"), new Owner(t.getString("uuid"), t.getString("name")));
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
        ListTag os = new ListTag();
        for (Map.Entry<String, Owner> e : owners.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putString("key", e.getKey());
            t.putString("uuid", e.getValue().uuid());
            t.putString("name", e.getValue().name());
            os.add(t);
        }
        tag.put("owners", os);
        return tag;
    }
}
