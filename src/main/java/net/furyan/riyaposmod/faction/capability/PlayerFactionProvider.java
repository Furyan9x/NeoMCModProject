package net.furyan.riyaposmod.faction.capability;

import net.furyan.riyaposmod.network.ModNetworking;
import net.furyan.riyaposmod.registries.FactionAttachmentRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/**
 * Provider for the player faction data.
 * This class provides utility methods for accessing player faction data.
 * It also handles synchronization of faction data between server and client.
 */
public class PlayerFactionProvider {

    /**
     * Gets the faction data for a player.
     *
     * @param player The player
     * @return The player's faction data
     */
    public static IPlayerFaction getPlayerFaction(Player player) {
        return player.getData(FactionAttachmentRegistry.PLAYER_FACTION_ATTACHMENT.value());
    }

    /**
     * Gets the faction ID for a player.
     *
     * @param player The player
     * @return An Optional containing the faction ID, or empty if the player has no faction
     */
    public static Optional<String> getPlayerFactionId(Player player) {
        return getPlayerFaction(player).getFactionId();
    }

    /**
     * Sets the faction for a player.
     *
     * @param player The player
     * @param factionId The faction ID to set, or null to clear the faction
     * @return True if the faction was set successfully, false otherwise
     */
    public static boolean setPlayerFaction(Player player, String factionId) {
        boolean result = getPlayerFaction(player).setFaction(factionId);

        // Sync to client if on server side
        if (result && player instanceof ServerPlayer serverPlayer) {
            syncToClient(serverPlayer);
        }

        return result;
    }

    /**
     * Clears the faction for a player.
     *
     * @param player The player
     */
    public static void clearPlayerFaction(Player player) {
        getPlayerFaction(player).clearFaction();

        // Sync to client if on server side
        if (player instanceof ServerPlayer serverPlayer) {
            syncToClient(serverPlayer);
        }
    }

    /**
     * Gets the player's reputation with a faction.
     *
     * @param player The player
     * @param factionId The faction ID
     * @return The player's reputation with the faction
     */
    public static int getReputation(Player player, String factionId) {
        return getPlayerFaction(player).getReputation(factionId);
    }

    /**
     * Sets the player's reputation with a faction.
     *
     * @param player The player
     * @param factionId The faction ID
     * @param reputation The reputation value to set
     */
    public static void setReputation(Player player, String factionId, int reputation) {
        getPlayerFaction(player).setReputation(factionId, reputation);

        // Sync to client if on server side
        if (player instanceof ServerPlayer serverPlayer) {
            syncToClient(serverPlayer);
        }
    }

    /**
     * Modifies the player's reputation with a faction.
     *
     * @param player The player
     * @param factionId The faction ID
     * @param amount The amount to modify the reputation by
     * @return The new reputation value
     */
    public static int modifyReputation(Player player, String factionId, int amount) {
        int result = getPlayerFaction(player).modifyReputation(factionId, amount);

        // Sync to client if on server side
        if (player instanceof ServerPlayer serverPlayer) {
            syncToClient(serverPlayer);
        }

        return result;
    }
    /**
     * Syncs faction data to the client.
     * This should be called whenever faction data changes on the server.
     *
     * @param player The server player to sync data for
     */
    public static void syncToClient(ServerPlayer player) {
        IPlayerFaction factionData = getPlayerFaction(player);
        ModNetworking.syncFactionDataToClient(player, factionData);
    }
}
