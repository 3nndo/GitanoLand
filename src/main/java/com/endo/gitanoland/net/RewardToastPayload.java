package com.endo.gitanoland.net;

import com.endo.gitanoland.GitanoLand;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Servidor -> cliente: "has ganado N Kirkys por un logro" para que el cliente muestre un toast. */
public record RewardToastPayload(int kirkys, boolean challenge) implements CustomPacketPayload {
    public static final Type<RewardToastPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GitanoLand.MODID, "reward_toast"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RewardToastPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RewardToastPayload::kirkys,
            ByteBufCodecs.BOOL, RewardToastPayload::challenge,
            RewardToastPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @EventBusSubscriber(modid = GitanoLand.MODID)
    public static class Register {
        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            var registrar = event.registrar("1").optional();
            registrar.playToClient(TYPE, CODEC,
                    (payload, ctx) -> ctx.enqueueWork(() -> com.endo.gitanoland.client.RewardToast.show(payload.kirkys(), payload.challenge())));
            registrar.playToServer(OpenRewardsPayload.TYPE, OpenRewardsPayload.CODEC, OpenRewardsPayload::handle);
        }
    }
}
