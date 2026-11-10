package com.endo.gitanoland.shop;

import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Una oferta ya resuelta: precio en Kirkys, costes extra, el item que se entrega y la XP que da al aldeano. */
public record ShopOffer(int kirkys, List<Cost> extras, ItemStack result, int xp, int baseKirkys) {
    /** Sin recargo: el precio base es el mismo. */
    public ShopOffer(int kirkys, List<Cost> extras, ItemStack result, int xp) {
        this(kirkys, extras, result, xp, kirkys);
    }

    /** ¿Está más caro de lo normal por un recargo? */
    public boolean marked() {
        return kirkys > baseKirkys;
    }

    public record Cost(Item item, int count) {}
}
