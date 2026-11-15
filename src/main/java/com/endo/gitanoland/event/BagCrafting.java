package com.endo.gitanoland.event;

import com.endo.gitanoland.GitanoLand;
import com.endo.gitanoland.item.KirkyBagItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Al mejorar una bolsa en la mesa de crafteo, los Kirkys de la bolsa vieja pasan a la nueva. */
@EventBusSubscriber(modid = GitanoLand.MODID)
public class BagCrafting {
    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack result = event.getCrafting();
        if (!(result.getItem() instanceof KirkyBagItem)) return;
        int coins = 0;
        for (int i = 0; i < event.getInventory().getContainerSize(); i++) {
            ItemStack s = event.getInventory().getItem(i);
            if (s.getItem() instanceof KirkyBagItem) coins += KirkyBagItem.getCoins(s);
        }
        if (coins > 0) KirkyBagItem.setCoins(result, coins);
    }
}
