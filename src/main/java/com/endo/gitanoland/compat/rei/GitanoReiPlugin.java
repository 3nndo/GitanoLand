package com.endo.gitanoland.compat.rei;

import com.endo.gitanoland.GitanoLand;
import com.endo.gitanoland.compat.viewer.ShopRecipes;
import com.endo.gitanoland.shop.ShopOffer;
import java.util.ArrayList;
import java.util.List;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.world.item.ItemStack;

/** Muestra en REI los tratos de las tiendas de GitanoLand y las recetas de las máquinas de Smokeleaf. */
@REIPluginClient
public class GitanoReiPlugin implements REIClientPlugin {
    public static final CategoryIdentifier<ShopDisplay> SHOP = CategoryIdentifier.of(GitanoLand.MODID, "shop");

    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(new ShopCategory());
        registry.addWorkstations(SHOP, EntryStacks.of(new ItemStack(GitanoLand.AMMO_MAKER_EGG.get())),
                EntryStacks.of(new ItemStack(GitanoLand.GUNSMITH_EGG.get())),
                EntryStacks.of(new ItemStack(GitanoLand.WEEDSGUY_EGG.get())));
        SmokeleafRei.registerCategories(registry);
    }

    @Override
    public void registerEntries(EntryRegistry registry) {
        SmokeleafRei.registerEntries(registry);
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        for (ShopRecipes.Entry e : ShopRecipes.all()) {
            ShopOffer o = e.offer();
            List<EntryIngredient> in = new ArrayList<>();
            if (o.kirkys() > 0) {
                in.add(EntryIngredient.of(EntryStacks.of(GitanoLand.KIRKY.get(), o.kirkys())));
            }
            for (ShopOffer.Cost c : o.extras()) {
                in.add(EntryIngredient.of(EntryStacks.of(c.item(), c.count())));
            }
            registry.add(new ShopDisplay(e.shop(), in, List.of(EntryIngredient.of(EntryStacks.of(o.result())))));
        }
        SmokeleafRei.registerDisplays(registry);
    }
}
