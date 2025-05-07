package net.furyan.riyaposmod.weight.ships;

import java.util.HashMap;
import java.util.Map;

public class ShipTypeCapacity {
    private static final Map<String, Integer> SHIP_CAPACITIES = new HashMap<>();

    static {
        // Base ship types and their capacities
        SHIP_CAPACITIES.put("galley", 1000);
        SHIP_CAPACITIES.put("drakkar", 1500);
        SHIP_CAPACITIES.put("cog", 2500);
        SHIP_CAPACITIES.put("brigg", 5000);

    }

    public static int getMaxCapacity(String shipType) {
        return SHIP_CAPACITIES.getOrDefault(shipType.toLowerCase(), 0);
    }

    // Optional: expose the map for debugging or listing
    public static Map<String, Integer> getAllCapacities() {
        return SHIP_CAPACITIES;
    }
} 