package com.endo.gitanoland.bank;

import com.endo.gitanoland.Config;
import com.endo.gitanoland.GitanoLand;
import com.endo.gitanoland.shop.Wallet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Banco digital: guarda Kirkys en la cuenta del jugador (sin ocupar inventario). Al depositar se cobra un impuesto. */
public final class Bank {
    private static final String KEY = "gitanoland_bank";

    private Bank() {}

    public static int balance(ServerPlayer p) {
        return p.getPersistentData().getInt(KEY);
    }

    public static void setBalance(ServerPlayer p, int amount) {
        p.getPersistentData().putInt(KEY, Math.max(0, amount));
    }

    /** Impuesto de un depósito de 'amount' Kirkys (redondeado hacia arriba; mínimo 1 si hay impuesto). */
    public static int tax(int amount) {
        int pct = Config.BANK_TAX_PERCENT.get();
        if (pct <= 0 || amount <= 0) return 0;
        return Math.min(amount, Math.max(1, (int) Math.ceil(amount * pct / 100.0)));
    }

    /** Deposita 'amount' Kirkys del inventario/bolsas. Devuelve {cobrados, impuesto} o null si no tiene suficientes. */
    public static int[] deposit(ServerPlayer p, int amount) {
        if (amount <= 0 || Wallet.countKirkys(p) < amount) return null;
        int tax = tax(amount);
        Wallet.takeKirkys(p, amount);
        setBalance(p, balance(p) + (amount - tax));
        return new int[]{amount, tax};
    }

    /** Retira hasta 'amount' Kirkys a su inventario (lo que no quepa cae al suelo). Devuelve cuántos retiró. */
    public static int withdraw(ServerPlayer p, int amount) {
        int n = Math.min(amount, balance(p));
        if (n <= 0) return 0;
        setBalance(p, balance(p) - n);
        for (int left = n; left > 0; ) {
            int c = Math.min(64, left);
            ItemStack stack = new ItemStack(GitanoLand.KIRKY.get(), c);
            if (!p.getInventory().add(stack)) p.drop(stack, false);
            left -= c;
        }
        return n;
    }
}
