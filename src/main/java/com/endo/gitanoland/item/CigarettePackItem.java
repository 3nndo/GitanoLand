package com.endo.gitanoland.item;

import com.endo.gitanoland.GitanoLand;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

/**
 * Cajetilla de tabaco: guarda hasta 20 cigarrillos y/o porros (smokeleafindustries:joint).
 * Con la cajetilla en la mano: clic derecho saca uno; agachado + clic derecho guarda todos los del inventario.
 * En el inventario se maneja como el bundle (clic derecho con el cursor).
 */
public class CigarettePackItem extends Item {
    public static final int CAPACITY = 20;
    private static final ResourceLocation JOINT = ResourceLocation.fromNamespaceAndPath("smokeleafindustries", "joint");

    public CigarettePackItem(Properties properties) {
        super(properties);
    }

    public static boolean accepts(ItemStack s) {
        if (s.isEmpty()) return false;
        if (s.is(GitanoLand.CIGARETTE.get())) return true;
        return BuiltInRegistries.ITEM.getKey(s.getItem()).equals(JOINT);
    }

    private static List<ItemStack> read(ItemStack pack) {
        List<ItemStack> list = new ArrayList<>();
        ItemContainerContents c = pack.get(GitanoLand.PACK_CONTENTS.get());
        if (c != null) c.nonEmptyItems().forEach(s -> list.add(s.copy()));
        return list;
    }

    private static void write(ItemStack pack, List<ItemStack> list) {
        if (list.isEmpty()) pack.remove(GitanoLand.PACK_CONTENTS.get());
        else pack.set(GitanoLand.PACK_CONTENTS.get(), ItemContainerContents.fromItems(list));
    }

    public static int count(ItemStack pack) {
        return read(pack).size();
    }

    /** Guarda hasta llenar; devuelve cuantos metio. */
    private static int insert(ItemStack pack, ItemStack from) {
        if (!accepts(from)) return 0;
        List<ItemStack> list = read(pack);
        int moved = 0;
        while (list.size() < CAPACITY && !from.isEmpty()) {
            list.add(from.copyWithCount(1));
            from.shrink(1);
            moved++;
        }
        if (moved > 0) write(pack, list);
        return moved;
    }

    /** Saca el ultimo; ItemStack.EMPTY si esta vacia. */
    private static ItemStack extract(ItemStack pack) {
        List<ItemStack> list = read(pack);
        if (list.isEmpty()) return ItemStack.EMPTY;
        ItemStack out = list.remove(list.size() - 1);
        write(pack, list);
        return out;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack pack, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;
        ItemStack target = slot.getItem();
        if (target.isEmpty()) {
            ItemStack out = extract(pack);
            if (!out.isEmpty()) {
                ItemStack rest = slot.safeInsert(out);
                if (!rest.isEmpty()) insert(pack, rest);
                player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 1.0F);
            }
            return true;
        }
        if (accepts(target)) {
            if (insert(pack, target) > 0) {
                slot.setChanged();
                player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 1.0F);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack pack, ItemStack other, Slot slot, ClickAction action,
                                            Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player)) return false;
        if (other.isEmpty()) {
            ItemStack out = extract(pack);
            if (!out.isEmpty()) {
                access.set(out);
                player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 1.0F);
            }
            return true;
        }
        if (accepts(other)) {
            if (insert(pack, other) > 0) player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 1.0F);
            return true;
        }
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack pack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                int moved = 0;
                Inventory inv = player.getInventory();
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack s = inv.getItem(i);
                    if (s != pack) moved += insert(pack, s);
                }
                if (moved > 0) player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 1.0F);
            } else {
                ItemStack out = extract(pack);
                if (!out.isEmpty()) {
                    if (!player.getInventory().add(out)) player.drop(out, false);
                    player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 1.0F);
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(pack, level.isClientSide());
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return count(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.min(13, Math.round(13.0F * count(stack) / CAPACITY));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xE0A040;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        List<ItemStack> list = read(stack);
        long cigs = list.stream().filter(s -> s.is(GitanoLand.CIGARETTE.get())).count();
        tooltip.add(Component.translatable("item.gitanoland.cigarette_pack.contents", list.size(), CAPACITY).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.gitanoland.cigarette_pack.detail", cigs, list.size() - cigs).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.gitanoland.cigarette_pack.help").withStyle(ChatFormatting.DARK_GRAY));
    }
}
