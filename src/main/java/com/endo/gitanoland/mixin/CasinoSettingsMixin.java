package com.endo.gitanoland.mixin;

import com.endo.gitanoland.compat.CasinoAccess;
import fr.minevegas.casino.CasinoMachine;
import fr.minevegas.casino.CasinoSettings;
import fr.minevegas.casino.CroupierSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MineVegas: (1) cualquiera con la Mano del Crupier puede editar SUS maquinas (el dueno); lo global solo creativo/OP;
 * (2) con casinoKirkyOnly solo se aceptan Kirkys como apuesta en TODAS las maquinas, sean cuales sean sus ajustes.
 */
@Pseudo
@Mixin(targets = "fr.minevegas.casino.CasinoSettings", remap = false)
public abstract class CasinoSettingsMixin {
    @Inject(method = "allowed", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void gitanoland$allowed(Player p, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(CasinoAccess.holdsHand(p));
    }

    /** Clic con la Mano sobre una maquina: solo su dueno (o creativo/OP); sin dueno, quien la toca la reclama. */
    @Inject(method = "interact", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void gitanoland$interact(Player p, Level level, BlockPos pos, CallbackInfoReturnable<InteractionResult> cir) {
        if (level.isClientSide || !(p instanceof ServerPlayer sp) || !CasinoAccess.holdsHand(p)) return;
        BlockEntity machine = CasinoMachine.at(level, pos);
        if (machine != null && !CasinoAccess.mayEdit(sp, machine)) cir.setReturnValue(InteractionResult.FAIL);
    }

    /** Paquete de edicion (lo manda el cliente: no se fia): solo ajustes de una maquina propia; lo global, solo creativo/OP. */
    @Inject(method = "edit", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void gitanoland$edit(ServerPlayer p, CasinoSettings.EditPayload payload, CallbackInfo ci) {
        if (CasinoAccess.isAdmin(p)) return;
        if (!"machine".equals(payload.target())) {
            ci.cancel();
            return;
        }
        Level target = p.level();
        for (ServerLevel l : p.getServer().getAllLevels()) {
            if (CasinoSettings.dimensionOf(l).equals(payload.dimension())) {
                target = l;
                break;
            }
        }
        BlockEntity machine = CasinoMachine.at(target, BlockPos.of(payload.position()));
        if (machine == null || !CasinoAccess.mayEdit(p, machine)) ci.cancel();
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
