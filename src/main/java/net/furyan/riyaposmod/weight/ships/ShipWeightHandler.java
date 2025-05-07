package net.furyan.riyaposmod.weight.ships;

import com.talhanation.smallships.world.entity.ship.ContainerShip;

import net.furyan.riyaposmod.weight.WeightCalculator;
import net.minecraft.world.item.ItemStack;

public class ShipWeightHandler {

    private static long lastLogTime = 0;
   private static final long LOG_INTERVAL = 5000;
    /**
     * Calculates the total weight of all items in the ship's inventory.
     * @param ship The ship's inventory (Container interface)
     * @return The total weight of all items
     */
    public static float getCurrentInventoryWeight(ContainerShip ship) {
        float totalWeight = 0.0f;
        for (int i = 0; i < ship.getContainerSize(); i++) {
            ItemStack stack = ship.getItem(i);
            if (!stack.isEmpty()) {
                totalWeight += WeightCalculator.getWeight(stack) * stack.getCount();
            }
        }
        long currentTime = System.currentTimeMillis();
       if (currentTime - lastLogTime > LOG_INTERVAL) {
           System.out.println("Total weight: " + totalWeight);
           lastLogTime = currentTime;
       }

       return totalWeight;
    }

    /**
     * Checks if the ship's inventory exceeds its max capacity.
     * @param shipType The ship type string (e.g., "oak_cog")
     * @param ship The ship's inventory
     * @return The percent of capacity used (0.0-1.0)
     */
    public static float getCapacityPercent(String shipType, ContainerShip ship) {
        int maxCapacity = ShipTypeCapacity.getMaxCapacity(shipType);
        if (maxCapacity <= 0) {
            System.out.println("[ShipWeightHandler] WARNING: maxCapacity is 0 for shipType: '" + shipType + "'");
            return 0.0f;
        }
        float currentWeight = getCurrentInventoryWeight(ship);
        long currentTime = System.currentTimeMillis();
        // Log the capacity percent only if the cooldown has expired
        if (currentTime - lastLogTime > LOG_INTERVAL) {
            System.out.println("[ShipWeightHandler] shipType: '" + shipType + "', maxCapacity: " + maxCapacity + ", currentWeight: " + currentWeight);
            lastLogTime = currentTime;
        }
        float percentUsed = currentWeight / maxCapacity;
        // Notify players if a threshold is crossed
        ShipWeightNotifier.notifyThresholdCrossed(ship, percentUsed);
        return percentUsed;
    }

    /**
     * Returns the speed modifier for the ship based on its capacity usage.
     * @param percentUsed The percent of capacity used (0.0-1.0)
     * @return The speed modifier (1.0 = normal, 0.0 = immobile)
     */
    public static float getSpeedModifier(float percentUsed) {
        if (percentUsed >= 1.01f) return 0.0f; // 101% or more, immobile
        if (percentUsed > 0.90f) return 0.6f;  // 90-100%, -40% speed
        if (percentUsed > 0.50f) return 0.8f;  // 50-90%, -20% speed
        return 1.0f;                          // 0-50%, normal speed
    }
} 