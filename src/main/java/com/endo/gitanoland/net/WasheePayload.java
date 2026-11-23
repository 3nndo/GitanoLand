package com.endo.gitanoland.net;

import com.endo.gitanoland.GitanoLand;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Sr. Washee Washee. El cliente avisa al servidor de que ha aparecido (Appear) y el SERVIDOR escucha el chat:
 * si el jugador dice "washee washee" le da entre 30 y 120 Kirkys y le manda Bye para que se vaya.
 */
public final class WasheePayload {
    private WasheePayload() {}

    public static final int MIN = 30, MAX = 120;
    private static final Set<UUID> PENDING = new HashSet<>();

    /** Cliente -> servidor: "me ha salido Washee Washee". */
    public record Appear() implements CustomPacketPayload {
        public static final Type<Appear> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(GitanoLand.MODID, "washee_appear"));
        public static final Appear INSTANCE = new Appear();
        public static final StreamCodec<RegistryFriendlyByteBuf, Appear> CODEC = StreamCodec.unit(INSTANCE);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Servidor -> cliente: "se va"; kirkys > 0 si ademas ha cobrado. */
    public record Bye(int kirkys) implements CustomPacketPayload {
        public static final Type<Bye> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(GitanoLand.MODID, "washee_bye"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Bye> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Bye::kirkys, Bye::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void onAppear(Appear p, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer sp) PENDING.add(sp.getUUID()); });
    }

    @EventBusSubscriber(modid = GitanoLand.MODID)
    public static class Events {
        @SubscribeEvent
        public static void chat(ServerChatEvent event) {
            ServerPlayer sp = event.getPlayer();
            if (!PENDING.contains(sp.getUUID())) return;
            String m = event.getRawText().toLowerCase(Locale.ROOT).replaceAll("[^a-z ]", " ").replaceAll("\s+", " ");
            if (!m.contains("washee washee")) return;
            PENDING.remove(sp.getUUID());
            int n = MIN + sp.getRandom().nextInt(MAX - MIN + 1);
            for (int left = n; left > 0; ) {
                int c = Math.min(64, left);
                ItemStack stack = new ItemStack(GitanoLand.KIRKY.get(), c);
                if (!sp.getInventory().add(stack)) sp.drop(stack, false);
                left -= c;
            }
            PacketDistributor.sendToPlayer(sp, new Bye(n));
            sp.sendSystemMessage(Component.literal("Sr. Washee Washee: \"Gracias, toma " + n + " Kirkys.\""));
        }

        @SubscribeEvent
        public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
            PENDING.remove(event.getEntity().getUUID());
        }
    }
}
