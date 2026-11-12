package com.endo.gitanoland.event;

import com.endo.gitanoland.GitanoLand;
import com.endo.gitanoland.entity.WeedsGuyEntity;
import com.endo.gitanoland.shop.Shops;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Si llevas Kirkys en la mano, los aldeanos-tienda cercanos se acercan a ti y, al llegar, sostienen en la mano el
 * objeto que te ofrecen (uno que puedas pagar). Lo guardan mientras sigas con Kirkys en la mano; luego descansan un
 * minuto antes de ofrecer otro. Con clic derecho se abre la tienda de siempre.
 */
@EventBusSubscriber(modid = GitanoLand.MODID)
public class ShopApproach {
    private static final String COOLDOWN = "gitanoland_offer_until";
    private static final String HELD_UNTIL = "gitanoland_offer_held_until";
    private static final long HOLD_TICKS = 1200L;
    private static final long REST_TICKS = 1200L;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 10 != 0) return;
        boolean kirky = p.getMainHandItem().is(GitanoLand.KIRKY.get()) || p.getOffhandItem().is(GitanoLand.KIRKY.get());

        List<AbstractVillager> near = p.level().getEntitiesOfClass(AbstractVillager.class,
                p.getBoundingBox().inflate(14, 4, 14), ShopApproach::isShop);
        long now = p.level().getGameTime();
        for (AbstractVillager v : near) {
            if (!v.isAlive()) continue;
            boolean holding = v.getPersistentData().getLong(HELD_UNTIL) > 0;
            if (holding && (!kirky || v.isTrading() || now > v.getPersistentData().getLong(HELD_UNTIL)
                    || v.distanceToSqr(p) > 13 * 13)) {
                putAway(v, now);
                continue;
            }
            if (!kirky || v.isTrading()) continue;
            if (v.distanceToSqr(p) > 9.0) {
                approach(v, p);
            } else {
                offer(v, p, now);
            }
        }
    }

    /** Al abrir la tienda el aldeano guarda lo que sostenía. */
    public static void putAway(AbstractVillager v, long now) {
        var data = v.getPersistentData();
        if (data.getLong(HELD_UNTIL) > 0) {
            v.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            data.putLong(HELD_UNTIL, 0L);
            data.putLong(COOLDOWN, now + REST_TICKS);
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

    private static void offer(AbstractVillager v, ServerPlayer p, long now) {
        v.getLookControl().setLookAt(p);
        var data = v.getPersistentData();
        if (data.getLong(HELD_UNTIL) > 0 || now < data.getLong(COOLDOWN)) return;
        Shops.Featured f = Shops.featured(p, v);
        if (f == null) return;
        v.setItemSlot(EquipmentSlot.MAINHAND, f.offer().result().copyWithCount(1));
        v.setDropChance(EquipmentSlot.MAINHAND, 0f);
        data.putLong(HELD_UNTIL, now + HOLD_TICKS);
        v.playSound(SoundEvents.VILLAGER_YES, 1f, 1f);
    }
}
