package com.endo.gitanoland.client;

import com.endo.gitanoland.GitanoLand;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * El Sr. Washee Washee aparece de vez en cuando en una esquina de la pantalla y se queda ahi,
 * sin molestar mucho, hasta que escribes "washee washee" en el chat.
 */
@EventBusSubscriber(modid = GitanoLand.MODID, value = Dist.CLIENT)
public class WasheeWashee {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(GitanoLand.MODID, "textures/gui/washee.png");
    private static final int TEX_W = 218, TEX_H = 747;
    private static final int W = 40, H = 137;           // tamanyo en pantalla (pequenyo)
    /** Una vez por segundo se tira un dado de 1 entre CHANCE: de media ~ cada 25 minutos. */
    private static final int CHANCE = 1500;

    private static boolean here = false;
    private static boolean right = true;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) { here = false; return; }
        if (here || mc.player.tickCount % 20 != 0) return;
        if (mc.player.getRandom().nextInt(CHANCE) == 0) {
            here = true;
            right = mc.player.getRandom().nextBoolean();
        }
    }

    @SubscribeEvent
    public static void chat(ClientChatEvent event) {
        if (!here) return;
        String m = event.getMessage().toLowerCase(Locale.ROOT).replaceAll("[^a-z ]", " ").replaceAll("\\s+", " ");
        if (m.contains("washee washee")) here = false;
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
