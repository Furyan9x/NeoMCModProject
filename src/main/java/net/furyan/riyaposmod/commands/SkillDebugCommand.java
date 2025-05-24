package net.furyan.riyaposmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.furyan.riyaposmod.skills.capability.SkillCapabilities;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class SkillDebugCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("skilldebug")
            .requires(source -> source.hasPermission(2)) // Example permission level
            .executes(SkillDebugCommand::run)
        );
    }

    private static int run(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (source.getEntity() instanceof ServerPlayer player) {
            boolean hasCap = player.getCapability(SkillCapabilities.PLAYER_SKILLS) != null;
            if (hasCap) {
                source.sendSuccess(() -> Component.literal("PlayerSkills capability is ATTACHED to player: " + player.getName().getString()), true);
            } else {
                source.sendFailure(Component.literal("PlayerSkills capability is NOT ATTACHED to player: " + player.getName().getString()));
            }
            return 1;
        } else {
            source.sendFailure(Component.literal("This command can only be run by a player."));
            return 0;
        }
    }
} 