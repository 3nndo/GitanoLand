package com.endo.gitanoland.shop;

import com.endo.gitanoland.Config;
import com.endo.gitanoland.GitanoLand;
import com.endo.gitanoland.entity.WeedsGuyEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;

public final class Shops {
    private Shops() {}

    public static void openVillager(ServerPlayer player, Villager villager, String profession) {
        try {
            doOpenVillager(player, villager, profession);
        } catch (Exception e) {
            fail(player, profession, e);
        }
    }

    public static void openWeedsGuy(ServerPlayer player, WeedsGuyEntity trader) {
        try {
            doOpenWeedsGuy(player, trader);
        } catch (Exception e) {
            fail(player, "weedsguy", e);
        }
    }

    private static void fail(ServerPlayer player, String shop, Exception e) {
        GitanoLand.LOGGER.error("[GitanoLand] Error al abrir la tienda '" + shop + "'", e);
        player.sendSystemMessage(Component.literal("[GitanoLand] Error al abrir la tienda: " + e)
                .withStyle(ChatFormatting.RED));
    }

    private static void doOpenVillager(ServerPlayer player, Villager villager, String profession) {
        TradeTables.Table table = TradeTables.load(profession);
        long seed = villager.getUUID().getLeastSignificantBits();
        TradeTables.Result res = TradeTables.buildLeveled(table,
                Config.UNLOCK_ALL_TIERS.get() ? 5 : villager.getVillagerData().getLevel(), seed);
        warn(player, profession, res);
        int markup = Markup.get(villager);
        TradeTables.Specialty spec = TradeTables.specialty(table, seed);
        Component title = Component.translatable("gitanoland.shop.title." + profession);
        if (spec != null) {
            title = title.copy().append(" · ")
                    .append(Component.translatable("gitanoland.specialty." + profession + "." + spec.id()));
        }
        ShopMenu.open(player, title, profession, markup, Markup.apply(res.offers(), markup), villager);
    }

    private static void doOpenWeedsGuy(ServerPlayer player, WeedsGuyEntity trader) {
        TradeTables.Table table = TradeTables.load("weedsguy");
        long day = trader.level().getDayTime() / 24000L;
        long seed = trader.getUUID().getLeastSignificantBits() ^ (day * 0x9E3779B97F4A7C15L);
        TradeTables.Result res = TradeTables.buildStock(table, seed);
        warn(player, "weedsguy", res);
        int markup = Markup.get(trader);
        ShopMenu.open(player, Component.translatable("gitanoland.shop.title.weedsguy"), "weedsguy", markup,
                Markup.apply(res.offers(), markup), null);
    }

    private static void warn(ServerPlayer player, String shop, TradeTables.Result res) {
        if (res.missing().isEmpty()) return;
        GitanoLand.LOGGER.warn("[GitanoLand] {}: {} ofertas sin resolver (id no encontrado): {}", shop,
                res.missing().size(), res.missing());
        if (player.hasPermissions(2)) {
            player.sendSystemMessage(Component.translatable("gitanoland.shop.missing", res.missing().size())
                    .withStyle(ChatFormatting.YELLOW));
        }
    }
}
