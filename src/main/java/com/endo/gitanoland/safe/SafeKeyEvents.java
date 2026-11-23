package com.endo.gitanoland.safe;

import com.endo.gitanoland.GitanoLand;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Usos de la Llave de almacen: sobre un cofre de SecurityCraft (enlazar la llave), sobre una maquina o un aldeano (enlazarlos). */
@EventBusSubscriber(modid = GitanoLand.MODID)
public class SafeKeyEvents {
    private static boolean sc() {
        return ModList.get().isLoaded("securitycraft");
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void block(PlayerInteractEvent.RightClickBlock e) {
        if (e.getHand() != InteractionHand.MAIN_HAND || !e.getItemStack().is(ModSafe.SAFE_KEY.get())) return;
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof ServerPlayer p) || !sc()) return;
        Level level = e.getLevel();
        ItemStack key = e.getItemStack();
        BlockEntity be = level.getBlockEntity(e.getPos());
        if (be != null && ScLink.isWarehouse(be)) {
            if (!ScLink.ownedBy(be, p)) {
                p.displayClientMessage(Component.translatable("gitanoland.safe.not_yours", ScLink.ownerName(be)), true);
                return;
            }
            key.set(ModSafe.WAREHOUSE.get(), GlobalPos.of(level.dimension(), be.getBlockPos()));
            p.displayClientMessage(Component.translatable("gitanoland.safe.key_linked"), true);
            return;
        }
        BlockEntity machine = SafeKeyItem.machineAt(level, e.getPos());
        if (machine == null) {
            p.displayClientMessage(Component.translatable("gitanoland.safe.not_linkable"), true);
            return;
        }
        linkTarget(p, key, WarehouseLinks.key(level, machine.getBlockPos()), Component.translatable("gitanoland.safe.linked"));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void entity(PlayerInteractEvent.EntityInteract e) {
        if (e.getHand() != InteractionHand.MAIN_HAND || !e.getItemStack().is(ModSafe.SAFE_KEY.get())) return;
        if (!(e.getTarget() instanceof AbstractVillager v)) return;
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof ServerPlayer p) || !sc()) return;
        linkTarget(p, e.getItemStack(), WarehouseLinks.entityKey(v.getUUID()), Component.translatable("gitanoland.safe.npc_linked"));
    }

    /** Enlaza (o, agachado, desenlaza) un objetivo al cofre guardado en la llave. */
    private static void linkTarget(ServerPlayer p, ItemStack key, String targetKey, Component linkedMsg) {
        GlobalPos chest = key.get(ModSafe.WAREHOUSE.get());
        if (chest == null) {
            p.displayClientMessage(Component.translatable("gitanoland.safe.key_empty"), true);
            return;
        }
        WarehouseLinks links = WarehouseLinks.get(p.getServer());
        WarehouseLinks.Link current = links.linkOf(targetKey);
        String me = p.getUUID().toString();
        if (current != null && !current.ownerUuid().equals(me) && !p.hasPermissions(2)) {
            p.displayClientMessage(Component.translatable("gitanoland.safe.other_owner", current.ownerName()), true);
            return;
        }
        if (p.isShiftKeyDown()) {
            links.unlink(targetKey);
            p.displayClientMessage(Component.translatable("gitanoland.safe.unlinked"), true);
            return;
        }
        Level lvl = p.getServer().getLevel(chest.dimension());
        BlockEntity be = lvl != null && lvl.isLoaded(chest.pos()) ? lvl.getBlockEntity(chest.pos()) : null;
        if (be == null || !ScLink.isWarehouse(be) || !ScLink.ownedBy(be, p)) {
            p.displayClientMessage(Component.translatable("gitanoland.safe.chest_unavailable"), true);
            return;
        }
        links.link(targetKey, new WarehouseLinks.Link(chest, ScLink.ownerUuid(be), ScLink.ownerName(be)));
        p.displayClientMessage(linkedMsg, true);
    }
}
