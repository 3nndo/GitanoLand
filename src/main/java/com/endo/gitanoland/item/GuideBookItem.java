package com.endo.gitanoland.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Libro con paginas, iconos de items y recetas (sistema de libros de Citadel, como el Animal Dictionary de Alex's Mobs). */
public class GuideBookItem extends Item {
    public GuideBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            com.endo.gitanoland.client.GuideBookScreen.open(stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
