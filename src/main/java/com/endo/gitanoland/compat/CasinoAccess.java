package com.endo.gitanoland.compat;

import com.endo.gitanoland.Config;
import java.util.Locale;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.endo.gitanoland.GitanoLand;

/** Reglas de GitanoLand sobre MineVegas: quien puede usar la Mano del Crupier y que se puede apostar. Las usan los mixins. */
public final class CasinoAccess {
    private CasinoAccess() {}

    /** Creativo, OP (nivel 2) o jugador de la lista casinoOperators del config del servidor. */
    public static boolean canOperate(Player p) {
        if (p == null) return false;
        if (p.isCreative()) return true;
        // En el cliente no se conoce la lista del servidor: deja pasar y que decida el servidor (el unico que guarda cambios).
        if (p.level().isClientSide) return true;
        try {
            if (p.hasPermissions(2)) return true;
            String name = p.getGameProfile().getName().toLowerCase(Locale.ROOT);
            for (Object o : Config.CASINO_OPERATORS.get()) {
                if (o != null && o.toString().trim().toLowerCase(Locale.ROOT).equals(name)) return true;
            }
        } catch (Throwable ignored) {
            // config todavia sin cargar: solo creativo
        }
        return false;
    }

    /** true = las maquinas solo aceptan Kirkys como apuesta, aunque el operador intente anadir otros objetos. */
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
