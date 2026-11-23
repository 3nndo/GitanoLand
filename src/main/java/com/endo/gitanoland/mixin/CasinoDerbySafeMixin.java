package com.endo.gitanoland.mixin;

import com.endo.gitanoland.safe.SafeEconomy;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Duck Derby: las carreras solo se aceptan si el almacen enlazado cubre el premio; los pagos salen del almacen. */
@Pseudo
@Mixin(targets = "fr.minevegas.casino.DuckDerbyBlockEntity", remap = false)
public abstract class CasinoDerbySafeMixin {
    @Shadow(remap = false)
    public abstract SimpleContainer bets();

    private static SimpleContainer gitanoland$menuBets(Object menu) {
        try {
            return (SimpleContainer) menu.getClass().getMethod("bets").invoke(menu);
        } catch (Throwable t) {
            return new SimpleContainer(0);
        }
    }

    @Inject(method = "startSolo", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void gitanoland$solo(ServerPlayer player, @Coerce Object menu, int lane, CallbackInfoReturnable<Boolean> cir) {
        if (!SafeEconomy.gate((BlockEntity) (Object) this, player, gitanoland$menuBets(menu))) cir.setReturnValue(false);
    }

    @Inject(method = "joinGroup", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void gitanoland$group(ServerPlayer player, @Coerce Object menu, CallbackInfoReturnable<Boolean> cir) {
        if (!SafeEconomy.gate((BlockEntity) (Object) this, player, gitanoland$menuBets(menu))) cir.setReturnValue(false);
    }

    @Inject(method = "start(Lnet/minecraft/server/level/ServerPlayer;I)V", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void gitanoland$legacy(ServerPlayer player, int lane, CallbackInfo ci) {
        if (!SafeEconomy.gate((BlockEntity) (Object) this, player, bets())) ci.cancel();
    }

    @Inject(method = "launchPersonal", at = @At("HEAD"), require = 0, remap = false)
    private void gitanoland$launch(ServerLevel level, int n, CallbackInfo ci) {
        SafeEconomy.begin((BlockEntity) (Object) this);
    }

    @Inject(method = "startLegacy", at = @At("HEAD"), require = 0, remap = false)
    private void gitanoland$startLegacy(ServerPlayer player, int n, CallbackInfo ci) {
        SafeEconomy.begin((BlockEntity) (Object) this);
    }

    @Inject(method = "personalTick", at = @At("HEAD"), require = 0, remap = false)
    private void gitanoland$ptick(ServerLevel level, CallbackInfo ci) {
        SafeEconomy.begin((BlockEntity) (Object) this);
    }
}
