package com.endo.gitanoland.events;

import com.endo.gitanoland.GitanoLand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** La invisibilidad no emite particulas (algunos texture packs las hacen visibles y delatan al jugador). */
@EventBusSubscriber(modid = GitanoLand.MODID)
public class InvisibilityNoParticles {
    @SubscribeEvent
    public static void tick(EntityTickEvent.Post e) {
        if (!(e.getEntity() instanceof LivingEntity le) || le.level().isClientSide) return;
        MobEffectInstance inv = le.getEffect(MobEffects.INVISIBILITY);
        if (inv == null || !inv.isVisible()) return;
        le.forceAddEffect(new MobEffectInstance(MobEffects.INVISIBILITY, inv.getDuration(), inv.getAmplifier(), inv.isAmbient(), false, inv.showIcon()), null);
    }
}
