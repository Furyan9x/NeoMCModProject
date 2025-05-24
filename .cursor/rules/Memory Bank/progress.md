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
- **Skill System (Initial Implementation):**
    - Core capability (`ISkillData`/`SkillData`) for storing XP (long) and levels using RuneScape formula (`SkillConstants`).
    - `SkillDataProvider` for attaching capability, `SkillCapabilities` for registration.
    - `SkillCapabilityEvents` for persistence (NBT save/load on login/logout/clone).
    - **Client-Side Notifications:**
        - `ClientboundSkillUpdatePacket` sends skill ID, XP gained, new level, total XP, and level-up status.
        - `SkillDisplayTickHandler` manages a queue of `DisplayEntry` objects for on-screen XP gain popups:
            - Handles consolidation of rapid XP gains for the same skill.
            - Manages fade-in, duration, and fade-out of messages.
        - `SkillOverlayGui` (implements `LayeredDraw.Layer`) renders the XP gain popups from `SkillDisplayTickHandler`.
        - Level-up notifications are displayed as prominent, colored Minecraft titles (e.g., "Level Up!" in green, "Your Mining skill is now level X!" with skill name in dark gray).
        - Level-ups trigger multiple, sequenced `FireworkRocketEntity` instances at the player's location with randomized properties (colors, flicker, trail, varying flight times).
        - Correct usage of `DataComponents.FIREWORKS` with `FireworkExplosion` and `Fireworks` classes for 1.21.1.
    - **XP Granting & Dispatching (Mining Skill Example):**
        - `XpGainResult` record encapsulates XP gain details.
        - `SkillXPDispatcher.dispatchMiningBlockBreakXP` calculates XP based on `MiningXPConfigEntry` (from `mining.json` via `XPConfigLoader`) and returns `XpGainResult`.
        - `MiningEventHandler` calls the dispatcher.
        - `SkillCapabilityEvents.onBlockBreak` uses the result from `MiningEventHandler` to send the `ClientboundSkillUpdatePacket`.
    - **Debug Commands:**
        - `/skillinfo` command displays skill levels and progress: "Skill Name: Level (Total Accumulated XP / Total XP For Next Level)".
        - `/skillset` command allows setting a skill to a specific level (updates total XP accordingly).
    - Robust NBT handling for `SkillData` including `long` for total experience.
    - Correct calculation and display of XP within levels and total XP.

## In Progress
- **Skill System Expansion:**
    - Adding more skills (e.g., Woodcutting, Foraging, Combat skills).
    - Implementing configuration files (e.g., `woodcutting.json`) and corresponding handlers for new skills.
    - Developing logic for different XP trigger events (e.g., item use, entity interaction).

## What's Left to Build
- Optimization (top priority for all systems)
- UI tracking: Encumbrance icon, color, and stats display that updates on all relevant events (Weight System)
- Compatibility/integration with other mods (both Weight and Skill systems)
- Custom enchantment or upgrade item for increasing max capacity (Weight System)
- Expand faction system
  - Faction commands and permissions
  - Faction territory mechanics
  - Faction benefits and progression
- Custom GUI elements for RPG features (potentially a dedicated Skill UI)
- Economic systems and trade mechanics
- Transport systems (ships, aircraft, mounts) - further skill integration?
- Combat role mechanics and balancing
- Region-specific resource distribution
- Continued in-game testing for edge cases and mod interactions


- **Skill System Implementation (Advanced):**
    - Full implementation for remaining planned skills.
    - Efficient and optimized Configuration system for all Skills.
    - Potential new Forge config values for global/skill-specific XP rates.
    - Advanced XP calculation modifiers (e.g., from gear, buffs, tools, block properties).
    - Skill perks and unlocks.
    - Dedicated UI for viewing skill levels, progress, and perks.
    - Prevention of XP exploits (e.g., self-place block breaking penalties).
- **Considerations for Block-Based Skills:**
    - Detailed logic for tool-less harvesting (e.g., differentiating Farming/Herblore crops).
    - Handling for specific tool interactions beyond basic tool category (e.g., shears for leaves).
- Sound effects for level-ups and XP gains.

## Current Status
- Weight system is stable and feature-complete for current requirements.
- **Skill system has a functional core for Mining, including data handling, XP gain, client-side notifications (XP popups & title-based level-ups with fireworks), and debug commands.** Focus is now on expanding this system with more skills and features.

## Known Issues
- Need to ensure weight system is compatible with other inventory mods.
- Faction system requires persistence and synchronization refinement.
- Cross-mod integration planning still in early stages for both systems.
- Skill system:
    - Currently only Mining skill is implemented with block breaking.
    - No exploit prevention for block breaking yet.

## Evolution of Project Decisions
- Started with basic feature set to establish core architecture.
- Focusing on capability-based systems for extensibility (Skills, Weight).
- Prioritizing the weight and faction systems as foundation for RPG mechanics, with skills now becoming a major focus.
- Planning data-driven approach for easier balancing and configuration (Skills, Weight).
- Using mixins to integrate with external mods without creating hard dependencies.
- Moved from entity ID to persistent UUID for ship tracking.
- Centralized notification logic for both threshold crossing and inventory open events (Weight).
- Improved message clarity and color-coding for player feedback (Skills, Weight).
- **Skill System Client Feedback:** Iterated from basic toast concepts to a robust PMMO-inspired system using `LayeredDraw.Layer` for XP popups and Minecraft's title system for prominent level-up messages with enhanced firework effects (using Data Components).
- **Skill Data:** Refined `ISkillData` and `SkillData` to use `long` for total experience and ensure accurate XP calculations for RuneScape-style leveling.

The mod is entering a new phase focused on designing and implementing a custom skill system like Project MMO with Inspiration from Runescape/Archeage. **The initial implementation of this skill system is now functional for the Mining skill.** 