package com.endo.gitanoland.mixin;

import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Arreglo para Weather2 2.8.7 + CoroUtil 1.21.0-1.3.8: las hojas que arrastra el viento (ParticleTexLeafColor) llaman a
 * CoroUtilColor.getColors(BlockState), que no existe en esa CoroUtil → NoSuchMethodError varias veces por segundo en el log.
 * Se cambia la llamada por un array vacio: Weather2 ya tiene un color de reserva para ese caso (tinte del bloque).
 * Con @Pseudo y require = 0 no hace nada si Weather2 no esta instalado o cambia el codigo.
 */
@Pseudo
@Mixin(targets = "extendedrenderer.particle.entity.ParticleTexLeafColor", remap = false)
public abstract class Weather2LeafColorMixin {
    @Redirect(
            method = "<init>",
            at = @At(value = "INVOKE",
                    target = "Lcom/corosus/coroutil/util/CoroUtilColor;getColors(Lnet/minecraft/world/level/block/state/BlockState;)[I"),
            require = 0,
            remap = false)
    private static int[] gitanoland$noColors(BlockState state) {
        return new int[0];
    }
}
