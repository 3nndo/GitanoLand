package com.endo.gitanoland.client;

import com.endo.gitanoland.ClientConfig;
import com.endo.gitanoland.net.TeamHudPayload;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Pantalla para colocar el cuadro de vida del equipo: arrastra el cuadro con el raton, la rueda cambia la
 * transparencia del fondo (Mayus + rueda: la de las barras), clic derecho lo deja por defecto y Esc guarda.
 */
public class TeamHudMoveScreen extends Screen {
    private static final List<TeamHudPayload.Member> SAMPLE = List.of(
            new TeamHudPayload.Member("Tu", 20f, 20f),
            new TeamHudPayload.Member("Companero", 11f, 20f),
            new TeamHudPayload.Member("Otro", 4f, 20f));

    private int x, y;
    private boolean dragging;
    private int dx, dy;

    public TeamHudMoveScreen() {
        super(Component.literal("Mover cuadro de vida"));
    }

    @Override
    protected void init() {
        x = TeamHud.clampX(ClientConfig.HUD_X.get(), minecraft);
        y = TeamHud.clampY(ClientConfig.HUD_Y.get(), minecraft, SAMPLE.size());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Sin fondo oscuro: se ve el juego (y el mapa) de fondo para colocarlo bien.
        int w = TeamHud.WIDTH, h = TeamHud.height(SAMPLE.size());
        g.fill(x - 5, y - 5, x + w + 5, y + h + 1, dragging ? 0x6055FF55 : 0x40FFFFFF);       // marco de agarre
        TeamHud.draw(g, x, y, "Equipo", SAMPLE, ClientConfig.HUD_BG_OPACITY.get(), ClientConfig.HUD_OPACITY.get());
        String t1 = "Arrastra el cuadro donde quieras";
        String t2 = "Rueda: transparencia del fondo (" + ClientConfig.HUD_BG_OPACITY.get() + " %) · Mayus + rueda: barras ("
                + ClientConfig.HUD_OPACITY.get() + " %)";
        String t3 = "Clic derecho: por defecto · Esc: guardar";
        int cx = width / 2;
        g.drawCenteredString(font, t1, cx, height / 2 - 14, 0xFFFFFF);
        g.drawCenteredString(font, t2, cx, height / 2, 0xDDDDDD);
        g.drawCenteredString(font, t3, cx, height / 2 + 14, 0xDDDDDD);
    }

    private boolean over(double mx, double my) {
        return mx >= x - 5 && mx <= x + TeamHud.WIDTH + 5 && my >= y - 5 && my <= y + TeamHud.height(SAMPLE.size()) + 1;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 1) {
            x = 6;
            y = 90;
            ClientConfig.HUD_BG_OPACITY.set(0);
            ClientConfig.HUD_OPACITY.set(75);
            return true;
        }
        if (button == 0 && over(mx, my)) {
            dragging = true;
            dx = (int) mx - x;
            dy = (int) my - y;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double ddx, double ddy) {
        if (dragging) {
            x = TeamHud.clampX((int) mx - dx, minecraft);
            y = TeamHud.clampY((int) my - dy, minecraft, SAMPLE.size());
            return true;
        }
        return super.mouseDragged(mx, my, button, ddx, ddy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int step = sy > 0 ? 5 : -5;
        if (hasShiftDown()) {
            ClientConfig.HUD_OPACITY.set(Math.max(10, Math.min(100, ClientConfig.HUD_OPACITY.get() + step)));
        } else {
            ClientConfig.HUD_BG_OPACITY.set(Math.max(0, Math.min(100, ClientConfig.HUD_BG_OPACITY.get() + step)));
        }
        return true;
    }

    @Override
    public void onClose() {
        ClientConfig.HUD_X.set(x);
        ClientConfig.HUD_Y.set(y);
        ClientConfig.SPEC.save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
