package com.endo.gitanoland.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/** Cigarrillo: se "fuma" manteniendo clic derecho; da Velocidad I durante 30 s y se gasta. */
public class CigaretteItem extends Item {
    public CigaretteItem(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.TOOT_HORN;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 40;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        level.playSound(null, player.blockPosition(), SoundEvents.FLINTANDSTEEL_USE, player.getSoundSource(), 0.6F, 1.0F);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (level instanceof ServerLevel sl && remaining % 8 == 0) {
            sl.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getEyeY() - 0.1, entity.getZ(), 3, 0.15, 0.1, 0.15, 0.01);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0));
        }
        if (!(entity instanceof Player p && p.hasInfiniteMaterials())) stack.shrink(1);
        return stack;
    }
}
