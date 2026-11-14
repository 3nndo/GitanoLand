package com.endo.gitanoland.event;

import com.endo.gitanoland.GitanoLand;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import java.util.ArrayList;
import java.util.List;

/**
 * El Stoner de Smokeleaf (el que compra kush y demás) paga en Kirkys en la mayoría de sus tratos:
 * de cada 100 pagos en esmeraldas, 35 se quedan así y 65 se convierten en Kirkys (3 Kirkys por esmeralda).
 */
@EventBusSubscriber(modid = GitanoLand.MODID)
public class StonerTrades {
    private static final ResourceLocation STONER = ResourceLocation.fromNamespaceAndPath("smokeleafindustries", "stoner");
    private static final int EMERALD_PERCENT = 35;
    private static final int KIRKYS_PER_EMERALD = 3;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTrades(VillagerTradesEvent event) {
        VillagerProfession p = event.getType();
        if (!STONER.equals(BuiltInRegistries.VILLAGER_PROFESSION.getKey(p))) return;
        event.getTrades().int2ObjectEntrySet().forEach(e -> {
            List<ItemListing> wrapped = new ArrayList<>();
            for (ItemListing orig : e.getValue()) {
                wrapped.add((trader, random) -> {
                    MerchantOffer o = orig.getOffer(trader, random);
                    if (o == null || !o.getResult().is(Items.EMERALD) || random.nextInt(100) < EMERALD_PERCENT) return o;
                    return new MerchantOffer(o.getItemCostA(), o.getItemCostB(),
                            new ItemStack(GitanoLand.KIRKY.get(), o.getResult().getCount() * KIRKYS_PER_EMERALD),
                            0, o.getMaxUses(), o.getXp(), o.getPriceMultiplier(), 0);
                });
            }
            e.setValue(wrapped);
        });
    }
}
