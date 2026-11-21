package com.endo.gitanoland.client;

import com.endo.gitanoland.GitanoLand;
import com.endo.gitanoland.net.OpenRewardsPayload;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Boton "Recompensas" en la esquina superior derecha del inventario: abre el menu de recompensas (sin atajo de teclado). */
@EventBusSubscriber(modid = GitanoLand.MODID, value = Dist.CLIENT)
public class RewardsButton {
    private static final int W = 78, H = 18;

    @SubscribeEvent
    public static void init(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;
        int x = screen.getGuiLeft() + screen.getXSize() - W;
        int y = screen.getGuiTop() - H - 2;
        event.addListener(Button.builder(Component.translatable("gitanoland.rewards.title"),
                        b -> PacketDistributor.sendToServer(OpenRewardsPayload.INSTANCE))
                .bounds(x, y, W, H)
                .build());
    }
}
