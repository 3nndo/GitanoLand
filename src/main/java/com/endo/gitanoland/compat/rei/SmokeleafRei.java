package com.endo.gitanoland.compat.rei;

import com.endo.gitanoland.GitanoLand;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.micaxs.smokeleaf.block.ModBlocks;
import net.micaxs.smokeleaf.component.ModDataComponentTypes;
import net.micaxs.smokeleaf.item.ModItems;
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
import net.micaxs.smokeleaf.strain.StrainItemFactory;
import net.micaxs.smokeleaf.strain.StrainRegistry;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
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
                                              boolean strain, Function<R, SmokeDisplay> make) {
        CategoryIdentifier<SmokeDisplay> id() {
            return CategoryIdentifier.of(GitanoLand.MODID, "smokeleaf_" + key);
        }
    }

    private static List<Kind<?>> kinds() {
        List<Kind<?>> k = new ArrayList<>();
        k.add(new Kind<GrinderRecipe>("grinder", "Grinder", ModBlocks.GRINDER.get(), ModRecipes.GRINDER_TYPE.get(), true,
                r -> display(cid("grinder"), List.of(ing(r.inputItem())), List.of(stack(r.output())), "")));

        k.add(new Kind<ExtractorRecipe>("extractor", "Extractor", ModBlocks.EXTRACTOR.get(), ModRecipes.EXTRACTOR_TYPE.get(), true,
                r -> display(cid("extractor"), List.of(ing(r.inputItem())), List.of(stack(r.output())), "")));

        k.add(new Kind<DryingRecipe>("drying", "Drying", ModBlocks.DRYING_RACK.get(), ModRecipes.DRYING_TYPE.get(), true,
                r -> r.dryBud()
                        ? display(cid("drying"), List.of(ing(r.ingredient())), List.of(dried(r.ingredient())), "")
                        : display(cid("drying"), List.of(ing(r.ingredient())), List.of(stack(r.result())),
                                (r.time() / 20) + " s")));

        k.add(new Kind<GeneratorRecipe>("generator", "Generator", ModBlocks.GENERATOR.get(), ModRecipes.GENERATOR_TYPE.get(), false,
                r -> display(cid("generator"), List.of(ing(r.ingredient())), List.of(), r.totalEnergy() + " FE")));

        k.add(new Kind<LiquifierRecipe>("liquifier", "Liquifier", ModBlocks.LIQUIFIER.get(), ModRecipes.LIQUIFIER_TYPE.get(), false,
                r -> display(cid("liquifier"), List.of(ing(r.ingredient())), List.of(fluid(r.output())), fluidNote(r.output()))));

        k.add(new Kind<GummyRecipe>("gummy_machine", "Gummy Machine", ModBlocks.GUMMY_MACHINE.get(), ModRecipes.GUMMY_TYPE.get(), true,
                r -> display(cid("gummy_machine"), List.of(ing(r.mold()), ing(r.catalyst()), fluid(r.oil())),
                        List.of(stack(r.output())), fluidNote(r.oil()))));

        k.add(new Kind<MutatorRecipe>("mutator", "Mutator", ModBlocks.MUTATOR.get(), ModRecipes.MUTATOR_TYPE.get(), false, r -> {
            List<EntryIngredient> in = new ArrayList<>();
            for (IngredientWithCount i : r.inputItems()) in.add(ing(i));
            in.add(fluid(r.fluid()));
            return display(cid("mutator"), in, List.of(stack(r.output())), fluidNote(r.fluid()));
        }));

        k.add(new Kind<SequencerRecipe>("sequencer", "Sequencer", ModBlocks.SEQUENCER.get(), ModRecipes.SEQUENCER_TYPE.get(), false, r -> {
            List<EntryIngredient> in = new ArrayList<>();
            in.add(ing(r.dnaIngredient()));
            in.add(ing(r.baseExtractIngredient()));
            for (Ingredient i : r.requiredReagents()) in.add(ing(i));
            return display(cid("sequencer"), in, List.of(stack(r.result())), "");
        }));

        k.add(new Kind<SynthesizerRecipe>("synthesizer", "Synthesizer", ModBlocks.SYNTHESIZER.get(), ModRecipes.SYNTHESIZER_TYPE.get(), false,
                r -> display(cid("synthesizer"), List.of(ing(r.dnaIngredient())), List.of(stack(r.result())), "")));
        return k;
    }

    static void registerCategories(CategoryRegistry registry) {
        for (Kind<?> k : kinds()) {
            registry.add(new SmokeCategory(k.id(), Component.literal("Smokeleaf: " + k.title()), k.icon()));
            registry.addWorkstations(k.id(), EntryStacks.of(k.icon()));
        }
        registry.add(new SmokeGridCategory(JOINT, Component.literal("Smokeleaf: Joint"), ModItems.JOINT.get()));
        registry.add(new SmokeGridCategory(BLUNT, Component.literal("Smokeleaf: Blunt"), ModItems.BLUNT.get()));
        registry.add(new SmokeCategory(BAG, Component.literal("Smokeleaf: Bolsas"), ModItems.GENERIC_BAG.get()));
    }

    static void registerDisplays(DisplayRegistry registry) {
        for (Kind<?> k : kinds()) addAll(registry, k);
        addCrafting(registry);
    }

    /** Todas las cepas (ids ordenados). */
    private static List<String> strains() {
        return StrainRegistry.ids().stream().sorted().toList();
    }

    private static Set<Item> strainItems() {
        return Set.of(ModItems.GENERIC_BUD.get(), ModItems.GENERIC_WEED.get(), ModItems.GENERIC_EXTRACT.get(),
                ModItems.GENERIC_SEEDS.get(), ModItems.GENERIC_BAG.get(), ModItems.GENERIC_GUMMY.get(),
                ModItems.GENERIC_GUMMY_WORM.get());
    }

    /** Añade al buscador de REI todas las variantes por cepa (buds, weed, gummies...). */
    static void registerEntries(EntryRegistry registry) {
        List<String> ids = strains();
        for (Item item : strainItems()) {
            for (String id : ids) {
                try {
                    registry.addEntry(EntryStacks.of(StrainItemFactory.create(item, id)));
                } catch (Exception e) {
                    GitanoLand.LOGGER.warn("[GitanoLand] Cepa no mostrable en REI: {} {}", item, id, e);
                }
            }
        }
    }

    private static final CategoryIdentifier<SmokeDisplay> JOINT = cid("joint");
    private static final CategoryIdentifier<SmokeDisplay> BLUNT = cid("blunt");
    private static final CategoryIdentifier<SmokeDisplay> BAG = cid("bag");

    /** Joint, blunt y bolsas: recetas especiales de Smokeleaf que ningún visor muestra solo. */
    private static void addCrafting(DisplayRegistry registry) {
        try {
            List<ItemStack> weeds = new ArrayList<>();
            for (String id : strains()) weeds.add(StrainItemFactory.weed(id));
            EntryIngredient weed = EntryIngredients.ofItemStacks(weeds);
            EntryIngredient paper = EntryIngredient.of(EntryStacks.of(Items.PAPER));
            EntryIngredient none = EntryIngredient.empty();
            EntryIngredient tobacco = EntryIngredient.of(EntryStacks.of(ModItems.TOBACCO.get()));
            Component mix = Component.empty();

            registry.add(new SmokeDisplay(JOINT,
                    List.of(none, paper, none, weed, tobacco, weed, none, paper, none),
                    List.of(EntryIngredient.of(EntryStacks.of(ModItems.JOINT.get()))), mix));
            registry.add(new SmokeDisplay(BLUNT,
                    List.of(paper, paper, paper, weed, weed, weed, paper, paper, paper),
                    List.of(EntryIngredient.of(EntryStacks.of(ModItems.BLUNT.get()))), mix));

            Item emptyBag = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("smokeleafindustries", "empty_bag"));
            for (String id : strains()) {
                ItemStack bag = StrainItemFactory.create(ModItems.GENERIC_BAG.get(), id);
                ItemStack w8 = StrainItemFactory.weed(id);
                w8.setCount(8);
                registry.add(new SmokeDisplay(BAG, List.of(stackIng(bag)), List.of(stackIng(w8)), Component.literal("Abrir bolsa · " + pretty(id))));
                List<EntryIngredient> in = new ArrayList<>();
                in.add(EntryIngredient.of(EntryStacks.of(emptyBag)));
                in.add(stackIng(w8));
                registry.add(new SmokeDisplay(BAG, in, List.of(stackIng(bag)), Component.literal("Guardar 8 weed · " + pretty(id))));
            }
        } catch (Exception e) {
            GitanoLand.LOGGER.warn("[GitanoLand] No se pudieron añadir joint/blunt/bolsas a REI", e);
        }
    }

    /** "og_kush" -> "Og Kush". */
    private static String pretty(String id) {
        StringBuilder b = new StringBuilder();
        for (String part : id.split("_")) {
            if (part.isEmpty()) continue;
            if (b.length() > 0) b.append(' ');
            b.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return b.toString();
    }

    /** Añade el nombre de la cepa a la nota cuando la receta es de una cepa concreta. */
    private static Component withStrain(Component note) {
        if (currentStrain == null) return note;
        if (note.getString().isEmpty()) return Component.literal(pretty(currentStrain));
        return Component.literal(note.getString() + " · " + pretty(currentStrain));
    }

    private static EntryIngredient stackIng(ItemStack s) {
        return EntryIngredient.of(EntryStacks.of(s));
    }

    // Cepa con la que se construye la receta que se está generando (null = sin cepa).
    private static String currentStrain;
    private static boolean touchedStrain;

    /** Convierte el item genérico en su variante de la cepa actual (si toca). */
    private static ItemStack sv(ItemStack s) {
        if (!strainItems().contains(s.getItem())) return s;
        touchedStrain = true;
        if (currentStrain == null) return s;
        ItemStack c = StrainItemFactory.create(s.getItem(), currentStrain);
        c.setCount(s.getCount());
        return c;
    }

    private static EntryIngredient dried(Ingredient i) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack s : i.getItems()) {
            ItemStack c = sv(s).copy();
            c.set(ModDataComponentTypes.DRY.get(), Boolean.TRUE);
            out.add(c);
        }
        return EntryIngredients.ofItemStacks(out);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <R extends Recipe<?>> void addAll(DisplayRegistry registry, Kind<R> k) {
        List all = registry.getRecipeManager().getAllRecipesFor((RecipeType) k.type());
        for (Object o : all) {
            RecipeHolder<R> h = (RecipeHolder<R>) o;
            try {
                currentStrain = null;
                touchedStrain = false;
                SmokeDisplay base = k.make().apply(h.value());
                if (k.strain() && touchedStrain) {
                    for (String id : strains()) {
                        currentStrain = id;
                        registry.add(k.make().apply(h.value()));
                    }
                } else {
                    registry.add(base);
                }
                currentStrain = null;
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
        return new SmokeDisplay(id, in, out, withStrain(Component.literal(note)));
    }

    private static SmokeDisplay display(CategoryIdentifier<SmokeDisplay> id, List<EntryIngredient> in,
                                        List<EntryIngredient> out, Component note) {
        return new SmokeDisplay(id, in, out, withStrain(note));
    }

    private static EntryIngredient ing(Ingredient i) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack s : i.getItems()) out.add(sv(s));
        return EntryIngredients.ofItemStacks(out);
    }

    private static EntryIngredient ing(IngredientWithCount i) {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStack s : i.ingredient().getItems()) {
            ItemStack c = sv(s).copy();
            c.setCount(Math.max(1, i.count()));
            stacks.add(c);
        }
        return EntryIngredients.ofItemStacks(stacks);
    }

    private static EntryIngredient stack(ItemStack s) {
        return EntryIngredient.of(EntryStacks.of(sv(s)));
    }

    /** Fluido mostrado como su cubo; la cantidad va en el texto. */
    private static EntryIngredient fluid(FluidStack f) {
        return EntryIngredient.of(EntryStacks.of(new ItemStack(f.getFluid().getBucket())));
    }

    private static Component fluidNote(FluidStack f) {
        return Component.literal(f.getAmount() + " mB ").append(f.getHoverName());
    }
}
