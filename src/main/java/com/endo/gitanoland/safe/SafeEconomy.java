package com.endo.gitanoland.safe;

import com.endo.gitanoland.Config;
import com.endo.gitanoland.GitanoLand;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Economia con almacen: las maquinas del casino enlazadas a un cofre de SecurityCraft guardan las apuestas en el cofre y pagan
 * los premios en Kirkys desde el; si el cofre no cubre el premio maximo estimado, la maquina no arranca. Lo usan los mixins.
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
    public static WarehouseLinks.Link linkOf(BlockEntity be) {
        Level level = be.getLevel();
        if (level == null || level.getServer() == null) return null;
        return WarehouseLinks.get(level.getServer()).linkOf(WarehouseLinks.key(level, be.getBlockPos()));
    }

    @Nullable
    private static Container chestOf(net.minecraft.server.MinecraftServer server, @Nullable WarehouseLinks.Link link) {
        return link == null ? null : Warehouse.container(server, link.chest());
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
        return gate(be, player, kirkysIn(bets));
    }

    /** Igual, con las apuestas dadas como lista de objetos (mesas de poker/blackjack). Minimo 1 Kirky de apuesta. */
    public static boolean gate(BlockEntity be, ServerPlayer player, List<ItemStack> stakes) {
        long n = 0;
        for (ItemStack s : stakes) if (!s.isEmpty() && s.is(GitanoLand.KIRKY.get())) n += s.getCount();
        return gate(be, player, Math.max(1, n));
    }

    public static boolean gate(BlockEntity be, ServerPlayer player, long stakeKirkys) {
        if (!required()) return true;
        WarehouseLinks.Link link = linkOf(be);
        if (link == null) {
            player.displayClientMessage(Component.translatable("gitanoland.safe.no_safe"), true);
            return false;
        }
        Container chest = chestOf(player.getServer(), link);
        int factor;
        try {
            factor = Config.CASINO_SAFE_RESERVE_FACTOR.get();
        } catch (Throwable t) {
            factor = 50;
        }
        if (chest == null || Warehouse.balance(chest) < stakeKirkys * factor) {
            player.displayClientMessage(Component.translatable("gitanoland.safe.no_funds", link.ownerName()), true);
            return false;
        }
        begin(be);
        return true;
    }

    public static void begin(BlockEntity be) {
        ctxMachine = be;
        ctxTick = be.getLevel() == null ? Long.MIN_VALUE : be.getLevel().getGameTime();
    }

    private static void spill(Level level, net.minecraft.core.BlockPos near, long kirkys) {
        long left = kirkys;
        while (left > 0) {
            int n = (int) Math.min(64, left);
            level.addFreshEntity(new ItemEntity(level, near.getX() + 0.5, near.getY() + 1.0, near.getZ() + 0.5, new ItemStack(GitanoLand.KIRKY.get(), n)));
            left -= n;
        }
    }

    /**
     * Se llama con cada premio calculado de la maquina que acaba de arrancar. Mete la apuesta en Kirkys en el cofre y saca de el
     * los Kirkys del premio (si no llega, el premio se recorta a lo que haya). Otros objetos del premio no se tocan.
     */
    public static List<ItemStack> settle(ItemStack stake, List<ItemStack> payout) {
        BlockEntity be = ctxMachine;
        if (be == null || be.getLevel() == null || be.getLevel().getGameTime() != ctxTick) return payout;
        if (stake.isEmpty() || !stake.is(GitanoLand.KIRKY.get())) return payout;
        Container chest = chestOf(be.getLevel().getServer(), linkOf(be));
        if (chest == null) return payout;
        long notFit = Warehouse.deposit(chest, stake.getCount());
        if (notFit > 0) spill(be.getLevel(), be.getBlockPos(), notFit); // cofre lleno: lo que no cabe cae al suelo, no se pierde
        long want = 0;
        for (ItemStack s : payout) if (!s.isEmpty() && s.is(GitanoLand.KIRKY.get())) want += s.getCount();
        long paid = Warehouse.withdraw(chest, want);
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

    /** Los Kirkys que un jugador paga en una tienda con NPC entran en el cofre enlazado a ese NPC (si lo tiene). */
    public static void income(Entity npc, int kirkys) {
        if (kirkys <= 0 || npc == null || npc.level().getServer() == null) return;
        WarehouseLinks.Link link = WarehouseLinks.get(npc.level().getServer()).linkOf(WarehouseLinks.entityKey(npc.getUUID()));
        Container chest = chestOf(npc.level().getServer(), link);
        if (chest == null) return;
        long notFit = Warehouse.deposit(chest, kirkys);
        if (notFit > 0) spill(npc.level(), npc.blockPosition(), notFit);
    }
}
