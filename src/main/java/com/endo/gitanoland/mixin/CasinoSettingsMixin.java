package com.endo.gitanoland.mixin;

import com.endo.gitanoland.compat.CasinoAccess;
import fr.minevegas.casino.CroupierSettings;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MineVegas: (1) quien puede editar las maquinas (creativo, OP o casinoOperators);
 * (2) con casinoKirkyOnly solo se aceptan Kirkys como apuesta en TODAS las maquinas, sean cuales sean sus ajustes.
 */
@Pseudo
@Mixin(targets = "fr.minevegas.casino.CasinoSettings", remap = false)
public abstract class CasinoSettingsMixin {
    @Inject(method = "allowed", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void gitanoland$allowed(Player p, CallbackInfoReturnable<Boolean> cir) {
        if (CasinoAccess.canOperate(p)) cir.setReturnValue(true);
    }

    @Inject(method = "accepts(Lfr/minevegas/casino/CroupierSettings;Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void gitanoland$acceptsSettings(CroupierSettings s, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (CasinoAccess.kirkyOnly()) cir.setReturnValue(CasinoAccess.isKirky(stack));
    }

    @Inject(method = "accepts(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void gitanoland$acceptsBlock(BlockEntity be, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (CasinoAccess.kirkyOnly()) cir.setReturnValue(CasinoAccess.isKirky(stack));
    }
}
