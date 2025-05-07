# Progress Tracking

## What Works
- Project setup and basic structure established
- Core mod registration and initialization
- Weight system capability framework (functionally complete and stable)
- Sophisticated Backpacks integration for capacity bonus via JSON tags
- Item tooltips for weight and capacity bonus
- Handler registration and deregistration for backpacks is robust
- Debounce/cooldown system prevents weight calculation spam
- Backpack movement between inventory and curio slots maintains functionality
- Basic networking infrastructure
- Custom item registration
- Ship weight system applies speed penalties based on inventory contents
- Persistent UUIDs for all ContainerShip entities
- Threshold-based notifications sent to all riders when ship crosses weight thresholds
- Current threshold message sent to player when ship inventory is opened
- System robustly distinguishes between ships of the same type
- Backpack weight system is stable and performant
- JSON-driven configuration for capacity and upgrades
- Immersive Aircraft weight system is now implemented and functional, including inventory, upgrade, weapon, and fuel slots
- Weight properly affects aircraft engine power and fuel consumption

## In Progress
- Prepare for Create + Steam N Rails Integration

## What's Left to Build
- optimization (top priority)
- UI tracking: Encumbrance icon, color, and stats display that updates on all relevant events
- Compatibility/integration with other mods (Create: Steam n Rails next, more to follow)
- Custom enchantment or upgrade item for increasing max capacity (must work with JSON-driven system)
- Expand faction system
  - Faction commands and permissions
  - Faction territory mechanics
  - Faction benefits and progression
- Custom GUI elements for RPG features
- Integration with Project MMO for skills
- Economic systems and trade mechanics
- Transport systems (ships, aircraft, mounts)
- Combat role mechanics and balancing
- Region-specific resource distribution
- Continued in-game testing for edge cases and mod interactions
- Implement effects and colored messages for aircraft weight thresholds
- Troubleshoot and resolve double execution of logic on inventory open

## Current Status
Weight system is stable and feature-complete for current requirements. Major bugs are resolved. Small Ships integration is complete with weight calculation and capacity working, as well as speed penalties being fully applied. Immersive Aircraft weight system is now implemented and functional, including all relevant slots. Effects work on aircraft. Focus will now shift to Create integration with the Steam N Rails Addon

## Known Issues
- Need to ensure weight system is compatible with other inventory mods
- Faction system requires persistence and synchronization refinement
- Cross-mod integration planning still in early stages
- Logic for aircraft weight system fires twice on inventory open; to be investigated
- None currently blocking; further testing may reveal edge cases

## Evolution of Project Decisions
- Started with basic feature set to establish core architecture
- Focusing on capability-based systems for extensibility, but attachment and data component systems should also be considered where possible
- Prioritizing the weight and faction systems as foundation for RPG mechanics
- Planning data-driven approach for easier balancing and configuration
- Using mixins to integrate with external mods without creating hard dependencies
- Moved from entity ID to persistent UUID for ship tracking
- Centralized notification logic for both threshold crossing and inventory open events
- Improved message clarity and color-coding for player feedback

The mod is entering a new phase focused on optimization, UI/UX, and integration. The foundation is now stable, enabling confident addition of new features and compatibility work. 