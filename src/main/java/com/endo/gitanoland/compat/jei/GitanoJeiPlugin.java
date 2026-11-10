package com.endo.gitanoland.compat.jei;

import com.endo.gitanoland.GitanoLand;
import com.endo.gitanoland.compat.viewer.ShopRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Muestra en JEI los tratos de las tiendas de GitanoLand. (Smokeleaf ya trae su propio plugin de JEI.) */
@JeiPlugin
public class GitanoJeiPlugin implements IModPlugin {
    public static final RecipeType<ShopRecipes.Entry> SHOP =
            RecipeType.create(GitanoLand.MODID, "shop", ShopRecipes.Entry.class);

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(GitanoLand.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new ShopJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(SHOP, ShopRecipes.all());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(GitanoLand.AMMO_MAKER_EGG.get()), SHOP);
        registration.addRecipeCatalyst(new ItemStack(GitanoLand.GUNSMITH_EGG.get()), SHOP);
        registration.addRecipeCatalyst(new ItemStack(GitanoLand.WEEDSGUY_EGG.get()), SHOP);
    }
}
