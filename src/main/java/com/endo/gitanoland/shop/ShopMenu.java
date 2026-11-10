package com.endo.gitanoland.shop;

import com.endo.gitanoland.Config;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Menú de la tienda. El servidor manda la lista de ofertas al abrir y el cliente (ShopScreen) las dibuja;
 * comprar es un "clic de botón" estándar de vanilla, así que no hacen falta paquetes propios.
 * Incluye los slots del inventario del jugador para que los cambios (Kirkys cobrados, items entregados) se sincronicen.
 */
public class ShopMenu extends AbstractContainerMenu {
    public static final int INV_X = 8;
    public static final int INV_Y = 140;
    public static final int HOTBAR_Y = 198;

    private final String theme;
    private final int markup;
    private final List<ShopOffer> offers;
    private final Villager villager; // solo en el servidor y solo para aldeanos
    private final Player owner;

    /** Servidor y cliente. */
    private ShopMenu(int id, Inventory inv, String theme, int markup, List<ShopOffer> offers, Villager villager) {
        super(ModMenus.SHOP.get(), id);
        this.theme = theme;
        this.markup = markup;
        this.offers = offers;
        this.villager = villager;
        this.owner = inv.player;
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, INV_X + col * 18, HOTBAR_Y));
    }

    /** Cliente: lee las ofertas que mandó el servidor. */
    public ShopMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, buf.readUtf(), buf.readVarInt(), read(buf), null);
    }

    /** Abre la tienda a un jugador (servidor). */
    public static void open(ServerPlayer player, Component title, String theme, int markup,
                            List<ShopOffer> offers, Villager villager) {
        player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (id, inv, p) -> new ShopMenu(id, inv, theme, markup, offers, villager), title),
                buf -> {
                    buf.writeUtf(theme);
                    buf.writeVarInt(markup);
                    write(buf, offers);
                });
    }

    /** Tema visual de la pantalla: "ammo_maker", "gunsmith" o "weedsguy". */
    public String theme() {
        return theme;
    }

    /** Recargo actual (%) por haber pegado al aldeano. */
    public int markup() {
        return markup;
    }

    public List<ShopOffer> offers() {
        return offers;
    }

    // ------------------------------------------------------------------ red

    public static void write(RegistryFriendlyByteBuf buf, List<ShopOffer> offers) {
        buf.writeVarInt(offers.size());
        for (ShopOffer o : offers) {
            buf.writeVarInt(o.kirkys());
            buf.writeVarInt(o.baseKirkys());
            buf.writeVarInt(o.extras().size());
            for (ShopOffer.Cost c : o.extras()) {
                ItemStack.STREAM_CODEC.encode(buf, new ItemStack(c.item(), c.count()));
            }
            ItemStack.STREAM_CODEC.encode(buf, o.result());
            buf.writeVarInt(o.xp());
        }
    }

    private static List<ShopOffer> read(RegistryFriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<ShopOffer> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            int kirkys = buf.readVarInt();
            int base = buf.readVarInt();
            int extraCount = buf.readVarInt();
            List<ShopOffer.Cost> extras = new ArrayList<>();
            for (int k = 0; k < extraCount; k++) {
                ItemStack c = ItemStack.STREAM_CODEC.decode(buf);
                extras.add(new ShopOffer.Cost(c.getItem(), c.getCount()));
            }
            ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
            int xp = buf.readVarInt();
            out.add(new ShopOffer(kirkys, extras, result, xp, base));
        }
        return out;
    }

    // ------------------------------------------------------------------ compra (servidor)

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer sp) || id < 0 || id >= offers.size()) return false;
        purchase(sp, offers.get(id));
        return true;
    }

    private void purchase(ServerPlayer p, ShopOffer o) {
        boolean ok = Wallet.countKirkys(p) >= o.kirkys();
        for (ShopOffer.Cost c : o.extras()) ok &= Wallet.countItem(p, c.item()) >= c.count();
        if (!ok) {
            p.sendSystemMessage(colored(Component.translatable("gitanoland.shop.fail"), ChatFormatting.RED));
            p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1f, 1f);
            return;
        }
        Wallet.takeKirkys(p, o.kirkys());
        for (ShopOffer.Cost c : o.extras()) Wallet.takeItem(p, c.item(), c.count());

        ItemStack out = o.result().copy();
        Component name = out.getHoverName();
        if (!p.getInventory().add(out)) p.drop(out, false);
        p.sendSystemMessage(colored(Component.translatable("gitanoland.shop.bought", name), ChatFormatting.GREEN));
        p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1f, 1f);

        if (!Config.UNLOCK_ALL_TIERS.get()) addXp(o.xp());
    }

    private void addXp(int xp) {
        if (villager == null || xp <= 0) return;
        villager.setVillagerXp(villager.getVillagerXp() + xp);
        VillagerData d = villager.getVillagerData();
        boolean up = false;
        while (VillagerData.canLevelUp(d.getLevel())
                && villager.getVillagerXp() >= VillagerData.getMaxXpPerLevel(d.getLevel())) {
            d = d.setLevel(d.getLevel() + 1);
            up = true;
        }
        if (up) {
            villager.setVillagerData(d);
            owner.sendSystemMessage(colored(Component.translatable("gitanoland.shop.levelup",
                    Component.translatable("gitanoland.level." + d.getLevel())), ChatFormatting.GOLD));
        }
    }

    private static MutableComponent colored(Component c, ChatFormatting color) {
        return c.copy().withStyle(color);
    }

    // ------------------------------------------------------------------ resto del menú

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return villager == null || villager.isAlive();
    }

    /** Para la pantalla: ¿es este item un Kirky? (evita depender de GitanoLand desde aquí). */
    public static boolean isSame(ItemStack a, Item b) {
        return a.is(b);
    }
}
