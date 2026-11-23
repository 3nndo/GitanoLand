package com.endo.gitanoland.safe;

import com.endo.gitanoland.GitanoLand;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Llave de almacen: enlaza maquinas del casino y aldeanos a un cofre de SecurityCraft (el almacen de Kirkys del dueno). */
public final class ModSafe {
    private ModSafe() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GitanoLand.MODID);
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, GitanoLand.MODID);

    /** Cofre al que esta enlazada la llave. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> WAREHOUSE = COMPONENTS.register("warehouse",
            () -> DataComponentType.<GlobalPos>builder().persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC).build());

    public static final DeferredItem<Item> SAFE_KEY = ITEMS.registerItem("safe_key", SafeKeyItem::new, new Item.Properties().stacksTo(1));

    public static void init(IEventBus bus) {
        ITEMS.register(bus);
        COMPONENTS.register(bus);
    }
}
