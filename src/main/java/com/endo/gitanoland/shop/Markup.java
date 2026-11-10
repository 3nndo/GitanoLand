package com.endo.gitanoland.shop;

import com.endo.gitanoland.Config;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

/** Recargo de precios de un aldeano-tienda por haberle pegado. Va en sus datos persistentes; nunca baja de 0. */
public final class Markup {
    private static final String KEY = "gitanoland_markup";
    private static final String DAY = "gitanoland_markup_day";

    private Markup() {}

    /** Recargo actual en %, ya aplicado el paso de los días. */
    public static int get(Entity e) {
        CompoundTag t = e.getPersistentData();
        int m = t.getInt(KEY);
        if (m <= 0) return 0;
        long day = e.level().getDayTime() / 24000L;
        long days = Math.max(0L, day - t.getLong(DAY));
        long dec = days * Config.HIT_MARKUP_DECAY.get();
        m = (int) Math.max(0L, m - dec);
        t.putInt(KEY, m);
        t.putLong(DAY, day);
        return m;
    }

    /** Sube el recargo un escalón (hasta el máximo) y devuelve el nuevo valor. */
    public static int bump(Entity e) {
        int m = Math.min(Config.HIT_MARKUP_MAX.get(), get(e) + Config.HIT_MARKUP_STEP.get());
        CompoundTag t = e.getPersistentData();
        t.putInt(KEY, m);
        t.putLong(DAY, e.level().getDayTime() / 24000L);
        return m;
    }

    /** Sube los Kirkys de cada oferta según el recargo. Con recargo 0 no cambia nada; nunca resulta más barato. */
    public static List<ShopOffer> apply(List<ShopOffer> offers, int pct) {
        if (pct <= 0) return offers;
        List<ShopOffer> out = new ArrayList<>(offers.size());
        for (ShopOffer o : offers) {
            int k = o.kirkys();
            if (k > 0) k = Math.max(k + 1, (int) Math.ceil(k * (100 + pct) / 100.0));
            out.add(new ShopOffer(k, o.extras(), o.result(), o.xp(), o.kirkys()));
        }
        return out;
    }
}
