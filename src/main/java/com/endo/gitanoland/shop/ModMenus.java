package com.endo.gitanoland.shop;

import com.endo.gitanoland.GitanoLand;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, GitanoLand.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ShopMenu>> SHOP =
            MENUS.register("shop", () -> IMenuTypeExtension.create(ShopMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<com.endo.gitanoland.reward.RewardsMenu>> REWARDS =
            MENUS.register("rewards", () -> IMenuTypeExtension.create((id, inv, buf) -> new com.endo.gitanoland.reward.RewardsMenu(id, inv)));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
