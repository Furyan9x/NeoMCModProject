package net.furyan.riyaposmod.weight;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.furyan.riyaposmod.RiyaposMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TieredItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates weight data files for vanilla Minecraft items.
 * This class creates a reference JSON file in the world save directory
 * that includes categories and default weights for vanilla items.
 */
public class WeightDataGenerator {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Default categories
    private static final Map<String, Float> CATEGORIES = new HashMap<>();

    // Item to category mappings
    private static final Map<String, String> ITEM_CATEGORIES = new HashMap<>();

    // Direct weight overrides
    private static final Map<String, Float> OVERRIDES = new HashMap<>();

    static {
        // Initialize default categories
        CATEGORIES.put("light_metals", 1.0f);
        CATEGORIES.put("heavy_metals", 2.0f);
        CATEGORIES.put("gems", 0.5f);
        CATEGORIES.put("tools", 3.0f);
        CATEGORIES.put("weapons", 4.0f);
        CATEGORIES.put("armor", 5.0f);
        CATEGORIES.put("food", 0.5f);
        CATEGORIES.put("blocks", 1.0f);

        // Initialize some vanilla item categories
        // Metals
        ITEM_CATEGORIES.put("minecraft:iron_ingot", "light_metals");
        ITEM_CATEGORIES.put("minecraft:gold_ingot", "heavy_metals");
        ITEM_CATEGORIES.put("minecraft:copper_ingot", "light_metals");
        ITEM_CATEGORIES.put("minecraft:netherite_ingot", "heavy_metals");

        // Gems
        ITEM_CATEGORIES.put("minecraft:diamond", "gems");
        ITEM_CATEGORIES.put("minecraft:emerald", "gems");
        ITEM_CATEGORIES.put("minecraft:amethyst_shard", "gems");

        // Tools
        ITEM_CATEGORIES.put("minecraft:iron_pickaxe", "tools");
        ITEM_CATEGORIES.put("minecraft:iron_axe", "tools");
        ITEM_CATEGORIES.put("minecraft:iron_shovel", "tools");
        ITEM_CATEGORIES.put("minecraft:iron_hoe", "tools");

        // Weapons
        ITEM_CATEGORIES.put("minecraft:iron_sword", "weapons");
        ITEM_CATEGORIES.put("minecraft:bow", "weapons");
        ITEM_CATEGORIES.put("minecraft:crossbow", "weapons");
        ITEM_CATEGORIES.put("minecraft:trident", "weapons");

        // Armor
        ITEM_CATEGORIES.put("minecraft:iron_helmet", "armor");
        ITEM_CATEGORIES.put("minecraft:iron_chestplate", "armor");
        ITEM_CATEGORIES.put("minecraft:iron_leggings", "armor");
        ITEM_CATEGORIES.put("minecraft:iron_boots", "armor");

        // Food
        ITEM_CATEGORIES.put("minecraft:apple", "food");
        ITEM_CATEGORIES.put("minecraft:bread", "food");
        ITEM_CATEGORIES.put("minecraft:cooked_beef", "food");
        ITEM_CATEGORIES.put("minecraft:cooked_chicken", "food");

        // Overrides for special items
        OVERRIDES.put("minecraft:anvil", 10.0f);
        OVERRIDES.put("minecraft:enchanting_table", 8.0f);
        OVERRIDES.put("minecraft:obsidian", 5.0f);
        OVERRIDES.put("minecraft:bedrock", 100.0f);
    }

    /**
     * Event handler for server started event.
     * Generates the weight data file when the server starts.
     *
     * @param event The server started event
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        if (server.isDedicatedServer() || server.isSingleplayer()) {
            generateWeightDataFile(server);
        }
    }

    /**
     * Generates a weight data file in the world save directory.
     *
     * @param server The Minecraft server
     */
    private static void generateWeightDataFile(MinecraftServer server) {
        try {
            // Get the world save directory
            // Get the world save directory and construct the datapack path
            String worldName = server.getWorldData().getLevelName();
            Path worldDir = server.getWorldPath(new net.minecraft.world.level.storage.LevelResource(""));
            Path datapackDir = Paths.get(worldDir.toString(), "datapacks", "weights", "data", RiyaposMod.MOD_ID, "weights");


            // Create the directory if it doesn't exist
            Files.createDirectories(datapackDir);

            // Create the weight data file
            File weightFile = new File(datapackDir.toFile(), "vanilla_weights.json");

            // Skip if the file already exists
            if (weightFile.exists()) {
                LOGGER.info("Weight data file already exists at {}", weightFile.getAbsolutePath());
                return;
            }

            // Create the JSON object
            JsonObject rootJson = new JsonObject();

            // Add categories
            JsonObject categoriesJson = new JsonObject();
            for (Map.Entry<String, Float> entry : CATEGORIES.entrySet()) {
                categoriesJson.addProperty(entry.getKey(), entry.getValue());
            }
            rootJson.add("categories", categoriesJson);

            // Add item categories
            JsonObject itemsJson = new JsonObject();
            for (Map.Entry<String, String> entry : ITEM_CATEGORIES.entrySet()) {
                itemsJson.addProperty(entry.getKey(), entry.getValue());
            }
            rootJson.add("items", itemsJson);

            // Add overrides
            JsonObject overridesJson = new JsonObject();
            for (Map.Entry<String, Float> entry : OVERRIDES.entrySet()) {
                overridesJson.addProperty(entry.getKey(), entry.getValue());
            }
            rootJson.add("overrides", overridesJson);

            // Write the JSON to the file
            try (FileWriter writer = new FileWriter(weightFile)) {
                GSON.toJson(rootJson, writer);
            }

            LOGGER.info("Generated weight data file at {}", weightFile.getAbsolutePath());

            // Generate a more comprehensive file with all vanilla items
            generateCompleteVanillaWeightFile(datapackDir.toFile());
        } catch (IOException e) {
            LOGGER.error("Failed to generate weight data file: {}", e.getMessage(), e);
        }
    }

    /**
     * Generates a comprehensive weight data file with all vanilla items.
     *
     * @param dataDir The data directory
     * @throws IOException If an I/O error occurs
     */
    private static void generateCompleteVanillaWeightFile(File dataDir) throws IOException {
        File completeFile = new File(dataDir, "all_vanilla_weights.json");

        // Skip if the file already exists
        if (completeFile.exists()) {
            LOGGER.info("Complete weight data file already exists at {}", completeFile.getAbsolutePath());
            return;
        }

        // Create the JSON object
        JsonObject rootJson = new JsonObject();

        // Add all vanilla items with calculated weights
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            if (itemId.getNamespace().equals("minecraft")) {
                float weight = calculateWeight(item);
                rootJson.addProperty(itemId.toString(), weight);
            }
        }

        // Write the JSON to the file
        try (FileWriter writer = new FileWriter(completeFile)) {
            GSON.toJson(rootJson, writer);
        }

        LOGGER.info("Generated complete vanilla weight data file at {}", completeFile.getAbsolutePath());
    }

    /**
     * Registers the weight data generator with NeoForge.
     * This method should be called during mod initialization.
     */
    public static void register() {
        LOGGER.info("Registering weight data generator");
        NeoForge.EVENT_BUS.addListener(WeightDataGenerator::onServerStarted);
    }

    /**
     * Calculates a weight for an item based on its properties.
     *
     * @param item The item to calculate weight for
     * @return The calculated weight
     */
    private static float calculateWeight(Item item) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        String itemKey = itemId.toString();

        // Check for override
        if (OVERRIDES.containsKey(itemKey)) {
            return OVERRIDES.get(itemKey);
        }

        // Check for category
        if (ITEM_CATEGORIES.containsKey(itemKey)) {
            String category = ITEM_CATEGORIES.get(itemKey);
            if (CATEGORIES.containsKey(category)) {
                return CATEGORIES.get(category);
            }
        }

        // Calculate based on item type
        if (item instanceof ArmorItem) {
            return 3.0f;
        } else if (item instanceof TieredItem) {
            return 2.0f;
        } else if (item instanceof BlockItem) {
            return 1.0f;
        }

        // Default weight
        return 0.5f;
    }
}
