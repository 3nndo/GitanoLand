package com.endo.gitanoland.compat.rei;

import java.util.List;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import net.minecraft.network.chat.Component;

/** Un trato de tienda en REI: entradas (Kirkys + materiales) -> un objeto. */
public class ShopDisplay extends BasicDisplay {
    private final Component shop;

    public ShopDisplay(Component shop, List<EntryIngredient> inputs, List<EntryIngredient> outputs) {
        super(inputs, outputs);
        this.shop = shop;
    }

    public Component shop() {
        return shop;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return GitanoReiPlugin.SHOP;
    }
}
