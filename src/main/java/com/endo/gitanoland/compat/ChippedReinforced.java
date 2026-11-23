package com.endo.gitanoland.compat;

import com.endo.gitanoland.GitanoLand;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.geforcemods.securitycraft.SCContent;
import net.geforcemods.securitycraft.api.IReinforcedBlock;
import net.geforcemods.securitycraft.blocks.reinforced.BaseReinforcedBlock;
import net.geforcemods.securitycraft.blocks.reinforced.ReinforcedRotatedPillarBlock;
import net.geforcemods.securitycraft.blocks.reinforced.ReinforcedFenceBlock;
import net.geforcemods.securitycraft.blocks.reinforced.ReinforcedGlassBlock;
import net.geforcemods.securitycraft.blocks.reinforced.ReinforcedPaneBlock;
import net.geforcemods.securitycraft.blocks.reinforced.ReinforcedSlabBlock;
import net.geforcemods.securitycraft.blocks.reinforced.ReinforcedWallBlock;
import net.geforcemods.securitycraft.blocks.reinforced.ReinforcedStairsBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Versiones reforzadas (SecurityCraft) de los bloques de construccion de Chipped: cubos, pilares, losas y escaleras.
 * Se registran en el mapa de SecurityCraft para que el Reforzador Universal y /sc convert las acepten.
 * La lista esta en chipped_reinforced.txt (una linea "nombre tipo" por bloque). Solo se carga si estan SecurityCraft y Chipped.
 */
public final class ChippedReinforced {
    private ChippedReinforced() {}

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(GitanoLand.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GitanoLand.MODID);
    private static final List<Entry> ENTRIES = new ArrayList<>();

    private record Entry(String modName, DeferredBlock<Block> block) {}

    public static void init(IEventBus bus) {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                ChippedReinforced.class.getResourceAsStream("/chipped_reinforced.txt"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] p = line.trim().split(" ");
                if (p.length == 2) add(p[0], p[1]);
            }
        } catch (Exception e) {
            GitanoLand.LOGGER.error("No se pudo leer chipped_reinforced.txt", e);
        }
        BLOCKS.register(bus);
        ITEMS.register(bus);
        bus.addListener(ChippedReinforced::addToBlockEntity);
        bus.addListener(ChippedReinforced::setup);
    }

    private static void add(String name, String kind) {
        Supplier<Block> chipped = () -> BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("chipped", name));
        SoundType sound = name.contains("planks") || name.endsWith("_fence") ? SoundType.WOOD : (name.contains("glass") ? SoundType.GLASS : SoundType.STONE);
        String id = "reinforced_chipped_" + name;
        DeferredBlock<Block> b = BLOCKS.register(id, () -> {
            BlockBehaviour.Properties props = BlockBehaviour.Properties.of().strength(2.0F, Float.MAX_VALUE).sound(sound);
            // Estos tipos necesitan el bloque de Chipped ya creado: GitanoLand va DESPUES de Chipped (ordering AFTER en mods.toml).
            Block q = chipped.get();
            if (q == Blocks.AIR) q = Blocks.STONE; // bloque que este Chipped no tiene: se registra igual pero sin enlazar
            return switch (kind) {
                case "pillar" -> new ReinforcedRotatedPillarBlock(props, chipped);
                case "slab" -> new ReinforcedSlabBlock(props, chipped);
                case "stairs" -> new ReinforcedStairsBlock(props, chipped);
                case "wall" -> new ReinforcedWallBlock(props, q);
                case "fence" -> new ReinforcedFenceBlock(props, q);
                case "glass" -> new ReinforcedGlassBlock(props.noOcclusion(), q);
                case "pane" -> new ReinforcedPaneBlock(props.noOcclusion(), q);
                default -> new BaseReinforcedBlock(props, chipped);
            };
        });
        ITEMS.registerSimpleBlockItem(id, b);
        ENTRIES.add(new Entry(name, b));
    }

    /** Los bloques reforzados guardan al dueño en la entidad "ownable" de SecurityCraft: hay que declararlos validos para ella. */
    private static void addToBlockEntity(BlockEntityTypeAddBlocksEvent event) {
        Block[] all = ENTRIES.stream().map(e -> e.block().get()).toArray(Block[]::new);
        event.modify(SCContent.OWNABLE_BLOCK_ENTITY.get(), all);
    }

    /** Ya estan todos los bloques registrados: enlaza cada bloque de Chipped con el suyo reforzado. */
    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            int ok = 0;
            for (Entry e : ENTRIES) {
                Block vanilla = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("chipped", e.modName()));
                if (vanilla == Blocks.AIR) continue;
                Block reinforced = e.block().get();
                IReinforcedBlock.VANILLA_TO_SECURITYCRAFT.put(vanilla, reinforced);
                IReinforcedBlock.SECURITYCRAFT_TO_VANILLA.put(reinforced, vanilla);
                ok++;
            }
            GitanoLand.LOGGER.info("Chipped reforzado: {} de {} bloques enlazados con SecurityCraft", ok, ENTRIES.size());
        });
    }
}
