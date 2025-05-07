package net.furyan.riyaposmod.weight.util;

import com.mojang.logging.LogUtils;
import net.furyan.riyaposmod.weight.WeightCalculator;
import net.furyan.riyaposmod.weight.data.WeightDataManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Utility for benchmarking the weight and capacity system.
 * Provides static methods to benchmark item weight, capacity bonus, and container weight calculations.
 * Logs timings and results to the console.
 */
public class BenchmarkingHelper {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Benchmarks item weight calculation for a large number of items.
     * @param items List of ItemStacks to test
     * @param iterations Number of times to repeat the calculation
     * @return Time taken in milliseconds
     */
    public static long benchmarkItemWeight(List<ItemStack> items, int iterations) {
        long start = System.nanoTime();
        float total = 0f;
        for (int i = 0; i < iterations; i++) {
            for (ItemStack stack : items) {
                total += WeightCalculator.getWeight(stack);
            }
        }
        long end = System.nanoTime();
        LOGGER.info("[Benchmark] Item weight calculation: {} items x {} iterations = {} ms (total: {})", items.size(), iterations, (end - start) / 1_000_000, total);
        return (end - start) / 1_000_000;
    }

    /**
     * Benchmarks capacity bonus calculation for a large number of items.
     * @param items List of ItemStacks to test
     * @param iterations Number of times to repeat the calculation
     * @return Time taken in milliseconds
     */
    public static long benchmarkCapacityBonus(List<ItemStack> items, int iterations) {
        long start = System.nanoTime();
        float total = 0f;
        for (int i = 0; i < iterations; i++) {
            for (ItemStack stack : items) {
                total += WeightCalculator.getCapacityBonus(stack);
            }
        }
        long end = System.nanoTime();
        LOGGER.info("[Benchmark] Capacity bonus calculation: {} items x {} iterations = {} ms (total: {})", items.size(), iterations, (end - start) / 1_000_000, total);
        return (end - start) / 1_000_000;
    }

    /**
     * Benchmarks container weight calculation for a list of containers.
     * @param containers List of ItemStacks (containers) to test
     * @param provider HolderLookup.Provider for NBT/context
     * @param iterations Number of times to repeat the calculation
     * @return Time taken in milliseconds
     */
    public static long benchmarkContainerWeight(List<ItemStack> containers, HolderLookup.Provider provider, int iterations) {
        long start = System.nanoTime();
        float total = 0f;
        for (int i = 0; i < iterations; i++) {
            for (ItemStack stack : containers) {
                total += ContainerWeightHelper.getContainerWeight(stack, provider);
            }
        }
        long end = System.nanoTime();
        LOGGER.info("[Benchmark] Container weight calculation: {} containers x {} iterations = {} ms (total: {})", containers.size(), iterations, (end - start) / 1_000_000, total);
        return (end - start) / 1_000_000;
    }

    /**
     * Example main method for standalone benchmarking (dev only).
     * Replace with real items/containers as needed.
     */
    public static void main(String[] args) {
        // --- Ready-to-run benchmark: Use real Minecraft items ---
        List<ItemStack> items = new ArrayList<>();
        List<ItemStack> containers = new ArrayList<>();
        // Use a few real items from the registry
        Item stone = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("minecraft:stone"));
        Item ironSword = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("minecraft:iron_sword"));
        Item chest = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("minecraft:chest"));
        // Fill with 10,000 items (mix of types)
        for (int i = 0; i < 5000; i++) {
            items.add(new ItemStack(stone));
            items.add(new ItemStack(ironSword));
        }
        // Create 1000 chests, each with a stone and a sword inside (simulate containers)
        for (int i = 0; i < 1000; i++) {
            ItemStack chestStack = new ItemStack(chest);
            // NOTE: In a real mod, you would set up the chest's NBT to contain items here
            // For this test, we use empty chests (replace with real filled containers for deeper tests)
            containers.add(chestStack);
        }
        // Use a dummy HolderLookup.Provider (replace with real one in mod integration)
        HolderLookup.Provider provider = null;
        System.out.println("--- Benchmark: Item Weight Calculation ---");
        benchmarkItemWeight(items, 10);
        System.out.println("--- Benchmark: Capacity Bonus Calculation ---");
        benchmarkCapacityBonus(items, 10);
        System.out.println("--- Benchmark: Container Weight Calculation ---");
        benchmarkContainerWeight(containers, provider, 10);
        System.out.println("--- Benchmark Complete ---");
    }
} 