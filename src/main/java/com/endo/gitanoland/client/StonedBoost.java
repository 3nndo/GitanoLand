package com.endo.gitanoland.client;

import com.endo.gitanoland.ClientConfig;
import com.endo.gitanoland.GitanoLand;
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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onClientTick(ClientTickEvent.Post event) {
        double mult = ClientConfig.EFFECT_STRENGTH.get();
        if (mult == 1.0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        MobEffectInstance stoned = mc.player.getEffect(ModEffects.STONED);
        if (stoned == null || !stoned.isVisible()) return;
        PostChain chain = mc.gameRenderer.currentEffect();
        if (chain == null) return;

        float pulse = 0.5f + 0.5f * (float) Math.sin(mc.level.getGameTime() * 0.12f);
        float base = 0.45f + 0.5f * pulse;
        if (stoned.getDuration() <= 60) base *= stoned.getDuration() / 60.0f;
        chain.setUniform("Intensity", (float) Math.min(2.0, base * mult));
    }
}
