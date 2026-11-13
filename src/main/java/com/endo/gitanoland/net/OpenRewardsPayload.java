package com.endo.gitanoland.net;

import com.endo.gitanoland.GitanoLand;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Cliente -> servidor: "abre el menu de recompensas" (sin pasar por el chat ni por comandos). */
public record OpenRewardsPayload() implements CustomPacketPayload {
    public static final Type<OpenRewardsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GitanoLand.MODID, "open_rewards"));
    public static final OpenRewardsPayload INSTANCE = new OpenRewardsPayload();
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenRewardsPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenRewardsPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) com.endo.gitanoland.reward.RewardsMenu.open(sp);
        });
    }
}
