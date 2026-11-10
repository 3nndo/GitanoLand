package com.endo.gitanoland.compat.jei;

import com.endo.gitanoland.GitanoLand;
import com.endo.gitanoland.compat.viewer.ShopRecipes;
import com.endo.gitanoland.shop.ShopOffer;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ShopJeiCategory implements IRecipeCategory<ShopRecipes.Entry> {
    private final IDrawable icon;

    public ShopJeiCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(GitanoLand.KIRKY.get()));
    }

    @Override
    public RecipeType<ShopRecipes.Entry> getRecipeType() {
        return GitanoJeiPlugin.SHOP;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gitanoland.recipe.shop");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return 130;
    }

    @Override
    public int getHeight() {
        return 48;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ShopRecipes.Entry e, IFocusGroup focuses) {
        ShopOffer o = e.offer();
        int i = 0;
        if (o.kirkys() > 0) {
            builder.addSlot(RecipeIngredientRole.INPUT, 4 + (i % 3) * 18, 20 + (i / 3) * 18)
                    .addItemStack(new ItemStack(GitanoLand.KIRKY.get(), o.kirkys()));
            i++;
        }
        for (ShopOffer.Cost c : o.extras()) {
            builder.addSlot(RecipeIngredientRole.INPUT, 4 + (i % 3) * 18, 20 + (i / 3) * 18)
                    .addItemStack(new ItemStack(c.item(), c.count()));
            i++;
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 104, 20).addItemStack(o.result());
    }

    @Override
    public void draw(ShopRecipes.Entry e, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        g.drawString(font, e.shop(), 4, 4, 0xFF404040, false);
        g.drawString(font, "->", 70, 24, 0xFF404040, false);
    }
}
