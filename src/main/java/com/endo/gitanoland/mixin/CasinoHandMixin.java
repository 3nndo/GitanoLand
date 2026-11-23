package com.endo.gitanoland.mixin;

import com.endo.gitanoland.compat.CasinoAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** MineVegas: la Mano del Crupier solo pedia modo creativo; y aqui solo se deja a creativo u OP (los ajustes globales); las maquinas propias se editan con CasinoSettingsMixin. */
@Pseudo
@Mixin(targets = "fr.minevegas.casino.CroupierHandItem", remap = false)
public abstract class CasinoHandMixin {
    @Redirect(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isCreative()Z", remap = false), require = 0, remap = false)
    private static boolean gitanoland$operator(Player p) {
        return CasinoAccess.isAdmin(p);
    }
}
