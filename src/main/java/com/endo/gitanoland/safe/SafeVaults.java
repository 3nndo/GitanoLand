package com.endo.gitanoland.safe;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Almacen de Kirkys de las Cajas fuertes. El saldo vive aqui (datos del mundo) y no en el bloque, asi las maquinas y tiendas
 * enlazadas pagan aunque la caja este en un chunk sin cargar. Cada caja tiene un id; el bloque y su objeto llevan ese id.
 */
public class SafeVaults extends SavedData {
    private static final String NAME = "gitanoland_safes";

    public static class Vault {
        public final UUID id;
        public final UUID owner;
        public String ownerName;
        public long balance;

        Vault(UUID id, UUID owner, String ownerName, long balance) {
            this.id = id;
            this.owner = owner;
            this.ownerName = ownerName;
            this.balance = balance;
        }
    }

    private final Map<UUID, Vault> vaults = new HashMap<>();
    /** clave "dimension|posLong" de una maquina o tienda -> id de la caja a la que esta enlazada. */
    private final Map<String, UUID> links = new HashMap<>();

    public static SafeVaults get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SafeVaults::new, SafeVaults::load, null), NAME);
    }

    public static String key(Level level, BlockPos pos) {
        return level.dimension().location() + "|" + pos.asLong();
    }

    public static String entityKey(UUID entity) {
        return "e|" + entity;
    }

    public Vault create(UUID owner, String ownerName) {
        Vault v = new Vault(UUID.randomUUID(), owner, ownerName, 0);
        vaults.put(v.id, v);
        setDirty();
        return v;
    }

    @Nullable
    public Vault vault(@Nullable UUID id) {
        return id == null ? null : vaults.get(id);
    }

    @Nullable
    public Vault vaultOfLink(String key) {
        return vault(links.get(key));
    }

    @Nullable
    public UUID linkId(String key) {
        return links.get(key);
    }

    public void link(String key, UUID vault) {
        links.put(key, vault);
        setDirty();
    }

    public void unlink(String key) {
        if (links.remove(key) != null) setDirty();
    }

    public void dirty() {
        setDirty();
    }

    // ---- guardado ----
    public static SafeVaults load(CompoundTag tag, HolderLookup.Provider provider) {
        SafeVaults s = new SafeVaults();
        ListTag vs = tag.getList("vaults", Tag.TAG_COMPOUND);
        for (int i = 0; i < vs.size(); i++) {
            CompoundTag t = vs.getCompound(i);
            Vault v = new Vault(t.getUUID("id"), t.getUUID("owner"), t.getString("ownerName"), t.getLong("balance"));
            s.vaults.put(v.id, v);
        }
        ListTag ls = tag.getList("links", Tag.TAG_COMPOUND);
        for (int i = 0; i < ls.size(); i++) {
            CompoundTag t = ls.getCompound(i);
            s.links.put(t.getString("key"), t.getUUID("vault"));
        }
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag vs = new ListTag();
        for (Vault v : vaults.values()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("id", v.id);
            t.putUUID("owner", v.owner);
            t.putString("ownerName", v.ownerName);
            t.putLong("balance", v.balance);
            vs.add(t);
        }
        tag.put("vaults", vs);
        ListTag ls = new ListTag();
        for (Map.Entry<String, UUID> e : links.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putString("key", e.getKey());
            t.putUUID("vault", e.getValue());
            ls.add(t);
        }
        tag.put("links", ls);
        return tag;
    }
}
