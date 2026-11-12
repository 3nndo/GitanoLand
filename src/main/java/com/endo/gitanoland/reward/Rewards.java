package com.endo.gitanoland.reward;

import com.endo.gitanoland.Config;
import com.endo.gitanoland.GitanoLand;
import java.time.LocalDate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/** Kirkys pendientes de logros (se reclaman en el menú) y recompensa diaria (una vez por día real). */
public final class Rewards {
    public static final String PENDING = "gitanoland_pending";
    public static final String DAILY = "gitanoland_daily_day";

    private Rewards() {}

    public static int pending(Player p) {
        return p.getPersistentData().getInt(PENDING);
    }

    public static void addPending(Player p, int n) {
        p.getPersistentData().putInt(PENDING, pending(p) + n);
    }

    /** La recompensa diaria solo existe en servidores dedicados (ni un solo jugador ni LAN). */
    public static boolean dailySupported(Player p) {
        return p.getServer() != null && p.getServer().isDedicatedServer();
    }

    public static boolean dailyAvailable(Player p) {
        CompoundTag t = p.getPersistentData();
        return !t.contains(DAILY) || t.getLong(DAILY) < LocalDate.now().toEpochDay();
    }

    public static void claimPending(ServerPlayer p) {
        int n = pending(p);
        if (n <= 0) return;
        p.getPersistentData().putInt(PENDING, 0);
        give(p, n);
    }

    public static void claimDaily(ServerPlayer p) {
        if (!dailySupported(p) || !dailyAvailable(p)) return;
        p.getPersistentData().putLong(DAILY, LocalDate.now().toEpochDay());
        give(p, Config.DAILY_KIRKYS.get());
    }

    private static void give(ServerPlayer p, int n) {
        int left = n;
        while (left > 0) {
            ItemStack s = new ItemStack(GitanoLand.KIRKY.get(), Math.min(64, left));
            left -= s.getCount();
            if (!p.getInventory().add(s)) p.drop(s, false);
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.2f);
        p.displayClientMessage(Component.translatable("gitanoland.rewards.claimed", n), true);
    }

    public static MenuType<RewardsMenu> type() {
        return com.endo.gitanoland.shop.ModMenus.REWARDS.get();
    }
}
