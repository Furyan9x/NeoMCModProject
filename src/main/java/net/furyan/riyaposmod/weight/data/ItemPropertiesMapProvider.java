package net.furyan.riyaposmod.weight.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.DataGenerator;

import net.minecraft.tags.TagKey;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.datamaps.DataMapType;


import java.util.concurrent.CompletableFuture;

/**
 * DataMapProvider for generating the `item_properties` data map.
 */
public class ItemPropertiesMapProvider extends DataMapProvider {
    // Register the DataMapType for our item_properties map
    public static final DataMapType<Item, ItemPropertyDataManager.PropertyData> ITEM_PROPERTIES = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath("riyaposmod", "item_properties"),
            Registries.ITEM,
            ItemPropertyDataManager.PropertyData.CODEC
    ).build();

    public ItemPropertiesMapProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, lookupProvider);
    }

    @Override
    protected void gather() {
        // Build the map entries
        builder(ITEM_PROPERTIES)
                // We do not replace by default; only override specific entries
                .replace(false)

                // Example: apply to our custom backpack
                .add(Items.BUNDLE.builtInRegistryHolder(), new ItemPropertyDataManager.PropertyData(0.5f, 64.0f), false)

    }

    @Override
    public String getName() {
        return "RiyaposMod Item Properties DataMap";
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        // other providers here
        generator.addProvider(
                event.includeServer(),
                new ItemPropertiesMapProvider(output, lookupProvider)
        );
    }
}
