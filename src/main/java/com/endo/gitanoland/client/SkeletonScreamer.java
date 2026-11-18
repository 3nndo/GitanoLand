package com.endo.gitanoland.client;

import com.endo.gitanoland.GitanoLand;
import com.mojang.brigadier.Command;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/** Susto: un esqueleto cruza la pantalla corriendo, muy de vez en cuando. /esqueleto lo prueba. */
@EventBusSubscriber(modid = GitanoLand.MODID, value = Dist.CLIENT)
public class SkeletonScreamer {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(GitanoLand.MODID, "textures/gui/skeleton_run.png");
    private static final int COLS = 6, FRAMES = 24, FW = 160, FH = 290;
    private static final int TEX_W = COLS * FW, TEX_H = 4 * FH;
    private static final int DURATION = 36;          // ticks que tarda en cruzar la pantalla (~1.8 s)
    /** Cada 30 minutos de juego (36000 ticks) se tira un dado: 1 entre 100 de que salga. */
    private static final int INTERVAL = 36000;
    private static final int CHANCE = 100;
    private static int wait = 0;

    private static int t = -1;                       // -1 = no esta corriendo

    private static void start(Minecraft mc) {
        t = 0;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.SKELETON_AMBIENT, 0.6f, 1.0f));
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.GHAST_SCREAM, 0.5f, 0.8f));
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) { t = -1; return; }
        if (t >= 0) {
            if (++t >= DURATION) t = -1;
            return;
        }
        if (++wait >= INTERVAL) {
            wait = 0;
            if (mc.player.getRandom().nextInt(CHANCE) == 0) start(mc);
        }
    }

    @SubscribeEvent
    public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(GitanoLand.MODID, "skeleton_screamer"), (GuiGraphics g, DeltaTracker delta) -> {
            if (t < 0) return;
            Minecraft mc = Minecraft.getInstance();
            float p = (t + delta.getGameTimeDeltaPartialTick(false)) / DURATION;      // 0..1
            int sh = mc.getWindow().getGuiScaledHeight();
            int sw = mc.getWindow().getGuiScaledWidth();
            int h = (int) (sh * 0.85f);
            int w = h * FW / FH;
            float x = -w + p * (sw + 2 * w);
            int f = (int) ((t + delta.getGameTimeDeltaPartialTick(false)) * 1.0f) % FRAMES;
            float u = (f % COLS) * FW, v = (f / COLS) * FH;
            int y = sh - h;
            g.blit(TEX, (int) x, y, w, h, u, v, FW, FH, TEX_W, TEX_H);
        });
    }

    @SubscribeEvent
    public static void commands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("esqueleto").executes(ctx -> {
            Minecraft mc = Minecraft.getInstance();
            mc.tell(() -> start(mc));
            ctx.getSource().sendSuccess(() -> Component.literal("Buuu."), false);
            return Command.SINGLE_SUCCESS;
        }));
    }
}
