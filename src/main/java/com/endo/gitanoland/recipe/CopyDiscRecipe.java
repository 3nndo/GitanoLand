package com.endo.gitanoland.recipe;

import com.endo.gitanoland.GitanoLand;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;

/**
 * Copiar un disco: disco de música (cualquiera) + disco en blanco de Etched = una copia. El original se queda en la mesa.
 * (El mod Etch and Copy trae esta receta pero no la registra en 1.21.1, así que no funciona.)
 */
public class CopyDiscRecipe extends CustomRecipe {
    private static final ResourceLocation BLANK = ResourceLocation.parse("etched:blank_music_disc");

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, GitanoLand.MODID);
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<CopyDiscRecipe>> SERIALIZER =
            SERIALIZERS.register("copy_disc", () -> new SimpleCraftingRecipeSerializer<>(CopyDiscRecipe::new));

    public CopyDiscRecipe(CraftingBookCategory category) {
        super(category);
    }

    private static boolean isBlank(ItemStack s) {
        return BLANK.equals(BuiltInRegistries.ITEM.getKey(s.getItem()));
    }

    private static boolean isDisc(ItemStack s) {
        return !s.isEmpty() && !isBlank(s) && s.has(DataComponents.JUKEBOX_PLAYABLE);
    }

    private static int[] find(CraftingInput in) {
        int disc = -1, blank = -1, count = 0;
        for (int i = 0; i < in.size(); i++) {
            ItemStack s = in.getItem(i);
            if (s.isEmpty()) continue;
            count++;
            if (isBlank(s) && blank < 0) blank = i;
            else if (isDisc(s) && disc < 0) disc = i;
            else return null;
        }
        return count == 2 && disc >= 0 && blank >= 0 ? new int[]{disc, blank} : null;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return find(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        int[] f = find(input);
        return f == null ? ItemStack.EMPTY : input.getItem(f[0]).copyWithCount(1);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> rest = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        int[] f = find(input);
        if (f != null) rest.set(f[0], input.getItem(f[0]).copyWithCount(1));
        return rest;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER.get();
    }
}
