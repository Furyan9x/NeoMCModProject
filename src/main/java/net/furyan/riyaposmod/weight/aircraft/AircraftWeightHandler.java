package net.furyan.riyaposmod.weight.aircraft;

import immersive_aircraft.entity.InventoryVehicleEntity;
import immersive_aircraft.entity.inventory.VehicleInventoryDescription;
import immersive_aircraft.entity.inventory.slots.SlotDescription;
import net.furyan.riyaposmod.weight.WeightCalculator;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AircraftWeightHandler {

    // Cache for getCapacityPercent
    private static final Map<UUID, Float> riyaposmod$cachedCapacityPercent = new HashMap<>();
    private static final Map<UUID, Long> riyaposmod$lastCapacityCalcTime = new HashMap<>();
    private static final long RIYAPOSMOD_CAPACITY_CALC_COOLDOWN_MS = 250; // Recalculate ~4 times per second

    /**
     * Calculates the total weight of all items in the aircraft's inventory.
     * This method is intended to be called by getCapacityPercent, which handles caching.
     * @param aircraft The InventoryVehicleEntity (aircraft)
     * @return The total weight of all items
     */
    private static float calculateCurrentInventoryWeightInternal(InventoryVehicleEntity aircraft) {
        float totalWeight = 0.0f;
        VehicleInventoryDescription desc = aircraft.getInventoryDescription();
        if (desc == null) {
            // This warning is important if it ever occurs, but should be rare.
            // System.err.println("[AircraftWeightHandler] ERROR: VehicleInventoryDescription is null for aircraft: " + aircraft.getId());
            return 0.0f;
        }

        Map<String, Integer> slotTypeCounts = new HashMap<>();
        // Map<String, Integer> slotTypeFilledCounts = new HashMap<>(); // No longer used in summarized logging if fully removed
        // Map<String, Float> slotTypeWeights = new HashMap<>();      // No longer used in summarized logging if fully removed

        for (SlotDescription slot : desc.getSlots()) {
            // String type = slot.type() != null ? slot.type() : "unknown"; // Needed if detailed summary logging is re-enabled
            // slotTypeCounts.put(type, slotTypeCounts.getOrDefault(type, 0) + 1);

            ItemStack stack = aircraft.getInventory().getItem(slot.index());
            if (!stack.isEmpty()) {
                // slotTypeFilledCounts.put(type, slotTypeFilledCounts.getOrDefault(type, 0) + 1);
                float itemWeight = WeightCalculator.getWeight(stack) * stack.getCount();
                totalWeight += itemWeight;
                // slotTypeWeights.put(type, slotTypeWeights.getOrDefault(type, 0.0f) + itemWeight);
            }
        }
        // Verbose summary logging commented out by default to reduce spam.
        // Can be re-enabled with the maps above if specific debugging of internal calculation is needed.
        /*
        System.out.println("[AircraftWeightHandler] --- Recalculated Inventory Weight Summary for: " + aircraft.getEncodeId() + " ---");
        slotTypeCounts.forEach((type, count) -> {
            int filledCount = slotTypeFilledCounts.getOrDefault(type, 0);
            float weightInType = slotTypeWeights.getOrDefault(type, 0.0f);
            System.out.printf("[AircraftWeightHandler] SlotType: %s, TotalSlots: %d, FilledSlots: %d, WeightInType: %.2f%n",
                    type, count, filledCount, weightInType);
        });
        System.out.printf("[AircraftWeightHandler] Total Recalculated Weight for %s: %.2f%n", aircraft.getEncodeId(), totalWeight);
        */
        return totalWeight;
    }

    /**
     * Gets the aircraft's current inventory weight. 
     * This is now a public accessor that should primarily be used by getCapacityPercent or specific debug scenarios.
     */
    public static float getCurrentInventoryWeight(InventoryVehicleEntity aircraft) {
        return calculateCurrentInventoryWeightInternal(aircraft);
    }

    /**
     * Checks if the aircraft's inventory exceeds its max capacity.
     * This method now includes caching to avoid frequent recalculations.
     * @param aircraftType The aircraft type string (e.g., "immersive_aircraft:airship")
     * @param aircraft The InventoryVehicleEntity which should also implement AircraftUuidAccessor
     * @return The percent of capacity used (0.0-1.0+)
     */
    public static float getCapacityPercent(String aircraftType, InventoryVehicleEntity aircraft) {
        String side = aircraft.level().isClientSide() ? "CLIENT" : "SERVER";

        if (!(aircraft instanceof AircraftUuidAccessor uuidAccessor)) {
            System.err.printf("[AircraftWeightHandler/%s] ERROR: Aircraft %s does not implement AircraftUuidAccessor. Calculating without cache.%n", side, aircraft.getId());
            float currentWeightNoCache = calculateCurrentInventoryWeightInternal(aircraft);
            int maxCapacityNoCache = AircraftTypeCapacity.getMaxCapacity(aircraftType);
            if (maxCapacityNoCache <= 0) return 0.0f;
            float percentUsedNoCache = currentWeightNoCache / maxCapacityNoCache;
            if (!aircraft.level().isClientSide()) {
                AircraftWeightNotifier.notifyThresholdCrossed(aircraft, percentUsedNoCache);
            }
            return percentUsedNoCache;
        }

        UUID aircraftUUID = uuidAccessor.riyaposmod$getUniqueId();
        if (aircraftUUID == null) {
             // This warning is important as it signifies the UUID system isn't working as expected for this entity on this side.
             System.err.printf("[AircraftWeightHandler/%s] WARN: Aircraft UUID is null for %s. Calculating without cache.%n", side, aircraft.getId());
            float currentWeightNullId = calculateCurrentInventoryWeightInternal(aircraft);
            int maxCapacityNullId = AircraftTypeCapacity.getMaxCapacity(aircraftType);
            if (maxCapacityNullId <= 0) return 0.0f;
            float percentUsedNullId = currentWeightNullId / maxCapacityNullId;
            if (!aircraft.level().isClientSide()) {
                AircraftWeightNotifier.notifyThresholdCrossed(aircraft, percentUsedNullId);
            }
            return percentUsedNullId;
        }

        long currentTime = System.currentTimeMillis();
        Long lastCalcTime = riyaposmod$lastCapacityCalcTime.get(aircraftUUID);

        if (lastCalcTime != null && (currentTime - lastCalcTime < RIYAPOSMOD_CAPACITY_CALC_COOLDOWN_MS)) {
            return riyaposmod$cachedCapacityPercent.getOrDefault(aircraftUUID, 0.0f); // Cache hit
        }

        // Cache miss or expired, perform full calculation
        float currentWeight = calculateCurrentInventoryWeightInternal(aircraft);
        int maxCapacity = AircraftTypeCapacity.getMaxCapacity(aircraftType);
        float percentUsed;

        if (maxCapacity <= 0) {
            // System.err.printf("[AircraftWeightHandler/%s] WARN: Max capacity is 0 for %s (type: %s).%n", side, aircraft.getId(), aircraftType);
            percentUsed = 0.0f;
        } else {
            percentUsed = currentWeight / maxCapacity;
        }

        riyaposmod$cachedCapacityPercent.put(aircraftUUID, percentUsed);
        riyaposmod$lastCapacityCalcTime.put(aircraftUUID, currentTime);

        if (!aircraft.level().isClientSide()) {
            AircraftWeightNotifier.notifyThresholdCrossed(aircraft, percentUsed);
        }
        return percentUsed;
    }

    /**
     * Calculates the performance modifier based on the aircraft's weight percentage.
     * @param percentUsed The percentage of capacity used (0.0 - 1.0+)
     * @return A modifier to be applied to engine power/performance (e.g., 1.0 for normal, <1.0 for penalty)
     */
    public static float getPerformanceModifier(float percentUsed) {
        if (percentUsed > 1.0f) return 0.1f;  
        if (percentUsed > 0.9f) return 0.4f;  
        if (percentUsed > 0.75f) return 0.7f; 
        if (percentUsed > 0.5f) return 0.9f;  
        return 1.0f;                         
    }

    /**
     * Calculates the fuel consumption modifier based on the aircraft's weight percentage.
     * @param percentUsed The percentage of capacity used (0.0 - 1.0+)
     * @return A modifier to be applied to fuel consumption (e.g., 1.0 for normal, >1.0 for penalty)
     */
    public static float getFuelConsumptionModifier(float percentUsed) {
        if (percentUsed > 0.9f) return 2.0f;  
        if (percentUsed > 0.75f) return 1.5f; 
        if (percentUsed > 0.5f) return 1.25f; 
        return 1.0f;                         
    }
} 