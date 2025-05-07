package net.furyan.riyaposmod.weight.aircraft;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AircraftTypeCapacity {
    private static final Map<String, Integer> AIRCRAFT_CAPACITIES = new HashMap<>();

    static {
        // Base aircraft types and their capacities
        AIRCRAFT_CAPACITIES.put("immersive_aircraft:airship", 1200);
        AIRCRAFT_CAPACITIES.put("immersive_aircraft:cargo_airship", 3500);
        AIRCRAFT_CAPACITIES.put("immersive_aircraft:warship", 2000);
        AIRCRAFT_CAPACITIES.put("immersive_aircraft:biplane", 300);
        AIRCRAFT_CAPACITIES.put("immersive_aircraft:gyrodyne", 500);
        AIRCRAFT_CAPACITIES.put("immersive_aircraft:quadrocopter", 200);
    }

    public static int getMaxCapacity(String aircraftType) {
        return AIRCRAFT_CAPACITIES.getOrDefault(aircraftType.toLowerCase(), 0);
    }

    public static void registerAircraftType(String aircraftType, int capacity) {
        AIRCRAFT_CAPACITIES.put(aircraftType.toLowerCase(), capacity);
    }

    public static Map<String, Integer> getAllCapacities() {
        return Collections.unmodifiableMap(AIRCRAFT_CAPACITIES);
    }
} 