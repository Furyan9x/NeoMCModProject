package net.furyan.riyaposmod.faction;

import net.furyan.riyaposmod.RiyaposMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for all factions in the Riyapos world.
 * This class manages the registration and retrieval of factions.
 */
public class FactionRegistry {
    private static final Map<String, Faction> FACTIONS = new HashMap<>();
    private static boolean initialized = false;

    // Placeholder faction IDs - these will be replaced with actual faction data
    public static final String FACTION_ONE = "faction_one";
    public static final String FACTION_TWO = "faction_two";
    public static final String FACTION_THREE = "faction_three";
    public static final String FACTION_FOUR = "faction_four";
    public static final String FACTION_FIVE = "faction_five";

    /**
     * Registers this registry with the event bus.
     *
     * @param eventBus The mod event bus
     */
    public static void register(IEventBus eventBus) {
        NeoForge.EVENT_BUS.addListener(FactionRegistry::onServerStarting);
    }

    /**
     * Initializes the faction registry with default factions.
     * This is called when the server starts.
     *
     * @param event The server starting event
     */
    private static void onServerStarting(ServerStartingEvent event) {
        if (!initialized) {
            registerDefaultFactions();
            initialized = true;
        }
    }

    /**
     * Registers the default factions.
     * These are placeholder factions that can be replaced with actual faction data.
     */
    private static void registerDefaultFactions() {
        // Create placeholder factions with minimal data
        // These will be replaced with actual faction data later

        // Faction One
        Faction factionOne = new Faction(
            FACTION_ONE,
            Component.translatable("faction." + RiyaposMod.MOD_ID + "." + FACTION_ONE + ".name"),
            Component.translatable("faction." + RiyaposMod.MOD_ID + "." + FACTION_ONE + ".description"),
            ResourceLocation.parse(RiyaposMod.MOD_ID + ":textures/gui/faction/" + FACTION_ONE + ".png"),
            0xFF0000, // Red color
            Items.DIAMOND_SWORD // Placeholder item
        );

        // Add placeholder perks and hindrances
        factionOne.addPerk(new Faction.FactionPerk(
            "strength_bonus",
            Component.translatable("faction." + RiyaposMod.MOD_ID + "." + FACTION_ONE + ".perk.strength_bonus.name"),
            Component.translatable("faction." + RiyaposMod.MOD_ID + "." + FACTION_ONE + ".perk.strength_bonus.description")
        ));

        factionOne.addHindrance(new Faction.FactionPerk(
            "magic_weakness",
            Component.translatable("faction." + RiyaposMod.MOD_ID + "." + FACTION_ONE + ".hindrance.magic_weakness.name"),
            Component.translatable("faction." + RiyaposMod.MOD_ID + "." + FACTION_ONE + ".hindrance.magic_weakness.description")
        ));

        // Faction Two
        Faction factionTwo = new Faction(
            FACTION_TWO,
            Component.translatable("faction." + RiyaposMod.MOD_ID + "." + FACTION_TWO + ".name"),
            Component.translatable("faction." + RiyaposMod.MOD_ID + "." + FACTION_TWO + ".description"),
            ResourceLocation.parse(RiyaposMod.MOD_ID + ":textures/gui/faction/" + FACTION_TWO + ".png"),
            0x0000FF, // Blue color
            Items.BOOK // Placeholder item
        );

        // Add placeholder perks and hindrances
        factionTwo.addPerk(new Faction.FactionPerk(
            "magic_bonus",
            Component.translatable("faction." + RiyaposMod.MOD_ID + "." + FACTION_TWO + ".perk.magic_bonus.name"),
            Component.translatable("faction." + RiyaposMod.MOD_ID + "." + FACTION_TWO + ".perk.magic_bonus.description")
        ));

        factionTwo.addHindrance(new Faction.FactionPerk(
            "physical_weakness",
            Component.translatable("faction." + RiyaposMod.MOD_ID + "." + FACTION_TWO + ".hindrance.physical_weakness.name"),
            Component.translatable("faction." + RiyaposMod.MOD_ID + "." + FACTION_TWO + ".hindrance.physical_weakness.description")
        ));

        // Faction Three
        Faction factionThree = new Faction(
            FACTION_THREE,
            Component.translatable("faction." + RiyaposMod.MOD_ID + "." + FACTION_THREE + ".name"),
            Component.translatable("faction." + RiyaposMod.MOD_ID + "." + FACTION_THREE + ".description"),
            ResourceLocation.parse(RiyaposMod.MOD_ID + ":textures/gui/faction/" + FACTION_THREE + ".png"),
            0x00FF00, // Green color
            Items.OAK_SAPLING // Placeholder item
        );

        // Add placeholder perks and hindrances
        factionThree.addPerk(new Faction.FactionPerk(
            "nature_bonus",
            Component.translatable("faction." + RiyaposMod.MOD_ID + "." + FACTION_THREE + ".perk.nature_bonus.name"),
            Component.translatable("faction." + RiyaposMod.MOD_ID + "." + FACTION_THREE + ".perk.nature_bonus.description")
        ));

        factionThree.addHindrance(new Faction.FactionPerk(
            "technology_weakness",
            Component.translatable("faction." + RiyaposMod.MOD_ID + "." + FACTION_THREE + ".hindrance.technology_weakness.name"),
            Component.translatable("faction." + RiyaposMod.MOD_ID + "." + FACTION_THREE + ".hindrance.technology_weakness.description")
        ));

        // Faction Four
        Faction factionFour = new Faction(
            FACTION_FOUR,
            Component.translatable("faction." + RiyaposMod.MOD_ID + "." + FACTION_FOUR + ".name"),
            Component.translatable("faction." + RiyaposMod.MOD_ID + "." + FACTION_FOUR + ".description"),
            ResourceLocation.parse(RiyaposMod.MOD_ID + ":textures/gui/faction/" + FACTION_FOUR + ".png"),
            0xFFFF00, // Yellow color
            Items.GOLD_INGOT // Placeholder item
        );

        // Add placeholder perks and hindrances
        factionFour.addPerk(new Faction.FactionPerk(
            "trade_bonus",
            Component.translatable("faction." + RiyaposMod.MOD_ID + "." + FACTION_FOUR + ".perk.trade_bonus.name"),
            Component.translatable("faction." + RiyaposMod.MOD_ID + "." + FACTION_FOUR + ".perk.trade_bonus.description")
        ));

        factionFour.addHindrance(new Faction.FactionPerk(
            "combat_weakness",
            Component.translatable("faction." + RiyaposMod.MOD_ID + "." + FACTION_FOUR + ".hindrance.combat_weakness.name"),
            Component.translatable("faction." + RiyaposMod.MOD_ID + "." + FACTION_FOUR + ".hindrance.combat_weakness.description")
        ));

        // Faction Five
        Faction factionFive = new Faction(
            FACTION_FIVE,
            Component.translatable("faction." + RiyaposMod.MOD_ID + "." + FACTION_FIVE + ".name"),
            Component.translatable("faction." + RiyaposMod.MOD_ID + "." + FACTION_FIVE + ".description"),
            ResourceLocation.parse(RiyaposMod.MOD_ID + ":textures/gui/faction/" + FACTION_FIVE + ".png"),
            0xFFFFFF, // White color
            Items.SHIELD // Placeholder item
        );

        // Add placeholder perks and hindrances
        factionFive.addPerk(new Faction.FactionPerk(
            "protection_bonus",
            Component.translatable("faction." + RiyaposMod.MOD_ID + "." + FACTION_FIVE + ".perk.protection_bonus.name"),
            Component.translatable("faction." + RiyaposMod.MOD_ID + "." + FACTION_FIVE + ".perk.protection_bonus.description")
        ));

        factionFive.addHindrance(new Faction.FactionPerk(
            "stealth_weakness",
            Component.translatable("faction." + RiyaposMod.MOD_ID + "." + FACTION_FIVE + ".hindrance.stealth_weakness.name"),
            Component.translatable("faction." + RiyaposMod.MOD_ID + "." + FACTION_FIVE + ".hindrance.stealth_weakness.description")
        ));

        // Register all factions
        registerFaction(factionOne);
        registerFaction(factionTwo);
        registerFaction(factionThree);
        registerFaction(factionFour);
        registerFaction(factionFive);
    }

    /**
     * Registers a faction with the registry.
     *
     * @param faction The faction to register
     */
    public static void registerFaction(Faction faction) {
        FACTIONS.put(faction.getId(), faction);
    }

    /**
     * Gets a faction by its ID.
     *
     * @param id The faction ID
     * @return An Optional containing the faction, or empty if not found
     */
    public static Optional<Faction> getFaction(String id) {
        return Optional.ofNullable(FACTIONS.get(id));
    }

    /**
     * Gets all registered factions.
     *
     * @return A collection of all factions
     */
    public static Collection<Faction> getAllFactions() {
        return FACTIONS.values();
    }

    /**
     * Checks if a faction with the given ID exists.
     *
     * @param id The faction ID to check
     * @return True if the faction exists, false otherwise
     */
    public static boolean factionExists(String id) {
        return FACTIONS.containsKey(id);
    }
}
