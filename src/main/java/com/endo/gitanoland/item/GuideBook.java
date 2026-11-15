package com.endo.gitanoland.item;

import com.endo.gitanoland.GitanoLand;
import net.minecraft.world.item.ItemStack;

/** El libro-guia de GitanoLand (ver GuideBookItem y assets/gitanoland/book/guide). Se da al entrar la primera vez y con /gitanoland libro. */
public final class GuideBook {
    private GuideBook() {}

    public static ItemStack create() {
        return new ItemStack(GitanoLand.GUIDE_BOOK.get());
    }
}
