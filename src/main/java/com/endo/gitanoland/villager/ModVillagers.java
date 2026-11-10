package com.endo.gitanoland.villager;

import com.google.common.collect.ImmutableSet;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Profesiones propias. Solo aparecen de forma natural si hay un aldeano sin profesión cerca de su mesa de TaCZ;
 * también se pueden invocar con /summon (ver los comandos en el README del chat).
 * Para cambiar qué mesa usa cada una, edita los dos ids de abajo.
 */
public class ModVillagers {
    private static final String AMMO_TABLE = "tacz:workbench_a";
    private static final String GUNSMITH_TABLE = "tacz:gun_smith_table";

    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, "gitanoland");
    public static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(Registries.VILLAGER_PROFESSION, "gitanoland");

    public static final Holder<PoiType> AMMO_MAKER_POI = POI_TYPES.register("ammo_maker_poi", () -> poi(AMMO_TABLE));
    public static final Holder<PoiType> GUNSMITH_POI = POI_TYPES.register("gunsmith_poi", () -> poi(GUNSMITH_TABLE));

    public static final Holder<VillagerProfession> AMMO_MAKER = PROFESSIONS.register("ammo_maker",
            () -> new VillagerProfession("ammo_maker",
                    h -> h.value() == AMMO_MAKER_POI.value(), h -> h.value() == AMMO_MAKER_POI.value(),
                    ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_WEAPONSMITH));
    public static final Holder<VillagerProfession> GUNSMITH = PROFESSIONS.register("gunsmith",
            () -> new VillagerProfession("gunsmith",
                    h -> h.value() == GUNSMITH_POI.value(), h -> h.value() == GUNSMITH_POI.value(),
                    ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_WEAPONSMITH));

    private static PoiType poi(String blockId) {
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockId));
        return new PoiType(ImmutableSet.copyOf(block.getStateDefinition().getPossibleStates()), 1, 1);
    }

    public static void register(IEventBus bus) {
        POI_TYPES.register(bus);
        PROFESSIONS.register(bus);
    }
}
