package com.endo.gitanoland.client;

import com.endo.gitanoland.reward.RewardsMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Pantalla de recompensas: panel gris oscuro con borde verde y un botón por recompensa. */
public class RewardsScreen extends AbstractContainerScreen<RewardsMenu> {
    private static final int GREEN = 0xFF3FAE4B;
    private static final int PANEL = 0xFF2B2B2B;
    private static final int ROW = 0xFF1F1F1F;
    private Button claimPending;
    private Button claimDaily;

    public RewardsScreen(RewardsMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 112;
    }

    @Override
    protected void init() {
        super.init();
        claimPending = addRenderableWidget(Button.builder(Component.translatable("gitanoland.rewards.claim"),
                b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, RewardsMenu.BTN_PENDING))
                .bounds(leftPos + 108, topPos + 28, 58, 20).build());
        claimDaily = addRenderableWidget(Button.builder(Component.translatable("gitanoland.rewards.claim"),
                b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, RewardsMenu.BTN_DAILY))
                .bounds(leftPos + 108, topPos + 72, 58, 20).build());
    }

    @Override
    protected void containerTick() {
        claimPending.active = menu.pending() > 0;
        claimDaily.active = menu.dailyAvailable();
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        int x = leftPos, y = topPos;
        g.fill(x - 2, y - 2, x + imageWidth + 2, y + imageHeight + 2, GREEN);
        g.fill(x, y, x + imageWidth, y + imageHeight, PANEL);
        g.fill(x + 6, y + 24, x + imageWidth - 6, y + 52, ROW);
        g.fill(x + 6, y + 68, x + imageWidth - 6, y + 96, ROW);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, 8, 8, 0x6FD87A, false);
        g.drawString(font, Component.translatable("gitanoland.rewards.achievements"), 10, 29, 0xE0E0E0, false);
        g.drawString(font, Component.translatable("gitanoland.rewards.pending", menu.pending()), 10, 40, 0x9A9A9A, false);
        g.drawString(font, Component.translatable("gitanoland.rewards.daily"), 10, 73, 0xE0E0E0, false);
        Component d = menu.dailyAvailable()
                ? Component.translatable("gitanoland.rewards.pending", menu.dailyAmount())
                : Component.translatable("gitanoland.rewards.tomorrow");
        g.drawString(font, d, 10, 84, menu.dailyAvailable() ? 0x9A9A9A : 0x6A6A6A, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        renderTooltip(g, mx, my);
    }
}
