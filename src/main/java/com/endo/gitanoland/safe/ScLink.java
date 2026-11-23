package com.endo.gitanoland.safe;

import net.geforcemods.securitycraft.api.IOwnable;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Unico sitio que toca clases de SecurityCraft (solo se usa si SecurityCraft esta instalado). */
public final class ScLink {
    private ScLink() {}

    /** Cofre o barril con dueno de SecurityCraft (los "protegidos con codigo"). */
    public static boolean isWarehouse(BlockEntity be) {
        return be instanceof IOwnable && be instanceof Container;
    }

    public static boolean ownedBy(BlockEntity be, Player p) {
        return be instanceof IOwnable o && (o.isOwnedBy(p) || p.hasPermissions(2));
    }

    public static String ownerName(BlockEntity be) {
        return be instanceof IOwnable o ? o.getOwner().getName() : "?";
    }

    public static String ownerUuid(BlockEntity be) {
        return be instanceof IOwnable o ? o.getOwner().getUUID() : "";
    }
}
