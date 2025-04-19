package net.furyan.riyaposmod.faction;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a faction in the Riyapos world.
 * Each faction has a unique identifier, name, description, and various attributes.
 */
public class Faction {
    private final String id;
    private final Component name;
    private final Component description;
    private final ResourceLocation icon;
    private final int color;
    private final List<FactionPerk> perks;
    private final List<FactionPerk> hindrances;
    private final Item representativeItem;
    private final UUID uuid;

    /**
     * Creates a new faction with the specified attributes.
     *
     * @param id The unique identifier for this faction
     * @param name The display name of the faction
     * @param description The lore description of the faction
     * @param icon The icon resource location for UI display
     * @param color The color associated with this faction (for UI elements)
     * @param representativeItem An item that represents this faction
     */
    public Faction(String id, Component name, Component description, ResourceLocation icon, 
                  int color, Item representativeItem) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.color = color;
        this.perks = new ArrayList<>();
        this.hindrances = new ArrayList<>();
        this.representativeItem = representativeItem;
        this.uuid = UUID.nameUUIDFromBytes(id.getBytes());
    }

    /**
     * Gets the unique identifier for this faction.
     *
     * @return The faction ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the display name of the faction.
     *
     * @return The faction name as a Component
     */
    public Component getName() {
        return name;
    }

    /**
     * Gets the lore description of the faction.
     *
     * @return The faction description as a Component
     */
    public Component getDescription() {
        return description;
    }

    /**
     * Gets the icon resource location for this faction.
     *
     * @return The faction icon ResourceLocation
     */
    public ResourceLocation getIcon() {
        return icon;
    }

    /**
     * Gets the color associated with this faction.
     *
     * @return The faction color as an integer
     */
    public int getColor() {
        return color;
    }

    /**
     * Gets the list of perks for this faction.
     *
     * @return The list of faction perks
     */
    public List<FactionPerk> getPerks() {
        return perks;
    }

    /**
     * Gets the list of hindrances for this faction.
     *
     * @return The list of faction hindrances
     */
    public List<FactionPerk> getHindrances() {
        return hindrances;
    }

    /**
     * Gets the item that represents this faction.
     *
     * @return The representative item
     */
    public Item getRepresentativeItem() {
        return representativeItem;
    }

    /**
     * Gets the UUID for this faction.
     *
     * @return The faction UUID
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Adds a perk to this faction.
     *
     * @param perk The perk to add
     * @return This faction instance for chaining
     */
    public Faction addPerk(FactionPerk perk) {
        this.perks.add(perk);
        return this;
    }

    /**
     * Adds a hindrance to this faction.
     *
     * @param hindrance The hindrance to add
     * @return This faction instance for chaining
     */
    public Faction addHindrance(FactionPerk hindrance) {
        this.hindrances.add(hindrance);
        return this;
    }

    /**
     * Represents a perk or hindrance for a faction.
     */
    public static class FactionPerk {
        private final Component name;
        private final Component description;
        private final String id;

        /**
         * Creates a new faction perk.
         *
         * @param id The unique identifier for this perk
         * @param name The display name of the perk
         * @param description The description of the perk
         */
        public FactionPerk(String id, Component name, Component description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }

        /**
         * Gets the unique identifier for this perk.
         *
         * @return The perk ID
         */
        public String getId() {
            return id;
        }

        /**
         * Gets the display name of the perk.
         *
         * @return The perk name as a Component
         */
        public Component getName() {
            return name;
        }

        /**
         * Gets the description of the perk.
         *
         * @return The perk description as a Component
         */
        public Component getDescription() {
            return description;
        }
    }
}