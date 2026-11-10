package com.endo.gitanoland.event;

import com.endo.gitanoland.Config;
import com.endo.gitanoland.GitanoLand;
import com.endo.gitanoland.entity.WeedsGuyEntity;
import com.endo.gitanoland.shop.Markup;
import com.endo.gitanoland.shop.Shops;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = GitanoLand.MODID)
public class ShopEvents {
    /** Los aldeanos con profesión de GitanoLand abren la tienda en vez del comercio de vanilla. */
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Villager villager)) return;
        ResourceLocation prof = BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession());
        if (!GitanoLand.MODID.equals(prof.getNamespace())) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
        if (event.getHand() == InteractionHand.MAIN_HAND && !villager.isBaby()
                && event.getEntity() instanceof ServerPlayer sp) {
            Shops.openVillager(sp, villager, prof.getPath());
        }
    }

    /** Pegar a un aldeano-tienda puede subirle los precios (nunca bajarlos). */
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        Entity target = event.getTarget();
        if (target.level().isClientSide || !(event.getEntity() instanceof ServerPlayer)) return;
        boolean shop = target instanceof WeedsGuyEntity;
        if (target instanceof Villager v) {
            shop = GitanoLand.MODID.equals(
                    BuiltInRegistries.VILLAGER_PROFESSION.getKey(v.getVillagerData().getProfession()).getNamespace());
        }
        if (!shop || target.level().random.nextDouble() >= Config.HIT_MARKUP_CHANCE.get()) return;
        Markup.bump(target);
        // Como en vanilla: sin mensajes, el aldeano solo niega con la cabeza.
        target.level().playSound(null, target.blockPosition(), net.minecraft.sounds.SoundEvents.VILLAGER_NO,
                net.minecraft.sounds.SoundSource.NEUTRAL, 1f, 1f);
    }
}
