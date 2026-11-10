package com.endo.gitanoland.shop;

import com.endo.gitanoland.GitanoLand;
import com.endo.gitanoland.item.KirkyBagItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Cuenta y cobra Kirkys (sueltos y dentro de bolsas) y otros items del inventario del jugador. */
public final class Wallet {
    private Wallet() {}

    public static int countKirkys(Player player) {
        int total = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(GitanoLand.KIRKY.get())) total += s.getCount();
            else if (s.getItem() instanceof KirkyBagItem) total += KirkyBagItem.getCoins(s);
        }
        return total;
    }

    /** Cobra primero los Kirkys sueltos y después los de las bolsas. Comprobar antes con countKirkys. */
    public static void takeKirkys(Player player, int amount) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && amount > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(GitanoLand.KIRKY.get())) {
                int t = Math.min(amount, s.getCount());
                s.shrink(t);
                amount -= t;
            }
        }
        for (int i = 0; i < inv.getContainerSize() && amount > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof KirkyBagItem) {
                int t = Math.min(amount, KirkyBagItem.getCoins(s));
                KirkyBagItem.setCoins(s, KirkyBagItem.getCoins(s) - t);
                amount -= t;
            }
        }
    }

    public static int countItem(Player player, Item item) {
        int total = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item)) total += s.getCount();
        }
        return total;
    }

    public static void takeItem(Player player, Item item, int amount) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && amount > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item)) {
                int t = Math.min(amount, s.getCount());
                s.shrink(t);
                amount -= t;
            }
        }
    }
}
