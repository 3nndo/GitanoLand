package com.endo.gitanoland.mixin;

import com.endo.gitanoland.compat.CasinoAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** MineVegas: la Mano del Crupier solo pedia modo creativo; ahora vale tambien para OP y para los casinoOperators del config. */
@Pseudo
@Mixin(targets = "fr.minevegas.casino.CroupierHandItem", remap = false)
public abstract class CasinoHandMixin {
    @Redirect(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isCreative()Z", remap = false), require = 0, remap = false)
    private static boolean gitanoland$operator(Player p) {
        return CasinoAccess.canOperate(p);
    }
}
