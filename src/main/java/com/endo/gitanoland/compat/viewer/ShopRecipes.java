package com.endo.gitanoland.compat.viewer;

import com.endo.gitanoland.GitanoLand;
import com.endo.gitanoland.shop.ShopOffer;
import com.endo.gitanoland.shop.TradeTables;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;

/** Los tratos de las tres tiendas como "recetas" para REI y JEI (sin depender de ninguno de los dos). */
public final class ShopRecipes {
    private ShopRecipes() {}

    /** Un trato: tienda que lo vende y la oferta (Kirkys + materiales -> objeto). */
    public record Entry(String shopId, Component shop, ShopOffer offer) {}

    public static List<Entry> all() {
        List<Entry> out = new ArrayList<>();
        for (String shop : new String[]{"ammo_maker", "gunsmith"}) {
            try {
                TradeTables.Table table = TradeTables.load(shop);
                // nivel 5 = todos los tratos; la semilla fija solo afecta a los "con suerte"
                add(out, shop, TradeTables.buildLeveled(table, 5, 0L).offers());
            } catch (Exception e) {
                GitanoLand.LOGGER.warn("[GitanoLand] No se pudieron listar los tratos de '{}' para REI/JEI", shop, e);
            }
        }
        try {
            TradeTables.Table table = TradeTables.load("weedsguy");
            add(out, "weedsguy", TradeTables.buildStock(table, 0L).offers());
        } catch (Exception e) {
            GitanoLand.LOGGER.warn("[GitanoLand] No se pudieron listar los tratos de 'weedsguy' para REI/JEI", e);
        }
        return out;
    }

    private static void add(List<Entry> out, String shop, List<ShopOffer> offers) {
        Component name = Component.translatable("gitanoland.shop.title." + shop);
        for (ShopOffer o : offers) {
            if (!o.result().isEmpty()) out.add(new Entry(shop, name, o));
        }
    }
}
