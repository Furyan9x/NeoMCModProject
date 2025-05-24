package net.furyan.riyaposmod.skills.events;

import net.furyan.riyaposmod.skills.dispatcher.SkillXPDispatcher; // Import the dispatcher
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState; // Still needed for dispatcher
import net.neoforged.neoforge.event.level.BlockEvent;
// No @SubscribeEvent here, this will be a static handler class

public class MiningEventHandler {

    // Method now returns XpGainResult
    public static SkillCapabilityEvents.XpGainResult handle(BlockEvent.BreakEvent event) {
        // The initial check for event.isCanceled() will be done in SkillCapabilityEvents
        // if (event.isCanceled()) {
        //     return null;
        // }

        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return null; // Only handle for server-side players
        }

        // TODO: Add checks for game mode (e.g., survival only)
        // if (!player.isCreative() && !player.isSpectator()) { ... }

        BlockState blockState = event.getState();
        // Call the renamed method in SkillXPDispatcher and return its result
        return SkillXPDispatcher.dispatchMiningBlockBreakXP(player, blockState);
    }
} 