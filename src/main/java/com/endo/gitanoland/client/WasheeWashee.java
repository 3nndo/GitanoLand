package com.endo.gitanoland.client;

import com.endo.gitanoland.GitanoLand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * El Sr. Washee Washee aparece de vez en cuando en una esquina de la pantalla y se queda ahi,
 * sin molestar mucho, hasta que escribes "washee washee" en el chat (el servidor te da 30-120 Kirkys).
 */
@EventBusSubscriber(modid = GitanoLand.MODID, value = Dist.CLIENT)
public class WasheeWashee {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(GitanoLand.MODID, "textures/gui/washee.png");
    private static final int TEX_W = 218, TEX_H = 747;
    private static final int W = 40, H = 137;           // tamanyo en pantalla (pequenyo)
    /** Cada 2 h 30 min de juego (180000 ticks) se tira un dado: 1 entre 250 de que salga. */
    private static final int INTERVAL = 180000;
    private static final int CHANCE = 250;
    private static int wait = 0;

    private static boolean here = false;
    private static boolean right = true;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) { here = false; return; }
        if (here) return;
        if (++wait >= INTERVAL) {
            wait = 0;
            if (mc.player.getRandom().nextInt(CHANCE) == 0) {
                here = true;
                right = mc.player.getRandom().nextBoolean();
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.endo.gitanoland.net.WasheePayload.Appear());
            }
        }
    }

    /** El servidor ha oido "washee washee": se va (y si ha cobrado, sale el toast de Kirkys). */
    public static void bye(int kirkys) {
        here = false;
        if (kirkys > 0) RewardToast.show(kirkys, false);
    }

    @SubscribeEvent
    public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(GitanoLand.MODID, "washee_washee"), (GuiGraphics g, net.minecraft.client.DeltaTracker delta) -> {
            if (!here) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.options.hideGui) return;
            int x = right ? mc.getWindow().getGuiScaledWidth() - W - 4 : 4;
            int y = mc.getWindow().getGuiScaledHeight() - H - 30;   // por encima de la barra de objetos
            g.blit(TEX, x, y, W, H, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);
        });
    }
}
