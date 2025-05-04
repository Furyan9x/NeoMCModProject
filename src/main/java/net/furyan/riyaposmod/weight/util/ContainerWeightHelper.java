package net.furyan.riyaposmod.weight.util;

import com.mojang.logging.LogUtils;
import net.furyan.riyaposmod.weight.WeightCalculator;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for calculating weights of container items and their contents.
 * Handles recursion protection, caching, and dirty flags for performance.
 */
public final class ContainerWeightHelper {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_RECURSION_DEPTH = 3;
    
    // Cache container weights with NBT hash as key
    private static final ConcurrentHashMap<Integer, CacheEntry> weightCache = new ConcurrentHashMap<>();
    
    // Track dirty containers to invalidate their parents
    private static final Set<Integer> dirtyContainers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    private ContainerWeightHelper() {} // Prevent instantiation
    
    /**
     * Cache entry for container weights
     */
    private static class CacheEntry {
        final float weight;
        final long timestamp;
        final Set<Integer> childContainers;
        
        CacheEntry(float weight, Set<Integer> childContainers) {
            this.weight = weight;
            this.timestamp = System.currentTimeMillis();
            this.childContainers = childContainers;
        }
    }
    
    /**
     * Gets the total weight of a container including its contents.
     * Uses caching for performance and prevents infinite recursion.
     *
     * @param containerStack The container ItemStack to calculate weight for
     * @param provider The HolderLookup.Provider from the world context
     * @return The total weight of the container and its contents
     */
    public static float getContainerWeight(ItemStack containerStack, HolderLookup.Provider provider) {
        if (containerStack.isEmpty() || !WeightCalculator.isContainer(containerStack)) {
            return 0f;
        }
        
        // Use trace level instead of debug to reduce log spam
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Calculating weight for container: {} ({})", 
                containerStack.getItem().toString(), 
                containerStack.getCount());
        }
        
        // Use NBT hash as cache key
        CompoundTag nbt = (CompoundTag) containerStack.saveOptional(provider);
        int cacheKey = nbt != null ? nbt.hashCode() : 0;
        
        // Check if this container or any of its children are dirty
        if (isDirty(cacheKey)) {
            LOGGER.debug("Container {} or its children are dirty, recalculating", 
                containerStack.getItem().toString());
            return recalculateWeight(containerStack, cacheKey, provider);
        }
        
        // Check cache
        CacheEntry entry = weightCache.get(cacheKey);
        if (entry != null) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Found cached weight for container {}: {}", 
                    containerStack.getItem().toString(), 
                    entry.weight);
            }
            return entry.weight;
        }
        
        // Calculate weight if not cached
        return recalculateWeight(containerStack, cacheKey, provider);
    }
    
    /**
     * Checks if a container or any of its children are dirty
     */
    private static boolean isDirty(int containerKey) {
        if (dirtyContainers.contains(containerKey)) {
            return true;
        }
        
        CacheEntry entry = weightCache.get(containerKey);
        if (entry != null) {
            for (int childKey : entry.childContainers) {
                if (isDirty(childKey)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Recalculates the weight of a container and updates the cache
     */
    private static float recalculateWeight(ItemStack containerStack, int cacheKey, HolderLookup.Provider provider) {
        Set<Integer> childContainers = new HashSet<>();
        float weight = calculateContainerWeight(containerStack, new HashSet<>(), 0, provider, childContainers);
        
        // Update cache with new weight and child containers
        weightCache.put(cacheKey, new CacheEntry(weight, childContainers));
        // Remove from dirty set since we just recalculated
        dirtyContainers.remove(cacheKey);
        
        LOGGER.debug("Recalculated and cached weight for container {}: {} (children: {})", 
            containerStack.getItem().toString(), 
            weight,
            childContainers.size());
            
        return weight;
    }
    
    /**
     * Internal method to calculate container weight with recursion protection.
     * Also tracks child containers for cache invalidation.
     */
    private static float calculateContainerWeight(ItemStack containerStack, Set<Item> visited, int depth, 
            HolderLookup.Provider provider, Set<Integer> childContainers) {
        if (depth >= MAX_RECURSION_DEPTH || !visited.add(containerStack.getItem())) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Skipping container {} due to {} (depth: {}, already visited: {})", 
                    containerStack.getItem().toString(),
                    depth >= MAX_RECURSION_DEPTH ? "max depth" : "recursion protection",
                    depth,
                    !visited.add(containerStack.getItem()));
            }
            return 0f;
        }
        
        float totalWeight = 0f;
        
        // Get the container's inventory capability
        IItemHandler inventory = containerStack.getCapability(Capabilities.ItemHandler.ITEM);
        if (inventory != null) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Checking container {} contents ({} slots)", 
                    containerStack.getItem().toString(),
                    inventory.getSlots());
            }
                
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    float itemWeight = WeightCalculator.getWeight(stack) * stack.getCount();
                    
                    // If this is also a container, recursively calculate its contents
                    if (WeightCalculator.isContainer(stack)) {
                        // Add child container to tracking set
                        CompoundTag childNbt = (CompoundTag) stack.saveOptional(provider);
                        if (childNbt != null) {
                            childContainers.add(childNbt.hashCode());
                        }
                        
                        float containerContentWeight = calculateContainerWeight(stack, visited, depth + 1, provider, childContainers);
                        itemWeight += containerContentWeight;
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("Found nested container {} in slot {} with content weight: {}", 
                                stack.getItem().toString(), 
                                i,
                                containerContentWeight);
                        }
                    }
                    
                    totalWeight += itemWeight;
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Item in slot {}: {} x{} - Weight: {}", 
                            i,
                            stack.getItem().toString(),
                            stack.getCount(),
                            itemWeight);
                    }
                }
            }
        } else {
            LOGGER.warn("Container {} does not have an IItemHandler capability!", 
                containerStack.getItem().toString());
        }
        
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Total weight for container {} at depth {}: {}", 
                containerStack.getItem().toString(),
                depth,
                totalWeight);
        }
            
        return totalWeight;
    }
    
    /**
     * Invalidates the cache for a specific container.
     * Should be called when the container's contents change.
     *
     * @param containerStack The container whose cache should be invalidated
     * @param provider The HolderLookup.Provider from the world context
     */
    public static void invalidateCache(ItemStack containerStack, HolderLookup.Provider provider) {
        if (!containerStack.isEmpty()) {
            CompoundTag nbt = (CompoundTag) containerStack.saveOptional(provider);
            if (nbt != null) {
                int cacheKey = nbt.hashCode();
                // Check if it was already dirty before adding
                boolean wasAlreadyDirty = dirtyContainers.contains(cacheKey);
                dirtyContainers.add(cacheKey);
                if (!wasAlreadyDirty) {
                     LOGGER.debug("Marked container {} (CacheKey: {}) as dirty for cache invalidation.",
                         containerStack.getItem(), cacheKey);
                } else {
                     LOGGER.trace("Container {} (CacheKey: {}) was already marked dirty.",
                          containerStack.getItem(), cacheKey);
                }
            } else {
                 LOGGER.warn("Attempted to invalidate cache for container {} but could not get NBT.", containerStack.getItem());
            }
        } else {
             LOGGER.trace("Attempted to invalidate cache for empty ItemStack.");
        }
    }
    
    /**
     * Clears the entire weight cache and dirty tracking.
     * Should be called on world unload or when data packs are reloaded.
     */
    public static void clearCache() {
        weightCache.clear();
        dirtyContainers.clear();
        LOGGER.debug("Cleared container weight cache and dirty tracking");
    }
    
    /**
     * Gets an unmodifiable view of the currently visited items.
     * Useful for debugging and testing.
     *
     * @param containerStack The container ItemStack to analyze
     * @param provider The HolderLookup.Provider from the world context
     * @return An unmodifiable set of visited items
     */
    public static Set<Item> getVisitedItems(ItemStack containerStack, HolderLookup.Provider provider) {
        Set<Item> visited = new HashSet<>();
        Set<Integer> childContainers = new HashSet<>();
        if (!containerStack.isEmpty() && WeightCalculator.isContainer(containerStack)) {
            calculateContainerWeight(containerStack, visited, 0, provider, childContainers);
        }
        return Collections.unmodifiableSet(visited);
    }
} 