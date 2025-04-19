package net.furyan.riyaposmod.weight.capability;

import net.furyan.riyaposmod.weight.EncumbranceLevel;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * Interface for the player weight capability.
 * This capability tracks a player's current inventory weight and maximum capacity.
 */
public interface IPlayerWeight extends INBTSerializable<CompoundTag> {
    /**
     * Gets the player's current inventory weight.
     * 
     * @return The current weight
     */
    float getCurrentWeight();

    void setCurrentWeight(float weight);

    EncumbranceLevel getPreviousEncumbranceLevel();

    void setPreviousEncumbranceLevel(EncumbranceLevel level);

    /**
     * Gets the player's maximum weight capacity.
     * 
     * @return The maximum capacity
     */
    float getMaxCapacity();

    /**
     * Sets the player's maximum weight capacity.
     * 
     * @param capacity The new maximum capacity
     */
    void setMaxCapacity(float capacity);

    /**
     * Adds a bonus to the player's maximum capacity.
     * 
     * @param bonus The bonus to add
     * @param source The source of the bonus (for tracking)
     * @return The new maximum capacity
     */
    float addCapacityBonus(float bonus, String source);

    /**
     * Removes a capacity bonus from a specific source.
     * 
     * @param source The source of the bonus to remove
     * @return The new maximum capacity
     */
    float removeCapacityBonus(String source);

    /**
     * Calculates the current weight from the player's inventory.
     * This should be called whenever the inventory changes.
     * 
     * @param player The player to calculate weight for
     * @return The calculated weight
     */
    float calculateWeight(Player player);

    /**
     * Checks if the player is overencumbered.
     * 
     * @return True if the current weight exceeds the maximum capacity
     */
    boolean isOverencumbered();

    /**
     * Gets the overencumbrance percentage (current weight / max capacity).
     * 
     * @return The overencumbrance percentage (1.0 = at capacity, >1.0 = overencumbered)
     */
    float getOverencumbrancePercentage();

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


    boolean isDirty();

    void setDirty(boolean dirty);
}
