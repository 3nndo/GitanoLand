package com.endo.gitanoland.event;

import com.endo.gitanoland.GitanoLand;
import com.endo.gitanoland.entity.WeedsGuyEntity;
import com.endo.gitanoland.shop.Shops;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Si llevas Kirkys en la mano, los aldeanos-tienda cercanos se acercan a ti y te ofrecen algo que puedas pagar
 * (mensaje en la barra de acción). Con clic derecho se abre la tienda de siempre.
 */
@EventBusSubscriber(modid = GitanoLand.MODID)
public class ShopApproach {
    private static final String COOLDOWN = "gitanoland_offer_until";

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 10 != 0) return;
        if (!p.getMainHandItem().is(GitanoLand.KIRKY.get()) && !p.getOffhandItem().is(GitanoLand.KIRKY.get())) return;

        List<AbstractVillager> near = p.level().getEntitiesOfClass(AbstractVillager.class,
                p.getBoundingBox().inflate(12, 4, 12), ShopApproach::isShop);
        for (AbstractVillager v : near) {
            if (v.isTrading() || !v.isAlive()) continue;
            if (v.distanceToSqr(p) > 9.0) {
                approach(v, p);
            } else {
                offer(v, p);
            }
        }
    }

    private static boolean isShop(AbstractVillager v) {
        if (v instanceof WeedsGuyEntity) return true;
        return v instanceof Villager vv && !vv.isBaby() && GitanoLand.MODID.equals(
                BuiltInRegistries.VILLAGER_PROFESSION.getKey(vv.getVillagerData().getProfession()).getNamespace());
    }

    private static void approach(AbstractVillager v, ServerPlayer p) {
        if (v instanceof Villager vv) {
            vv.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new EntityTracker(p, false), 0.55F, 2));
            vv.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(p, true));
        } else {
            v.getNavigation().moveTo(p, 0.55D);
        }
        v.getLookControl().setLookAt(p);
    }

    private static void offer(AbstractVillager v, ServerPlayer p) {
        v.getLookControl().setLookAt(p);
        long now = v.level().getGameTime();
        if (now < v.getPersistentData().getLong(COOLDOWN)) return;
        Shops.Featured f = Shops.featured(p, v);
        if (f == null) return;
        v.getPersistentData().putLong(COOLDOWN, now + 1200L);
        p.displayClientMessage(Component.translatable("gitanoland.shop.offer",
                Component.translatable("gitanoland.shop.title." + f.shop()), f.offer().result().getHoverName(),
                f.offer().kirkys()), true);
        v.playSound(SoundEvents.VILLAGER_YES, 1f, 1f);
    }
}
