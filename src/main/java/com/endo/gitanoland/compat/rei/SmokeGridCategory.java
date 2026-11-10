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
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;

/** Categoría de REI para crafteos de mesa con forma fija (joint, blunt): rejilla de 3x3 con los 9 huecos. */
public class SmokeGridCategory implements DisplayCategory<SmokeDisplay> {
    private final CategoryIdentifier<SmokeDisplay> id;
    private final Component title;
    private final ItemLike icon;

    public SmokeGridCategory(CategoryIdentifier<SmokeDisplay> id, Component title, ItemLike icon) {
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
        return 66;
    }

    @Override
    public List<Widget> setupDisplay(SmokeDisplay display, Rectangle bounds) {
        List<Widget> w = new ArrayList<>();
        w.add(Widgets.createRecipeBase(bounds));
        int x0 = bounds.getCenterX() - (54 + 10 + 24 + 10 + 18) / 2;
        int arrowX = x0 + 54 + 10;
        int outX = arrowX + 24 + 10;
        int y0 = bounds.y + 6;
        List<EntryIngredient> in = display.getInputEntries();
        for (int i = 0; i < 9; i++) {
            EntryIngredient e = i < in.size() ? in.get(i) : EntryIngredient.empty();
            w.add(Widgets.createSlot(new Point(x0 + (i % 3) * 18, y0 + (i / 3) * 18)).entries(e).markInput());
        }
        w.add(Widgets.createArrow(new Point(arrowX, y0 + 19)));
        List<EntryIngredient> out = display.getOutputEntries();
        for (int i = 0; i < out.size(); i++) {
            w.add(Widgets.createSlot(new Point(outX, y0 + 18 + i * 18)).entries(out.get(i)).markOutput());
        }
        if (!display.note().getString().isEmpty()) {
            w.add(Widgets.createLabel(new Point(bounds.getCenterX(), bounds.getMaxY() - 12), fit(display.note(), bounds.width - 16))
                    .noShadow().centered().color(0xFF404040, 0xFFBBBBBB));
        }
        return w;
    }

    /** Recorta el texto con "..." para que no se salga del cuadro de la receta. */
    static Component fit(Component text, int maxWidth) {
        var font = Minecraft.getInstance().font;
        String s = text.getString();
        if (font.width(s) <= maxWidth) return text;
        return Component.literal(font.plainSubstrByWidth(s, maxWidth - font.width("...")) + "...");
    }
}
