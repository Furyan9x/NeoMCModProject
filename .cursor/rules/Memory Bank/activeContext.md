# Active Context

**Summary:**
The weight system for backpacks is now stable and performant, with robust handler registration and JSON-driven configuration. The immediate focus is on performance optimization, UI/UX improvements (encumbrance tracking), and preparing for integration with other major mods. The foundation is solid, enabling confident expansion and new feature development.

## Current Work Focus
- Performance review and optimization of the weight system for backpacks and inventory
- Planning and design for UI tracking of player encumbrance (icon, color, and stats display)
- Ensuring compatibility with other major mods (Small Ships, Immersive Aircraft, Create: Steam n Rails, etc.)
- Exploring custom enchantment or upgrade item for increasing max capacity

## Recent Changes
- Major bug with backpack handler registration after slot changes fixed
- Debounce/cooldown system implemented to prevent weight calculation spam
- Confirmed that backpack movement between inventory and curio slots maintains functionality
- Item tooltips for weight and capacity bonus are implemented and working
- Sophisticated Backpacks integration for capacity bonus via JSON tags is complete

## Next Steps
- Profile and optimize the weight system for both client and server performance
- Design and implement a UI element for encumbrance tracking (icon, color, and stats)
- Begin compatibility/integration work with other mods (prioritize Small Ships, Immersive Aircraft, Create: Steam n Rails)
- Design and implement a custom enchantment or upgrade item for increasing max capacity (ensure it works with JSON-driven system)
- Continue thorough in-game testing for edge cases and mod interactions

## Active Decisions and Considerations
- Performance and optimization are the top priority before adding new features
- UI/UX for encumbrance must be clear, responsive, and update on all relevant events
- Compatibility/integration should be modular and not introduce hard dependencies
- Custom enchantment/upgrade must not break JSON-driven capacity system

## Important Patterns and Preferences
- Centralized management of backpack handlers
- Event-driven communication between system components
- UUID-based tracking for unique backpack identification
- Comprehensive logging for testing and debugging
- Clear separation of responsibilities between event handlers
- Data-driven configuration for backpack capacity and upgrades

## Learnings and Project Insights
- Handler registration/unregistration must be robust to all inventory changes
- Debounce/cooldown logic is essential for performance
- JSON-driven configuration enables flexible integration with other mods
- UI/UX is critical for player experience and must be prioritized after optimization

## Integration and Future Planning
- JSON-driven configuration is enabling flexible integration with other mods and will support future compatibility work
- No hard dependencies on other mods yet; integration points are being identified for Small Ships, Immersive Aircraft, Create: Steam n Rails, and others
- Performance profiling tools and strategies (e.g., Java profilers, in-game debug overlays) are being considered to guide optimization efforts 