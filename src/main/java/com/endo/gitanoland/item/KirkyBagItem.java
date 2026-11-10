package com.endo.gitanoland.item;

import com.endo.gitanoland.Config;
import com.endo.gitanoland.GitanoLand;
import java.util.List;
import java.util.function.IntSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Bolsa que guarda solo Kirkys (por defecto hasta 1000). Se maneja como el bundle de vanilla. */
public class KirkyBagItem extends Item {
    private final IntSupplier capacity;

    public KirkyBagItem(Properties properties, IntSupplier capacity) {
        super(properties);
        this.capacity = capacity;
    }

    // ---------- datos ----------
    public static int getCoins(ItemStack bag) {
        return bag.getOrDefault(GitanoLand.BAG_COINS.get(), 0);
    }

    public static void setCoins(ItemStack bag, int amount) {
        bag.set(GitanoLand.BAG_COINS.get(), Math.max(0, amount));
    }

    /** Estado visual: 0 = vacía (abierta), 0.5 = con monedas a la vista, 1 = llena (cerrada). */
    public static float fillState(ItemStack bag) {
        if (!(bag.getItem() instanceof KirkyBagItem b)) return 0f;
        int coins = getCoins(bag);
        if (coins <= 0) return 0f;
        return coins >= b.capacity() ? 1f : 0.5f;
    }

    private int capacity() {
        return capacity.getAsInt();
    }

    private static boolean isKirky(ItemStack stack) {
        return stack.is(GitanoLand.KIRKY.get());
    }

    /** Pasa Kirkys de 'from' a la bolsa hasta llenarla. Devuelve cuántos movió. */
    private int deposit(ItemStack bag, ItemStack from) {
        int moved = Math.min(capacity() - getCoins(bag), from.getCount());
        if (moved > 0) {
            setCoins(bag, getCoins(bag) + moved);
            from.shrink(moved);
        }
        return moved;
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    // ---------- clic en el inventario (clic derecho, como el bundle) ----------
    /** La bolsa está en el cursor y se hace clic derecho sobre un slot. */
    @Override
    public boolean overrideStackedOnOther(ItemStack bag, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;
        ItemStack target = slot.getItem();
        if (target.isEmpty()) {
            int out = Math.min(getCoins(bag), 64);
            if (out > 0) {
                ItemStack rest = slot.safeInsert(new ItemStack(GitanoLand.KIRKY.get(), out));
                setCoins(bag, getCoins(bag) - (out - rest.getCount()));
                player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F);
            }
            return true;
        }
        if (isKirky(target)) {
            if (deposit(bag, target) > 0) {
                slot.setChanged();
                player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F);
            }
            return true;
        }
        return false;
    }

    /** La bolsa está en un slot y se hace clic derecho con otra cosa en el cursor. */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack bag, ItemStack other, Slot slot, ClickAction action,
                                            Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player)) return false;
        if (other.isEmpty()) {
            int out = Math.min(getCoins(bag), 64);
            if (out > 0) {
                setCoins(bag, getCoins(bag) - out);
                access.set(new ItemStack(GitanoLand.KIRKY.get(), out));
                player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F);
            }
            return true;
        }
        if (isKirky(other)) {
            if (deposit(bag, other) > 0) {
                player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F);
            }
            return true;
        }
        return false;
    }

    // ---------- clic derecho con la bolsa en la mano ----------
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack bag = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                int out = Math.min(getCoins(bag), 64);
                if (out > 0) {
                    setCoins(bag, getCoins(bag) - out);
                    give(player, new ItemStack(GitanoLand.KIRKY.get(), out));
                    player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F);
                }
            } else {
                int moved = 0;
                Inventory inv = player.getInventory();
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack s = inv.getItem(i);
                    if (isKirky(s)) moved += deposit(bag, s);
                }
                if (moved > 0) player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F);
            }
        }
        return InteractionResultHolder.sidedSuccess(bag, level.isClientSide());
    }

    // ---------- barra de llenado y tooltip ----------
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getCoins(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.min(13, Math.round(13.0F * getCoins(stack) / capacity()));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x4CC24C;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("item.gitanoland.kirky_bag.contents", getCoins(stack), capacity())
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.gitanoland.kirky_bag.help").withStyle(ChatFormatting.GRAY));
    }
}
