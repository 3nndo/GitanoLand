package com.endo.gitanoland.compat.rei;

import com.endo.gitanoland.GitanoLand;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.micaxs.smokeleaf.block.ModBlocks;
import net.micaxs.smokeleaf.recipe.DryingRecipe;
import net.micaxs.smokeleaf.recipe.ExtractorRecipe;
import net.micaxs.smokeleaf.recipe.GeneratorRecipe;
import net.micaxs.smokeleaf.recipe.GrinderRecipe;
import net.micaxs.smokeleaf.recipe.GummyRecipe;
import net.micaxs.smokeleaf.recipe.IngredientWithCount;
import net.micaxs.smokeleaf.recipe.LiquifierRecipe;
import net.micaxs.smokeleaf.recipe.ModRecipes;
import net.micaxs.smokeleaf.recipe.MutatorRecipe;
import net.micaxs.smokeleaf.recipe.SequencerRecipe;
import net.micaxs.smokeleaf.recipe.SynthesizerRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Las recetas de las máquinas de Smokeleaf en REI (el mod solo trae plugin de JEI).
 * No se muestran los fluidos como tal: aparecen como su cubo y la cantidad va en el texto de abajo.
 */
final class SmokeleafRei {
    private SmokeleafRei() {}

    private record Kind<R extends Recipe<?>>(String key, String title, ItemLike icon, RecipeType<R> type,
                                              Function<R, SmokeDisplay> make) {
        CategoryIdentifier<SmokeDisplay> id() {
            return CategoryIdentifier.of(GitanoLand.MODID, "smokeleaf_" + key);
        }
    }

    private static List<Kind<?>> kinds() {
        List<Kind<?>> k = new ArrayList<>();
        k.add(new Kind<GrinderRecipe>("grinder", "Grinder", ModBlocks.GRINDER.get(), ModRecipes.GRINDER_TYPE.get(),
                r -> display(cid("grinder"), List.of(ing(r.inputItem())), List.of(stack(r.output())), "")));

        k.add(new Kind<ExtractorRecipe>("extractor", "Extractor", ModBlocks.EXTRACTOR.get(), ModRecipes.EXTRACTOR_TYPE.get(),
                r -> display(cid("extractor"), List.of(ing(r.inputItem())), List.of(stack(r.output())), "")));

        k.add(new Kind<DryingRecipe>("drying", "Drying", ModBlocks.DRYING_RACK.get(), ModRecipes.DRYING_TYPE.get(),
                r -> display(cid("drying"), List.of(ing(r.ingredient())), List.of(stack(r.result())),
                        (r.time() / 20) + " s")));

        k.add(new Kind<GeneratorRecipe>("generator", "Generator", ModBlocks.GENERATOR.get(), ModRecipes.GENERATOR_TYPE.get(),
                r -> display(cid("generator"), List.of(ing(r.ingredient())), List.of(), r.totalEnergy() + " FE")));

        k.add(new Kind<LiquifierRecipe>("liquifier", "Liquifier", ModBlocks.LIQUIFIER.get(), ModRecipes.LIQUIFIER_TYPE.get(),
                r -> display(cid("liquifier"), List.of(ing(r.ingredient())), List.of(fluid(r.output())), fluidNote(r.output()))));

        k.add(new Kind<GummyRecipe>("gummy_machine", "Gummy Machine", ModBlocks.GUMMY_MACHINE.get(), ModRecipes.GUMMY_TYPE.get(),
                r -> display(cid("gummy_machine"), List.of(ing(r.mold()), ing(r.catalyst()), fluid(r.oil())),
                        List.of(stack(r.output())), fluidNote(r.oil()))));

        k.add(new Kind<MutatorRecipe>("mutator", "Mutator", ModBlocks.MUTATOR.get(), ModRecipes.MUTATOR_TYPE.get(), r -> {
            List<EntryIngredient> in = new ArrayList<>();
            for (IngredientWithCount i : r.inputItems()) in.add(ing(i));
            in.add(fluid(r.fluid()));
            return display(cid("mutator"), in, List.of(stack(r.output())), fluidNote(r.fluid()));
        }));

        k.add(new Kind<SequencerRecipe>("sequencer", "Sequencer", ModBlocks.SEQUENCER.get(), ModRecipes.SEQUENCER_TYPE.get(), r -> {
            List<EntryIngredient> in = new ArrayList<>();
            in.add(ing(r.dnaIngredient()));
            in.add(ing(r.baseExtractIngredient()));
            for (Ingredient i : r.requiredReagents()) in.add(ing(i));
            return display(cid("sequencer"), in, List.of(stack(r.result())), "");
        }));

        k.add(new Kind<SynthesizerRecipe>("synthesizer", "Synthesizer", ModBlocks.SYNTHESIZER.get(), ModRecipes.SYNTHESIZER_TYPE.get(),
                r -> display(cid("synthesizer"), List.of(ing(r.dnaIngredient())), List.of(stack(r.result())), "")));
        return k;
    }

    static void registerCategories(CategoryRegistry registry) {
        for (Kind<?> k : kinds()) {
            registry.add(new SmokeCategory(k.id(), Component.literal("Smokeleaf: " + k.title()), k.icon()));
            registry.addWorkstations(k.id(), EntryStacks.of(k.icon()));
        }
    }

    static void registerDisplays(DisplayRegistry registry) {
        for (Kind<?> k : kinds()) addAll(registry, k);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <R extends Recipe<?>> void addAll(DisplayRegistry registry, Kind<R> k) {
        List all = registry.getRecipeManager().getAllRecipesFor((RecipeType) k.type());
        for (Object o : all) {
            RecipeHolder<R> h = (RecipeHolder<R>) o;
            try {
                registry.add(k.make().apply(h.value()));
            } catch (Exception e) {
                GitanoLand.LOGGER.warn("[GitanoLand] Receta de Smokeleaf no mostrable en REI: {}", h.id(), e);
            }
        }
    }

    // ------------------------------------------------------------------ utilidades

    private static CategoryIdentifier<SmokeDisplay> cid(String key) {
        return CategoryIdentifier.of(GitanoLand.MODID, "smokeleaf_" + key);
    }

    private static SmokeDisplay display(CategoryIdentifier<SmokeDisplay> id, List<EntryIngredient> in,
                                        List<EntryIngredient> out, String note) {
        return new SmokeDisplay(id, in, out, Component.literal(note));
    }

    private static SmokeDisplay display(CategoryIdentifier<SmokeDisplay> id, List<EntryIngredient> in,
                                        List<EntryIngredient> out, Component note) {
        return new SmokeDisplay(id, in, out, note);
    }

    private static EntryIngredient ing(Ingredient i) {
        return EntryIngredients.ofItemStacks(Arrays.asList(i.getItems()));
    }

    private static EntryIngredient ing(IngredientWithCount i) {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStack s : i.ingredient().getItems()) {
            ItemStack c = s.copy();
            c.setCount(Math.max(1, i.count()));
            stacks.add(c);
        }
        return EntryIngredients.ofItemStacks(stacks);
    }

    private static EntryIngredient stack(ItemStack s) {
        return EntryIngredient.of(EntryStacks.of(s));
    }

    /** Fluido mostrado como su cubo; la cantidad va en el texto. */
    private static EntryIngredient fluid(FluidStack f) {
        return EntryIngredient.of(EntryStacks.of(new ItemStack(f.getFluid().getBucket())));
    }

    private static Component fluidNote(FluidStack f) {
        return Component.literal(f.getAmount() + " mB ").append(f.getHoverName());
    }
}
