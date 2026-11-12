package com.endo.gitanoland.command;

import com.endo.gitanoland.GitanoLand;
import com.tacz.guns.api.TimelessAPI;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = GitanoLand.MODID)
public class GitanoCommands {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("gitanoland")
                .then(Commands.literal("rewards").executes(ctx -> {
                    com.endo.gitanoland.reward.RewardsMenu.open(ctx.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("tacz_ids").requires(s -> s.hasPermission(2)).executes(ctx -> dumpIds(ctx.getSource())))
                .then(Commands.literal("poi").requires(s -> s.hasPermission(2)).executes(ctx -> checkPoi(ctx.getSource()))));
    }

    /** Vuelca todos los ids de TaCZ (armas, munición, accesorios) a config/gitanoland/tacz_ids.txt. */
    private static int dumpIds(CommandSourceStack src) {
        List<String> lines = new ArrayList<>();
        TimelessAPI.getAllCommonGunIndex().forEach(e ->
                lines.add("gun | " + e.getKey() + " | " + e.getValue().getPojo().getName()));
        TimelessAPI.getAllCommonAmmoIndex().forEach(e ->
                lines.add("ammo | " + e.getKey() + " | " + e.getValue().getPojo().getName()));
        TimelessAPI.getAllCommonAttachmentIndex().forEach(e ->
                lines.add("attachment | " + e.getKey() + " | " + e.getValue().getPojo().getName()));
        lines.sort(String::compareTo);
        Path file = FMLPaths.CONFIGDIR.get().resolve("gitanoland").resolve("tacz_ids.txt");
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, lines);
        } catch (IOException e) {
            src.sendFailure(Component.literal("No se pudo escribir " + file + ": " + e.getMessage()));
            return 0;
        }
        src.sendSuccess(() -> Component.literal(lines.size() + " ids escritos en " + file), false);
        return lines.size();
    }

    /** Mira un bloque y dice si es un puesto de trabajo registrado (para depurar las mesas de TaCZ). */
    private static int checkPoi(CommandSourceStack src) {
        try {
            ServerPlayer p = src.getPlayerOrException();
            HitResult hit = p.pick(6.0, 0.0F, false);
            if (!(hit instanceof BlockHitResult bhr) || hit.getType() != HitResult.Type.BLOCK) {
                src.sendFailure(Component.literal("Mira a un bloque (a menos de 6 bloques)."));
                return 0;
            }
            BlockPos pos = bhr.getBlockPos();
            String block = BuiltInRegistries.BLOCK.getKey(p.level().getBlockState(pos).getBlock()).toString();
            Optional<Holder<PoiType>> type = p.serverLevel().getPoiManager().getType(pos);
            String result = type.map(h -> h.unwrapKey().map(k -> k.location().toString()).orElse("?"))
                    .orElse("NO está registrado como puesto de trabajo (rompe el bloque y vuelve a colocarlo)");
            src.sendSuccess(() -> Component.literal(block + " -> " + result), false);
            return 1;
        } catch (CommandSyntaxException e) {
            return 0;
        }
    }
}
