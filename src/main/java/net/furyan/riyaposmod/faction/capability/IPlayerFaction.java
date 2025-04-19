package net.furyan.riyaposmod.faction.capability;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.Optional;

/**
 * Interface for the player faction capability.
 * This capability stores the player's faction and reputation data.
 */
public interface IPlayerFaction extends INBTSerializable<CompoundTag> {

    /**
     * Gets the ID of the faction the player belongs to.
     *
     * @return An Optional containing the faction ID, or empty if the player has no faction
     */
    Optional<String> getFactionId();

    /**
     * Sets the player's faction.
     *
     * @param factionId The ID of the faction to set, or null to clear the faction
     * @return True if the faction was set successfully, false otherwise
     */
    boolean setFaction(String factionId);

    /**
     * Clears the player's faction.
     */
    void clearFaction();

    /**
     * Gets the player's reputation with a faction.
     *
     * @param factionId The ID of the faction to get reputation for
     * @return The player's reputation with the faction
     */
    int getReputation(String factionId);

    /**
     * Sets the player's reputation with a faction.
     *
     * @param factionId The ID of the faction to set reputation for
     * @param reputation The reputation value to set
     */
    void setReputation(String factionId, int reputation);

    /**
     * Modifies the player's reputation with a faction.
     *
     * @param factionId The ID of the faction to modify reputation for
     * @param amount The amount to modify the reputation by
     * @return The new reputation value
     */
    int modifyReputation(String factionId, int amount);

    /**
     * Gets a message describing the player's reputation with a faction.
     *
     * @param factionId The ID of the faction to get the reputation message for
     * @return A Component containing the reputation message
     */
    Component getReputationMessage(String factionId);

    /**
     * Saves the capability data to NBT with a provider.
     *
     * @param provider The HolderLookup.Provider
     * @return A CompoundTag containing the capability data
     */
    CompoundTag serializeNBT(HolderLookup.Provider provider);

    /**
     * Loads the capability data from NBT with a provider.
     *
     * @param provider The HolderLookup.Provider
     * @param nbt The CompoundTag containing the capability data
     */
    void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt);

    /**
     * Saves the capability data to NBT.
     * This is a compatibility method for the old serialization system.
     *
     * @return A CompoundTag containing the capability data
     */
    CompoundTag serializeNBT();

    /**
     * Saves the capability data to NBT with a provided tag.
     * This is a utility method for internal use.
     *
     * @param provider The CompoundTag to save to, or null to create a new one
     * @return A CompoundTag containing the capability data
     */
    CompoundTag serializeNBT(CompoundTag provider);

    /**
     * Loads the capability data from NBT.
     * This is a compatibility method for the old serialization system.
     *
     * @param nbt The CompoundTag containing the capability data
     */
    void deserializeNBT(CompoundTag nbt);
}