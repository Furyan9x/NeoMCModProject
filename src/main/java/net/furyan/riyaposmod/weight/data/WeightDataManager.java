package net.furyan.riyaposmod.weight.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class WeightDataManager extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static final Map<String, Map<ResourceLocation, ContainerItemEntry>> containerItems = new HashMap<>();
    private static final Map<String, Map<ResourceLocation, CustomTagEntry>> customTags = new HashMap<>();
    private static final Map<ResourceLocation, DataEntry> perItem = new HashMap<>();
    private static final Map<String, DataEntry> perNamespace = new HashMap<>();
    private static final Map<TagKey<Item>, DataEntry> perTag = new HashMap<>();
    
    // Minimum and maximum values for validation
    private static final float MIN_WEIGHT = 0.0f;
    private static final float MAX_WEIGHT = 1000.0f;
    private static final float MIN_CAPACITY_BONUS = 0.0f;
    private static final float MAX_CAPACITY_BONUS = 1000.0f;
    private static final float MIN_SLOT_MULTIPLIER = 0.0f;
    private static final float MAX_SLOT_MULTIPLIER = 100.0f;
    private static final int MIN_SLOTS = 0;
    private static final int MAX_SLOTS = 1000;
    
    // --- Optimization: Per-tick/per-inventory item weight cache ---
    // Package-private for WeightTickHandler integration
    static final ThreadLocal<Map<Item, Float>> perTickWeightCache = ThreadLocal.withInitial(HashMap::new);
    // --- Optimization: Static cache for per-item capacity bonuses ---
    private static final Map<Item, Float> staticCapacityBonusCache = new HashMap<>();
    
    public WeightDataManager() {
        super(GSON, "weight");
    }
    
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager manager, ProfilerFiller profiler) {
        perItem.clear();
        perNamespace.clear();
        perTag.clear();
        containerItems.clear();
        customTags.clear();
        // --- Optimization: Clear static caches on reload ---
        perTickWeightCache.remove();
        staticCapacityBonusCache.clear();
        
        // Load per-namespace item data
        jsons.forEach((location, json) -> {
            try {
                String namespace = location.getNamespace();
                Map<String, DataEntry> entries = GSON.fromJson(json, 
                    new TypeToken<Map<String, DataEntry>>(){}.getType());
                
                // Handle namespace default
                if (entries.containsKey("_default")) {
                    perNamespace.put(namespace, entries.get("_default"));
                    entries.remove("_default");
                }
                
                // Process individual items
                entries.forEach((path, entry) -> {
                    ResourceLocation itemId = ResourceLocation.tryParse(path);
                    if (itemId != null) {
                        perItem.put(itemId, entry);
                    } else {
                        LOGGER.error("Invalid item ID format: {}", path);
                    }
                });
                
            } catch (Exception e) {
                LOGGER.error("Error loading weight data from {}: {}", location, e.getMessage());
            }
        });
        
        // Load existing tag data
        try {
            ResourceLocation tagFile = ResourceLocation.tryParse("riyaposmod:weight/tags");
            if (tagFile == null) {
                LOGGER.error("Invalid tag file path: riyaposmod:weight/tags");
                return;
            }
            
            JsonElement tagJson = manager.getResource(tagFile)
                .map(resource -> {
                    try {
                        return GSON.fromJson(resource.openAsReader(), JsonElement.class);
                    } catch (Exception e) {
                        LOGGER.error("Error reading tag file: {}", e.getMessage());
                        return null;
                    }
                })
                .orElse(null);
                
            if (tagJson != null) {
                Map<String, DataEntry> tagEntries = GSON.fromJson(tagJson,
                    new TypeToken<Map<String, DataEntry>>(){}.getType());
                tagEntries.forEach((tagStr, entry) -> {
                    if (tagStr.startsWith("#")) {
                        String tagPath = tagStr.substring(1); // Remove the # prefix
                        ResourceLocation tagId = ResourceLocation.tryParse(tagPath);
                        if (tagId != null) {
                            TagKey<Item> tag = TagKey.create(BuiltInRegistries.ITEM.key(), tagId);
                            perTag.put(tag, entry);
                        } else {
                            LOGGER.error("Invalid tag format: {}", tagPath);
                        }
                    }
                });
            }
        } catch (Exception e) {
            LOGGER.error("Error loading tag weight data: {}", e.getMessage());
        }

        // Load custom tags data
        jsons.forEach((location, json) -> {
            if (location.getPath().startsWith("custom_tags/")) {
                try {
                    String category = location.getPath().substring("custom_tags/".length()).replace(".json", "");
                    Map<String, CustomTagEntry> entries = GSON.fromJson(json, 
                        new TypeToken<Map<String, CustomTagEntry>>(){}.getType());
                    LOGGER.info("Parsed {} custom tag entries from {}", entries.size(), location);
                    Map<ResourceLocation, CustomTagEntry> categoryMap = customTags.computeIfAbsent(category, k -> new HashMap<>());
                    entries.forEach((path, entry) -> {
                        ResourceLocation itemId = ResourceLocation.tryParse(path);
                        if (itemId != null) {
                            categoryMap.put(itemId, entry);
                        } else {
                            LOGGER.error("Invalid custom tag item ID format: {}", path);
                        }
                    });
                    
                } catch (Exception e) {
                    LOGGER.error("Error loading custom tag data from {}: {}", location, e.getMessage());
                }
            }
        });

        // Load container items data with validation
        jsons.forEach((location, json) -> {
            if (location.getPath().startsWith("container_items/")) {
                try {
                    String category = location.getPath().substring("container_items/".length()).replace(".json", "");
                    Map<String, ContainerItemEntry> entries = GSON.fromJson(json, 
                        new TypeToken<Map<String, ContainerItemEntry>>(){}.getType());
                    LOGGER.info("Parsed {} container entries from {}", entries.size(), location);
                    Map<ResourceLocation, ContainerItemEntry> categoryMap = containerItems.computeIfAbsent(category, k -> new HashMap<>());
                    entries.forEach((path, entry) -> {
                        ResourceLocation itemId = ResourceLocation.tryParse(path);
                        if (itemId != null) {
                            if (validateContainerEntry(entry, itemId)) {
                                categoryMap.put(itemId, entry);
                            }
                        } else {
                            LOGGER.error("Invalid container item ID format: {}", path);
                        }
                    });
                    
                } catch (Exception e) {
                    LOGGER.error("Error loading container data from {}: {}", location, e.getMessage());
                }
            }
        });
        
        LOGGER.info("Loaded weight data: {} items, {} namespaces, {} tags", 
            perItem.size(), perNamespace.size(), perTag.size());
        LOGGER.info("Loaded container data: {} categories with {} items", 
            containerItems.size(), 
            containerItems.values().stream().mapToInt(Map::size).sum());
        LOGGER.info("Loaded custom tag data: {} categories with {} items", 
            customTags.size(), 
            customTags.values().stream().mapToInt(Map::size).sum());
    }
    
    /**
     * Validates a container entry's values are within acceptable ranges.
     * @param entry The entry to validate
     * @param itemId The item ID for error reporting
     * @return true if valid, false if invalid
     */
    private static boolean validateContainerEntry(ContainerItemEntry entry, ResourceLocation itemId) {
        if (entry == null) {
            LOGGER.error("Container entry is null for {}", itemId);
            return false;
        }

        boolean valid = true;
        
        if (entry.weight() < MIN_WEIGHT || entry.weight() > MAX_WEIGHT) {
            LOGGER.error("Invalid weight {} for container {}, must be between {} and {}", 
                entry.weight(), itemId, MIN_WEIGHT, MAX_WEIGHT);
            valid = false;
        }
        
        if (entry.baseCapacityBonus() < MIN_CAPACITY_BONUS || entry.baseCapacityBonus() > MAX_CAPACITY_BONUS) {
            LOGGER.error("Invalid base capacity bonus {} for container {}, must be between {} and {}", 
                entry.baseCapacityBonus(), itemId, MIN_CAPACITY_BONUS, MAX_CAPACITY_BONUS);
            valid = false;
        }
        
        if (entry.slots() < MIN_SLOTS || entry.slots() > MAX_SLOTS) {
            LOGGER.error("Invalid slot count {} for container {}, must be between {} and {}", 
                entry.slots(), itemId, MIN_SLOTS, MAX_SLOTS);
            valid = false;
        }
        
        if (entry.slotMultiplier() < MIN_SLOT_MULTIPLIER || entry.slotMultiplier() > MAX_SLOT_MULTIPLIER) {
            LOGGER.error("Invalid slot multiplier {} for container {}, must be between {} and {}", 
                entry.slotMultiplier(), itemId, MIN_SLOT_MULTIPLIER, MAX_SLOT_MULTIPLIER);
            valid = false;
        }
        
        return valid;
    }
    
    /**
     * Gets the weight of an item following strict priority order:
     * 1. Per Item Overrides
     * 2. Container Item Entry
     * 3. Custom Tag Entry
     * 4. Normal Tags
     * 5. Namespace defaults
     * 6. Default weight (1.0)
     */
    public static float getWeight(ItemStack stack) {
        if (stack.isEmpty()) return 0.0f;
        // --- Optimization: Use per-tick/per-inventory cache ---
        Map<Item, Float> cache = perTickWeightCache.get();
        Item item = stack.getItem();
        Float cached = cache.get(item);
        if (cached != null) return cached;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        
        // 1. Check specific item override
        DataEntry itemEntry = perItem.get(id);
        if (itemEntry != null) { cache.put(item, itemEntry.weight()); return itemEntry.weight(); }
        
        // 2. Check container items
        for (Map<ResourceLocation, ContainerItemEntry> category : containerItems.values()) {
            ContainerItemEntry containerEntry = category.get(id);
            if (containerEntry != null) { cache.put(item, containerEntry.weight()); return containerEntry.weight(); }
        }
        
        // 3. Check custom tags
        for (Map<ResourceLocation, CustomTagEntry> category : customTags.values()) {
            CustomTagEntry tagEntry = category.get(id);
            if (tagEntry != null) { cache.put(item, tagEntry.weight()); return tagEntry.weight(); }
        }
        
        // 4. Check normal tags
        for (Map.Entry<TagKey<Item>, DataEntry> entry : perTag.entrySet()) {
            if (stack.is(entry.getKey())) {
                cache.put(item, entry.getValue().weight());
                return entry.getValue().weight();
            }
        }
        
        // 5. Check namespace default
        DataEntry nsEntry = perNamespace.get(id.getNamespace());
        if (nsEntry != null) { cache.put(item, nsEntry.weight()); return nsEntry.weight(); }
        
        // 6. Fall back to default
        cache.put(item, DataEntry.DEFAULT.weight());
        return DataEntry.DEFAULT.weight();
    }

    /**
     * Gets a container entry for an item in a specific category.
     * Returns DEFAULT if the item is not a container or category doesn't exist.
     */
    public static ContainerItemEntry getContainerEntry(ItemStack stack, String category) {
        if (stack.isEmpty()) return ContainerItemEntry.DEFAULT;
        // --- Optimization: Use static cache for capacity bonus ---
        Item item = stack.getItem();
        Float cached = staticCapacityBonusCache.get(item);
        if (cached != null) {
            // Return a new ContainerItemEntry with cached bonus if needed
            return new ContainerItemEntry(0.0f, cached, 0, 0.0f, false);
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        Map<ResourceLocation, ContainerItemEntry> categoryMap = containerItems.get(category);
        if (categoryMap == null) return ContainerItemEntry.DEFAULT;
        ContainerItemEntry entry = categoryMap.getOrDefault(id, ContainerItemEntry.DEFAULT);
        staticCapacityBonusCache.put(item, entry.getCapacityBonus());
        return entry;
    }
    
    /**
     * Checks if an item is registered as a container in any category
     */
    public static boolean isContainer(ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return containerItems.values().stream()
            .anyMatch(category -> category.containsKey(id));
    }

    /**
     * Clears all static and per-tick caches. Call on data pack reload or in tests.
     */
    public static void clearAllCaches() {
        perTickWeightCache.remove();
        staticCapacityBonusCache.clear();
    }

    /**
     * Clears only the per-tick item weight cache. Call at the start of each tick.
     */
    public static void clearPerTickWeightCache() {
        perTickWeightCache.get().clear();
    }
} 