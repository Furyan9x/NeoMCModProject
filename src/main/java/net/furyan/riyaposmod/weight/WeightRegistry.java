package net.furyan.riyaposmod.weight;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TieredItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for item weights.
 * This class loads item weights from datapacks and provides methods to access them.
 */
public class WeightRegistry extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FOLDER_NAME = "weights";

    // Cache for item weights
    private static final Map<ResourceLocation, Float> ITEM_WEIGHTS = new ConcurrentHashMap<>();

    // Category-based weight system
    private static final Map<String, Float> WEIGHT_CATEGORIES = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, String> ITEM_CATEGORIES = new ConcurrentHashMap<>();

    // Default weight for items not in the registry
    private static final float DEFAULT_WEIGHT = 1.0f;

    // Default category weights
    private static final Map<String, Float> DEFAULT_CATEGORIES = new HashMap<>();

    static {
        // Initialize default categories
        DEFAULT_CATEGORIES.put("light_metals", 1.0f);
        DEFAULT_CATEGORIES.put("heavy_metals", 2.0f);
        DEFAULT_CATEGORIES.put("gems", 0.5f);
        DEFAULT_CATEGORIES.put("tools", 3.0f);
        DEFAULT_CATEGORIES.put("weapons", 4.0f);
        DEFAULT_CATEGORIES.put("armor", 5.0f);
        DEFAULT_CATEGORIES.put("food", 0.5f);
        DEFAULT_CATEGORIES.put("blocks", 1.0f);
    }

    public WeightRegistry() {
        super(GSON, FOLDER_NAME);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager, ProfilerFiller profiler) {
        // Clear existing data
        ITEM_WEIGHTS.clear();
        WEIGHT_CATEGORIES.clear();
        ITEM_CATEGORIES.clear();

        // Load default categories
        WEIGHT_CATEGORIES.putAll(DEFAULT_CATEGORIES);

        resources.forEach((location, jsonElement) -> {
            try {
                JsonObject json = jsonElement.getAsJsonObject();
                int itemCount = 0;
                int categoryCount = 0;
                int categoryItemCount = 0;

                // Check if this is a category-based weight file
                if (json.has("categories") && json.has("items")) {
                    // Load categories
                    JsonObject categoriesJson = json.getAsJsonObject("categories");
                    categoriesJson.entrySet().forEach(entry -> {
                        String categoryName = entry.getKey();
                        float weight = entry.getValue().getAsFloat();
                        WEIGHT_CATEGORIES.put(categoryName, weight);
                    });
                    categoryCount = categoriesJson.size();

                    // Load item-to-category mappings
                    JsonObject itemsJson = json.getAsJsonObject("items");
                    itemsJson.entrySet().forEach(entry -> {
                        String itemKey = entry.getKey();
                        String categoryName = entry.getValue().getAsString();

                        if (WEIGHT_CATEGORIES.containsKey(categoryName)) {
                            ResourceLocation itemId = ResourceLocation.parse(itemKey);
                            ITEM_CATEGORIES.put(itemId, categoryName);
                        } else {
                            LOGGER.warn("Unknown category '{}' for item '{}'", categoryName, itemKey);
                        }
                    });
                    categoryItemCount = itemsJson.size();

                    // Load direct overrides if present
                    if (json.has("overrides")) {
                        JsonObject overridesJson = json.getAsJsonObject("overrides");
                        overridesJson.entrySet().forEach(entry -> {
                            String itemKey = entry.getKey();
                            float weight = entry.getValue().getAsFloat();

                            ResourceLocation itemId = ResourceLocation.parse(itemKey);
                            ITEM_WEIGHTS.put(itemId, weight);
                        });
                        itemCount = overridesJson.size();
                    }

                    LOGGER.info("Loaded {} categories, {} category items, and {} direct overrides from {}",
                            categoryCount, categoryItemCount, itemCount, location);
                } else {
                    // Legacy format - direct item weights
                    json.entrySet().forEach(entry -> {
                        String itemKey = entry.getKey();
                        float weight = entry.getValue().getAsFloat();

                        ResourceLocation itemId = ResourceLocation.parse(itemKey);
                        ITEM_WEIGHTS.put(itemId, weight);
                    });
                    itemCount = json.size();

                    LOGGER.info("Loaded {} direct item weights from {}", itemCount, location);
                }
            } catch (Exception e) {
                LOGGER.error("Error loading weights from {}: {}", location, e.getMessage(), e);
            }
        });

        LOGGER.info("Loaded a total of {} direct item weights, {} categories, and {} categorized items",
                ITEM_WEIGHTS.size(), WEIGHT_CATEGORIES.size(), ITEM_CATEGORIES.size());
    }

    /**
     * Gets the weight of an item.
     * 
     * @param item The item to get the weight of
     * @return The weight of the item, or the calculated default weight if not specified
     */
    public static float getWeight(Item item) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);

        // Check for direct weight override first
        if (ITEM_WEIGHTS.containsKey(itemId)) {
            return ITEM_WEIGHTS.get(itemId);
        }

        // Check for category-based weight
        if (ITEM_CATEGORIES.containsKey(itemId)) {
            String category = ITEM_CATEGORIES.get(itemId);
            if (WEIGHT_CATEGORIES.containsKey(category)) {
                return WEIGHT_CATEGORIES.get(category);
            }
        }

        // Calculate default weight based on item properties
        return calculateDefaultWeight(item);
    }

    /**
     * Calculates a default weight for an item based on its properties.
     * 
     * @param item The item to calculate weight for
     * @return The calculated default weight
     */
    private static float calculateDefaultWeight(Item item) {
        // Base weight on item properties
        if (item instanceof ArmorItem) {
            // Armor items are heavier
            return 3.0f;
        } else if (item instanceof TieredItem) {
            // Tools and weapons
            return 2.0f;
        } else if (item instanceof BlockItem) {
            // Blocks are generally heavy
            return 1.0f;
        }

        // For other items, use a default weight
        return DEFAULT_WEIGHT;
    }

    /**
     * Registers the weight registry with the event bus.
     * 
     * @param eventBus The event bus to register with
     */
    public static void register(IEventBus eventBus) {
        // Register resource reload listener with the NeoForge event bus, not the mod event bus
       NeoForge.EVENT_BUS.addListener(WeightRegistry::onResourceReload);
    }

    private static void onResourceReload(AddReloadListenerEvent event) {
        event.addListener(new WeightRegistry());
    }
}
