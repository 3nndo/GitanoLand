package com.endo.gitanoland.client;

import com.endo.gitanoland.ClientConfig;
import com.endo.gitanoland.GitanoLand;
import java.lang.reflect.Field;
import net.micaxs.smokeleaf.client.render.StonedShaderManager;
import net.micaxs.smokeleaf.effect.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Refuerza el efecto de Smokeleaf: después de que el mod fije la intensidad de su shader cada tick,
 * la volvemos a fijar multiplicada por "stonedEffectStrength" (misma fórmula que el original).
 */
@EventBusSubscriber(modid = GitanoLand.MODID, value = Dist.CLIENT)
public final class StonedBoost {
    private StonedBoost() {}

    private static Field activeTierField;
    private static boolean reflectionFailed;

    /**
     * Vanilla cierra el post-proceso al cambiar de cámara (F5), pero el gestor de Smokeleaf cree que sigue cargado
     * (guarda el tier en `activeTier`) y no lo vuelve a cargar. Si falta el efecto con Stoned activo, borramos
     * ese tier para que Smokeleaf lo recargue en su siguiente tick.
     */
    private static void restoreIfLost(Minecraft mc) {
        if (reflectionFailed) return;
        try {
            if (activeTierField == null) {
                activeTierField = StonedShaderManager.class.getDeclaredField("activeTier");
                activeTierField.setAccessible(true);
            }
            if (activeTierField.get(null) != null) activeTierField.set(null, null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            reflectionFailed = true;
            GitanoLand.LOGGER.warn("[GitanoLand] No se pudo restaurar el efecto Stoned tras cambiar de cámara", e);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        MobEffectInstance stoned = mc.player.getEffect(ModEffects.STONED);
        if (stoned == null || !stoned.isVisible()) return;
        PostChain chain = mc.gameRenderer.currentEffect();
        if (chain == null) {
            restoreIfLost(mc);
            return;
        }
        double mult = ClientConfig.EFFECT_STRENGTH.get();
        if (mult == 1.0) return;

        float pulse = 0.5f + 0.5f * (float) Math.sin(mc.level.getGameTime() * 0.12f);
        float base = 0.45f + 0.5f * pulse;
        if (stoned.getDuration() <= 60) base *= stoned.getDuration() / 60.0f;
        chain.setUniform("Intensity", (float) Math.min(2.0, base * mult));
    }
}
