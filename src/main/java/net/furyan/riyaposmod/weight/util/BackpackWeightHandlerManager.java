package net.furyan.riyaposmod.weight.util;

import com.mojang.logging.LogUtils;
import net.furyan.riyaposmod.registries.WeightAttachmentRegistry;
import net.furyan.riyaposmod.weight.capability.IPlayerWeight;
import net.furyan.riyaposmod.weight.events.WeightEventHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import org.slf4j.Logger;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.IntConsumer;

import net.minecraft.nbt.CompoundTag;

/**
 * Manages weight calculation handlers for Sophisticated Backpacks equipped in Curios slots or inventory.
 * Ensures that weight is recalculated instantly when backpack contents change.
 * <p>
 * This class directly hooks into the Sophisticated Backpacks InventoryHandler's listener system
 * to detect when items are added/removed/modified within backpacks.
 * </p>
 */
public final class BackpackWeightHandlerManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String BACKPACK_MOD_ID = "sophisticatedbackpacks";

    // Track registered listeners by backpack's UUID to prevent duplicates
    // and allow proper cleanup when the backpack is unequipped
    private static final Map<UUID, IntConsumer> registeredListeners = new HashMap<>();
    private static final Map<UUID, WeakReference<ItemStack>> backpackStacks = new HashMap<>();

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
     * Registers a content change listener for the given backpack ItemStack.
     * The listener invalidates the container weight cache and marks the player's weight as dirty
     * whenever items are added/removed from the backpack.
     *
     * @param stack  The backpack ItemStack
     * @param player The player equipping the backpack
     */
    public static void registerHandler(ItemStack stack, Player player) {
        if (stack.isEmpty() || player == null || player.level().isClientSide()) {
            return; // Do nothing for empty stacks, null players, or on the client side
        }

        // Skip if not a Sophisticated Backpack
        if (!isSophisticatedBackpack(stack)) {
            LOGGER.debug("Item is not a Sophisticated Backpack, skipping handler registration: {}", stack.getItem());
            return;
        }

        LOGGER.debug("Attempting to register handler for backpack: {}", stack.getItem());

        try {
            // Get the wrapper using the static fromStack method
            IBackpackWrapper wrapper = BackpackWrapper.fromStack(stack);
            
            // Get the backpack's UUID, which uniquely identifies this backpack instance
            Optional<UUID> backpackUuidOpt = wrapper.getContentsUuid();
            if (backpackUuidOpt.isEmpty()) {
                LOGGER.debug("Backpack has no UUID yet (new/unused), can't register handler: {}", stack.getItem());
                return;
            }
            
            UUID backpackUuid = backpackUuidOpt.get();
            
            // Skip if a handler is already registered for this backpack UUID
            if (registeredListeners.containsKey(backpackUuid)) {
                LOGGER.debug("Handler already registered for backpack UUID: {}", backpackUuid);
                return;
            }

            // Store a weak reference to the backpack stack
            backpackStacks.put(backpackUuid, new WeakReference<>(stack));
            
            // Define the slot change listener to be executed when backpack contents change
            IntConsumer slotChangeListener = (slot) -> {
                IPlayerWeight weightCap = player.getData(WeightAttachmentRegistry.PLAYER_WEIGHT_ATTACHMENT);
                if (weightCap != null) {
                    LOGGER.info("Backpack contents changed in slot {} for player {}: Invalidating cache", 
                            slot, player.getName().getString());
                    // Get fresh reference to the backpack stack
                    final ItemStack[] currentStack = {null};
                    // Check inventory first
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        ItemStack invStack = player.getInventory().getItem(i);
                        if (!invStack.isEmpty() && isSophisticatedBackpack(invStack)) {
                            Optional<UUID> stackUuid = BackpackWrapper.fromStack(invStack).getContentsUuid();
                            if (stackUuid.isPresent() && stackUuid.get().equals(backpackUuid)) {
                                currentStack[0] = invStack;
                                break;
                            }
                        }
                    }
                    // If not found in inventory, check curios
                    if (currentStack[0] == null && net.neoforged.fml.ModList.get().isLoaded("curios")) {
                        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                            for (String slotType : WeightEventHandler.SLOTS_TO_CHECK) {
                                var slotHandler = handler.getCurios().get(slotType);
                                if (slotHandler != null) {
                                    for (int i = 0; i < slotHandler.getSlots(); i++) {
                                        ItemStack curioStack = slotHandler.getStacks().getStackInSlot(i);
                                        if (!curioStack.isEmpty() && isSophisticatedBackpack(curioStack)) {
                                            Optional<UUID> stackUuid = BackpackWrapper.fromStack(curioStack).getContentsUuid();
                                            if (stackUuid.isPresent() && stackUuid.get().equals(backpackUuid)) {
                                                currentStack[0] = curioStack;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        });
                    }
                    
                    if (currentStack[0] != null) {
                        ContainerWeightHelper.invalidateCache(currentStack[0], player.level().registryAccess());
                        // Force immediate weight calculation
                        ContainerWeightHelper.getContainerWeight(currentStack[0], player.level().registryAccess());
                    } else {
                        LOGGER.warn("Could not find backpack with UUID {} in player inventory or curios", backpackUuid);
                    }
                    weightCap.setDirty(true);
                } else {
                    LOGGER.warn("Could not get IPlayerWeight capability for player {} during backpack change handling", 
                            player.getName().getString());
                }
            };

            // Access the underlying InventoryHandler and register our listener directly
            InventoryHandler inventoryHandler = wrapper.getInventoryHandler();
            inventoryHandler.addListener(slotChangeListener);
            
            // Log detailed backpack info to help diagnose integration issues
            LOGGER.info("Backpack details - Type: {}, Slots: {}, UUID: {}", 
                    stack.getItem().toString(), 
                    inventoryHandler.getSlots(),
                    backpackUuid);

            // Track this backpack as having a registered listener
            registeredListeners.put(backpackUuid, slotChangeListener);
            LOGGER.info("Successfully registered inventory change listener for backpack: {} (UUID: {}) for player: {}", 
                    stack.getItem(), backpackUuid, player.getName().getString());
        } catch (Exception e) {
            LOGGER.error("Error registering content change listener for backpack: {}", stack.getItem(), e);
        }
    }

    /**
     * Unregisters the content change listener for the given backpack ItemStack.
     * Should be called when the backpack is unequipped or the player logs out.
     *
     * @param stack The backpack ItemStack
     */
    public static void unregisterHandler(ItemStack stack) {
        if (stack.isEmpty() || !isSophisticatedBackpack(stack)) {
            return;
        }

        LOGGER.debug("Attempting to unregister handler for backpack: {}", stack.getItem());

        try {
            // Get the wrapper and UUID
            IBackpackWrapper wrapper = BackpackWrapper.fromStack(stack);
            Optional<UUID> backpackUuidOpt = wrapper.getContentsUuid();
            
            if (backpackUuidOpt.isEmpty()) {
                LOGGER.debug("Backpack has no UUID, nothing to unregister: {}", stack.getItem());
                return;
            }
            
            UUID backpackUuid = backpackUuidOpt.get();

            // Only proceed if we have a listener registered for this backpack UUID
            if (registeredListeners.containsKey(backpackUuid)) {
                // Note: We can't selectively remove our listener, so we need to clear all
                // The backpack will re-register any internal listeners it needs
                wrapper.getInventoryHandler().clearListeners();
                
                // Remove from our tracking map
                registeredListeners.remove(backpackUuid);
                backpackStacks.remove(backpackUuid);
                LOGGER.info("Unregistered inventory change listener for backpack: {} (UUID: {})", 
                        stack.getItem(), backpackUuid);
            } else {
                LOGGER.debug("No handler was registered for backpack UUID: {}, skipping unregister", backpackUuid);
            }
        } catch (Exception e) {
            LOGGER.error("Error unregistering content change listener for backpack: {}", stack.getItem(), e);
        }
    }
    
    /**
     * Clears all registered handlers.
     * Should be called on server shutdown or world unload.
     */
    public static void clearAllHandlers() {
        int count = registeredListeners.size();
        registeredListeners.clear();
        backpackStacks.clear();
        LOGGER.info("Cleared all backpack inventory change listeners (count: {})", count);
    }

    /**
     * Checks if the given backpack needs a handler registration.
     * This avoids registering handlers multiple times for the same backpack.
     *
     * @param stack The backpack ItemStack to check
     * @return true if a handler needs to be registered, false otherwise
     */
    public static boolean needsRegistration(ItemStack stack) {
        if (stack.isEmpty() || !isSophisticatedBackpack(stack)) {
            return false;
        }
        
        try {
            // Get the wrapper and UUID
            IBackpackWrapper wrapper = BackpackWrapper.fromStack(stack);
            Optional<UUID> backpackUuidOpt = wrapper.getContentsUuid();
            
            // New backpacks without UUID will need registration later
            if (backpackUuidOpt.isEmpty()) {
                return true;
            }
            
            UUID backpackUuid = backpackUuidOpt.get();
            
            // Check if the backpack is registered
            if (!registeredListeners.containsKey(backpackUuid)) return true;
            
            // Check if the stored backpack reference is still valid
            WeakReference<ItemStack> storedBackpackRef = backpackStacks.get(backpackUuid);
            if (storedBackpackRef == null || storedBackpackRef.get() == null) {
                // Clean up invalid reference
                registeredListeners.remove(backpackUuid);
                backpackStacks.remove(backpackUuid);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            LOGGER.error("Error checking if backpack needs registration: {}", stack.getItem(), e);
            return false; // Safer to return false on error
        }
    }

    /**
     * Creates a unique hash for a backpack based on its NBT data
     * Used for tracking which backpacks have been processed
     */
    public static int getBackpackHash(ItemStack stack, Player player) {
        // Use the stack's NBT hash if available, otherwise fallback to simple hash
        if (!stack.isEmpty()) {
            CompoundTag nbt = (CompoundTag) stack.saveOptional(player.level().registryAccess());
            if (nbt != null) {
                return nbt.hashCode();
            }
        }
        return stack.getItem().hashCode() + stack.getCount();
    }

    /**
     * Handle a backpack being picked up by a player
     * Centralizes all backpack pickup logic in one place
     */
    public static void handleBackpackPickup(ItemStack backpackStack, Player player) {
        if (player.level().isClientSide() || backpackStack.isEmpty() || !isSophisticatedBackpack(backpackStack)) {
            return;
        }

        LOGGER.debug("Handling backpack pickup: {} for player: {}", 
            backpackStack.getItem(), player.getName().getString());
        
        // Invalidate cache and ensure handler is registered
        ContainerWeightHelper.invalidateCache(backpackStack, player.level().registryAccess());
        
        if (needsRegistration(backpackStack)) {
            registerHandler(backpackStack, player);
            LOGGER.debug("Registered handler for picked up backpack: {}", backpackStack.getItem());
        } else {
            LOGGER.debug("Backpack already has handler, skipping registration: {}", backpackStack.getItem());
        }
        
        // Force immediate weight calculation to ensure accurate values
        ContainerWeightHelper.getContainerWeight(backpackStack, player.level().registryAccess());
        
        // Mark player's weight as dirty
        IPlayerWeight weightCap = player.getData(WeightAttachmentRegistry.PLAYER_WEIGHT_ATTACHMENT);
        if (weightCap != null) {
            weightCap.setDirty(true);
            LOGGER.debug("Marked player weight dirty after backpack pickup");
        }
    }
    
    /**
     * Scan the player's inventory and curios slots for backpacks
     * and ensure they all have properly registered handlers
     */
    public static void scanPlayerForBackpacks(Player player) {
        if (player.level().isClientSide()) return;
        
        LOGGER.debug("Scanning player inventory for backpacks: {}", player.getName().getString());
        final boolean[] foundBackpack = {false};
        
        // Check main inventory
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isSophisticatedBackpack(stack)) {
                foundBackpack[0] = true;
                
                if (needsRegistration(stack)) {
                    registerHandler(stack, player);
                    LOGGER.debug("Registered handler for backpack in inventory slot {}: {}", i, stack.getItem());
                }
            }
        }
        
        // Check Curios slots
        if (net.neoforged.fml.ModList.get().isLoaded("curios")) {
            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                for (String slotType : net.furyan.riyaposmod.weight.events.WeightEventHandler.SLOTS_TO_CHECK) {
                    var slotHandler = handler.getCurios().get(slotType);
                    if (slotHandler != null) {
                        for (int i = 0; i < slotHandler.getSlots(); i++) {
                            ItemStack stack = slotHandler.getStacks().getStackInSlot(i);
                            if (isSophisticatedBackpack(stack)) {
                                foundBackpack[0] = true;
                                
                                if (needsRegistration(stack)) {
                                    registerHandler(stack, player);
                                    LOGGER.debug("Registered handler for backpack in curio slot {}: {}", slotType, stack.getItem());
                                }
                            }
                        }
                    }
                }
            });
        }
        
        // Mark player weight as dirty if we found any backpacks
        if (foundBackpack[0]) {
            LOGGER.debug("Found backpacks in player inventory, marking weight dirty");
            IPlayerWeight weightCap = player.getData(WeightAttachmentRegistry.PLAYER_WEIGHT_ATTACHMENT);
            if (weightCap != null) {
                weightCap.setDirty(true);
            }
        } else {
            LOGGER.debug("No backpacks found in player inventory");
        }
    }
} 