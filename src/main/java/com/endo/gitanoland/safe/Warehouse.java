package com.endo.gitanoland.safe;

import com.endo.gitanoland.GitanoLand;
import com.endo.gitanoland.item.KirkyBagItem;
import javax.annotation.Nullable;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Operaciones sobre el cofre de SecurityCraft que hace de almacen: contar, sacar y meter Kirkys (sueltos o en bolsas). */
public final class Warehouse {
    private Warehouse() {}

    /** El contenedor del cofre enlazado (cofre doble incluido), o null si el chunk no esta cargado o el cofre ya no esta. */
    @Nullable
    public static Container container(MinecraftServer server, GlobalPos gp) {
        ServerLevel level = server.getLevel(gp.dimension());
        if (level == null || !level.isLoaded(gp.pos())) return null;
        BlockEntity be = level.getBlockEntity(gp.pos());
        if (be == null || !ScLink.isWarehouse(be)) return null;
        BlockState st = level.getBlockState(gp.pos());
        if (st.getBlock() instanceof ChestBlock cb) {
            Container c = ChestBlock.getContainer(cb, st, level, gp.pos(), true);
            if (c != null) return c;
        }
        return be instanceof Container c ? c : null;
    }

    private static boolean isKirky(ItemStack s) {
        return !s.isEmpty() && s.is(GitanoLand.KIRKY.get());
    }

    public static long balance(Container c) {
        long n = 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack s = c.getItem(i);
            if (isKirky(s)) n += s.getCount();
            else if (s.getItem() instanceof KirkyBagItem) n += KirkyBagItem.getCoins(s);
        }
        return n;
    }

    /** Saca hasta n Kirkys (primero los sueltos, luego de las bolsas). Devuelve cuantos saco. */
    public static long withdraw(Container c, long n) {
        long left = n;
        for (int i = 0; i < c.getContainerSize() && left > 0; i++) {
            ItemStack s = c.getItem(i);
            if (!isKirky(s)) continue;
            int take = (int) Math.min(left, s.getCount());
            s.shrink(take);
            left -= take;
            if (s.isEmpty()) c.setItem(i, ItemStack.EMPTY);
        }
        for (int i = 0; i < c.getContainerSize() && left > 0; i++) {
            ItemStack s = c.getItem(i);
            if (!(s.getItem() instanceof KirkyBagItem)) continue;
            int coins = KirkyBagItem.getCoins(s);
            int take = (int) Math.min(left, coins);
            if (take > 0) {
                KirkyBagItem.setCoins(s, coins - take);
                left -= take;
            }
        }
        c.setChanged();
        return n - left;
    }

    /** Mete hasta n Kirkys sueltos en el cofre. Devuelve cuantos NO cupieron. */
    public static long deposit(Container c, long n) {
        long left = n;
        for (int i = 0; i < c.getContainerSize() && left > 0; i++) {
            ItemStack s = c.getItem(i);
            if (isKirky(s) && s.getCount() < s.getMaxStackSize()) {
                int add = (int) Math.min(left, s.getMaxStackSize() - s.getCount());
                s.grow(add);
                left -= add;
            }
        }
        for (int i = 0; i < c.getContainerSize() && left > 0; i++) {
            if (c.getItem(i).isEmpty()) {
                int add = (int) Math.min(left, 64);
                c.setItem(i, new ItemStack(GitanoLand.KIRKY.get(), add));
                left -= add;
            }
        }
        c.setChanged();
        return left;
    }
}
