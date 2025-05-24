package net.furyan.riyaposmod.skills.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.furyan.riyaposmod.RiyaposMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.BuiltInRegistries;


import java.io.FileReader;

import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class XPConfigLoader {

    private static final Gson GSON = new Gson();
    private static final String CONFIG_DIR_NAME = "riyaposmod/skills_xp";
    private static final Path CONFIG_PATH = Paths.get("config").resolve(CONFIG_DIR_NAME);

    // We will store parsed configs here. 
    // For mining: Map<Block ResourceLocation OR Tag String, MiningXPConfigEntry>
    // Using String as key for simplicity to hold either "minecraft:stone" or "#forge:ores"
    private static final Map<String, MiningXPConfigEntry> miningXPConfig = new HashMap<>();

    public static void loadAllConfigs() {
        RiyaposMod.LOGGER.info("Loading skill XP configurations...");
        try {
            Files.createDirectories(CONFIG_PATH);
            loadMiningXPConfig();
            // Future: Call loaders for other skills here
        } catch (Exception e) {
            RiyaposMod.LOGGER.error("Failed to create or access skill XP config directory: " + CONFIG_PATH, e);
        }
        RiyaposMod.LOGGER.info("Skill XP configurations loaded.");
    }

    private static void loadMiningXPConfig() {
        Path miningConfigPath = CONFIG_PATH.resolve("mining_xp.json");
        miningXPConfig.clear();

        if (!Files.exists(miningConfigPath)) {
            RiyaposMod.LOGGER.warn("Mining XP config not found: {}. No custom mining XP will be loaded.", miningConfigPath);
            // Optionally, create a default one here or ensure your mod includes a default in JAR that gets copied.
            return;
        }

        try (Reader reader = new FileReader(miningConfigPath.toFile())) {
            Type listType = new TypeToken<ArrayList<MiningXPConfigEntry>>() {}.getType();
            List<MiningXPConfigEntry> entries = GSON.fromJson(reader, listType);
            if (entries != null) {
                for (MiningXPConfigEntry entry : entries) {
                    if (entry.block_id != null && !entry.block_id.isBlank()) {
                        miningXPConfig.put(entry.block_id.trim(), entry);
                    } else {
                        RiyaposMod.LOGGER.warn("Found mining XP entry with null or blank block_id. Skipping.");
                    }
                }
                RiyaposMod.LOGGER.info("Loaded {} mining XP entries from {}", miningXPConfig.size(), miningConfigPath);
            } else {
                RiyaposMod.LOGGER.warn("Mining XP config file {} was empty or malformed.", miningConfigPath);
            }
        } catch (Exception e) {
            RiyaposMod.LOGGER.error("Failed to load mining XP config: " + miningConfigPath, e);
        }
    }

    /**
     * Gets the MiningXPConfigEntry for a given block.
     * It first checks for a direct block ID match, then for matching tags.
     * This needs to be more robust for tag handling.
     */
    public static Optional<MiningXPConfigEntry> getMiningXPConfig(Block block) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);

        // 1. Check direct block ID
        MiningXPConfigEntry entry = miningXPConfig.get(blockId.toString());
        if (entry != null) {
            return Optional.of(entry);
        }

        // 2. Check tags (simple check, assumes tags are stored with leading '#')
        // A more robust system would iterate over all tag entries in the config
        // and check if the block is part of that tag.
        for (TagKey<Block> tagKey : block.defaultBlockState().getTags().toList()) {
            String tagName = "#" + tagKey.location().toString();
            entry = miningXPConfig.get(tagName);
            if (entry != null) {
                // TODO: Add specificity logic if multiple tags match (e.g., most specific tag wins)
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }
} 