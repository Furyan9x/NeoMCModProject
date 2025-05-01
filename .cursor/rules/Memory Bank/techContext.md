# Technical Context

## Technologies Used
- **Java**: Primary programming language
- **Neoforge**: Modding API for Minecraft 1.21.1
- **Gradle**: Build automation tool
- **Mixins**: For modifying Minecraft base classes
- **JEI**: Just Enough Items integration for recipe viewing
- **Patchouli**: For documentation and guidebooks

## Development Setup
- Gradle-based build system with wrapper
- NeoForge development environment
- Automated asset and data generation pipeline
- Client and server-side testing capabilities

## Technical Constraints
- Compatibility with Minecraft 1.21.1 and Neoforge API
- Performance considerations for client and server
- Compatibility with other mods in the ecosystem:
  - Project MMO + Classes
  - Create + Addons
  - Iron Spells & Spellbooks
  - Apotheosis
  - Sophisticated Backpacks
  - Ars Noveau
  - Better Combat
  - MineColonies

## Dependencies
- Core Minecraft 1.21.1 classes and APIs
- Neoforge libraries and capabilities
- Integration points with other mods in the ecosystem

## Tool Usage Patterns
- Data generation for items, recipes, loot tables, and advancements
- Client-side GUI development for custom interfaces
- Server-side command and event handling
- Network packet system for client-server communication
- Capability system for extending entity functionality 