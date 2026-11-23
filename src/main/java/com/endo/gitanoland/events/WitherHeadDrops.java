package com.endo.gitanoland.events;

import com.endo.gitanoland.GitanoLand;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * More Mobs pone cabezas personalizadas (player_head con ts.mm.custom) a los wither skeletons y las suelta al morir.
 * Aqui se quitan de los drops: la calavera de wither skeleton sale solo por el loot vanilla (2,5 % + 1 % por nivel de Looting).
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
        e.getDrops().removeIf(it -> modHead(it.getItem()));
    }
}
