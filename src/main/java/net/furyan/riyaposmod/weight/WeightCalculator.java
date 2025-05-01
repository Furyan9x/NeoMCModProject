package net.furyan.riyaposmod.weight;

import net.furyan.riyaposmod.weight.data.*;
import net.minecraft.world.item.ItemStack;

public class WeightCalculator {
    private static final String CONTAINER_CATEGORY = "containers";
    
    /**
     * Gets the weight of an item following the priority hierarchy:
     * 1. Per Item Overrides
     * 2. Container Item Entry
     * 3. Custom Tag Entry
     * 4. Normal Tags
     * 5. Namespace defaults
     * 6. Default weight (1.0)
     */
    public static float getWeight(ItemStack stack) {
        if (stack.isEmpty()) return 0.0f;
        return WeightDataManager.getWeight(stack);
    }
    
    /**
     * Gets the capacity bonus for an item.
     * Only container items (defined in container_items/*.json) can have capacity bonuses.
     */
    public static float getCapacityBonus(ItemStack stack) {
        if (stack.isEmpty()) return 0.0f;
        
        // Only check container entries - all container items must be registered in container_items/*.json
        ContainerItemEntry containerEntry = WeightDataManager.getContainerEntry(stack, CONTAINER_CATEGORY);
        return containerEntry.getCapacityBonus();
    }
    
    /**
     * Checks if an item is a container (has an entry in container_items/*.json)
     */
    public static boolean isContainer(ItemStack stack) {
        return !stack.isEmpty() && WeightDataManager.isContainer(stack);
    }
} 