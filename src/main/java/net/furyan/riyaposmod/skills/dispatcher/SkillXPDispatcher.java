package net.furyan.riyaposmod.skills.dispatcher;

import net.furyan.riyaposmod.RiyaposMod;
import net.furyan.riyaposmod.skills.api.ISkillData;
import net.furyan.riyaposmod.skills.capability.SkillCapabilities;
import net.furyan.riyaposmod.skills.config.MiningXPConfigEntry;
import net.furyan.riyaposmod.skills.config.XPConfigLoader;
import net.furyan.riyaposmod.skills.core.Skills;
import net.furyan.riyaposmod.skills.events.SkillCapabilityEvents; // Required for XpGainResult
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Optional;

public class SkillXPDispatcher {

    // Renamed for clarity and to avoid conflict if we add XpGainResult inside this class
    public static SkillCapabilityEvents.XpGainResult dispatchMiningBlockBreakXP(ServerPlayer player, BlockState blockState) {
        if (player == null || blockState == null) {
            return null;
        }

        Optional<MiningXPConfigEntry> configEntryOpt = XPConfigLoader.getMiningXPConfig(blockState.getBlock());

        if (configEntryOpt.isPresent()) {
            MiningXPConfigEntry configEntry = configEntryOpt.get();
            int xpToGrant = configEntry.getBaseXp();

            if (xpToGrant > 0) {
                ISkillData skills = player.getCapability(SkillCapabilities.PLAYER_SKILLS);
                if (skills != null) {
                    String skillName = Skills.MINING.getSkillName();
                    int oldLevel = skills.getSkillLevel(skillName);
                    // int oldExp = skills.getSkillExp(skillName); // Total XP before adding

                    skills.addSkillExp(skillName, xpToGrant);

                    int newLevel = skills.getSkillLevel(skillName);
                    long newTotalExp = skills.getSkillTotalExp(skillName); // Get total XP for the skill
                    boolean levelledUp = newLevel > oldLevel;
                    
                    // Logging can remain here or be moved if preferred
                    RiyaposMod.LOGGER.info(
                        String.format(
                            "Player %s mined %s. Granted %d %s XP. %s: Lvl %d -> Lvl %d (Total XP: %d)",
                            player.getName().getString(),
                            blockState.getBlock().getName().getString(),
                            xpToGrant,
                            skillName,
                            skillName, // Skill name for logging levels
                            oldLevel, 
                            newLevel, newTotalExp
                        )
                    );
                    if (levelledUp) {
                        RiyaposMod.LOGGER.info("Player {} leveled up {} to level {}!", player.getName().getString(), skillName, newLevel);
                    }
                    
                    return new SkillCapabilityEvents.XpGainResult(skillName, xpToGrant, newLevel, newTotalExp, levelledUp);

                } else {
                    RiyaposMod.LOGGER.warn("Player {} does not have PlayerSkills capability when trying to grant Mining XP.", player.getName().getString());
                }
            }
        }
        return null; // No XP granted or no config
    }
    
    // Future methods for other action types:
    // public static void dispatchCombatXP(ServerPlayer player, LivingEntity target, DamageSource source) {}
    // public static void dispatchCraftingXP(ServerPlayer player, ItemStack craftedStack, List<ItemStack> ingredients) {}
} 