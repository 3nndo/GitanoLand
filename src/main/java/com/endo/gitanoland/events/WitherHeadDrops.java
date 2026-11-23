package com.endo.gitanoland.events;

import com.endo.gitanoland.GitanoLand;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * More Mobs pone cabezas personalizadas (player_head con ts.mm.custom) a los wither skeletons y las suelta al morir.
 * Cuando una de esas cabezas cae, se cambia por la calavera de wither skeleton vanilla (la que sirve para invocar al Wither).
 */
@EventBusSubscriber(modid = GitanoLand.MODID)
public class WitherHeadDrops {
    private static boolean modHead(ItemStack s) {
        if (!s.is(Items.PLAYER_HEAD)) return false;
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d != null && d.contains("ts.mm.custom");
    }

    @SubscribeEvent
    public static void drops(LivingDropsEvent e) {
        if (!(e.getEntity() instanceof WitherSkeleton)) return;
        boolean replaced = false;
        for (ItemEntity it : e.getDrops()) {
            if (modHead(it.getItem())) {
                it.setItem(new ItemStack(Items.WITHER_SKELETON_SKULL));
                replaced = true;
            }
        }
        if (!replaced) return;
        // una sola calavera por muerte: si ya se sustituyo la cabeza del mod, se quita la del loot vanilla
        boolean first = true;
        for (java.util.Iterator<ItemEntity> i = e.getDrops().iterator(); i.hasNext(); ) {
            ItemEntity it = i.next();
            if (!it.getItem().is(Items.WITHER_SKELETON_SKULL)) continue;
            if (first) first = false;
            else i.remove();
        }
    }
}
