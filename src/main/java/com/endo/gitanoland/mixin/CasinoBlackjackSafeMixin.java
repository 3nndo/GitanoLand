package com.endo.gitanoland.mixin;

import com.endo.gitanoland.safe.SafeEconomy;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Mesa de blackjack: solo empieza la mano si el almacen enlazado cubre el premio; los pagos del tick salen del almacen. */
@Pseudo
@Mixin(targets = "fr.minevegas.casino.BlackjackTableBlockEntity", remap = false)
public abstract class CasinoBlackjackSafeMixin {
    @Shadow(remap = false)
    private java.util.List<ItemStack> displayStakes() {
        throw new AssertionError();
    }

    @Inject(method = "beginRound", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void gitanoland$gate(ServerPlayer player, @org.spongepowered.asm.mixin.injection.Coerce Object menu, CallbackInfoReturnable<Boolean> cir) {
        if (!SafeEconomy.gate((BlockEntity) (Object) this, player, displayStakes())) cir.setReturnValue(false);
    }

    @Inject(method = "serverTick", at = @At("HEAD"), require = 0, remap = false)
    private void gitanoland$tick(ServerLevel level, CallbackInfo ci) {
        SafeEconomy.begin((BlockEntity) (Object) this);
    }

    @Inject(method = "menuButton", at = @At("HEAD"), require = 0, remap = false)
    private void gitanoland$button(ServerPlayer player, @org.spongepowered.asm.mixin.injection.Coerce Object menu, int id, CallbackInfoReturnable<Boolean> cir) {
        SafeEconomy.begin((BlockEntity) (Object) this);
    }
}
