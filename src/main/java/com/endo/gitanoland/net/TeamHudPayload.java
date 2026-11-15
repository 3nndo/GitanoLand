package com.endo.gitanoland.net;

import com.endo.gitanoland.GitanoLand;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Servidor -> cliente: la vida de tu equipo de atentado (lista vacia = el atentado ha terminado y se quita el cuadro). */
public record TeamHudPayload(String team, List<Member> members) implements CustomPacketPayload {
    /** Un miembro del equipo: nombre, vida actual y vida maxima. */
    public record Member(String name, float health, float max) {}

    public static final Type<TeamHudPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GitanoLand.MODID, "team_hud"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TeamHudPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeUtf(p.team());
                buf.writeVarInt(p.members().size());
                for (Member m : p.members()) {
                    buf.writeUtf(m.name());
                    buf.writeFloat(m.health());
                    buf.writeFloat(m.max());
                }
            },
            buf -> {
                String team = buf.readUtf();
                int n = buf.readVarInt();
                List<Member> list = new ArrayList<>(n);
                for (int i = 0; i < n; i++) list.add(new Member(buf.readUtf(), buf.readFloat(), buf.readFloat()));
                return new TeamHudPayload(team, list);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
