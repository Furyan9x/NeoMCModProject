package net.furyan.riyaposmod.weight.capability;

import net.furyan.riyaposmod.registries.WeightAttachmentRegistry;
import net.minecraft.world.entity.player.Player;

/**
 * Provider for the player weight capability.
 * This class provides utility methods for accessing the capability.
 */
public class PlayerWeightProvider {
    /**
     * Gets the player weight capability for a player.
     * 
     * @param player The player to get the capability for
     * @return The player weight capability, or null if not found
     */
    public static IPlayerWeight getPlayerWeight(Player player) {
        if (player == null) {
            return null;
        }

        return player.getData(WeightAttachmentRegistry.PLAYER_WEIGHT_ATTACHMENT);
    }
}
