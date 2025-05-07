# Active Context

**Summary:**
The weight system for Immersive Aircraft has been significantly enhanced, now including dynamic performance penalties (reduced engine power, increased fuel consumption) and clear, color-coded player notifications for weight status. Aircraft inventories trigger messages on open, and pilots receive automatic notifications when weight thresholds are crossed. Critical performance optimizations, including calculation caching and refined UUID management for client/server entities, have been implemented, resolving previous issues with log spam and inconsistent entity identification.

## Current Work Focus
- Tuning of weight effect modifiers (engine power, fuel consumption) for balanced gameplay.
- Continued performance monitoring and optimization, especially around client-server data synchronization for weight.
- Planning and design for UI tracking of player encumbrance (icon, color, and stats display)
- Ensuring compatibility with other major mods (Create: Steam n Rails, etc.)

## Recent Changes
- **Aircraft Weight Effects Implemented:**
    - Engine power is now reduced based on aircraft weight percentage via a mixin to `EngineVehicle.getEnginePower()`.
    - Fuel consumption is increased based on aircraft weight percentage via a mixin to `EngineVehicle.getFuelConsumption()`.
    - `AircraftWeightHandler` now includes `getPerformanceModifier()` and `getFuelConsumptionModifier()` methods.
- **Aircraft Threshold Notifications:**
    - Pilots receive color-coded messages when aircraft weight crosses defined thresholds (e.g., Normal to Heavy, Heavy to Overloaded).
    - `AircraftWeightNotifier` enhanced with `notifyThresholdCrossed()` and threshold tracking per aircraft UUID.
    - Triggered server-side from `AircraftWeightHandler.getCapacityPercent()` after fresh calculations.
- **Inventory Open Notifications (Aircraft):**
    - Players opening an aircraft inventory receive a color-coded message indicating the current weight status.
    - Mixin for `InventoryVehicleEntity.openInventory` triggers this, with a cooldown to prevent message spam from rapid/internal calls.
- **Performance Optimizations & Bug Fixes:**
    - **Calculation Caching:** `AircraftWeightHandler.getCapacityPercent()` now caches its results per aircraft UUID with a configurable cooldown (250ms), drastically reducing expensive inventory weight recalculations.
    - **Log Refinement:**
        - `AircraftWeightHandler.getCurrentInventoryWeight()` logs are now summarized by slot type, reducing verbosity.
        - `EngineVehicleMixin` logs for power/fuel modifications have a 5-second cooldown per aircraft to reduce spam.
    - **UUID Management Overhaul:**
        - Diagnosed and resolved issues where aircraft appeared to have multiple UUIDs due to client/server entity distinctions.
        - UUID assignment in `InventoryVehicleEntityMixin` is now server-authoritative.
        - Client-side instances of aircraft now generate a transient UUID if the server-assigned one isn't yet available, allowing client-side caching mechanisms to function without errors and reducing uncached calculations on the client.
- **Code Structure:**
    - `AircraftWeightNotifier` handles all player messaging logic for aircraft weight.
    - `AircraftWeightHandler` centralizes weight calculation, caching, and threshold checking calls.
    - `EngineVehicleMixin` applies game-altering effects to Immersive Aircraft.
    - `InventoryVehicleEntityMixin` handles UUID persistence and inventory open events.

## Next Steps
- Thorough in-game testing and balancing of the new aircraft weight effects and thresholds.
- Further UI/UX improvements for encumbrance tracking
- Continue compatibility/integration work with other mods (Create: Steam n Rails)
- Design and implement a custom enchantment or upgrade item for increasing max capacity (ensure it works with JSON-driven system)
- Continue thorough in-game testing for edge cases and mod interactions

## Active Decisions and Considerations
- Server-authoritative UUIDs are critical for consistent entity tracking and data management. Client-side entities use transient UUIDs for local caching if necessary before server ID sync.
- Performance impact of frequent calculations (like weight) must be managed with caching, especially for methods called every tick or by multiple systems.
- Player notifications should be clear, contextual, and avoid spam. Cooldowns and distinct message types (on-open vs. on-cross-threshold) help achieve this.
- Mixins should target the most appropriate classes and methods; sometimes parent classes (like `EngineVehicle`) offer broader impact than individual child entity classes.
- Performance and optimization are the top priority before adding new features
- UI/UX for encumbrance must be clear, responsive, and update on all relevant events
- Compatibility/integration should be modular
- Custom enchantment/upgrade must not break JSON-driven capacity system

## Important Patterns and Preferences
- Centralized management of backpack, ship, and aircraft handlers
- Event-driven communication between system components
- UUID-based tracking for unique ship, aircraft, and backpack identification
- Comprehensive logging for testing and debugging
- Clear separation of responsibilities between event handlers
- Data-driven configuration for backpack capacity and upgrades
- Mixins for integrating with external mods like Small Ships and Immersive Aircraft
- Event-driven notifications and server-authoritative game logic for core mechanics.
- Caching strategies (time-based cooldowns with UUID keys) for performance-critical calculations.
- Clear separation of concerns (e.g., calculation in Handler, notification in Notifier, effects in Mixin).
- Robust UUID management, especially considering client/server entity distinctions.

## Learnings and Project Insights
- Handler registration/unregistration must be robust to all inventory changes
- Debounce/cooldown logic is essential for performance
- JSON-driven configuration enables flexible integration with other mods
- UI/UX is critical for player experience and must be prioritized after optimization
- Ship and aircraft movement in mods is complex, with multiple variables affecting speed
- Mixin injection points must be carefully chosen to avoid being overridden by downstream code
- Persistent UUIDs are essential for robust entity tracking and notification systems
- Client-side and server-side entity instances can lead to seemingly duplicate logic execution and data inconsistencies if not handled explicitly. UUIDs and state must be managed with this in mind.
- Frequent calculations tied to entity ticks (e.g., from `getEnginePower` called every tick) require aggressive caching to prevent performance degradation.
- Careful logging (with context like client/server side, UUIDs, and cooldowns) is invaluable for diagnosing complex interaction issues in a modded environment.
- The order of operations and availability of data (like NBT-synced UUIDs on the client) can significantly impact logic flow.

## Integration and Future Planning
- Small Ships integration is complete for both speed penalties and player notifications
- JSON-driven configuration is enabling flexible integration with other mods and will support future compatibility work
- Hard dependencies for Iron's Spells n Spellbooks, Sophisticated Backpacks, Small Ships, Immersive Aircraft, Curios API 
- Immersive Aircraft integration is now functionally complete regarding weight-based performance effects and player notifications. Fine-tuning and balancing are the next major steps for this specific integration.
- The robust weight system (calculation, caching, notification patterns) can serve as a template for future integrations with other vehicle/inventory mods.
- Continued focus on modularity to ensure these systems can be maintained and extended.