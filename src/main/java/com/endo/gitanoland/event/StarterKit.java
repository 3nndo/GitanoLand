package com.endo.gitanoland.event;

import com.endo.gitanoland.Config;
import com.endo.gitanoland.GitanoLand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = GitanoLand.MODID)
public class StarterKit {
    private static final String TAG = "gitanoland_starter";

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        int amount = Config.STARTER_KIRKYS.get();
        if (player.getTags().contains(TAG)) return;
        player.addTag(TAG);
        // el librillo con la guia del mod (tambien se puede pedir con /gitanoland libro)
        ItemStack book = com.endo.gitanoland.item.GuideBook.create();
        if (!player.getInventory().add(book)) player.drop(book, false);
        if (amount <= 0) return;

        for (int left = amount; left > 0; ) {
            int n = Math.min(64, left);
            ItemStack stack = new ItemStack(GitanoLand.KIRKY.get(), n);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            left -= n;
        }
    }
}
