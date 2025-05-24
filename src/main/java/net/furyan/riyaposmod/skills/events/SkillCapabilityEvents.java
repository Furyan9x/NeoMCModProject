package net.furyan.riyaposmod.skills.events;

import net.furyan.riyaposmod.RiyaposMod;
import net.furyan.riyaposmod.network.ModNetworking;
import net.furyan.riyaposmod.network.packet.ClientboundSkillUpdatePacket;
import net.furyan.riyaposmod.skills.capability.SkillCapabilities;
import net.furyan.riyaposmod.skills.api.ISkillData;
import net.furyan.riyaposmod.skills.config.XPConfigLoader;
import net.furyan.riyaposmod.skills.util.SkillConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Handles skill-related events.
 */
@EventBusSubscriber(modid = RiyaposMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class SkillCapabilityEvents {

    /**
     * Simple record to hold the results of an XP gain action.
     */
    public record XpGainResult(String skillId, int xpGained, int newLevel, long totalXp, boolean levelledUp) {}

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        ISkillData skills = player.getCapability(SkillCapabilities.PLAYER_SKILLS);
        HolderLookup.Provider provider = player.level().registryAccess();

        if (skills != null && player.getPersistentData().contains(SkillConstants.PLAYER_SKILLS_NBT_KEY, CompoundTag.TAG_COMPOUND)) {
            CompoundTag skillsNBT = player.getPersistentData().getCompound(SkillConstants.PLAYER_SKILLS_NBT_KEY);
            skills.deserializeNBT(provider, skillsNBT);
        } else if (skills == null) {
            RiyaposMod.LOGGER.warn("Player {} does not have skill capability on login.", player.getName().getString());
        } else {
            RiyaposMod.LOGGER.info("No existing skill data found for player {}, will use defaults.", player.getName().getString());
        }
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        ISkillData skills = player.getCapability(SkillCapabilities.PLAYER_SKILLS);
        HolderLookup.Provider provider = player.level().registryAccess();

        if (skills != null) {
            CompoundTag skillsNBT = skills.serializeNBT(provider);
            player.getPersistentData().put(SkillConstants.PLAYER_SKILLS_NBT_KEY, skillsNBT);
        } else {
            RiyaposMod.LOGGER.warn("Player {} does not have skill capability on logout, cannot save skill data.", player.getName().getString());
        }
    }

    // onSleep can be removed if not used, or kept for future mechanics
    // public static void onSleep(SleepFinishedTimeEvent event) {}

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player originalPlayer = event.getOriginal();
        Player newPlayer = event.getEntity();
        HolderLookup.Provider provider = originalPlayer.level().registryAccess();

        ISkillData oldSkills = originalPlayer.getCapability(SkillCapabilities.PLAYER_SKILLS);
        if (oldSkills != null) {
            ISkillData newSkills = newPlayer.getCapability(SkillCapabilities.PLAYER_SKILLS);
            if (newSkills != null) {
                newSkills.deserializeNBT(provider, oldSkills.serializeNBT(provider));
            } else {
                RiyaposMod.LOGGER.warn("New player instance for {} does not have skill capability on clone.", newPlayer.getName().getString());
            }
        } else {
            RiyaposMod.LOGGER.warn("Original player {} does not have skill capability on clone.", originalPlayer.getName().getString());
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        Block block = event.getState().getBlock();
        XpGainResult result = null;

        // Check mining.json first
        if (XPConfigLoader.getMiningXPConfig(block).isPresent()) {
            result = MiningEventHandler.handle(event);
        } 
        // TODO: Add else if for woodcutting.json check and WoodcuttingEventHandler call
        // else if (XPConfigLoader.getWoodcuttingXPConfig(block).isPresent()) { // Assuming similar loader exists
        //     result = WoodcuttingEventHandler.handle(event); // Assuming similar handler exists
        // }

        if (result != null && player instanceof ServerPlayer serverPlayer) {
            ClientboundSkillUpdatePacket packet = new ClientboundSkillUpdatePacket(
                result.skillId(), 
                result.xpGained(), 
                result.newLevel(), 
                result.totalXp(), 
                result.levelledUp()
            );
            ModNetworking.sendToPlayer(packet, serverPlayer);
        }
    }
} 