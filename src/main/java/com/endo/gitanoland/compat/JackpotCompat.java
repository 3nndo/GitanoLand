package com.endo.gitanoland.compat;

import com.endo.gitanoland.GitanoLand;
import com.endo.gitanoland.Config;
import net.exavior.jackpot.block.CardDeckBlock;
import net.exavior.jackpot.block.PlinkoBlock;
import net.exavior.jackpot.block.PoolTableBlock;
import net.exavior.jackpot.block.SlotMachineBlock;
import net.exavior.jackpot.block.entity.CardDeckBlockEntity;
import net.exavior.jackpot.block.entity.PlinkoBlockEntity;
import net.exavior.jackpot.block.entity.PoolTableBlockEntity;
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
        var block = event.getPlacedBlock().getBlock();
        if (!(block instanceof SlotMachineBlock || block instanceof PlinkoBlock || block instanceof CardDeckBlock
                || block instanceof PoolTableBlock)) return;
        BlockPos pos = event.getPos().immutable();
        // Un tick después, para asegurarnos de que el block entity ya existe.
        level.getServer().execute(() -> configure(level, pos));
    }

    private static void configure(ServerLevel level, BlockPos pos) {
        var be = level.getBlockEntity(pos);
        boolean infinite = Config.INFINITE_REWARDS.get();
        // Plinko, mesa de cartas (blackjack/poker) y billar: solo se configuran si estan "vacios" (no pisamos nada)
        if (be instanceof PlinkoBlockEntity p) {
            if (!p.getPlayCost().isEmpty() || !p.getReward().isEmpty()) return;
            p.setPlayCost(kirkys(Config.PLINKO_COST.get()));
            p.setReward(kirkys(Config.PLINKO_REWARD.get()));
            p.setConfig(p.getChipTexture(), p.allowMultipleChips, p.loop, infinite);
            return;
        }
        if (be instanceof CardDeckBlockEntity c) {
            if (!c.getPlayCost().isEmpty() || !c.getReward().isEmpty()) return;
            c.setPlayCost(kirkys(Config.CARDS_COST.get()));
            c.setReward(kirkys(Config.CARDS_REWARD.get()));
            c.setConfig(c.getTableMode(), c.getBlackjackMode(), c.getDifficultyMode(), infinite);
            return;
        }
        if (be instanceof PoolTableBlockEntity t) {
            if (!t.getPlayCost().isEmpty() || !t.getReward().isEmpty()) return;
            t.setPlayCost(kirkys(Config.POOL_COST.get()));
            t.setReward(kirkys(Config.POOL_REWARD.get()));
            t.setConfig(t.getTableMode(), t.getDifficulty(), t.getAiStyle(), t.isLowFriction(), infinite);
            return;
        }
        if (!(be instanceof SlotMachineBlockEntity slot)) return;
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
