package com.endo.gitanoland.entity;

import com.endo.gitanoland.GitanoLand;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = GitanoLand.MODID)
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, GitanoLand.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<WeedsGuyEntity>> WEEDSGUY =
            ENTITIES.register("weedsguy", () -> EntityType.Builder.of(WeedsGuyEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).clientTrackingRange(10).build("weedsguy"));

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(WEEDSGUY.get(), Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.5D).build());
    }
}
