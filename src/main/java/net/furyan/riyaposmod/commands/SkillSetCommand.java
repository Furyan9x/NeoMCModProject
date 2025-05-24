package net.furyan.riyaposmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.furyan.riyaposmod.skills.api.ISkillData;
import net.furyan.riyaposmod.skills.capability.SkillCapabilities;
import net.furyan.riyaposmod.skills.core.Skills;
import net.furyan.riyaposmod.skills.util.SkillConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.concurrent.CompletableFuture;
import java.util.Arrays;

public class SkillSetCommand {

    private static final String SKILL_ARG = "skillName";
    private static final String LEVEL_ARG = "level";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("skillset")
            .requires(source -> source.hasPermission(2)) // Example permission level
            .then(Commands.argument(SKILL_ARG, StringArgumentType.string())
                .suggests(SkillSetCommand::suggestSkills)
                .then(Commands.argument(LEVEL_ARG, IntegerArgumentType.integer(SkillConstants.MIN_SKILL_LEVEL, SkillConstants.MAX_SKILL_LEVEL))
                    .executes(SkillSetCommand::run)
                )
            )
        );
    }

    private static CompletableFuture<Suggestions> suggestSkills(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(Arrays.stream(Skills.values()).map(Skills::getSkillName), builder);
    }

    private static int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException(); // Ensures player is running the command

        String skillNameArg = StringArgumentType.getString(context, SKILL_ARG);
        int levelArg = IntegerArgumentType.getInteger(context, LEVEL_ARG);

        Skills foundSkill = null;
        for (Skills s : Skills.values()) {
            if (s.getSkillName().equalsIgnoreCase(skillNameArg)) {
                foundSkill = s;
                break;
            }
        }

        if (foundSkill == null) {
            source.sendFailure(Component.literal("Invalid skill name: " + skillNameArg));
            return 0;
        }
        final String finalSkillName = foundSkill.getSkillName(); // Effectively final for lambda

        ISkillData skillsData = player.getCapability(SkillCapabilities.PLAYER_SKILLS);
        if (skillsData == null) {
            source.sendFailure(Component.literal("Skill data not found for player."));
            return 0;
        }

        skillsData.setSkillLevel(finalSkillName, levelArg);
        source.sendSuccess(() -> Component.literal(String.format("Set skill %s to level %d for player %s.", 
            finalSkillName, levelArg, player.getName().getString())), true);
        
        int newLevel = skillsData.getSkillLevel(finalSkillName);
        int newExp = skillsData.getSkillExp(finalSkillName);
        source.sendSuccess(() -> Component.literal(String.format("  -> %s is now Level %d (Total XP: %d)", finalSkillName, newLevel, newExp)), false);

        return 1;
    }
} 