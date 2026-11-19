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
import net.geforcemods.securitycraft.blocks.reinforced.ReinforcedSlabBlock;
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
 * Versiones reforzadas (SecurityCraft) de los bloques de construccion de Quark: cubos, pilares, losas y escaleras.
 * Se registran en el mapa de SecurityCraft para que el Reforzador Universal y /sc convert las acepten.
 * La lista esta en quark_reinforced.txt (una linea "nombre tipo" por bloque). Solo se carga si estan SecurityCraft y Quark.
 */
public final class QuarkReinforced {
    private QuarkReinforced() {}

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(GitanoLand.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GitanoLand.MODID);
    private static final List<Entry> ENTRIES = new ArrayList<>();

    private record Entry(String quarkName, DeferredBlock<Block> block) {}

    public static void init(IEventBus bus) {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                QuarkReinforced.class.getResourceAsStream("/quark_reinforced.txt"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] p = line.trim().split(" ");
                if (p.length == 2) add(p[0], p[1]);
            }
        } catch (Exception e) {
            GitanoLand.LOGGER.error("No se pudo leer quark_reinforced.txt", e);
        }
        BLOCKS.register(bus);
        ITEMS.register(bus);
        bus.addListener(QuarkReinforced::addToBlockEntity);
        bus.addListener(QuarkReinforced::setup);
    }

    private static void add(String name, String kind) {
        Supplier<Block> quark = () -> BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("quark", name));
        SoundType sound = name.contains("planks") ? SoundType.WOOD : SoundType.STONE;
        String id = "reinforced_quark_" + name;
        DeferredBlock<Block> b = BLOCKS.register(id, () -> {
            BlockBehaviour.Properties props = BlockBehaviour.Properties.of().strength(2.0F, Float.MAX_VALUE).sound(sound);
            return switch (kind) {
                case "pillar" -> new ReinforcedRotatedPillarBlock(props, quark);
                case "slab" -> new ReinforcedSlabBlock(props, quark);
                case "stairs" -> new ReinforcedStairsBlock(props, quark);
                default -> new BaseReinforcedBlock(props, quark);
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

    /** Ya estan todos los bloques registrados: enlaza cada bloque de Quark con el suyo reforzado. */
    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            int ok = 0;
            for (Entry e : ENTRIES) {
                Block vanilla = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("quark", e.quarkName()));
                if (vanilla == Blocks.AIR) continue;
                Block reinforced = e.block().get();
                IReinforcedBlock.VANILLA_TO_SECURITYCRAFT.put(vanilla, reinforced);
                IReinforcedBlock.SECURITYCRAFT_TO_VANILLA.put(reinforced, vanilla);
                ok++;
            }
            GitanoLand.LOGGER.info("Quark reforzado: {} de {} bloques enlazados con SecurityCraft", ok, ENTRIES.size());
        });
    }
}
