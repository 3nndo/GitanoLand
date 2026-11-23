package com.endo.gitanoland.safe;

import com.endo.gitanoland.GitanoLand;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Caja fuerte (almacen de Kirkys del dueno) y su llave para enlazarla a maquinas y tiendas. */
public final class ModSafe {
    private ModSafe() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(GitanoLand.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GitanoLand.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, GitanoLand.MODID);
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, GitanoLand.MODID);

    /** Id de la caja (en la llave: la caja a la que enlaza; en el objeto caja: para recuperar su saldo al recolocarla). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> SAFE_ID = COMPONENTS.register("safe_id",
            () -> DataComponentType.<UUID>builder().persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC).build());

    public static final DeferredBlock<Block> SAFE = BLOCKS.register("safe", () -> new SafeBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(-1.0F, 3_600_000.0F).sound(SoundType.NETHERITE_BLOCK)));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> SAFE_ITEM = ITEMS.registerSimpleBlockItem("safe", SAFE, new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SAFE_KEY = ITEMS.registerItem("safe_key", SafeKeyItem::new, new Item.Properties().stacksTo(1));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SafeBlockEntity>> SAFE_BE = BLOCK_ENTITIES.register("safe",
            () -> BlockEntityType.Builder.of(SafeBlockEntity::new, SAFE.get()).build(null));

    public static void init(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        COMPONENTS.register(bus);
    }
}
