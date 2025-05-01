package net.furyan.riyaposmod.util;

import net.furyan.riyaposmod.RiyaposMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModTags {
    public static class Items {
        public static final TagKey<Item> LIGHT_TOOLS = createTag("light_tools");
        // Tag for items that should not be equippable in the chest slot
        public static final TagKey<Item> NO_CHEST_EQUIP = createTag("no_chest_equip");

        private static TagKey<Item> createTag(String name) {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath(RiyaposMod.MOD_ID, name));
        }
    }


    public static class Blocks {



        private static TagKey<Block> createTag(String name) {
            return BlockTags.create(ResourceLocation.fromNamespaceAndPath(RiyaposMod.MOD_ID, name));
        }
    }
}
