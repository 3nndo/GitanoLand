package com.endo.gitanoland.client;

import com.endo.gitanoland.GitanoLand;
import com.endo.gitanoland.shop.ShopMenu;
import com.endo.gitanoland.shop.ShopOffer;
import com.endo.gitanoland.shop.Wallet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Pantalla de tienda: lista con scroll, icono del item, nombre y precio. Clic en una fila = comprar. */
public class ShopScreen extends AbstractContainerScreen<ShopMenu> {
    private static final int LIST_X = 7;
    private static final int LIST_Y = 18;
    private static final int LIST_W = 186;
    private static final int ROW_H = 24;
    private static final int VISIBLE = 5;

    /** Colores de la pantalla por tienda (ARGB; text* sin alfa). */
    private record Theme(int panel, int hi, int shadow, int listBg, int row, int rowHover, int rowNo, int slot,
                         int title, int balance) {}

    private static final Theme GUNSMITH = new Theme(0xFF3A3A3A, 0xFF5E5E5E, 0xFF1B1B1B, 0xFF1F1F1F,
            0xFF4D4D4D, 0xFF6B6B6B, 0xFF402B2B, 0xFF2B2B2B, 0xE0E0E0, 0xE8C14A);
    private static final Theme AMMO = new Theme(0xFF333333, 0xFFB02020, 0xFF5C0F0F, 0xFF1E1212,
            0xFF7A2424, 0xFFA83636, 0xFF3F3F3F, 0xFF262626, 0xFFD0D0, 0xE8C14A);
    private static final Theme WEEDS = new Theme(0xFF1F3B22, 0xFF3F7045, 0xFF0E1F10, 0xFF0F2012,
            0xFF2F5A33, 0xFF48804D, 0xFF3B3B26, 0xFF14261A, 0xD8F0D8, 0xE8C14A);

    private final Theme th;
    private final ItemStack kirkyIcon = new ItemStack(GitanoLand.KIRKY.get());
    private int scroll = 0;
    private int have = 0;

    public ShopScreen(ShopMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.th = switch (menu.theme()) {
            case "ammo_maker" -> AMMO;
            case "weedsguy" -> WEEDS;
            default -> GUNSMITH;
        };
        this.imageWidth = 200;
        this.imageHeight = 240;
    }

    // ------------------------------------------------------------------ utilidades

    private boolean canAfford(ShopOffer o) {
        if (have < o.kirkys()) return false;
        for (ShopOffer.Cost c : o.extras()) {
            if (Wallet.countItem(minecraft.player, c.item()) < c.count()) return false;
        }
        return true;
    }

    /** Índice de la oferta bajo el ratón, o -1. */
    private int rowAt(double mouseX, double mouseY) {
        double rx = mouseX - leftPos - LIST_X;
        double ry = mouseY - topPos - LIST_Y;
        if (rx < 0 || rx >= LIST_W || ry < 0 || ry >= VISIBLE * ROW_H) return -1;
        int idx = scroll + (int) (ry / ROW_H);
        return idx < menu.offers().size() ? idx : -1;
    }

    private int maxScroll() {
        return Math.max(0, menu.offers().size() - VISIBLE);
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        have = Wallet.countKirkys(minecraft.player);
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        int x = leftPos;
        int y = topPos;

        // panel
        g.fill(x, y, x + imageWidth, y + imageHeight, th.panel());
        g.fill(x, y, x + imageWidth, y + 2, th.hi());
        g.fill(x, y, x + 2, y + imageHeight, th.hi());
        g.fill(x + imageWidth - 2, y, x + imageWidth, y + imageHeight, th.shadow());
        g.fill(x, y + imageHeight - 2, x + imageWidth, y + imageHeight, th.shadow());

        // lista
        g.fill(x + LIST_X - 1, y + LIST_Y - 1, x + LIST_X + LIST_W + 1, y + LIST_Y + VISIBLE * ROW_H + 1, th.listBg());
        List<ShopOffer> offers = menu.offers();
        for (int i = 0; i < VISIBLE; i++) {
            int idx = scroll + i;
            if (idx >= offers.size()) break;
            ShopOffer o = offers.get(idx);
            int rx = x + LIST_X;
            int ry = y + LIST_Y + i * ROW_H;
            boolean hover = mouseX >= rx && mouseX < rx + LIST_W && mouseY >= ry && mouseY < ry + ROW_H;
            boolean can = canAfford(o);
            g.fill(rx, ry, rx + LIST_W, ry + ROW_H - 1, hover ? th.rowHover() : (can ? th.row() : th.rowNo()));

            g.renderItem(o.result(), rx + 3, ry + 4);
            g.renderItemDecorations(font, o.result(), rx + 3, ry + 4);
            // Dos líneas: nombre arriba (todo el ancho) y precio + materiales abajo, para que nada se solape.
            String name = font.plainSubstrByWidth(o.result().getHoverName().getString(), LIST_W - 30);
            g.drawString(font, name, rx + 24, ry + 3, 0xFFFFFF, true);

            int cx = rx + 24;
            int ty = ry + 14;
            if (o.kirkys() > 0) {
                smallItem(g, kirkyIcon, cx, ty - 1);
                cx += 10;
                if (o.baseKirkys() != o.kirkys()) {
                    // Como el comercio de vanilla: precio base tachado en gris y el nuevo al lado.
                    String b = String.valueOf(o.baseKirkys());
                    g.drawString(font, b, cx, ty, 0xFF888888, false);
                    g.fill(cx - 1, ty + 4, cx + font.width(b), ty + 5, 0xFF888888);
                    cx += font.width(b) + 3;
                }
                String t = String.valueOf(o.kirkys());
                g.drawString(font, t, cx, ty, have >= o.kirkys() ? 0xFFFFFF : 0xFF5555, true);
                cx += font.width(t) + 8;
            }
            for (ShopOffer.Cost c : o.extras()) {
                smallItem(g, new ItemStack(c.item()), cx, ty - 1);
                String t = String.valueOf(c.count());
                boolean enough = Wallet.countItem(minecraft.player, c.item()) >= c.count();
                g.drawString(font, t, cx + 10, ty, enough ? 0xFFFFFF : 0xFF5555, true);
                cx += 10 + font.width(t) + 8;
            }
        }

        // barra de scroll
        if (offers.size() > VISIBLE) {
            int trackH = VISIBLE * ROW_H;
            int barH = Math.max(10, trackH * VISIBLE / offers.size());
            int barY = y + LIST_Y + (trackH - barH) * scroll / maxScroll();
            g.fill(x + LIST_X + LIST_W + 2, y + LIST_Y, x + LIST_X + LIST_W + 4, y + LIST_Y + trackH, 0xFF555555);
            g.fill(x + LIST_X + LIST_W + 2, barY, x + LIST_X + LIST_W + 4, barY + barH, 0xFFFFFFFF);
        }

        // marcos de los slots del inventario
        for (Slot s : menu.slots) {
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, th.slot());
        }
    }

    /** Un item a la mitad de tamaño (iconos de precio y materiales). */
    private static void smallItem(GuiGraphics g, ItemStack s, int x, int y) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.5f, 0.5f, 1f);
        g.renderItem(s, 0, 0);
        g.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Título arriba; el saldo en su propia línea debajo de la lista, así no se pisan.
        String t = font.plainSubstrByWidth(title.getString(), imageWidth - 16);
        g.drawString(font, t, 8, 6, th.title(), false);
        String balance = Component.translatable("gitanoland.shop.balance", have).getString();
        g.drawString(font, balance, imageWidth - 8 - font.width(balance), LIST_Y + VISIBLE * ROW_H + 5, th.balance(), false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int idx = rowAt(mouseX, mouseY);
        if (idx >= 0) {
            ShopOffer o = menu.offers().get(idx);
            List<Component> tip = new ArrayList<>(Screen.getTooltipFromItem(minecraft, o.result()));
            tip.add(Component.empty());
            tip.add(Component.translatable("gitanoland.shop.price").withStyle(ChatFormatting.GRAY));
            if (o.marked()) {
                tip.add(Component.literal(String.valueOf(o.baseKirkys()))
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.STRIKETHROUGH));
            }
            if (o.kirkys() > 0) {
                tip.add(Component.translatable("gitanoland.shop.kirkys", o.kirkys(), have)
                        .withStyle(have >= o.kirkys() ? ChatFormatting.GREEN : ChatFormatting.RED));
            }
            for (ShopOffer.Cost c : o.extras()) {
                int h = Wallet.countItem(minecraft.player, c.item());
                tip.add(Component.literal(c.count() + "x ").append(c.item().getDescription())
                        .append(Component.literal(" (" + h + ")"))
                        .withStyle(h >= c.count() ? ChatFormatting.GREEN : ChatFormatting.RED));
            }
            tip.add(Component.translatable("gitanoland.shop.buy").withStyle(ChatFormatting.YELLOW));
            g.renderComponentTooltip(font, tip, mouseX, mouseY);
        }
    }

    // ------------------------------------------------------------------ entrada

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int idx = rowAt(mouseX, mouseY);
        if (idx >= 0 && button == 0) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, idx);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(scrollY)));
        return true;
    }
}
