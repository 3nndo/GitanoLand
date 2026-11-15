package com.endo.gitanoland.bank;

import com.endo.gitanoland.Config;
import com.endo.gitanoland.GitanoLand;
import com.endo.gitanoland.shop.Wallet;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** /banco saldo | depositar <n|todo> | retirar <n|todo> */
@EventBusSubscriber(modid = GitanoLand.MODID)
public class BankCommands {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("banco")
                .requires(src -> Config.BANK_ENABLED.get())
                .executes(ctx -> balance(ctx.getSource()))
                .then(Commands.literal("saldo").executes(ctx -> balance(ctx.getSource())))
                .then(Commands.literal("depositar")
                        .then(Commands.literal("todo").executes(ctx -> deposit(ctx.getSource(), -1)))
                        .then(Commands.argument("cantidad", IntegerArgumentType.integer(1))
                                .executes(ctx -> deposit(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "cantidad")))))
                .then(Commands.literal("retirar")
                        .then(Commands.literal("todo").executes(ctx -> withdraw(ctx.getSource(), -1)))
                        .then(Commands.argument("cantidad", IntegerArgumentType.integer(1))
                                .executes(ctx -> withdraw(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "cantidad"))))));
    }

    private static int balance(CommandSourceStack src) {
        try {
            ServerPlayer p = src.getPlayerOrException();
            int pct = Config.BANK_TAX_PERCENT.get();
            src.sendSuccess(() -> Component.literal("Banco: " + Bank.balance(p) + " Kirkys. ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("Depositar cuesta un " + pct + " % de impuesto; retirar es gratis. "
                            + "/banco depositar <n|todo>, /banco retirar <n|todo>").withStyle(ChatFormatting.GRAY)), false);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int deposit(CommandSourceStack src, int amount) {
        try {
            ServerPlayer p = src.getPlayerOrException();
            int n = amount < 0 ? Wallet.countKirkys(p) : amount;
            int[] r = Bank.deposit(p, n);
            if (n <= 0 || r == null) {
                src.sendFailure(Component.literal("No tienes esos Kirkys (sueltos o en bolsas)."));
                return 0;
            }
            src.sendSuccess(() -> Component.literal("Depositados " + r[0] + " Kirkys (impuesto: " + r[1] + "). Saldo: "
                    + Bank.balance(p)).withStyle(ChatFormatting.GREEN), false);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int withdraw(CommandSourceStack src, int amount) {
        try {
            ServerPlayer p = src.getPlayerOrException();
            int n = Bank.withdraw(p, amount < 0 ? Bank.balance(p) : amount);
            if (n <= 0) {
                src.sendFailure(Component.literal("No tienes saldo en el banco."));
                return 0;
            }
            src.sendSuccess(() -> Component.literal("Retirados " + n + " Kirkys. Saldo: " + Bank.balance(p))
                    .withStyle(ChatFormatting.GREEN), false);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    /** El saldo del banco sobrevive a la muerte. */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer np) || !(event.getOriginal() instanceof ServerPlayer old)) return;
        Bank.setBalance(np, Bank.balance(old));
    }
}
