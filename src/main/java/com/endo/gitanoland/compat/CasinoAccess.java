package com.endo.gitanoland.compat;

import com.endo.gitanoland.Config;
import com.endo.gitanoland.GitanoLand;
import com.endo.gitanoland.safe.WarehouseLinks;
import fr.minevegas.casino.CroupierHandItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Reglas de GitanoLand sobre MineVegas. Cualquiera puede fabricar la Mano del Crupier y abrir su propio casino:
 * edita solo las maquinas de las que es dueno (el primero que usa la Mano o la Llave en una maquina sin dueno la reclama).
 * Los ajustes globales (de todo el mundo o de todo un tipo de maquina) son solo de creativo/OP.
 */
public final class CasinoAccess {
    private CasinoAccess() {}

    /** Creativo u OP (nivel 2). En el cliente se deja pasar: el servidor es quien decide y guarda. */
    public static boolean isAdmin(Player p) {
        if (p == null) return false;
        if (p.isCreative()) return true;
        if (p.level().isClientSide) return true;
        return p.hasPermissions(2);
    }

    /** Lleva la Mano del Crupier en alguna mano. */
    public static boolean holdsHand(Player p) {
        return p != null && (p.getMainHandItem().is(CroupierHandItem.ITEM) || p.getOffhandItem().is(CroupierHandItem.ITEM));
    }

    /** true = puede tocar esa maquina. Sin dueno: la reclama. De otro: avisa y devuelve false. Admin: siempre. */
    public static boolean mayEdit(ServerPlayer p, BlockEntity machine) {
        if (isAdmin(p)) return true;
        Level level = machine.getLevel();
        if (level == null || level.getServer() == null) return false;
        WarehouseLinks links = WarehouseLinks.get(level.getServer());
        String key = WarehouseLinks.key(level, machine.getBlockPos());
        WarehouseLinks.Owner o = links.ownerOf(key);
        String me = p.getUUID().toString();
        if (o == null) {
            links.claim(key, new WarehouseLinks.Owner(me, p.getGameProfile().getName()));
            p.displayClientMessage(Component.translatable("gitanoland.casino.claimed"), true);
            return true;
        }
        if (o.uuid().equals(me)) return true;
        p.displayClientMessage(Component.translatable("gitanoland.casino.not_owner", o.name()), true);
        return false;
    }

    /** true = las maquinas solo aceptan Kirkys como apuesta, aunque el dueno intente anadir otros objetos. */
    public static boolean kirkyOnly() {
        try {
            return Config.CASINO_KIRKY_ONLY.get();
        } catch (Throwable t) {
            return true;
        }
    }

    public static boolean isKirky(ItemStack s) {
        return s.is(GitanoLand.KIRKY.get());
    }
}
