package net.furyan.riyaposmod.weight;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.core.registries.BuiltInRegistries;
import

import static net.furyan.riyaposmod.weight.components.CapacityBonus.registerTagCapBonus;
import static net.furyan.riyaposmod.weight.components.ItemWeight.registerTagWeight;

/**
 * Central manager for item properties: weight & capacity bonus.
 * Loads from a single JSON, supports tag-based defaults for future.
 */
public final class ItemPropertiesManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("RiyaposMod:ItemProps");

    private static final Map<ResourceLocation, Float> WEIGHT_OVERRIDES = new HashMap<>();
    private static final Map<ResourceLocation, Float> CAPACITY_OVERRIDES = new HashMap<>();

    private static final Map<TagKey<Item>, Float> TAG_WEIGHT = new LinkedHashMap<>();
    private static final Map<TagKey<Item>, Float> TAG_CAPACITY = new LinkedHashMap<>();

    private static final Gson GSON = new Gson();

    private ItemPropertiesManager() {}

    /**
     * JSON schema entries for an item's properties.
     */
    private record Props(float weight, float capacityBonus) {}

    /**
     * Load all overrides from `config/item_properties.json` (or datapack) into memory.
     */
    public static void loadProperties(Path configDir) {
        Path file = configDir.resolve("item_properties.json");
        if (!Files.exists(file)) {
            LOGGER.info("No item_properties.json found, skipping.");
            return;
        }

        Type mapType = new TypeToken<Map<String, Props>>() {}.getType();
        try (Reader reader = Files.newBufferedReader(file)) {
            Map<String, Props> raw = GSON.fromJson(reader, mapType);
            if (raw == null) {
                LOGGER.warn("item_properties.json is empty or invalid.");
                return;
            }
            for (Map.Entry<String, Props> entry : raw.entrySet()) {
                String key = entry.getKey();
                Props p = entry.getValue();
                ResourceLocation id = ResourceLocation.tryParse(key);
                if (id != null) {
                    WEIGHT_OVERRIDES.put(id, p.weight());
                    CAPACITY_OVERRIDES.put(id, p.capacityBonus());
                } else {
                    LOGGER.warn("Invalid ResourceLocation in item_properties.json: {}", key);
                }
            }
            LOGGER.info("Loaded {} item property entries.", WEIGHT_OVERRIDES.size());
        } catch (IOException e) {
            LOGGER.error("Failed loading item_properties.json", e);
        }
    }

    /**
     * Register hardcoded tag-based weights (for future use).
     */
    public static void setupTagRules() {
        // Example tags, change or add as needed

        TagKey<Item> small = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("riyaposmod", "small_containers"));
        TAG_CAPACITY.put(small, 16f);

        TagKey<Item> medium = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("riyaposmod", "medium_containers"));
        TAG_CAPACITY.put(medium, 48f);

        TagKey<Item> large = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("riyaposmod", "large_containers"));
        TAG_CAPACITY.put(large, 100f);


        registerTagCapBonus(large, 14f);

        registerTagWeight(ItemTags.SWORDS,   0.5f);
        registerTagWeight(ItemTags.ACACIA_LOGS,  1.0f);
        // You can also predefine weight tags here
        // TAG_WEIGHT.put(someTag, someWeight);
    }

    /**
     * Compute runtime weight for a given ItemStack.
     */
    public static float getWeight(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        // 1. JSON override
        Float override = WEIGHT_OVERRIDES.get(id);
        if (override != null) return override;
        // 2. Tag rule
        for (var e : TAG_WEIGHT.entrySet()) {
            if (stack.is(e.getKey())) return e.getValue();
        }
        // 3. Default
        return 1.0f;
    }

    /**
     * Compute runtime capacity bonus for a given ItemStack.
     */
    public static float getCapacity(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        // 1. JSON override
        Float override = CAPACITY_OVERRIDES.get(id);
        if (override != null) return override;
        // 2. Tag rule
        for (var e : TAG_CAPACITY.entrySet()) {
            if (stack.is(e.getKey())) return e.getValue();
        }
        // 3. Default
        return 0.0f;
    }
}
