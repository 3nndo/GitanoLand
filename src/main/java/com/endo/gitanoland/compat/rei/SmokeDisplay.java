package com.endo.gitanoland.compat.rei;

import java.util.List;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import net.minecraft.network.chat.Component;

/** Receta de una máquina de Smokeleaf mostrada en REI. */
public class SmokeDisplay extends BasicDisplay {
    private final CategoryIdentifier<?> category;
    private final Component note;

    public SmokeDisplay(CategoryIdentifier<?> category, List<EntryIngredient> inputs, List<EntryIngredient> outputs,
                        Component note) {
        super(inputs, outputs);
        this.category = category;
        this.note = note;
    }

    /** Texto de apoyo (fluidos, energía, tiempo...). Puede estar vacío. */
    public Component note() {
        return note;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return category;
    }
}
