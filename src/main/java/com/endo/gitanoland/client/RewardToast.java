package com.endo.gitanoland.client;

import com.endo.gitanoland.GitanoLand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Toast de Kirkys ganados por un logro: gris oscuro con borde verde (morado-verde si es de desafío). */
public class RewardToast implements Toast {
    private final ItemStack icon = new ItemStack(GitanoLand.KIRKY.get());
    private final Component title;
    private final Component line;
    private final int border;

    private RewardToast(int kirkys, boolean challenge) {
        this.title = Component.translatable(challenge ? "gitanoland.toast.title_challenge" : "gitanoland.toast.title");
        this.line = Component.translatable("gitanoland.toast.line", kirkys, ClientKeys.REWARDS.getTranslatedKeyMessage());
        this.border = challenge ? 0xFFA45BD6 : 0xFF3FAE4B;
    }

    public static void show(int kirkys, boolean challenge) {
        Minecraft.getInstance().getToasts().addToast(new RewardToast(kirkys, challenge));
    }

    @Override
    public Visibility render(GuiGraphics g, ToastComponent toasts, long visibleTime) {
        int w = width(), h = height();
        g.fill(0, 0, w, h, border);
        g.fill(1, 1, w - 1, h - 1, 0xFF2B2B2B);
        g.renderFakeItem(icon, 8, 8);
        var font = toasts.getMinecraft().font;
        g.drawString(font, title, 30, 7, 0x6FD87A, false);
        g.drawString(font, line, 30, 18, 0xE0E0E0, false);
        return visibleTime >= 5000L ? Visibility.HIDE : Visibility.SHOW;
    }
}
