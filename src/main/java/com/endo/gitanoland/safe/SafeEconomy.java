package com.endo.gitanoland.safe;

import com.endo.gitanoland.Config;
import com.endo.gitanoland.GitanoLand;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Economia con almacen: las maquinas del casino enlazadas a una Caja fuerte guardan las apuestas en ella y pagan los premios
 * en Kirkys desde ella; si la caja no cubre el premio maximo estimado, la maquina no arranca. Lo usan los mixins de MineVegas.
 */
public final class SafeEconomy {
    private SafeEconomy() {}

    // Maquina que acaba de arrancar una jugada (el pago se calcula en ese mismo tick, justo despues).
    private static BlockEntity ctxMachine;
    private static long ctxTick = Long.MIN_VALUE;

    public static boolean required() {
        try {
            return Config.CASINO_REQUIRE_SAFE.get();
        } catch (Throwable t) {
            return false;
        }
    }

    @Nullable
    public static SafeVaults.Vault vaultOf(BlockEntity be) {
        Level level = be.getLevel();
        if (level == null || level.getServer() == null) return null;
        return SafeVaults.get(level.getServer()).vaultOfLink(SafeVaults.key(level, be.getBlockPos()));
    }

    private static long kirkysIn(SimpleContainer c) {
        long n = 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack s = c.getItem(i);
            if (!s.isEmpty() && s.is(GitanoLand.KIRKY.get())) n += s.getCount();
        }
        return n;
    }

    /** true = la jugada puede empezar. Si no, avisa al jugador y la maquina no arranca (las apuestas se quedan donde estan). */
    public static boolean gate(BlockEntity be, ServerPlayer player, SimpleContainer bets) {
        if (!required()) return true;
        SafeVaults.Vault v = vaultOf(be);
        if (v == null) {
            player.displayClientMessage(Component.translatable("gitanoland.safe.no_safe"), true);
            return false;
        }
        int factor;
        try {
            factor = Config.CASINO_SAFE_RESERVE_FACTOR.get();
        } catch (Throwable t) {
            factor = 50;
        }
        long stake = kirkysIn(bets);
        if (v.balance < stake * factor) {
            player.displayClientMessage(Component.translatable("gitanoland.safe.no_funds"), true);
            return false;
        }
        begin(be);
        return true;
    }

    /** Los Kirkys que un jugador paga en una tienda con NPC entran en la Caja fuerte enlazada a ese NPC (si la tiene). */
    public static void income(net.minecraft.world.entity.Entity npc, int kirkys) {
        if (kirkys <= 0 || npc == null || npc.level().getServer() == null) return;
        SafeVaults vaults = SafeVaults.get(npc.level().getServer());
        SafeVaults.Vault v = vaults.vaultOfLink(SafeVaults.entityKey(npc.getUUID()));
        if (v == null) return;
        v.balance += kirkys;
        vaults.dirty();
    }

    public static void begin(BlockEntity be) {
        ctxMachine = be;
        ctxTick = be.getLevel() == null ? Long.MIN_VALUE : be.getLevel().getGameTime();
    }

    /**
     * Se llama con cada premio calculado de la maquina que acaba de arrancar. Mete la apuesta en Kirkys en la caja y saca de ella
     * los Kirkys del premio (si la caja no llega, el premio se recorta a lo que haya). Otros objetos del premio no se tocan.
     */
    public static List<ItemStack> settle(ItemStack stake, List<ItemStack> payout) {
        BlockEntity be = ctxMachine;
        if (be == null || be.getLevel() == null || be.getLevel().getGameTime() != ctxTick) return payout;
        if (stake.isEmpty() || !stake.is(GitanoLand.KIRKY.get())) return payout;
        SafeVaults.Vault v = vaultOf(be);
        if (v == null) return payout;
        v.balance += stake.getCount();
        long want = 0;
        for (ItemStack s : payout) if (!s.isEmpty() && s.is(GitanoLand.KIRKY.get())) want += s.getCount();
        long paid = Math.min(want, v.balance);
        v.balance -= paid;
        SafeVaults.get(be.getLevel().getServer()).dirty();
        if (paid == want) return payout;
        List<ItemStack> out = new ArrayList<>();
        long left = paid;
        for (ItemStack s : payout) {
            if (s.isEmpty() || !s.is(GitanoLand.KIRKY.get())) {
                out.add(s);
            } else {
                int give = (int) Math.min(left, s.getCount());
                left -= give;
                if (give > 0) out.add(s.copyWithCount(give));
            }
        }
        return out;
    }
}
