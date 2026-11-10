package com.endo.gitanoland;

import com.endo.gitanoland.compat.JackpotCompat;
import com.endo.gitanoland.entity.ModEntities;
import com.endo.gitanoland.shop.ModMenus;
import com.endo.gitanoland.villager.ModVillagers;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import com.endo.gitanoland.item.KirkyBagItem;
import com.endo.gitanoland.item.ProfessionEggItem;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(GitanoLand.MODID)
public class GitanoLand {
    public static final String MODID = "gitanoland";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MODID);

    // Cuántos Kirkys lleva dentro una bolsa.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BAG_COINS =
            COMPONENTS.register("coins", () -> DataComponentType.<Integer>builder()
                    .persistent(ExtraCodecs.NON_NEGATIVE_INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    // El Kirky: item normal, stackea hasta 64.
    public static final DeferredItem<Item> KIRKY = ITEMS.registerSimpleItem("kirky", new Item.Properties());
    // Solo para el icono de la pestaña creativa: cambia textures/item/tab_icon.png por la tuya.
    public static final DeferredItem<Item> TAB_ICON = ITEMS.registerSimpleItem("tab_icon", new Item.Properties());
    // Huevo para invocar al WeedsGuy (también: /summon gitanoland:weedsguy).
    public static final DeferredItem<Item> WEEDSGUY_EGG = ITEMS.register("weedsguy_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.WEEDSGUY, 0x2F5D2A, 0xC9E37A, new Item.Properties()));
    // Aldeanos-tienda ya con su profesión.
    public static final DeferredItem<Item> AMMO_MAKER_EGG = ITEMS.register("ammo_maker_spawn_egg",
            () -> new ProfessionEggItem(new Item.Properties(), () -> ModVillagers.AMMO_MAKER.value()));
    public static final DeferredItem<Item> GUNSMITH_EGG = ITEMS.register("gunsmith_spawn_egg",
            () -> new ProfessionEggItem(new Item.Properties(), () -> ModVillagers.GUNSMITH.value()));
    // Bolsas: solo admiten Kirkys (ver KirkyBagItem). Tres variantes con capacidad en el config.
    public static final DeferredItem<KirkyBagItem> KIRKY_BAG_SMALL = ITEMS.registerItem("kirky_bag_small",
            p -> new KirkyBagItem(p, () -> Config.BAG_CAPACITY_SMALL.get()), new Item.Properties().stacksTo(1));
    public static final DeferredItem<KirkyBagItem> KIRKY_BAG = ITEMS.registerItem("kirky_bag",
            p -> new KirkyBagItem(p, () -> Config.BAG_CAPACITY.get()), new Item.Properties().stacksTo(1));
    public static final DeferredItem<KirkyBagItem> KIRKY_BAG_LARGE = ITEMS.registerItem("kirky_bag_large",
            p -> new KirkyBagItem(p, () -> Config.BAG_CAPACITY_LARGE.get()), new Item.Properties().stacksTo(1));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("gitanoland_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MODID))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> TAB_ICON.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(KIRKY.get());
                        output.accept(KIRKY_BAG_SMALL.get());
                        output.accept(KIRKY_BAG.get());
                        output.accept(KIRKY_BAG_LARGE.get());
                        output.accept(WEEDSGUY_EGG.get());
                        output.accept(AMMO_MAKER_EGG.get());
                        output.accept(GUNSMITH_EGG.get());
                    })
                    .build());

    public GitanoLand(IEventBus modEventBus, ModContainer container) {
        ITEMS.register(modEventBus);
        TABS.register(modEventBus);
        COMPONENTS.register(modEventBus);
        ModVillagers.register(modEventBus);
        ModEntities.register(modEventBus);
        ModMenus.register(modEventBus);
        container.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);

        // Integración opcional: la clase solo se carga si Jackpot está presente.
        if (ModList.get().isLoaded("jackpot")) {
            JackpotCompat.init();
        }
    }
}
