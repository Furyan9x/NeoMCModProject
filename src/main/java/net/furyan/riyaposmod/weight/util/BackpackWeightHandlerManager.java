package net.furyan.riyaposmod.weight.util;

import com.mojang.logging.LogUtils;
import net.furyan.riyaposmod.weight.capability.IPlayerWeight;
import net.furyan.riyaposmod.weight.capability.PlayerWeightProvider;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import org.slf4j.Logger;

import java.lang.ref.WeakReference;

import java.util.Optional;
import java.util.UUID;
import java.util.function.IntConsumer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.HashSet;


import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import net.neoforged.fml.ModList;

/**
 * Manages weight calculation handlers for Sophisticated Backpacks equipped in Curios slots or inventory.
 * Centralizes the registration and unregistration logic via scanPlayerForBackpacks.
 */
public final class BackpackWeightHandlerManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String BACKPACK_MOD_ID = "sophisticatedbackpacks";

    // Define relevant Curios slots as a class constant
    private static final Set<String> RELEVANT_CURIO_SLOTS = Set.of("back");

    // Track registered listeners by backpack's UUID to prevent duplicates
    // Use ConcurrentHashMap for potential thread safety if scans happen concurrently (though unlikely with TickTask)
    private static final ConcurrentHashMap<UUID, IntConsumer> registeredListeners = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, WeakReference<ItemStack>> backpackStacks = new ConcurrentHashMap<>(); // Track associated stack

    private BackpackWeightHandlerManager() {
        // Private constructor to prevent instantiation
    }

    /**
     * Checks if the given ItemStack is a Sophisticated Backpack by examining its item ID.
     * 
     * @param stack The ItemStack to check
     * @return true if the stack is a Sophisticated Backpack, false otherwise
     */
    public static boolean isSophisticatedBackpack(ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        // Check the registry name of the item to identify Sophisticated Backpacks items
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return itemId.startsWith(BACKPACK_MOD_ID + ":") && itemId.contains("backpack");
    }

    /**
     * Registers a content change listener for a specific backpack ItemStack if needed.
     * This is now primarily called by scanPlayerForBackpacks.
     */
    private static void registerHandler(ItemStack stack, Player player, UUID backpackUuid) {
        // Basic validation already done by caller (scanPlayerForBackpacks)
        if (registeredListeners.containsKey(backpackUuid)) {
            // Attempt to update the weak reference if it's null but listener exists
            if (backpackStacks.get(backpackUuid) == null || backpackStacks.get(backpackUuid).get() == null) {
                 backpackStacks.put(backpackUuid, new WeakReference<>(stack));
                 LOGGER.debug("Updated weak reference for already registered backpack UUID: {}", backpackUuid);
            }
            return; // Already registered
        }

        LOGGER.debug("Registering new handler for backpack: {} (UUID: {}) for player: {}",
            stack.getItem(), backpackUuid, player.getName().getString());

        try {
            IBackpackWrapper wrapper = BackpackWrapper.fromStack(stack);
            InventoryHandler inventoryHandler = wrapper.getInventoryHandler();

             // Store a weak reference *before* creating the listener that uses it
             backpackStacks.put(backpackUuid, new WeakReference<>(stack));

            // Define the slot change listener
            IntConsumer slotChangeListener = createSlotChangeListener(player, backpackUuid);

            // Add the listener to the backpack's inventory handler
            inventoryHandler.addListener(slotChangeListener);

            // Track the registered listener
            registeredListeners.put(backpackUuid, slotChangeListener);
            LOGGER.info("Successfully registered listener for backpack: {} (UUID: {})", stack.getItem(), backpackUuid);

        } catch (Exception e) {
            LOGGER.error("Error registering listener for backpack: {} (UUID: {})", stack.getItem(), backpackUuid, e);
             // Clean up if registration failed midway
             registeredListeners.remove(backpackUuid);
             backpackStacks.remove(backpackUuid);
        }
    }

     /**
      * Creates the slot change listener lambda. Extracted for clarity.
      */
     private static IntConsumer createSlotChangeListener(Player player, UUID backpackUuid) {
         // Use a weak reference to the player in the listener to potentially help GC
         WeakReference<Player> playerRef = new WeakReference<>(player);

         return (slot) -> {
             Player currentPlayer = playerRef.get();
             if (currentPlayer == null || currentPlayer.level().isClientSide()) {
                 // Player is gone or we're on the client, listener should be removed soon anyway
                 return;
             }

             IPlayerWeight weightCap = PlayerWeightProvider.getPlayerWeight(currentPlayer);
             if (weightCap != null) {
                 LOGGER.debug("Backpack content changed: UUID {}, Slot {}. Attempting find & invalidate.", backpackUuid, slot);

                 // Find the current ItemStack instance corresponding to this UUID
                 LOGGER.trace("Searching for stack with UUID {} for player {}", backpackUuid, currentPlayer.getName().getString());
                 ItemStack currentBackpackStack = findBackpackStackByUUID(currentPlayer, backpackUuid);

                 if (currentBackpackStack != null && !currentBackpackStack.isEmpty()) {
                     LOGGER.trace("Found stack {} for UUID {}. Invalidating cache.", currentBackpackStack.getItem(), backpackUuid);
                     // Invalidate the specific backpack's cache
                     ContainerWeightHelper.invalidateCache(currentBackpackStack, currentPlayer.level().registryAccess());
                     // Optional: Force immediate recalculation of the container's weight if needed
                     // ContainerWeightHelper.getContainerWeight(currentBackpackStack, currentPlayer.level().registryAccess());
                 } else {
                     // Critical log: If this happens for Curios, weight won't update
                     LOGGER.warn("Listener Triggered: Could NOT find ItemStack for backpack UUID {} for player {}. Weight may not update.",
                         backpackUuid, currentPlayer.getName().getString());
                 }
                 // Mark the player's overall weight as dirty
                 weightCap.setDirty(true);
             } else {
                 LOGGER.warn("Listener Triggered: Could not get IPlayerWeight for player {} during backpack change handling.",
                         currentPlayer.getName().getString());
             }
         };
     }

    /**
     * Unregisters the content change listener for a specific backpack UUID.
     * This is now primarily called by scanPlayerForBackpacks.
     */
    public static void unregisterHandler(UUID backpackUuid) {
        if (!registeredListeners.containsKey(backpackUuid)) {
            return; // Not registered
        }

        LOGGER.debug("Unregistering handler for backpack UUID: {}", backpackUuid);

        // We need the ItemStack to get the wrapper to remove the listener.
        // Try to get it from the weak reference map.
        WeakReference<ItemStack> stackRef = backpackStacks.get(backpackUuid);
        ItemStack stack = (stackRef != null) ? stackRef.get() : null;

        // Only attempt to clear listeners on the SB object if we have a valid stack reference.
        if (stack != null && !stack.isEmpty() && isSophisticatedBackpack(stack)) {
            try {
                IBackpackWrapper wrapper = BackpackWrapper.fromStack(stack);
                // Check if UUID still matches, though it should if stackRef was valid
                Optional<UUID> currentUuidOpt = wrapper.getContentsUuid();
                 if (currentUuidOpt.isPresent() && currentUuidOpt.get().equals(backpackUuid)) {
                     IntConsumer listener = registeredListeners.get(backpackUuid);
                     if (listener != null) {
                         // Attempt to remove the specific listener if SB API supports it, otherwise clear all
                         // Assuming clearListeners is the only way for now:
                         wrapper.getInventoryHandler().clearListeners(); // This might remove other listeners too! Be cautious.
                         // Log as debug, as clearing all listeners has potential side effects
                         LOGGER.debug("Cleared ALL listeners via SB handler for backpack UUID {} during unregistration. This might affect other mods.", backpackUuid);
                     }
                 } else {
                     // Log as debug, stack might have changed
                     LOGGER.debug("UUID mismatch or stack changed during unregister for UUID {}", backpackUuid);
                 }
            } catch (Exception e) {
                LOGGER.error("Error accessing backpack wrapper during unregister for UUID: {}", backpackUuid, e);
            }
        } else {
             // Log as debug, this is somewhat expected if the item is gone before the scan runs
             LOGGER.debug("Could not get valid ItemStack reference to clear SB listeners for backpack UUID: {}. Listener might remain on SB object if not GC'd.", backpackUuid);
        }

        // Always remove from our tracking maps regardless of SB listener removal success
        registeredListeners.remove(backpackUuid);
        backpackStacks.remove(backpackUuid);
        LOGGER.info("Removed listener tracking for backpack UUID: {}", backpackUuid);
    }

    /**
     * Clears all registered handlers. Called on server shutdown or player logout.
     */
    public static void clearAllHandlers() {
        int count = registeredListeners.size();
        // Create a copy of keys to avoid ConcurrentModificationException while iterating and removing
         Set<UUID> uuidsToRemove = new HashSet<>(registeredListeners.keySet());
         uuidsToRemove.forEach(BackpackWeightHandlerManager::unregisterHandler); // Use unregister logic per UUID

        // Ensure maps are cleared even if unregister failed for some
        registeredListeners.clear();
        backpackStacks.clear();
        LOGGER.info("Cleared all tracked backpack listeners (attempted count: {})", count);
    }

    /**
     * Scans the player's inventory and relevant Curios slots for backpacks.
     * Registers handlers for new backpacks and unregisters handlers for backpacks no longer present.
     * This is the central function for managing backpack listeners.
     */
    public static void scanPlayerForBackpacks(Player player) {
        if (player == null || player.level().isClientSide()) return;

        LOGGER.debug("Scanning player inventory/curios for backpacks: {}", player.getName().getString());
        Set<UUID> foundBackpackUuids = new HashSet<>();
        // Use array to allow modification within lambda
        final boolean[] potentiallyDirty = {false};

        // 1. Scan Inventory & Curios, collect UUIDs of present backpacks
        // Check main inventory
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isSophisticatedBackpack(stack)) {
                Optional<UUID> uuidOpt = getBackpackUUID(stack);
                if (uuidOpt.isPresent()) {
                    foundBackpackUuids.add(uuidOpt.get());
                    // Register if needed
                    if (!registeredListeners.containsKey(uuidOpt.get())) {
                         registerHandler(stack, player, uuidOpt.get());
                         potentiallyDirty[0] = true; // New backpack found
                         // Invalidate cache for newly registered backpack
                         ContainerWeightHelper.invalidateCache(stack, player.level().registryAccess());
                    } else {
                         // Ensure weak ref is up-to-date
                         WeakReference<ItemStack> currentRef = backpackStacks.get(uuidOpt.get());
                         if (currentRef == null || currentRef.get() == null) {
                             backpackStacks.put(uuidOpt.get(), new WeakReference<>(stack));
                         }
                    }
                } else {
                    LOGGER.trace("Inventory backpack has no UUID yet: {}", stack.getItem());
                }
            }
        }

        // Check Curios slots (using the class constant now)
        // TODO: Confirm if SLOTS_TO_CHECK from WeightEventHandler should be used instead?
        // Using local definition for now as per request.
        if (net.neoforged.fml.ModList.get().isLoaded("curios")) {
            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                handler.getCurios().forEach((slotIdentifier, slotHandler) -> {
                     // Use the class constant here
                     if (RELEVANT_CURIO_SLOTS.contains(slotIdentifier)) {
                        for (int i = 0; i < slotHandler.getSlots(); i++) {
                            ItemStack stack = slotHandler.getStacks().getStackInSlot(i);
                            if (isSophisticatedBackpack(stack)) {
                                Optional<UUID> uuidOpt = getBackpackUUID(stack);
                                if (uuidOpt.isPresent()) {
                                    foundBackpackUuids.add(uuidOpt.get());
                                    if (!registeredListeners.containsKey(uuidOpt.get())) {
                                         registerHandler(stack, player, uuidOpt.get());
                                         potentiallyDirty[0] = true; // New backpack found
                                         // Invalidate cache for newly registered backpack
                                         ContainerWeightHelper.invalidateCache(stack, player.level().registryAccess());
                                    } else {
                                         // Ensure weak ref is up-to-date
                                         WeakReference<ItemStack> currentRef = backpackStacks.get(uuidOpt.get());
                                         if (currentRef == null || currentRef.get() == null) {
                                             backpackStacks.put(uuidOpt.get(), new WeakReference<>(stack));
                                         }
                                    }
                                } else {
                                     LOGGER.trace("Curios backpack has no UUID yet: {}", stack.getItem());
                                }
                            }
                        }
                    }
                });
            });
        }

        // 2. Unregister handlers for backpacks that are no longer present
        Set<UUID> uuidsToUnregister = new HashSet<>(registeredListeners.keySet());
        uuidsToUnregister.removeAll(foundBackpackUuids); // Keep only those not found

        if (!uuidsToUnregister.isEmpty()) {
             LOGGER.debug("Unregistering handlers for missing backpacks: {}", uuidsToUnregister);
             uuidsToUnregister.forEach(BackpackWeightHandlerManager::unregisterHandler);
             potentiallyDirty[0] = true; // Backpack removed
        }

        // 3. Mark player weight dirty if handlers changed or backpacks were found/removed
        if (potentiallyDirty[0]) {
             LOGGER.debug("Backpack scan resulted in changes, marking player weight dirty: {}", player.getName().getString());
             IPlayerWeight weightCap = PlayerWeightProvider.getPlayerWeight(player);
             if (weightCap != null) {
                 weightCap.setDirty(true);
             }
        } else {
            LOGGER.trace("Backpack scan completed, no handler changes needed for player {}", player.getName().getString());
        }
    }

    /**
     * Helper to get the UUID from a backpack stack.
     */
    public static Optional<UUID> getBackpackUUID(ItemStack stack) {
        if (stack.isEmpty() || !isSophisticatedBackpack(stack)) {
            return Optional.empty();
        }
        try {
            return BackpackWrapper.fromStack(stack).getContentsUuid();
        } catch (Exception e) {
            LOGGER.error("Error getting UUID from backpack: {}", stack.getItem(), e);
            return Optional.empty();
        }
    }

     /**
      * Finds the current ItemStack instance for a given backpack UUID in the player's inventory or curios.
      */
     private static ItemStack findBackpackStackByUUID(Player player, UUID backpackUuid) {
         LOGGER.trace("findBackpackStackByUUID: Searching inventory for {}", backpackUuid);
         // Check inventory
         for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
             ItemStack invStack = player.getInventory().getItem(i);
             if (isSophisticatedBackpack(invStack)) {
                 Optional<UUID> stackUuid = getBackpackUUID(invStack);
                 if (stackUuid.isPresent() && stackUuid.get().equals(backpackUuid)) {
                     LOGGER.trace("findBackpackStackByUUID: Found in inventory slot {}", i);
                     return invStack;
                 }
             }
         }

         LOGGER.trace("findBackpackStackByUUID: Searching curios for {}", backpackUuid);
         // Check curios
         if (ModList.get().isLoaded("curios")) {
             final ItemStack[] foundStack = {ItemStack.EMPTY};
             Optional<ICuriosItemHandler> curiosInvOpt = CuriosApi.getCuriosInventory(player);

             if (curiosInvOpt.isPresent()) {
                 ICuriosItemHandler curiosInv = curiosInvOpt.get();
                 // 1. Check the 'back' slot first (preferred)
                 if (RELEVANT_CURIO_SLOTS != null && !RELEVANT_CURIO_SLOTS.isEmpty()) {
                     for (String slotIdentifier : RELEVANT_CURIO_SLOTS) {
                         var slotResult = curiosInv.getCurios().get(slotIdentifier);
                         if (slotResult != null) {
                             for (int i = 0; i < slotResult.getStacks().getSlots(); i++) {
                                 ItemStack curioStack = slotResult.getStacks().getStackInSlot(i);
                                 if (isSophisticatedBackpack(curioStack)) {
                                     Optional<UUID> stackUuid = getBackpackUUID(curioStack);
                                     if (stackUuid.isPresent() && stackUuid.get().equals(backpackUuid)) {
                                         LOGGER.trace("findBackpackStackByUUID: Found in curio 'back' slot {} index {}", slotIdentifier, i);
                                         return curioStack;
                                     }
                                 }
                             }
                         }
                     }
                 }
             } else {
                 LOGGER.trace("findBackpackStackByUUID: Curios inventory ICuriosItemHandler not present for player {}", player.getName().getString());
             }
         } else {
             LOGGER.trace("findBackpackStackByUUID: Curios mod not loaded");
         }

         LOGGER.trace("findBackpackStackByUUID: Searching weak reference map for {}", backpackUuid);
         // Fallback: Check our weak reference map, though it might be stale
         WeakReference<ItemStack> stackRef = backpackStacks.get(backpackUuid);
         if (stackRef != null) {
             ItemStack refStack = stackRef.get();
             // Basic check if the item type is still a backpack
             if (refStack != null && !refStack.isEmpty() && isSophisticatedBackpack(refStack)) {
                 // Verify UUID just in case
                 Optional<UUID> refUuid = getBackpackUUID(refStack);
                 if (refUuid.isPresent() && refUuid.get().equals(backpackUuid)) {
                     LOGGER.trace("findBackpackStackByUUID: Found via weak reference map for UUID {}", backpackUuid);
                     return refStack;
                 }
             }
         }

         LOGGER.trace("findBackpackStackByUUID: Not found anywhere for UUID {}", backpackUuid);
         return null; // Not found
     }

    // --- Removed methods ---
    // needsRegistration is implicitly handled by scanPlayerForBackpacks checking registeredListeners
    // getBackpackHash is not currently used in the refactored logic
    // handleBackpackPickup is removed, its logic integrated into event handlers triggering scans


    /**
     * Utility method to clear handlers associated *only* with a specific player upon logout.
     * This is tricky because listeners are tied to backpack UUIDs, not directly to players.
     * A potential approach is to scan the logged-out player one last time and unregister
     * everything found, but this might affect backpacks shared or dropped.
     * A safer approach might be relying on server shutdown/world unload via clearAllHandlers.
     *
     * For now, this method is a placeholder concept.
     */
    // public static void clearPlayerHandlers(UUID loggedOutPlayerId) {
    //     LOGGER.warn("clearPlayerHandlers functionality is complex and not fully implemented. Relies on scan finding items or clearAllHandlers.");
    //     // TODO: Implement robust player-specific handler clearing if necessary.
    // }
} 