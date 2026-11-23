package com.endo.gitanoland.safe;

import com.endo.gitanoland.Config;
import com.endo.gitanoland.GitanoLand;
import com.endo.gitanoland.shop.Wallet;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;

/**
 * Aldeanos que pagan en Kirkys (compradores de weed, el Stoner...): su dueno es quien enlaza su cofre de SecurityCraft con la Llave
 * (clic derecho al aldeano). Con npcShopRequireSafe, un aldeano sin cofre no comercia con Kirkys: no se crean de la nada.
 * Lo que el jugador paga en Kirkys entra en el cofre; lo que el aldeano paga sale de el.
 */
@EventBusSubscriber(modid = GitanoLand.MODID)
public class SafeShopEvents {
    private static boolean required() {
        try {
            return Config.NPC_SHOP_REQUIRE_SAFE.get();
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean isKirky(ItemStack s) {
        return !s.isEmpty() && s.is(GitanoLand.KIRKY.get());
    }

    private static int kirkyCost(Optional<ItemCost> c) {
        return c.isPresent() && c.get().itemStack().is(GitanoLand.KIRKY.get()) ? c.get().count() : 0;
    }

    /** Menor premio en Kirkys entre los tratos del aldeano (0 si ninguno paga en Kirkys). */
    private static int smallestKirkyPayout(AbstractVillager v) {
        int min = 0;
        for (MerchantOffer o : v.getOffers()) {
            if (isKirky(o.getResult())) {
                int n = o.getResult().getCount();
                min = min == 0 ? n : Math.min(min, n);
            }
        }
        return min;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void interact(PlayerInteractEvent.EntityInteract e) {
        if (e.getLevel().isClientSide || e.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(e.getTarget() instanceof AbstractVillager v) || !(e.getEntity() instanceof ServerPlayer p)) return;
        if (p.getMainHandItem().is(ModSafe.SAFE_KEY.get())) return; // la llave la maneja SafeKeyEvents
        int min = smallestKirkyPayout(v);
        if (min == 0) return;
        WarehouseLinks.Link link = WarehouseLinks.get(e.getLevel().getServer()).linkOf(WarehouseLinks.entityKey(v.getUUID()));
        if (link == null) {
            if (required()) {
                e.setCanceled(true);
                e.setCancellationResult(InteractionResult.SUCCESS);
                p.displayClientMessage(Component.translatable("gitanoland.safe.npc_no_safe"), true);
            }
            return;
        }
        Container chest = Warehouse.container(e.getLevel().getServer(), link.chest());
        if (chest == null || Warehouse.balance(chest) < min) {
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.SUCCESS);
            p.displayClientMessage(Component.translatable("gitanoland.safe.npc_no_funds", link.ownerName()), true);
        }
    }

    @SubscribeEvent
    public static void traded(TradeWithVillagerEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.level().getServer() == null) return;
        AbstractVillager v = e.getAbstractVillager();
        WarehouseLinks.Link link = WarehouseLinks.get(p.level().getServer()).linkOf(WarehouseLinks.entityKey(v.getUUID()));
        Container chest = link == null ? null : Warehouse.container(p.level().getServer(), link.chest());
        if (chest == null) return;
        MerchantOffer o = e.getMerchantOffer();
        // lo que paga el jugador en Kirkys entra en el cofre (si esta lleno, cae al suelo junto al aldeano)
        int in = kirkyCost(Optional.of(o.getItemCostA())) + kirkyCost(o.getItemCostB());
        if (in > 0) SafeEconomy.income(v, in);
        // lo que paga el aldeano sale del cofre; si no llega, se le quita al jugador lo que sobre (no se crean Kirkys)
        if (isKirky(o.getResult())) {
            int pay = o.getResult().getCount();
            long fromChest = Warehouse.withdraw(chest, pay);
            long shortfall = pay - fromChest;
            if (shortfall > 0) Wallet.takeKirkys(p, (int) shortfall);
        }
    }
}
