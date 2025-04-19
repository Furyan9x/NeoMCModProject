package net.furyan.riyaposmod.weight.capability;

import com.mojang.logging.LogUtils;
import net.furyan.riyaposmod.weight.EncumbranceLevel;
import net.furyan.riyaposmod.weight.WeightRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of the IPlayerWeight capability.
 * This class stores and manages a player's weight data.
 */
public class PlayerWeightImpl implements IPlayerWeight {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NBT_MAX_CAPACITY = "MaxCapacity";
    private static final String NBT_BONUSES = "CapacityBonuses";
    private EncumbranceLevel previousLevel = EncumbranceLevel.NORMAL; // Add this field if not present
    private static final String NBT_CURRENT_WEIGHT = "CurrentWeight"; // Define NBT keys
    private static final String NBT_PREVIOUS_LEVEL = "PreviousLevel";
    // Base capacity for all players
    private static final float BASE_CAPACITY = 50.0f;

    // Current weight (calculated from inventory)
    private float currentWeight = 0.0f;
    private boolean dirty = true;

    // Maximum capacity (base + bonuses)
    private float baseCapacity = BASE_CAPACITY;

    // Map of capacity bonuses by source
    private final Map<String, Float> capacityBonuses = new HashMap<>();

    // Cache for performance
    private float cachedMaxCapacity = -1;

    public PlayerWeightImpl() {

    }

    @Override
    public float getCurrentWeight() {
        return currentWeight;
    }

    @Override
    public void setCurrentWeight(float weight) {
        this.currentWeight = weight;
    }
    @Override
    public float getMaxCapacity() {
        // Use cached value if available
        if (cachedMaxCapacity >= 0) {
            return cachedMaxCapacity;
        }

        // Calculate max capacity (base + sum of all bonuses)
        float total = baseCapacity;
        for (float bonus : capacityBonuses.values()) {
            total += bonus;
        }

        // Cache the result
        cachedMaxCapacity = total;
        return total;
    }

    @Override
    public void setMaxCapacity(float capacity) {
        this.baseCapacity = capacity;
        // Invalidate cache
        this.cachedMaxCapacity = -1;
    }

    @Override
    public float addCapacityBonus(float bonus, String source) {
        capacityBonuses.put(source, bonus);
        // Invalidate cache
        this.cachedMaxCapacity = -1;
        return getMaxCapacity();
    }

    @Override
    public float removeCapacityBonus(String source) {
        capacityBonuses.remove(source);
        // Invalidate cache
        this.cachedMaxCapacity = -1;
        return getMaxCapacity();
    }

    @Override
    public float calculateWeight(Player player) {
        float weight = 0.0f;

        // Calculate weight from main inventory
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                weight += WeightRegistry.getWeight(stack.getItem()) * stack.getCount();
            }
        }

        // Store the calculated weight
        this.currentWeight = weight;
        return weight;
    }

    @Override
    public boolean isOverencumbered() {
        return currentWeight > getMaxCapacity();
    }

    @Override
    public float getOverencumbrancePercentage() {
        float maxCapacity = getMaxCapacity();
        if (maxCapacity <= 0) {
            return 0.0f;
        }
        return currentWeight / maxCapacity;
    }
    public EncumbranceLevel getPreviousEncumbranceLevel() {
        return previousLevel;
    }

    public void setPreviousEncumbranceLevel(EncumbranceLevel previousLevel) {
        this.previousLevel = previousLevel;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag nbt = new CompoundTag(); // Create a new tag

        // Save base capacity
        nbt.putFloat(NBT_MAX_CAPACITY, baseCapacity);

        // Save capacity bonuses
        CompoundTag bonusesTag = new CompoundTag();
        for (Map.Entry<String, Float> entry : capacityBonuses.entrySet()) {
            bonusesTag.putFloat(entry.getKey(), entry.getValue());
        }
        nbt.put(NBT_BONUSES, bonusesTag);

        // Save current weight (Important!)
        nbt.putFloat(NBT_CURRENT_WEIGHT, this.currentWeight);

        // Save previous level (Important!)
        nbt.putString(NBT_PREVIOUS_LEVEL, this.previousLevel.name());

        // dirty flag is transient, no need to save

        return nbt;
    }


    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        // The provider parameter is available if needed for complex deserialization

        // Clear caches before loading
        this.cachedMaxCapacity = -1;
        capacityBonuses.clear(); // Clear bonuses before loading new ones

        // Load base capacity
        if (nbt.contains(NBT_MAX_CAPACITY, CompoundTag.TAG_FLOAT)) {
            this.baseCapacity = nbt.getFloat(NBT_MAX_CAPACITY);
        } else {
            this.baseCapacity = BASE_CAPACITY; // Use default if missing
        }

        // Load capacity bonuses
        if (nbt.contains(NBT_BONUSES, CompoundTag.TAG_COMPOUND)) {
            CompoundTag bonusesTag = nbt.getCompound(NBT_BONUSES);
            for (String key : bonusesTag.getAllKeys()) {
                if (bonusesTag.contains(key, CompoundTag.TAG_FLOAT)) { // Check type
                    capacityBonuses.put(key, bonusesTag.getFloat(key));
                } else {
                    LOGGER.warn("Non-float value found in capacity bonuses NBT for key: {}", key);
                }
            }
        } // else: No bonuses saved, map remains empty (cleared above)


        // Load current weight
        if (nbt.contains(NBT_CURRENT_WEIGHT, CompoundTag.TAG_FLOAT)) {
            this.currentWeight = nbt.getFloat(NBT_CURRENT_WEIGHT);
            // If weight is loaded successfully, we assume it's accurate
            // unless inventory could have changed while offline.
            // Setting dirty = false is reasonable here, let login handler mark dirty if needed.
            this.dirty = false;
        } else {
            // If weight wasn't saved, it MUST be recalculated
            this.currentWeight = 0; // Default to 0
            this.dirty = true; // Mark as dirty to force recalculation
        }

        // Load previous level
        if (nbt.contains(NBT_PREVIOUS_LEVEL, CompoundTag.TAG_STRING)) {
            String levelName = nbt.getString(NBT_PREVIOUS_LEVEL);
            try {
                this.previousLevel = EncumbranceLevel.valueOf(levelName);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Failed to parse saved PreviousLevel '{}', defaulting to NORMAL.", levelName);
                this.previousLevel = EncumbranceLevel.NORMAL; // Default on error
            }
        } else {
            this.previousLevel = EncumbranceLevel.NORMAL; // Default if missing
        }
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void setDirty(boolean dirty) {
    this.dirty = dirty;
    }

}
