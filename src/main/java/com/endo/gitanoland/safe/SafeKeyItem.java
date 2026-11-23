package com.endo.gitanoland.safe;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Llave de caja fuerte. Se enlaza usandola sobre tu caja; despues, clic derecho en una maquina del casino la une a esa caja
 * (las apuestas entran en la caja y los premios salen de ella). Agachado + clic derecho en una maquina enlazada: la desenlaza.
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
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide || !(ctx.getPlayer() instanceof ServerPlayer player)) return InteractionResult.SUCCESS;
        ItemStack key = ctx.getItemInHand();
        UUID id = key.get(ModSafe.SAFE_ID.get());
        if (id == null) {
            player.displayClientMessage(Component.translatable("gitanoland.safe.key_empty"), true);
            return InteractionResult.SUCCESS;
        }
        BlockEntity machine = machineAt(level, ctx.getClickedPos());
        if (machine == null) {
            player.displayClientMessage(Component.translatable("gitanoland.safe.not_linkable"), true);
            return InteractionResult.SUCCESS;
        }
        SafeVaults vaults = SafeVaults.get(level.getServer());
        SafeVaults.Vault mine = vaults.vault(id);
        if (mine == null || !(mine.owner.equals(player.getUUID()) || player.hasPermissions(2))) {
            player.displayClientMessage(Component.translatable("gitanoland.safe.key_empty"), true);
            return InteractionResult.SUCCESS;
        }
        String k = SafeVaults.key(level, machine.getBlockPos());
        SafeVaults.Vault current = vaults.vaultOfLink(k);
        if (current != null && !current.owner.equals(player.getUUID()) && !player.hasPermissions(2)) {
            player.displayClientMessage(Component.translatable("gitanoland.safe.other_owner", current.ownerName), true);
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            vaults.unlink(k);
            player.displayClientMessage(Component.translatable("gitanoland.safe.unlinked"), true);
        } else {
            vaults.link(k, mine.id);
            player.displayClientMessage(Component.translatable("gitanoland.safe.linked"), true);
        }
        return InteractionResult.SUCCESS;
    }

    /** Clic derecho de la llave a un aldeano: lo enlaza a tu caja (agachado: lo desenlaza). */
    @Override
    public InteractionResult interactLivingEntity(ItemStack key, net.minecraft.world.entity.player.Player pl,
                                                  net.minecraft.world.entity.LivingEntity target, net.minecraft.world.InteractionHand hand) {
        if (pl.level().isClientSide || !(pl instanceof ServerPlayer player)) return InteractionResult.SUCCESS;
        if (!(target instanceof net.minecraft.world.entity.npc.AbstractVillager)) return InteractionResult.PASS;
        UUID id = key.get(ModSafe.SAFE_ID.get());
        SafeVaults vaults = SafeVaults.get(player.level().getServer());
        SafeVaults.Vault mine = vaults.vault(id);
        if (mine == null || !(mine.owner.equals(player.getUUID()) || player.hasPermissions(2))) {
            player.displayClientMessage(Component.translatable("gitanoland.safe.key_empty"), true);
            return InteractionResult.SUCCESS;
        }
        String k = SafeVaults.entityKey(target.getUUID());
        SafeVaults.Vault current = vaults.vaultOfLink(k);
        if (current != null && !current.owner.equals(player.getUUID()) && !player.hasPermissions(2)) {
            player.displayClientMessage(Component.translatable("gitanoland.safe.other_owner", current.ownerName), true);
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            vaults.unlink(k);
            player.displayClientMessage(Component.translatable("gitanoland.safe.unlinked"), true);
        } else {
            vaults.link(k, mine.id);
            player.displayClientMessage(Component.translatable("gitanoland.safe.npc_linked"), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        boolean linked = stack.get(ModSafe.SAFE_ID.get()) != null;
        tooltip.add(Component.translatable(linked ? "gitanoland.safe.key.linked" : "gitanoland.safe.key.empty")
                .withStyle(linked ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        tooltip.add(Component.translatable("gitanoland.safe.key.help").withStyle(ChatFormatting.DARK_GRAY));
    }
}
