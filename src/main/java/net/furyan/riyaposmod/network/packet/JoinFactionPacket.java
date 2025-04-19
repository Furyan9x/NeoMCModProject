package net.furyan.riyaposmod.network.packet;

import net.furyan.riyaposmod.RiyaposMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class JoinFactionPacket implements CustomPacketPayload {
    private final String factionId;

    public static final CustomPacketPayload.Type<JoinFactionPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RiyaposMod.MOD_ID, "join_faction"));

    public static final StreamCodec<FriendlyByteBuf, JoinFactionPacket> STREAM_CODEC =
            CustomPacketPayload.codec(JoinFactionPacket::write, JoinFactionPacket::new);

    public JoinFactionPacket(String factionId) {
        this.factionId = factionId;
    }

    public JoinFactionPacket(FriendlyByteBuf buf) {
        this.factionId = buf.readUtf();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(factionId);
    }

    public String getFactionId() {
        return factionId;
    }

    public static void handle(JoinFactionPacket packet, IPayloadContext context) {
        // Make sure we're on the server side
        context.enqueueWork(() -> {
            // Get the server player from the context
            if (context.player() instanceof ServerPlayer serverPlayer) {
                    // Process the join faction request
                    net.furyan.riyaposmod.faction.capability.PlayerFactionProvider.setPlayerFaction(
                            serverPlayer, packet.getFactionId());
                }
            });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}