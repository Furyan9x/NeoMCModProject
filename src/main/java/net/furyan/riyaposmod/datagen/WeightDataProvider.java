package net.furyan.riyaposmod.datagen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.furyan.riyaposmod.weight.data.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class WeightDataProvider implements DataProvider {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final PackOutput output;
    private final CompletableFuture<HolderLookup.Provider> lookupProvider;
    private final ExistingFileHelper existingFileHelper;

    public WeightDataProvider(PackOutput output, 
                            CompletableFuture<HolderLookup.Provider> lookupProvider,
                            ExistingFileHelper existingFileHelper) {
        this.output = output;
        this.lookupProvider = lookupProvider;
        this.existingFileHelper = existingFileHelper;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        
        // 1. Generate namespace files with defaults
        Map<String, Map<String, DataEntry>> namespaceData = new HashMap<>();
        generateNamespaceData(namespaceData);
        writeNamespaceFiles(namespaceData, cache, futures);

        // 2. Generate container items data
        Map<String, ContainerItemEntry> containerData = new HashMap<>();
        generateContainerData(containerData);
        writeContainerFile(containerData, cache, futures);

        // 3. Generate custom tags data
        Map<String, CustomTagEntry> customTagData = new HashMap<>();
        generateCustomTagData(customTagData);
        writeCustomTagFile(customTagData, cache, futures);

        // 4. Generate normal tag data
        Map<String, DataEntry> tagData = new HashMap<>();
        generateTagData(tagData);
        writeTagFile(tagData, cache, futures);

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private void generateNamespaceData(Map<String, Map<String, DataEntry>> namespaceData) {
        // Process all items for per-item weights
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            String namespace = id.getNamespace();
            
            Map<String, DataEntry> nsMap = namespaceData.computeIfAbsent(namespace, k -> new HashMap<>());
            
            // Add namespace defaults
            if (!nsMap.containsKey("_default")) {
                nsMap.put("_default", new DataEntry(1.0f)); // Default weight for namespace
            }
            
            // Add specific item overrides
            DataEntry entry = computeDefaultEntry(item);
            if (!entry.equals(DataEntry.DEFAULT)) {
                nsMap.put(id.getPath(), entry);
            }
        }
    }

    private void generateContainerData(Map<String, ContainerItemEntry> containerData) {
        // Example container entries
        containerData.put("sophisticatedbackpacks:backpack", 
            new ContainerItemEntry(5.0f, 20.0f, 54, 0.5f, true));
        containerData.put("sophisticatedbackpacks:copper_backpack", 
            new ContainerItemEntry(6.0f, 30.0f, 81, 0.5f, true));
        containerData.put("sophisticatedbackpacks:iron_backpack", 
            new ContainerItemEntry(8.0f, 40.0f, 108, 0.5f, true));
            containerData.put("sophisticatedbackpacks:gold_backpack", 
            new ContainerItemEntry(5.0f, 20.0f, 54, 0.5f, true));
        containerData.put("sophisticatedbackpacks:diamond_backpack", 
            new ContainerItemEntry(6.0f, 30.0f, 81, 0.5f, true));
        containerData.put("sophisticatedbackpacks:netherite_backpack", 
            new ContainerItemEntry(8.0f, 40.0f, 108, 0.5f, true));
        // Add more container entries as needed
    }

    private void generateCustomTagData(Map<String, CustomTagEntry> customTagData) {
        // Example custom tag entries for heavy items
        customTagData.put("minecraft:anvil", new CustomTagEntry(25.0f));
        customTagData.put("minecraft:obsidian", new CustomTagEntry(10.0f));
        customTagData.put("minecraft:netherite_block", new CustomTagEntry(15.0f));
        // Add more custom tag entries as needed
    }

    private void generateTagData(Map<String, DataEntry> tagData) {
        // Vanilla tag weights
        addTagData(tagData, ItemTags.ANVIL, new DataEntry(25.0f));
        addTagData(tagData, ItemTags.LOGS, new DataEntry(2.0f));
    
        // Add more tag defaults as needed
    }

    private void writeNamespaceFiles(Map<String, Map<String, DataEntry>> namespaceData, 
                                   CachedOutput cache, 
                                   List<CompletableFuture<?>> futures) {
        namespaceData.forEach((namespace, data) -> {
            Path path = output.getOutputFolder()
                .resolve("data")
                .resolve("riyaposmod")
                .resolve("weight")
                .resolve("items")
                .resolve(namespace + ".json");
            futures.add(DataProvider.saveStable(cache, GSON.toJsonTree(data), path));
        });
    }

    private void writeContainerFile(Map<String, ContainerItemEntry> containerData, 
                                  CachedOutput cache, 
                                  List<CompletableFuture<?>> futures) {
        Path path = output.getOutputFolder()
            .resolve("data")
            .resolve("riyaposmod")
            .resolve("weight")
            .resolve("container_items")
            .resolve("containers.json");
        futures.add(DataProvider.saveStable(cache, GSON.toJsonTree(containerData), path));
    }

    private void writeCustomTagFile(Map<String, CustomTagEntry> customTagData, 
                                  CachedOutput cache, 
                                  List<CompletableFuture<?>> futures) {
        Path path = output.getOutputFolder()
            .resolve("data")
            .resolve("riyaposmod")
            .resolve("weight")
            .resolve("custom_tags")
            .resolve("custom.json");
        futures.add(DataProvider.saveStable(cache, GSON.toJsonTree(customTagData), path));
    }

    private void writeTagFile(Map<String, DataEntry> tagData, 
                            CachedOutput cache, 
                            List<CompletableFuture<?>> futures) {
        Path path = output.getOutputFolder()
            .resolve("data")
            .resolve("riyaposmod")
            .resolve("weight")
            .resolve("tags.json");
        futures.add(DataProvider.saveStable(cache, GSON.toJsonTree(tagData), path));
    }

    private void addTagData(Map<String, DataEntry> tagData, TagKey<Item> tag, DataEntry entry) {
        tagData.put("#" + tag.location(), entry);
    }

    private DataEntry computeDefaultEntry(Item item) {
        if (item instanceof TieredItem tiered) {
            if (item instanceof SwordItem 
            || item instanceof AxeItem
            || item instanceof TridentItem
            || item instanceof CrossbowItem
            || item instanceof BowItem
            ) {
                float dmg = tiered.getTier().getAttackDamageBonus();
                return new DataEntry(1.0f + dmg * 0.2f);
            }
            float dmg = tiered.getTier().getAttackDamageBonus();
            return new DataEntry(1.0f + dmg * 0.1f);
        }
        
        if (item instanceof ArmorItem armor) {
            float base = switch (armor.getEquipmentSlot()) {
                case HEAD -> 3.0f;
                case CHEST -> 5.0f;
                case LEGS -> 4.0f;
                case FEET -> 2.0f;
                case BODY -> 10.0f;
                default -> 0.0f;
            };
            int defense = armor.getDefense();
            return new DataEntry(base + defense * 0.2f);
        }

        return DataEntry.DEFAULT;
    }

    @Override
    public String getName() {
        return "RiyaposMod Weight Data";
    }
} 