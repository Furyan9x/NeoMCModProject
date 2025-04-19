package net.furyan.riyaposmod.faction.data;

import net.furyan.riyaposmod.RiyaposMod;
import net.furyan.riyaposmod.faction.FactionRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player faction data.
 * This class stores and manages faction data for all players.
 */
public class PlayerFactionData {
    // Map of player UUID to faction ID
    private static final Map<UUID, String> playerFactions = new ConcurrentHashMap<>();
    
    // Map of player UUID to faction reputation (faction ID -> reputation)
    private static final Map<UUID, Map<String, Integer>> playerReputations = new ConcurrentHashMap<>();
    
    /**
     * Initializes the player faction data system.
     * This should be called during mod initialization.
     */
    public static void init() {
        NeoForge.EVENT_BUS.register(PlayerFactionData.class);
    }
    
    /**
     * Event handler for player cloning (death and respawn).
     * Copies faction data from the old player to the new player.
     *
     * @param event The player clone event
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player oldPlayer = event.getOriginal();
        Player newPlayer = event.getEntity();
        
        // Copy faction
        getPlayerFaction(oldPlayer).ifPresent(factionId -> 
            setPlayerFaction(newPlayer, factionId)
        );
        
        // Copy reputation
        if (playerReputations.containsKey(oldPlayer.getUUID())) {
            Map<String, Integer> oldReputations = playerReputations.get(oldPlayer.getUUID());
            Map<String, Integer> newReputations = new HashMap<>(oldReputations);
            playerReputations.put(newPlayer.getUUID(), newReputations);
        }
    }
    
    /**
     * Event handler for player logging in.
     * Ensures the player has faction data.
     *
     * @param event The player logged in event
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        ensurePlayerData(event.getEntity().getUUID());
    }
    
    /**
     * Ensures that player data exists for the given player UUID.
     *
     * @param playerId The player UUID
     */
    private static void ensurePlayerData(UUID playerId) {
        if (!playerReputations.containsKey(playerId)) {
            Map<String, Integer> reputations = new HashMap<>();
            reputations.put(FactionRegistry.FACTION_ONE, 0);
            reputations.put(FactionRegistry.FACTION_TWO, 0);
            reputations.put(FactionRegistry.FACTION_THREE, 0);
            reputations.put(FactionRegistry.FACTION_FOUR, 0);
            reputations.put(FactionRegistry.FACTION_FIVE, 0);
            playerReputations.put(playerId, reputations);
        }
    }
    
    /**
     * Gets the faction ID for a player.
     *
     * @param player The player
     * @return An Optional containing the faction ID, or empty if the player has no faction
     */
    public static Optional<String> getPlayerFaction(Player player) {
        return Optional.ofNullable(playerFactions.get(player.getUUID()));
    }
    
    /**
     * Sets the faction for a player.
     *
     * @param player The player
     * @param factionId The faction ID to set, or null to clear the faction
     * @return True if the faction was set successfully, false otherwise
     */
    public static boolean setPlayerFaction(Player player, String factionId) {
        if (factionId != null && !FactionRegistry.factionExists(factionId)) {
            return false;
        }
        
        if (factionId == null) {
            playerFactions.remove(player.getUUID());
        } else {
            playerFactions.put(player.getUUID(), factionId);
        }
        
        return true;
    }
    
    /**
     * Clears the faction for a player.
     *
     * @param player The player
     */
    public static void clearPlayerFaction(Player player) {
        playerFactions.remove(player.getUUID());
    }
    
    /**
     * Gets the player's reputation with a faction.
     *
     * @param player The player
     * @param factionId The faction ID
     * @return The player's reputation with the faction
     */
    public static int getReputation(Player player, String factionId) {
        ensurePlayerData(player.getUUID());
        return playerReputations.get(player.getUUID()).getOrDefault(factionId, 0);
    }
    
    /**
     * Sets the player's reputation with a faction.
     *
     * @param player The player
     * @param factionId The faction ID
     * @param reputation The reputation value to set
     */
    public static void setReputation(Player player, String factionId, int reputation) {
        if (!FactionRegistry.factionExists(factionId)) {
            return;
        }
        
        ensurePlayerData(player.getUUID());
        playerReputations.get(player.getUUID()).put(factionId, reputation);
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
        if (!FactionRegistry.factionExists(factionId)) {
            return 0;
        }
        
        ensurePlayerData(player.getUUID());
        Map<String, Integer> reputations = playerReputations.get(player.getUUID());
        int currentRep = reputations.getOrDefault(factionId, 0);
        int newRep = currentRep + amount;
        reputations.put(factionId, newRep);
        return newRep;
    }
    
    /**
     * Gets a message describing the player's reputation with a faction.
     *
     * @param player The player
     * @param factionId The faction ID
     * @return A Component containing the reputation message
     */
    public static Component getReputationMessage(Player player, String factionId) {
        if (!FactionRegistry.factionExists(factionId)) {
            return Component.translatable("faction." + RiyaposMod.MOD_ID + ".reputation.unknown");
        }
        
        int reputation = getReputation(player, factionId);
        String reputationLevel;
        
        if (reputation >= 1000) {
            reputationLevel = "exalted";
        } else if (reputation >= 750) {
            reputationLevel = "revered";
        } else if (reputation >= 500) {
            reputationLevel = "honored";
        } else if (reputation >= 250) {
            reputationLevel = "friendly";
        } else if (reputation >= 0) {
            reputationLevel = "neutral";
        } else if (reputation >= -250) {
            reputationLevel = "unfriendly";
        } else if (reputation >= -500) {
            reputationLevel = "hostile";
        } else if (reputation >= -750) {
            reputationLevel = "hated";
        } else {
            reputationLevel = "despised";
        }
        
        return Component.translatable("faction." + RiyaposMod.MOD_ID + ".reputation." + reputationLevel);
    }
    
    /**
     * Saves player faction data to NBT.
     *
     * @param player The player
     * @param tag The CompoundTag to save to
     * @return The CompoundTag with saved data
     */
    public static CompoundTag savePlayerData(Player player, CompoundTag tag) {
        // Save faction
        Optional<String> factionId = getPlayerFaction(player);
        if (factionId.isPresent()) {
            tag.putString("Faction", factionId.get());
        }
        
        // Save reputation
        if (playerReputations.containsKey(player.getUUID())) {
            CompoundTag reputationsTag = new CompoundTag();
            Map<String, Integer> reputations = playerReputations.get(player.getUUID());
            
            for (Map.Entry<String, Integer> entry : reputations.entrySet()) {
                reputationsTag.putInt(entry.getKey(), entry.getValue());
            }
            
            tag.put("FactionReputations", reputationsTag);
        }
        
        return tag;
    }
    
    /**
     * Loads player faction data from NBT.
     *
     * @param player The player
     * @param tag The CompoundTag to load from
     */
    public static void loadPlayerData(Player player, CompoundTag tag) {
        // Load faction
        if (tag.contains("Faction")) {
            String factionId = tag.getString("Faction");
            
            // Validate faction
            if (FactionRegistry.factionExists(factionId)) {
                setPlayerFaction(player, factionId);
            }
        }
        
        // Load reputation
        if (tag.contains("FactionReputations")) {
            CompoundTag reputationsTag = tag.getCompound("FactionReputations");
            Map<String, Integer> reputations = new HashMap<>();
            
            // Initialize default reputation values
            reputations.put(FactionRegistry.FACTION_ONE, 0);
            reputations.put(FactionRegistry.FACTION_TWO, 0);
            reputations.put(FactionRegistry.FACTION_THREE, 0);
            reputations.put(FactionRegistry.FACTION_FOUR, 0);
            reputations.put(FactionRegistry.FACTION_FIVE, 0);
            
            // Override with saved values
            for (String factionId : reputationsTag.getAllKeys()) {
                if (FactionRegistry.factionExists(factionId)) {
                    reputations.put(factionId, reputationsTag.getInt(factionId));
                }
            }
            
            playerReputations.put(player.getUUID(), reputations);
        } else {
            // Ensure player has default reputation values
            ensurePlayerData(player.getUUID());
        }
    }
}