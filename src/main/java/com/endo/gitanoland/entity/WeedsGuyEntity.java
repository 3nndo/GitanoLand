package com.endo.gitanoland.entity;

import com.endo.gitanoland.shop.Shops;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Un Wandering Trader (mismo modelo y textura) que vende con la tienda de GitanoLand. */
public class WeedsGuyEntity extends WanderingTrader {
    public WeedsGuyEntity(EntityType<? extends WeedsGuyEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void updateTrades() {
        // Sin tratos de vanilla: la tienda propia sustituye al comercio normal.
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && !level().isClientSide && player instanceof ServerPlayer sp) {
            Shops.openWeedsGuy(sp, this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
}
