package com.endo.gitanoland.compat;

import com.endo.gitanoland.GitanoLand;
import com.endo.gitanoland.Config;
import net.exavior.jackpot.block.SlotMachineBlock;
import net.exavior.jackpot.block.entity.SlotMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Solo se carga si Jackpot está instalado (ver GitanoLand). */
public final class JackpotCompat {
    private JackpotCompat() {}

    public static void init() {
        NeoForge.EVENT_BUS.addListener(JackpotCompat::onPlace);
    }

    private static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!Config.AUTO_SLOTS.get()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getPlacedBlock().getBlock() instanceof SlotMachineBlock)) return;
        BlockPos pos = event.getPos().immutable();
        // Un tick después, para asegurarnos de que el block entity ya existe.
        level.getServer().execute(() -> configure(level, pos));
    }

    private static void configure(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof SlotMachineBlockEntity slot)) return;
        // Solo máquinas "vacías": no pisamos una configuración que ya exista.
        if (!slot.getPlayCost().isEmpty()
                || !slot.getTwoMatchReward().isEmpty()
                || !slot.getThreeMatchReward().isEmpty()
                || !slot.getJackpotReward().isEmpty()) return;

        slot.setPlayCost(kirkys(Config.SLOT_COST.get()));
        slot.setTwoMatchReward(kirkys(Config.SLOT_TWO_MATCH.get()));
        slot.setThreeMatchReward(kirkys(Config.SLOT_THREE_MATCH.get()));
        slot.setJackpotReward(kirkys(Config.SLOT_JACKPOT.get()));
        // setConfig sincroniza con el cliente; mantenemos las probabilidades de la máquina.
        slot.setConfig(slot.winChance, slot.twoMatchChance, slot.threeMatchChance,
                slot.jackpotChance, Config.INFINITE_REWARDS.get());
    }

    private static ItemStack kirkys(int count) {
        return new ItemStack(GitanoLand.KIRKY.get(), count);
    }
}
