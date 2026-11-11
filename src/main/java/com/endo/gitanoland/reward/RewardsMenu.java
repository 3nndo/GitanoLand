package com.endo.gitanoland.reward;

import com.endo.gitanoland.Config;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/** Menú de recompensas: sin slots; dos botones (logros pendientes y diaria) con clic de botón estándar de vanilla. */
public class RewardsMenu extends AbstractContainerMenu {
    public static final int BTN_PENDING = 0;
    public static final int BTN_DAILY = 1;

    private final ContainerData data;

    /** Cliente. */
    public RewardsMenu(int id, Inventory inv) {
        this(id, inv, new SimpleContainerData(3));
    }

    private RewardsMenu(int id, Inventory inv, ContainerData data) {
        super(Rewards.type(), id);
        this.data = data;
        addDataSlots(data);
    }

    /** Kirkys pendientes de logros. */
    public int pending() { return data.get(0); }
    public boolean dailyAvailable() { return data.get(1) == 1; }
    public int dailyAmount() { return data.get(2); }

    public static void open(ServerPlayer player) {
        ContainerData live = new ContainerData() {
            @Override public int get(int i) {
                return switch (i) {
                    case 0 -> Rewards.pending(player);
                    case 1 -> Rewards.dailyAvailable(player) ? 1 : 0;
                    default -> Config.DAILY_KIRKYS.get();
                };
            }
            @Override public void set(int i, int v) {}
            @Override public int getCount() { return 3; }
        };
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new RewardsMenu(id, inv, live),
                Component.translatable("gitanoland.rewards.title")), buf -> {});
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer sp)) return false;
        if (id == BTN_PENDING) Rewards.claimPending(sp);
        else if (id == BTN_DAILY) Rewards.claimDaily(sp);
        else return false;
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) { return true; }
}
