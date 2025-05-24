# Active Context

**Summary:**
The development of a custom skill system using Neoforge Capabilities is well underway and a functional core for the Mining skill has been established. This includes data storage, XP gain, client-side notifications (XP popups, level-up titles with fireworks), and debug commands. The previously developed aircraft weight system remains stable.

## Current Work Focus
- **Skill System Expansion:**
    - Adding more skills (e.g., Woodcutting, Foraging, Combat skills).
    - Implementing configuration files (e.g., `woodcutting.json`) and corresponding event handlers for new skills.
    - Developing logic for different XP trigger events (e.g., item use, entity interaction, crafting).
- Addressing skill exploit prevention (e.g., self-place penalty for block-breaking XP).
- Brainstorming methods for modifying XP based on attributes, capabilities, or qualities of blocks/items/entities.

## Recent Changes
- **Skill System Core Implementation (Mining Skill):**
    - Established `ISkillData`/`SkillData` capability for XP (long) and levels (RuneScape formula via `SkillConstants`).
    - Implemented NBT persistence and capability attachment (`SkillDataProvider`, `SkillCapabilityEvents`).
    - Created `ClientboundSkillUpdatePacket` for server-to-client skill/XP/level-up information.
    - Developed client-side XP gain popups (`SkillDisplayTickHandler`, `SkillOverlayGui`) with consolidation and fade effects.
    - Implemented prominent level-up notifications using Minecraft titles (colored) and multi-stage firework rocket displays (via `DataComponents.FIREWORKS`).
    - Set up XP granting for Mining via `XPConfigLoader` (`mining.json`), `SkillXPDispatcher`, and `MiningEventHandler`.
    - Created and refined debug commands: `/skillinfo` (now displays Total XP / XP for Next Level) and `/skillset`.
    - Corrected `PLAYER_SKILLS_NBT_KEY` location and NBT serialization (long for total XP).
    - Resolved issues with `FireworkExplosion` constructor and `ItemStack` NBT handling for 1.21.1 using `DataComponents`.
    - Ensured accurate XP calculation and display logic.

## Next Steps
- **Skill System:**
    - Implement the Woodcutting skill as the next example, including:
        - `woodcutting.json` configuration.
        - `WoodcuttingEventHandler` (or similar).
        - Logic in `SkillCapabilityEvents` to dispatch to the woodcutting handler.
    - Begin design and implementation for preventing self-place XP exploits for blocks.
    - Incrementally add support for other skills (Excavating, Farming, Herblore, Combat, etc.) following established patterns.
    - Consider adding sound effects for XP gains and level-ups.
- Investigate and implement methods for modifying XP gain (e.g., tool type, enchants, player buffs).
- Plan and begin development of a dedicated Skill UI.

## Active Decisions and Considerations
- Client-side feedback for skills is now a combination of custom overlay for XP gains and system titles for level-ups.
- Fireworks for level-ups use `FireworkRocketEntity` with `DataComponents` for customization.
- Skill XP is stored as `long` to accommodate RuneScape-like progression.
- `/skillinfo` format is "Skill Name: Level (Total Accumulated XP / Total XP For Next Level)".

## Important Patterns and Preferences
- **Capabilities:** Used for core player data storage (Skills, Weight).
- **Networking:** Custom packets for client-server communication of vital information (Skill updates).
- **Client-Side Rendering:** `LayeredDraw.Layer` for custom HUD elements (`SkillOverlayGui`). Minecraft's title system for major announcements.
- **Event-Driven Logic:** Using NeoForge events to trigger game logic (block breaks, player login/logout).
- **Configuration:** JSON-based configuration for skill-specific data (e.g., `mining.json` for `XPConfigLoader`).
- **Data Components:** Preferred method for `ItemStack` NBT modification in 1.21.1 (e.g., `DataComponents.FIREWORKS`).

## Learnings and Project Insights
- NeoForge 1.21.1 has specific ways of handling `ItemStack` NBT (Data Components) and client-side GUI rendering (`LayeredDraw.Layer`, title system) that require careful attention to API details.
- Iterative debugging and testing are crucial for complex systems like skill progression and client-server interactions.
- Referencing other well-structured mods (like PMMO for GUI/networking patterns) can provide valuable insights and save time.
- Clear communication of desired display formats (e.g., for `/skillinfo`) is essential for meeting user expectations.
- The RuneScape XP formula can lead to large XP numbers, necessitating `long` for storage.

## Integration and Future Planning
- The skill system will eventually need to integrate with or account for items from other mods (e.g., tools, blocks that should grant XP).
- A dedicated UI for displaying skill levels, progress, and perks will be a significant future work item.