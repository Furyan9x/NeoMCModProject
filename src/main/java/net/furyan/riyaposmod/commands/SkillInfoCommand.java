package net.furyan.riyaposmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.furyan.riyaposmod.skills.api.ISkillData;
import net.furyan.riyaposmod.skills.capability.SkillCapabilities;
import net.furyan.riyaposmod.skills.core.Skills;
import net.furyan.riyaposmod.skills.util.SkillConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class SkillInfoCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("skillinfo")
            .requires(source -> source.hasPermission(0)) // Allow all players
            .executes(SkillInfoCommand::run)
        );
    }

    private static int run(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be run by a player."));
            return 0;
        }

        ISkillData skillsData = player.getCapability(SkillCapabilities.PLAYER_SKILLS);
        if (skillsData == null) {
            source.sendFailure(Component.literal("Skill data not found for player."));
            return 0;
        }

        StringBuilder sb = new StringBuilder("--- Your Skills ---\n");
        for (Skills skillEnum : Skills.values()) {
            String skillName = skillEnum.getSkillName();
            int level = skillsData.getSkillLevel(skillName);
            long totalAccumulatedXp = skillsData.getSkillTotalExp(skillName);
            
            if (level >= SkillConstants.MAX_SKILL_LEVEL) {
                 sb.append(String.format("%s: Level %d (%d / MAX XP)\n", 
                    skillName, 
                    level,
                    totalAccumulatedXp
                ));
            } else {
                int xpForNextLevelActual = SkillConstants.getXpForLevel(level + 1); // Total XP needed to ding next level

                sb.append(String.format("%s: Level %d (%d / %d XP)\n", 
                    skillName, 
                    level, 
                    totalAccumulatedXp,    // Player's current total XP in this skill
                    xpForNextLevelActual // Total XP needed to reach the next level
                ));
            }
        }

        source.sendSuccess(() -> Component.literal(sb.toString()), false); // false for not broadcasting to ops
        return 1;
    }
} 