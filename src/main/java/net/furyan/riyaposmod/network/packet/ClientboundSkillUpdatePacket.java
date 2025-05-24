package net.furyan.riyaposmod.network.packet;

import net.furyan.riyaposmod.RiyaposMod;
import net.furyan.riyaposmod.client.events.SkillDisplayTickHandler; // We will create this shortly
import net.furyan.riyaposmod.client.gui.SkillOverlayGui; // Added import
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting; // Added for colors
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientboundSkillUpdatePacket(
    String skillId,
    int xpGained, // XP gained in this specific event, not total XP in skill
    int newLevel, // Current level after this XP gain (if level up, otherwise current level)
    long totalXpForSkill, // Total XP in this skill after this gain
    boolean isLevelUp
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClientboundSkillUpdatePacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RiyaposMod.MOD_ID, "clientbound_skill_update_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSkillUpdatePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, ClientboundSkillUpdatePacket::skillId,
        ByteBufCodecs.INT, ClientboundSkillUpdatePacket::xpGained,
        ByteBufCodecs.INT, ClientboundSkillUpdatePacket::newLevel,
        ByteBufCodecs.VAR_LONG, ClientboundSkillUpdatePacket::totalXpForSkill,
        ByteBufCodecs.BOOL, ClientboundSkillUpdatePacket::isLevelUp,
        ClientboundSkillUpdatePacket::new
    );

    @Override
    public CustomPacketPayload.Type<ClientboundSkillUpdatePacket> type() {
        return TYPE;
    }

    public static void handle(ClientboundSkillUpdatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;

            if (player == null) {
                return;
            }

            if (packet.isLevelUp()) {
                RiyaposMod.LOGGER.info("Client received LEVEL UP: {} to Lvl {}! Total XP: {}", packet.skillId(), packet.newLevel(), packet.totalXpForSkill());
                
                Component title = Component.literal("Level Up!").withStyle(ChatFormatting.GREEN);
                
                Component subtitle = Component.literal("Your ")
                    .append(Component.literal(packet.skillId().substring(0, 1).toUpperCase() + packet.skillId().substring(1)).withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(" skill is now level " + packet.newLevel() + "!"));
                
                // Timings: 10 ticks fade-in, 70 ticks stay, 20 ticks fade-out
                mc.gui.setTimes(10, 70, 20); // fadein, stay, fadeout
                mc.gui.setTitle(title);
                mc.gui.setSubtitle(subtitle);

                SkillOverlayGui.triggerLevelUpFireworks(player);
            } else if (packet.xpGained() > 0) {
                RiyaposMod.LOGGER.info("Client received XP GAIN: +{} {} XP. Total XP: {}", packet.xpGained(), packet.skillId(), packet.totalXpForSkill());
                SkillDisplayTickHandler.addXpGainMessage(packet.skillId(), packet.xpGained());
            }
        });
    }
} 