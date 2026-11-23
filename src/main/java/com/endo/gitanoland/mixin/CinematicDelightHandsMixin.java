package com.endo.gitanoland.mixin;

import java.lang.reflect.Method;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cinematic Delight (escenas de cocina) esconde las manos de vanilla cancelando GameRenderer.renderItemInHand, pero Iris
 * (shaders) pinta la mano por su cuenta llamando directamente a ItemInHandRenderer.renderHandsWithItems: con shaders salia
 * la mano normal ademas de los brazos de la escena (3 brazos). Aqui se cancela ese metodo mientras la escena esta activa.
 * Sin Cinematic Delight no hace nada.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class CinematicDelightHandsMixin {
    private static final Logger gitanoland$LOG = LogUtils.getLogger();
    private static boolean gitanoland$wasCancelling;
    private static Method gitanoland$active;
    private static boolean gitanoland$checked;

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true, require = 0)
    private void gitanoland$hideHandsInCookScene(CallbackInfo ci) {
        if (!gitanoland$checked) {
            gitanoland$checked = true;
            try {
                gitanoland$active = Class.forName("com.arbuzobr1.cinematicdelight.CookScene").getMethod("active");
            } catch (ReflectiveOperationException | LinkageError e) {
                gitanoland$active = null;
            }
        }
        if (gitanoland$active == null) {
            if (!gitanoland$wasCancelling) {
                gitanoland$wasCancelling = true;
                gitanoland$LOG.info("[GitanoLand] CookScene.active() no disponible: no se oculta la mano de vanilla");
            }
            return;
        }
        try {
            boolean on = Boolean.TRUE.equals(gitanoland$active.invoke(null));
            if (on != gitanoland$wasCancelling) {
                gitanoland$wasCancelling = on;
                gitanoland$LOG.info("[GitanoLand] Escena de Cinematic Delight {}: {} la mano de vanilla (renderHandsWithItems)", on ? "activa" : "terminada", on ? "oculto" : "dejo de ocultar");
            }
            if (on) {
                ci.cancel();
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            gitanoland$active = null;
        }
    }
}
