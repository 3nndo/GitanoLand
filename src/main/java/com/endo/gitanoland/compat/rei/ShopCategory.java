package com.endo.gitanoland.compat.rei;

import com.endo.gitanoland.GitanoLand;
import java.util.ArrayList;
import java.util.List;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ShopCategory implements DisplayCategory<ShopDisplay> {
    @Override
    public CategoryIdentifier<? extends ShopDisplay> getCategoryIdentifier() {
        return GitanoReiPlugin.SHOP;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gitanoland.recipe.shop");
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(new ItemStack(GitanoLand.KIRKY.get()));
    }

    @Override
    public int getDisplayHeight() {
        return 52;
    }

    @Override
    public List<Widget> setupDisplay(ShopDisplay display, Rectangle bounds) {
        List<Widget> w = new ArrayList<>();
        w.add(Widgets.createRecipeBase(bounds));
        w.add(Widgets.createLabel(new Point(bounds.getCenterX(), bounds.y + 6), display.shop())
                .noShadow().centered().color(0xFF404040, 0xFFBBBBBB));
        int cols0 = Math.min(3, Math.max(1, display.getInputEntries().size()));
        int x0 = bounds.getCenterX() - (cols0 * 18 + 10 + 24 + 10 + 18) / 2;
        int arrowX = x0 + cols0 * 18 + 10;
        int outX = arrowX + 24 + 10;
        int y0 = bounds.y + 22;
        List<EntryIngredient> in = display.getInputEntries();
        for (int i = 0; i < in.size(); i++) {
            w.add(Widgets.createSlot(new Point(x0 + (i % 3) * 18, y0 + (i / 3) * 18)).entries(in.get(i)).markInput());
        }
        w.add(Widgets.createArrow(new Point(arrowX, y0 + 1)));
        List<EntryIngredient> out = display.getOutputEntries();
        if (!out.isEmpty()) {
            w.add(Widgets.createSlot(new Point(outX, y0)).entries(out.get(0)).markOutput());
        }
        return w;
    }
}
