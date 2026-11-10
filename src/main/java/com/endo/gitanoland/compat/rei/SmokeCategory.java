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
        return 70;
    }

    @Override
    public List<Widget> setupDisplay(SmokeDisplay display, Rectangle bounds) {
        List<Widget> w = new ArrayList<>();
        w.add(Widgets.createRecipeBase(bounds));
        List<EntryIngredient> in = display.getInputEntries();
        List<EntryIngredient> out = display.getOutputEntries();
        int rows = Math.max(1, (in.size() + 2) / 3);
        // Contenido centrado en vertical en el hueco que deja la etiqueta de abajo.
        int top = bounds.y + 6;
        int avail = bounds.height - 6 - 18;
        int y0 = top + Math.max(0, (avail - rows * 18) / 2);
        int cy = y0 + rows * 9;
        int cols = Math.min(3, Math.max(1, in.size()));
        int x0 = bounds.getCenterX() - (cols * 18 + 10 + 24 + 10 + 18) / 2;
        int arrowX = x0 + cols * 18 + 10;
        int outX = arrowX + 24 + 10;
        for (int i = 0; i < in.size(); i++) {
            w.add(Widgets.createSlot(new Point(x0 + (i % 3) * 18, y0 + (i / 3) * 18)).entries(in.get(i)).markInput());
        }
        w.add(Widgets.createArrow(new Point(arrowX, cy - 8)));
        for (int i = 0; i < out.size(); i++) {
            w.add(Widgets.createSlot(new Point(outX, cy - 9 + i * 18)).entries(out.get(i)).markOutput());
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
