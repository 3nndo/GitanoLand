package com.endo.gitanoland.compat;

import com.endo.gitanoland.Config;
import com.endo.gitanoland.GitanoLand;
import com.endo.gitanoland.net.TeamHudPayload;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Atentados en equipo (se puede usar en cualquier sitio):
 *   /atentado equipo [nombre] [jugador] [jugador]   crea el equipo y anuncia ATENTADO a todos
 *   /atentado chat [mensaje]                         habla solo con tu equipo (para planificar)
 *   /atentado fin                                    termina el atentado de tu equipo
 * Mientras dura, cada miembro ve en pantalla la vida de todo su equipo. Tambien vale /casino atentado ...
 */
@EventBusSubscriber(modid = GitanoLand.MODID)
public final class CasinoWatch {
    /** Un equipo de atentado en marcha. */
    private static final class Team {
        final String name;
        final List<UUID> members;
        final long expires;

        Team(String name, List<UUID> members, long expires) {
            this.name = name;
            this.members = members;
            this.expires = expires;
        }
    }

    private static final Map<UUID, Team> BY_PLAYER = new HashMap<>();
    private static final List<Team> TEAMS = new ArrayList<>();
    private static final Map<UUID, Long> LAST_BY_PLAYER = new HashMap<>();

    private CasinoWatch() {}

    // ------------------------------------------------------------------ comandos
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(build(Commands.literal("atentado")));
        event.getDispatcher().register(Commands.literal("casino").then(build(Commands.literal("atentado"))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> build(LiteralArgumentBuilder<CommandSourceStack> root) {
        return root
                .executes(ctx -> usage(ctx.getSource()))
                .then(Commands.literal("equipo")
                        .executes(ctx -> usage(ctx.getSource()))
                        .then(Commands.argument("nombre", StringArgumentType.string())
                                .executes(ctx -> create(ctx.getSource(), StringArgumentType.getString(ctx, "nombre"), ""))
                                .then(Commands.argument("jugadores", StringArgumentType.greedyString())
                                        .executes(ctx -> create(ctx.getSource(), StringArgumentType.getString(ctx, "nombre"),
                                                StringArgumentType.getString(ctx, "jugadores"))))))
                .then(Commands.literal("chat")
                        .then(Commands.argument("mensaje", StringArgumentType.greedyString())
                                .executes(ctx -> chat(ctx.getSource(), StringArgumentType.getString(ctx, "mensaje")))))
                .then(Commands.literal("fin").executes(ctx -> end(ctx.getSource())));
    }

    private static int usage(CommandSourceStack src) {
        src.sendFailure(Component.literal("Uso: /atentado equipo [nombre] [jugador] [jugador]  |  /atentado chat [mensaje]  |  /atentado fin"));
        return 0;
    }

    private static ServerPlayer player(CommandSourceStack src) {
        try {
            return src.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            return null;
        }
    }

    private static int create(CommandSourceStack src, String teamName, String players) {
        ServerPlayer p = player(src);
        if (p == null) return 0;
        if (!Config.CASINO_ATTACKS.get()) {
            p.sendSystemMessage(Component.literal("Los atentados estan desactivados en este servidor.").withStyle(ChatFormatting.RED));
            return 0;
        }
        if (BY_PLAYER.containsKey(p.getUUID())) {
            p.sendSystemMessage(Component.literal("Ya estas en un atentado. Termina el actual con /atentado fin.").withStyle(ChatFormatting.RED));
            return 0;
        }
        MinecraftServer server = p.getServer();
        long now = server.overworld().getGameTime();
        Long last = LAST_BY_PLAYER.get(p.getUUID());
        long wait = Config.CASINO_ATTACK_COOLDOWN.get() * 1200L;
        if (last != null && now - last < wait) {
            p.sendSystemMessage(Component.literal("Espera " + ((wait - (now - last)) / 1200 + 1) + " min para otro atentado.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        // miembros: quien lo escribe + los jugadores conectados que indique (los demas no se pueden meter)
        Set<UUID> ids = new LinkedHashSet<>();
        List<String> names = new ArrayList<>();
        ids.add(p.getUUID());
        names.add(p.getGameProfile().getName());
        List<String> notFound = new ArrayList<>();
        for (String n : players.trim().split("\\s+")) {
            if (n.isEmpty()) continue;
            ServerPlayer online = server.getPlayerList().getPlayerByName(n);
            if (online == null) {
                notFound.add(n);
            } else if (BY_PLAYER.containsKey(online.getUUID())) {
                p.sendSystemMessage(Component.literal(online.getGameProfile().getName() + " ya esta en otro atentado.").withStyle(ChatFormatting.RED));
            } else if (ids.add(online.getUUID())) {
                names.add(online.getGameProfile().getName());
            }
        }
        if (!notFound.isEmpty()) {
            p.sendSystemMessage(Component.literal("No estan conectados (no entran en el equipo): " + String.join(", ", notFound))
                    .withStyle(ChatFormatting.YELLOW));
        }
        Team team = new Team(teamName, new ArrayList<>(ids), now + Config.CASINO_ATTACK_DURATION.get() * 1200L);
        TEAMS.add(team);
        for (UUID id : team.members) BY_PLAYER.put(id, team);
        LAST_BY_PLAYER.put(p.getUUID(), now);

        Component title = Component.literal("ATENTADO").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
        Component subtitle = Component.literal("Equipo " + teamName + " · " + String.join(", ", names)).withStyle(ChatFormatting.GOLD);
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            sp.connection.send(new ClientboundSetTitlesAnimationPacket(10, 90, 20));
            sp.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
            sp.connection.send(new ClientboundSetTitleTextPacket(title));
            sp.playNotifySound(SoundEvents.WITHER_SPAWN, SoundSource.MASTER, 0.6f, 1.2f);
        }
        for (UUID id : team.members) {
            ServerPlayer m = server.getPlayerList().getPlayer(id);
            if (m != null) m.sendSystemMessage(Component.literal("Estas en el equipo " + teamName + ". Veras la vida de tus companeros en pantalla; "
                    + "/atentado chat [mensaje] para planificar y /atentado fin para terminar.").withStyle(ChatFormatting.GOLD));
        }
        sendHud(server, team);
        return 1;
    }

    private static int chat(CommandSourceStack src, String msg) {
        ServerPlayer p = player(src);
        if (p == null) return 0;
        Team t = BY_PLAYER.get(p.getUUID());
        if (t == null) {
            p.sendSystemMessage(Component.literal("No estas en ningun atentado.").withStyle(ChatFormatting.RED));
            return 0;
        }
        Component line = Component.literal("[" + t.name + "] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(p.getGameProfile().getName() + ": ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(msg).withStyle(ChatFormatting.WHITE));
        for (UUID id : t.members) {
            ServerPlayer m = p.getServer().getPlayerList().getPlayer(id);
            if (m != null) m.sendSystemMessage(line);
        }
        return 1;
    }

    private static int end(CommandSourceStack src) {
        ServerPlayer p = player(src);
        if (p == null) return 0;
        Team t = BY_PLAYER.get(p.getUUID());
        if (t == null) {
            p.sendSystemMessage(Component.literal("No estas en ningun atentado.").withStyle(ChatFormatting.RED));
            return 0;
        }
        disband(p.getServer(), t, "El atentado del equipo " + t.name + " ha terminado.");
        return 1;
    }

    // ------------------------------------------------------------------ vida del equipo
    private static void sendHud(MinecraftServer server, Team t) {
        List<TeamHudPayload.Member> list = new ArrayList<>();
        for (UUID id : t.members) {
            ServerPlayer m = server.getPlayerList().getPlayer(id);
            if (m != null) list.add(new TeamHudPayload.Member(m.getGameProfile().getName(), m.isAlive() ? m.getHealth() : 0f, m.getMaxHealth()));
        }
        TeamHudPayload payload = new TeamHudPayload(t.name, list);
        for (UUID id : t.members) {
            ServerPlayer m = server.getPlayerList().getPlayer(id);
            if (m != null) PacketDistributor.sendToPlayer(m, payload);
        }
    }

    private static void disband(MinecraftServer server, Team t, String message) {
        TEAMS.remove(t);
        TeamHudPayload empty = new TeamHudPayload(t.name, List.of());
        for (UUID id : t.members) {
            BY_PLAYER.remove(id);
            ServerPlayer m = server.getPlayerList().getPlayer(id);
            if (m != null) {
                PacketDistributor.sendToPlayer(m, empty);
                if (!message.isEmpty()) m.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.GOLD));
            }
        }
    }

    /** Cada segundo manda la vida del equipo; acaba solo al cumplirse el tiempo o si no queda nadie conectado. */
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (TEAMS.isEmpty() || server.getTickCount() % 20 != 0) return;
        long now = server.overworld().getGameTime();
        for (Team t : new ArrayList<>(TEAMS)) {
            boolean anyOnline = false;
            for (UUID id : t.members) if (server.getPlayerList().getPlayer(id) != null) anyOnline = true;
            if (now >= t.expires) disband(server, t, "El atentado del equipo " + t.name + " se ha acabado (tiempo).");
            else if (!anyOnline) disband(server, t, "");
            else sendHud(server, t);
        }
    }
}
