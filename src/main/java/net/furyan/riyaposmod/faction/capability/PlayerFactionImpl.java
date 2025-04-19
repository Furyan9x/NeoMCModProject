package net.furyan.riyaposmod.faction.capability;

import net.furyan.riyaposmod.RiyaposMod;
import net.furyan.riyaposmod.faction.FactionRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of the IPlayerFaction capability.
 * This class stores and manages a player's faction and reputation data.
 */
public class PlayerFactionImpl implements IPlayerFaction {
    private static final String NBT_FACTION = "Faction";
    private static final String NBT_REPUTATION = "Reputation";

    private String factionId;
    private final Map<String, Integer> reputationMap = new ConcurrentHashMap<>();

    // Cache fields for improved performance
    private Optional<String> cachedFactionId = null;
    private final Map<String, Integer> cachedReputationValues = new HashMap<>();

    /**
     * Creates a new PlayerFactionImpl with no faction and default reputation values.
     */
    public PlayerFactionImpl() {
        this.factionId = null;

        // Initialize default reputation values for all factions
        reputationMap.put(FactionRegistry.FACTION_ONE, 0);
        reputationMap.put(FactionRegistry.FACTION_TWO, 0);
        reputationMap.put(FactionRegistry.FACTION_THREE, 0);
        reputationMap.put(FactionRegistry.FACTION_FOUR, 0);
        reputationMap.put(FactionRegistry.FACTION_FIVE, 0);
    }

    @Override
    public Optional<String> getFactionId() {
        // Use cached value if available
        if (cachedFactionId == null) {
            cachedFactionId = Optional.ofNullable(factionId);
        }
        return cachedFactionId;
    }

    @Override
    public boolean setFaction(String factionId) {
        // Check if the faction exists
        if (factionId != null && !FactionRegistry.factionExists(factionId)) {
            return false;
        }

        this.factionId = factionId;
        // Update cache
        this.cachedFactionId = Optional.ofNullable(factionId);
        return true;
    }

    @Override
    public void clearFaction() {
        this.factionId = null;
        // Update cache
        this.cachedFactionId = Optional.empty();
    }

    @Override
    public int getReputation(String factionId) {
        // Check cache first
        if (cachedReputationValues.containsKey(factionId)) {
            return cachedReputationValues.get(factionId);
        }

        // Cache miss, get from main storage and update cache
        int value = reputationMap.getOrDefault(factionId, 0);
        cachedReputationValues.put(factionId, value);
        return value;
    }

    @Override
    public void setReputation(String factionId, int reputation) {
        if (FactionRegistry.factionExists(factionId)) {
            reputationMap.put(factionId, reputation);
            // Update cache
            cachedReputationValues.put(factionId, reputation);
        }
    }

    @Override
    public int modifyReputation(String factionId, int amount) {
        if (!FactionRegistry.factionExists(factionId)) {
            return 0;
        }

        int currentRep = getReputation(factionId);
        int newRep = currentRep + amount;
        setReputation(factionId, newRep);
        // Cache is already updated in setReputation
        return newRep;
    }

    @Override
    public Component getReputationMessage(String factionId) {
        if (!FactionRegistry.factionExists(factionId)) {
            return Component.translatable("faction." + RiyaposMod.MOD_ID + ".reputation.unknown");
        }

        int reputation = getReputation(factionId);
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

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        // The provider is not used in this implementation, but we need to accept it
        // to comply with the INBTSerializable interface
        return serializeNBT((CompoundTag) null);
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        // The provider is not used in this implementation, but we need to accept it
        // to comply with the INBTSerializable interface
        deserializeNBT(nbt);
    }

    @Override
    public CompoundTag serializeNBT() {
        return serializeNBT((CompoundTag) null);
    }

    @Override
    public CompoundTag serializeNBT(CompoundTag provider) {
        CompoundTag tag = provider != null ? provider : new CompoundTag();

        // Save faction
        if (factionId != null) {
            tag.putString(NBT_FACTION, factionId);
        }

        // Save reputation
        CompoundTag reputationTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : reputationMap.entrySet()) {
            reputationTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put(NBT_REPUTATION, reputationTag);

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        // Clear caches
        cachedFactionId = null;
        cachedReputationValues.clear();

        // Load faction
        if (nbt.contains(NBT_FACTION)) {
            factionId = nbt.getString(NBT_FACTION);

            // Validate faction
            if (!FactionRegistry.factionExists(factionId)) {
                factionId = null;
            }
        } else {
            factionId = null;
        }

        // Load reputation
        reputationMap.clear();
        if (nbt.contains(NBT_REPUTATION)) {
            CompoundTag reputationTag = nbt.getCompound(NBT_REPUTATION);

            // Initialize default reputation values for all factions
            reputationMap.put(FactionRegistry.FACTION_ONE, 0);
            reputationMap.put(FactionRegistry.FACTION_TWO, 0);
            reputationMap.put(FactionRegistry.FACTION_THREE, 0);
            reputationMap.put(FactionRegistry.FACTION_FOUR, 0);
            reputationMap.put(FactionRegistry.FACTION_FIVE, 0);

            // Override with saved values
            for (String factionId : reputationMap.keySet()) {
                if (reputationTag.contains(factionId)) {
                    reputationMap.put(factionId, reputationTag.getInt(factionId));
                }
            }
        }
    }
}
