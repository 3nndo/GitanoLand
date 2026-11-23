package com.endo.gitanoland.mixin;

import com.endo.gitanoland.compat.KirkyBags;
import com.endo.gitanoland.safe.SafeEconomy;
import fr.minevegas.casino.CasinoExchange;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** MineVegas: los premios de mas de un stack de Kirkys se entregan en bolsas de Kirkys. */
@Pseudo
@Mixin(targets = "fr.minevegas.casino.CasinoExchange", remap = false)
public abstract class CasinoPayoutBagsMixin {
    @Inject(method = "payout(Lnet/minecraft/world/item/ItemStack;IIILfr/minevegas/casino/CasinoExchange$Rule;Z)Ljava/util/List;",
            at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private static void gitanoland$bags(ItemStack stake, int a, int b, int c, CasinoExchange.Rule rule, boolean d,
                                        CallbackInfoReturnable<List<ItemStack>> cir) {
        // 1) caja fuerte enlazada: la apuesta entra y los Kirkys del premio salen de ella; 2) premios grandes en bolsas
        List<ItemStack> settled = SafeEconomy.settle(stake, cir.getReturnValue());
        List<ItemStack> packed = KirkyBags.pack(settled);
        if (packed != cir.getReturnValue()) cir.setReturnValue(packed);
    }
}
