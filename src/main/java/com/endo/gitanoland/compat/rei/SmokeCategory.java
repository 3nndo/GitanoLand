package com.endo.gitanoland.compat.rei;

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
import net.minecraft.world.level.ItemLike;

/** Categoría de REI para un tipo de receta de Smokeleaf. */
public class SmokeCategory implements DisplayCategory<SmokeDisplay> {
    private final CategoryIdentifier<SmokeDisplay> id;
    private final Component title;
    private final ItemLike icon;

    public SmokeCategory(CategoryIdentifier<SmokeDisplay> id, Component title, ItemLike icon) {
        this.id = id;
        this.title = title;
        this.icon = icon;
    }

    @Override
    public CategoryIdentifier<? extends SmokeDisplay> getCategoryIdentifier() {
        return id;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(icon);
    }

    @Override
    public int getDisplayHeight() {
        return 78;
    }

    @Override
    public List<Widget> setupDisplay(SmokeDisplay display, Rectangle bounds) {
        List<Widget> w = new ArrayList<>();
        w.add(Widgets.createRecipeBase(bounds));
        int x0 = bounds.x + 10;
        int y0 = bounds.y + 10;
        List<EntryIngredient> in = display.getInputEntries();
        for (int i = 0; i < in.size(); i++) {
            w.add(Widgets.createSlot(new Point(x0 + (i % 3) * 18, y0 + (i / 3) * 18)).entries(in.get(i)).markInput());
        }
        w.add(Widgets.createArrow(new Point(bounds.x + 70, y0 + 10)));
        List<EntryIngredient> out = display.getOutputEntries();
        for (int i = 0; i < out.size(); i++) {
            w.add(Widgets.createSlot(new Point(bounds.x + 108, y0 + 9 + i * 18)).entries(out.get(i)).markOutput());
        }
        if (!display.note().getString().isEmpty()) {
            w.add(Widgets.createLabel(new Point(bounds.getCenterX(), bounds.getMaxY() - 12), display.note())
                    .noShadow().centered().color(0xFF404040, 0xFFBBBBBB));
        }
        return w;
    }
}
