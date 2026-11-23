package com.endo.gitanoland.safe;

import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Llave de almacen. Clic derecho en tu cofre protegido de SecurityCraft para enlazar la llave a el; luego clic derecho en una
 * maquina del casino o en un aldeano para enlazarlos: las apuestas y pagos van a ese cofre. Agachado + clic derecho: desenlaza.
 * La logica esta en SafeKeyEvents (el clic derecho de las maquinas/cofres abre su menu y no llegaria al objeto).
 */
public class SafeKeyItem extends Item {
    private static final Set<String> MACHINES = Set.of("SlotMachineBlockEntity", "PlinkoBlockEntity", "RouletteBlockEntity",
            "FortuneWheelBlockEntity", "DuckDerbyBlockEntity", "PokerTableBlockEntity", "BlackjackTableBlockEntity");

    public SafeKeyItem(Properties properties) {
        super(properties);
    }

    /** La maquina del casino en ese bloque (o justo debajo/encima, por si es de varios bloques). */
    public static BlockEntity machineAt(Level level, BlockPos pos) {
        for (BlockPos p : new BlockPos[]{pos, pos.below(), pos.above()}) {
            BlockEntity be = level.getBlockEntity(p);
            if (be != null && be.getClass().getName().startsWith("fr.minevegas.casino.") && MACHINES.contains(be.getClass().getSimpleName())) return be;
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        boolean linked = stack.get(ModSafe.WAREHOUSE.get()) != null;
        tooltip.add(Component.translatable(linked ? "gitanoland.safe.key.linked" : "gitanoland.safe.key.empty")
                .withStyle(linked ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        tooltip.add(Component.translatable("gitanoland.safe.key.help").withStyle(ChatFormatting.DARK_GRAY));
    }
}
