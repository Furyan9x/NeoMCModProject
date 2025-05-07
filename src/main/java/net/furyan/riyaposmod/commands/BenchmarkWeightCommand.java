package net.furyan.riyaposmod.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.furyan.riyaposmod.weight.util.BenchmarkingHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Command to run the weight/capacity/container benchmark in-game.
 * Usage: /benchmarkweight (OPs only)
 */
public class BenchmarkWeightCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("benchmarkweight")
            .requires(source -> source.hasPermission(2)) // OPs only
            .executes(context -> runBenchmark(context.getSource()))
        );
    }

    private static void fillContainerWithTestItems(ItemStack container, ItemStack... testItems) {
        IItemHandler handler = container.getCapability(Capabilities.ItemHandler.ITEM);
        if (handler != null) {
            int slot = 0;
            for (ItemStack testItem : testItems) {
                for (int i = 0; i < 100 && slot < handler.getSlots(); i++, slot++) {
                    handler.insertItem(slot, testItem.copy(), false);
                }
            }
        }
    }

    private static void fillBackpackWithTestItems(ItemStack backpack, ItemStack... testItems) {
        try {
            InventoryHandler handler = BackpackWrapper.fromStack(backpack).getInventoryHandler();
            int slot = 0;
            for (ItemStack testItem : testItems) {
                for (int i = 0; i < 100 && slot < handler.getSlots(); i++, slot++) {
                    handler.setStackInSlot(slot, testItem.copy());
                }
            }
        } catch (Exception e) {
            // Ignore if not a valid backpack
        }
    }

    private static int runBenchmark(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        HolderLookup.Provider provider = server.registryAccess();
        List<ItemStack> items = new ArrayList<>();
        List<ItemStack> containers = new ArrayList<>();
        // Use real items from the registry
        Item stonePickaxe = BuiltInRegistries.ITEM.get(ResourceLocation.parse("minecraft:stone_pickaxe"));
        Item ironSword = BuiltInRegistries.ITEM.get(ResourceLocation.parse("minecraft:iron_sword"));
        Item ironChestplate = BuiltInRegistries.ITEM.get(ResourceLocation.parse("minecraft:iron_chestplate"));
        Item chest = BuiltInRegistries.ITEM.get(ResourceLocation.parse("minecraft:chest"));
        Item sbBackpack = BuiltInRegistries.ITEM.get(ResourceLocation.parse("sophisticatedbackpacks:backpack"));
        ItemStack[] testItems = new ItemStack[] {
            new ItemStack(stonePickaxe),
            new ItemStack(ironSword),
            new ItemStack(ironChestplate)
        };
        for (int i = 0; i < 5000; i++) {
            items.add(new ItemStack(stonePickaxe));
            items.add(new ItemStack(ironSword));
            items.add(new ItemStack(ironChestplate));
        }
        for (int i = 0; i < 1000; i++) {
            ItemStack chestStack = new ItemStack(chest);
            fillContainerWithTestItems(chestStack, testItems);
            containers.add(chestStack);
        }
        for (int i = 0; i < 1000; i++) {
            ItemStack sbStack = new ItemStack(sbBackpack);
            fillBackpackWithTestItems(sbStack, testItems);
            items.add(sbStack);
            containers.add(sbStack);
        }
        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("--- Benchmark: Item Weight Calculation ---"), false);
        BenchmarkingHelper.benchmarkItemWeight(items, 10);
        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("--- Benchmark: Capacity Bonus Calculation ---"), false);
        BenchmarkingHelper.benchmarkCapacityBonus(items, 10);
        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("--- Benchmark: Container Weight Calculation ---"), false);
        BenchmarkingHelper.benchmarkContainerWeight(containers, provider, 10);
        if (source.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            net.furyan.riyaposmod.weight.util.BackpackWeightHandlerManager.scanPlayerForBackpacks(player);
            source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("Triggered SB backpack handler scan for your inventory."), false);
        }
        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("--- Benchmark Complete ---"), false);
        return Command.SINGLE_SUCCESS;
    }
} 