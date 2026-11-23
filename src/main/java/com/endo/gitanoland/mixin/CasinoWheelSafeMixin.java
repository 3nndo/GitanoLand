package com.endo.gitanoland.mixin;

import com.endo.gitanoland.safe.SafeEconomy;
import fr.minevegas.casino.FortuneWheelBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Rueda de la fortuna: solo arranca si su Caja fuerte enlazada puede cubrir el premio (ver SafeEconomy). */
@Pseudo
@Mixin(targets = "fr.minevegas.casino.FortuneWheelBlockEntity", remap = false)
public abstract class CasinoWheelSafeMixin {
    @Inject(method = "start(Lnet/minecraft/server/level/ServerPlayer;I)V", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void gitanoland$gate(ServerPlayer player, int n, CallbackInfo ci) {
        FortuneWheelBlockEntity self = (FortuneWheelBlockEntity) (Object) this;
        if (!SafeEconomy.gate(self, player, self.bets())) ci.cancel();
    }
}
