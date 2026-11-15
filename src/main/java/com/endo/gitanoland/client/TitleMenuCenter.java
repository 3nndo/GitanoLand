package com.endo.gitanoland.client;

import com.endo.gitanoland.GitanoLand;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Centra el bloque de botones del menu principal en el espacio libre entre el logo y el pie de la pantalla.
 * El bloque se detecta por proximidad (filas de botones pegadas entre si, empezando por la mas alta), no por
 * porcentajes de altura, asi funciona igual con cualquier resolucion o escala de la interfaz.
 */
@EventBusSubscriber(modid = GitanoLand.MODID, value = Dist.CLIENT)
public class TitleMenuCenter {
    /** Hueco maximo (px de GUI) entre filas para considerarlas del mismo bloque. */
    private static final int MAX_GAP = 14;

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof TitleScreen screen)) return;
        int h = screen.height;

        List<AbstractWidget> widgets = new ArrayList<>();
        for (var l : event.getListenersList()) {
            if (l instanceof AbstractWidget w && w.visible && w.getY() >= h * 0.15) widgets.add(w);
        }
        if (widgets.isEmpty()) return;
        widgets.sort(Comparator.comparingInt(AbstractWidget::getY));

        // El bloque: desde el widget mas alto, hacia abajo mientras no haya un hueco grande.
        List<AbstractWidget> group = new ArrayList<>();
        int bottom = widgets.get(0).getY();
        int firstOutsideY = Integer.MAX_VALUE;
        for (AbstractWidget w : widgets) {
            if (w.getY() - bottom <= MAX_GAP) {
                group.add(w);
                bottom = Math.max(bottom, w.getY() + w.getHeight());
            } else {
                firstOutsideY = w.getY();
                break;
            }
        }
        if (group.isEmpty()) return;

        // El pie (redes, copyright) empieza donde acaba el espacio libre.
        int footerTop = Math.min((int) (h * 0.82), firstOutsideY == Integer.MAX_VALUE ? Integer.MAX_VALUE : firstOutsideY - 6);
        int delta = (footerTop - bottom) / 2;
        if (delta <= 0) return;
        for (AbstractWidget w : group) w.setY(w.getY() + delta);
    }
}
