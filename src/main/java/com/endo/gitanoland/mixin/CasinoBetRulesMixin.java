package com.endo.gitanoland.mixin;

import com.endo.gitanoland.compat.CasinoAccess;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** MineVegas: el servidor tambien comprueba "creativo" al guardar la lista de apuestas; mismo permiso que la Mano. */
@Pseudo
@Mixin(targets = "fr.minevegas.casino.CasinoBetRules", remap = false)
public abstract class CasinoBetRulesMixin {
    @Redirect(method = "edit", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isCreative()Z", remap = false), require = 0, remap = false)
    private static boolean gitanoland$operator(ServerPlayer p) {
        return CasinoAccess.isAdmin(p);
    }
}
