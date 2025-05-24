# System Patterns

## System Architecture
The RiyaposMod is built on the Neoforge platform for Minecraft 1.21.1, following a modular architecture with clear separation of concerns:

- Client-side components for UI and rendering
- Server-side components for gameplay logic and persistence
- Common components shared between client and server
- Network layer for client-server communication

## Key Technical Decisions
- Using capability/Data Component/Attachment system for extending entity functionality (faction, weight)
- Data-driven approach for item properties and gameplay mechanics
- Event-based communication between different mod components
- Leveraging Minecraft's built-in advancement system for progression

## Design Patterns in Use
- Registry pattern for managing mod components and items
- Capability/Attachment/Data Component(Whichever makes the most sense) pattern for extending entity features
- Command pattern for implementing player interactions
- Event system for decoupled communication
- Data generation for generating assets and data at build time

## Component Relationships
- **Weight System**: Implements inventory weight mechanics with capability, data, and events components
- **Faction System**: Manages player factions with capability, data, and command components
- **Item System**: Custom items including specialized weapons with their own behaviors
- **Network System**: Handles packet transmission between client and server
- **Skill System**: Providers players a Runescape/Archeage like skill/life-skill system to train and unlock gameplay features and mechanics

## Critical Implementation Paths
- Registering mod components during game initialization
- Loading and applying capabilities/attachments/components to entities
- Processing player actions through commands and events
- Synchronizing data between client and server through the network layer 