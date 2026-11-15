package com.endo.gitanoland.client;

import com.endo.gitanoland.GitanoLand;
import com.github.alexthe666.citadel.client.gui.BookPage;
import com.github.alexthe666.citadel.client.gui.GuiBasicBook;
import com.github.alexthe666.citadel.client.gui.data.LinkData;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;

import java.io.Reader;

/** Pantalla del libro-guia: usa el sistema de libros de Citadel (paginas JSON en assets/gitanoland/book/guide). */
public class GuideBookScreen extends GuiBasicBook {
    private static final ResourceLocation ROOT = ResourceLocation.fromNamespaceAndPath(GitanoLand.MODID, "book/guide/root.json");

    public GuideBookScreen(ItemStack stack) {
        super(stack, Component.translatable("gitanoland.guide.title"));
    }

    public static void open(ItemStack stack) {
        Minecraft.getInstance().setScreen(new GuideBookScreen(stack));
    }

    /**
     * Citadel (1.21.1) lee "linked_page_buttons" con Gson, que falla con el ItemStack de LinkData y se traga el error:
     * la lista queda vacia y no sale ningun boton. Aqui se leen los botones del JSON de la pagina a mano
     * (campos linked_page, text, x, y, page e icon opcional) y se le pasan a Citadel ya construidos.
     */
    @Override
    protected void readInPageWidgets(BookPage page) {
        super.readInPageWidgets(page);
        links.clear();
        if (currentPageJSON == null) {
            return;
        }
        var res = Minecraft.getInstance().getResourceManager().getResource(currentPageJSON);
        if (res.isEmpty()) {
            return;
        }
        try (Reader reader = res.get().openAsReader()) {
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray arr = GsonHelper.getAsJsonArray(obj, "linked_page_buttons", new JsonArray());
            for (JsonElement el : arr) {
                JsonObject l = el.getAsJsonObject();
                links.add(new LinkData(
                        GsonHelper.getAsString(l, "linked_page"),
                        // Ya traducido: Citadel calcula el ancho del boton con este texto (con la clave salian anchos raros).
                        I18n.get(GsonHelper.getAsString(l, "text", "")),
                        GsonHelper.getAsInt(l, "x", 0),
                        GsonHelper.getAsInt(l, "y", 0),
                        GsonHelper.getAsInt(l, "page", 0),
                        icon(GsonHelper.getAsString(l, "icon", ""))));
            }
        } catch (Exception e) {
            GitanoLand.LOGGER.warn("Libro-guia: no se pudieron leer los botones de {}", currentPageJSON, e);
        }
    }

    private static ItemStack icon(String id) {
        ResourceLocation rl = id.isEmpty() ? null : ResourceLocation.tryParse(id);
        if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(BuiltInRegistries.ITEM.get(rl));
    }

    /** Para la prueba automatica (BookSelfTest). */
    public int linkCount() {
        return links.size();
    }

    @Override
    protected int getBindingColor() {
        return 0x2E6E3A;
    }

    /** Colores mas oscuros que los de Citadel para que el titulo y los botones se lean sobre el pergamino. */
    @Override
    protected int getWidgetColor() {
        return 0x7A5A2A;
    }

    @Override
    protected int getTextColor() {
        return 0x2A1D0E;
    }

    @Override
    protected int getTitleColor() {
        return 0x4A3418;
    }

    @Override
    public ResourceLocation getRootPage() {
        return ROOT;
    }

    @Override
    public String getTextFileDirectory() {
        return GitanoLand.MODID + ":book/guide/";
    }

    /** Sin fondo oscuro: se ve el juego detras, como el Animal Dictionary. */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}
}
