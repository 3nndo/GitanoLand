package com.endo.gitanoland.client;

import com.endo.gitanoland.ClientConfig;
import com.endo.gitanoland.GitanoLand;
import com.endo.gitanoland.net.TeamHudPayload;
import com.mojang.brigadier.Command;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * Cuadro con la vida de tu equipo mientras dura un atentado. Transparente y colocable donde quieras:
 * /atentadohud mover (arrastras el cuadro con el raton y la rueda cambia la transparencia), /atentadohud reset.
 */
@EventBusSubscriber(modid = GitanoLand.MODID, value = Dist.CLIENT)
public class TeamHud {
    public static final int WIDTH = 112;
    public static final int ROW = 16;

    private static String team = "";
    private static List<TeamHudPayload.Member> members = List.of();

    /** Lo llama el paquete del servidor. Lista vacia = fin del atentado. */
    public static void set(String name, List<TeamHudPayload.Member> list) {
        team = name;
        members = list;
    }

    public static int height(int rows) {
        return 12 + rows * ROW + 2;
    }

    @SubscribeEvent
    public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(GitanoLand.MODID, "team_hud"), (g, delta) -> {
            if (members.isEmpty()) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.options.hideGui || mc.screen instanceof TeamHudMoveScreen) return;
            draw(g, clampX(ClientConfig.HUD_X.get(), mc), clampY(ClientConfig.HUD_Y.get(), mc, members.size()), team, members,
                    ClientConfig.HUD_BG_OPACITY.get(), ClientConfig.HUD_OPACITY.get());
        });
    }

    @SubscribeEvent
    public static void commands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("atentadohud")
                .then(Commands.literal("mover").executes(ctx -> {
                    Minecraft.getInstance().tell(() -> Minecraft.getInstance().setScreen(new TeamHudMoveScreen()));
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("reset").executes(ctx -> {
                    ClientConfig.HUD_X.set(6);
                    ClientConfig.HUD_Y.set(90);
                    ClientConfig.HUD_BG_OPACITY.set(0);
                    ClientConfig.HUD_OPACITY.set(75);
                    ClientConfig.SPEC.save();
                    ctx.getSource().sendSuccess(() -> Component.literal("Cuadro de vida del equipo: posicion y transparencia por defecto."), false);
                    return Command.SINGLE_SUCCESS;
                }))
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("/atentadohud mover  |  /atentadohud reset"), false);
                    return Command.SINGLE_SUCCESS;
                }));
    }

    public static int clampX(int x, Minecraft mc) {
        return Math.max(0, Math.min(x, mc.getWindow().getGuiScaledWidth() - WIDTH));
    }

    public static int clampY(int y, Minecraft mc, int rows) {
        return Math.max(0, Math.min(y, mc.getWindow().getGuiScaledHeight() - height(rows)));
    }

    /** Dibuja el cuadro en (x, y). bg y alpha en porcentaje. */
    public static void draw(GuiGraphics g, int x, int y, String teamName, List<TeamHudPayload.Member> list, int bg, int alpha) {
        var font = Minecraft.getInstance().font;
        int a = Math.max(10, Math.min(100, alpha)) * 255 / 100;
        if (bg > 0) g.fill(x - 3, y - 3, x + WIDTH + 3, y + height(list.size()) - 1, (bg * 255 / 100) << 24);
        g.drawString(font, "ATENTADO · " + teamName, x, y, (a << 24) | 0xE0A030, true);
        y += 12;
        for (TeamHudPayload.Member m : list) {
            float frac = m.max() <= 0 ? 0 : Math.max(0f, Math.min(1f, m.health() / m.max()));
            int rgb = m.health() <= 0 ? 0x555555 : frac > 0.5f ? 0x4CC24C : frac > 0.25f ? 0xE8C14A : 0xD9453F;
            String name = font.plainSubstrByWidth(m.name(), 60);
            g.drawString(font, name, x, y, (a << 24) | (m.health() <= 0 ? 0x8A8A8A : 0xFFFFFF), true);
            int bx = x + 64, bw = 46;
            g.fill(bx, y + 1, bx + bw, y + 8, ((a / 2) << 24) | 0x2A2A2A);
            g.fill(bx, y + 1, bx + Math.round(bw * frac), y + 8, (a << 24) | rgb);
            String hp = m.health() <= 0 ? "caido" : String.valueOf(Math.round(m.health()));
            g.drawString(font, hp, bx + 2, y + 1, (a << 24) | 0xFFFFFF, false);
            y += ROW;
        }
    }
}
