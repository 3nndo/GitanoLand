package com.endo.gitanoland.compat;

import com.endo.gitanoland.Config;
import com.endo.gitanoland.GitanoLand;
import com.endo.gitanoland.item.KirkyBagItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** Convierte un premio grande en Kirkys en bolsas (la bolsa mas pequena que quepa) en vez de muchos stacks de 64. */
public final class KirkyBags {
    private KirkyBags() {}

    /** Si el premio en Kirkys pasa de un stack (64), lo mete en bolsas; el resto de objetos queda igual. */
    public static List<ItemStack> pack(List<ItemStack> payout) {
        long kirkys = 0;
        for (ItemStack s : payout) if (!s.isEmpty() && s.is(GitanoLand.KIRKY.get())) kirkys += s.getCount();
        if (kirkys <= 64) return payout;
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack s : payout) if (s.isEmpty() || !s.is(GitanoLand.KIRKY.get())) out.add(s);
        int small = Config.BAG_CAPACITY_SMALL.get(), normal = Config.BAG_CAPACITY.get(), large = Config.BAG_CAPACITY_LARGE.get();
        long left = kirkys;
        while (left > 0) {
            ItemStack bag;
            int amount;
            if (left <= small) { bag = new ItemStack(GitanoLand.KIRKY_BAG_SMALL.get()); amount = (int) left; }
            else if (left <= normal) { bag = new ItemStack(GitanoLand.KIRKY_BAG.get()); amount = (int) left; }
            else { bag = new ItemStack(GitanoLand.KIRKY_BAG_LARGE.get()); amount = (int) Math.min(left, large); }
            KirkyBagItem.setCoins(bag, amount);
            out.add(bag);
            left -= amount;
        }
        return out;
    }
}
