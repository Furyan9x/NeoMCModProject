package net.furyan.riyaposmod.weight.events;

import com.mojang.logging.LogUtils;
import net.furyan.riyaposmod.weight.WeightCalculator;
import net.furyan.riyaposmod.weight.capability.IPlayerWeight;
import net.furyan.riyaposmod.weight.capability.PlayerWeightProvider;
import net.furyan.riyaposmod.weight.util.BackpackWeightHandlerManager;
import net.furyan.riyaposmod.weight.util.ContainerWeightHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.items.IItemHandler;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

/**
 * Handles container-related events for weight calculation.
 * Specifically manages slot changes in containers to track when items
 * (especially containers) are moved, added, or removed.
 */
@EventBusSubscriber(modid = "riyaposmod")
public class ContainerEventHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Tracks currently open containers to prevent duplicate processing
     */
    private static final Set<Integer> openContainers = new HashSet<>();

    /**
     * Container listener implementation to track slot changes
     */
    private static class WeightContainerListener implements ContainerListener {
        private final Player player;
        private final AbstractContainerMenu menu;
        private final boolean isBackpackContainer;

        public WeightContainerListener(Player player, AbstractContainerMenu menu) {
            this.player = player;
            this.menu = menu;
            // Check if this is a Sophisticated Backpacks container by checking class name
            this.isBackpackContainer = menu.getClass().getName().contains("sophisticatedbackpacks") && 
                                       menu.getClass().getName().contains("BackpackContainer");
        }

        @Override
        public void slotChanged(@Nonnull AbstractContainerMenu container, int slotId, ItemStack stack) {
            if (container.containerId != menu.containerId) return;
            
            Slot slot = container.getSlot(slotId);
            ItemStack oldStack = slot.getItem();
            
            // Handle backpacks specially
            if (!oldStack.isEmpty() && BackpackWeightHandlerManager.isSophisticatedBackpack(oldStack)) {
                LOGGER.debug("Backpack removed from slot {}: {}", slotId, oldStack.getItem());
                BackpackWeightHandlerManager.unregisterHandler(oldStack);
            }
            
            if (!stack.isEmpty() && BackpackWeightHandlerManager.isSophisticatedBackpack(stack)) {
                LOGGER.debug("Backpack added to slot {}: {}", slotId, stack.getItem());
                BackpackWeightHandlerManager.handleBackpackPickup(stack, player);
            }
            
            // For standard containers, handle container items
            boolean oldIsContainer = !oldStack.isEmpty() && WeightCalculator.isContainer(oldStack);
            boolean newIsContainer = !stack.isEmpty() && WeightCalculator.isContainer(stack);
            
            if (oldIsContainer || newIsContainer) {
                LOGGER.debug("Container item changed in slot {}: {} -> {}", 
                    slotId,
                    oldStack.isEmpty() ? "empty" : oldStack.getItem().toString(), 
                    stack.isEmpty() ? "empty" : stack.getItem().toString());
                    
                // Invalidate caches for both old and new containers if they exist
                if (oldIsContainer) {
                    ContainerWeightHelper.invalidateCache(oldStack, player.level().registryAccess());
                }
                if (newIsContainer) {
                    ContainerWeightHelper.invalidateCache(stack, player.level().registryAccess());
                }
                
                // Mark player weight as dirty
                IPlayerWeight playerWeight = PlayerWeightProvider.getPlayerWeight(player);
                if (playerWeight != null) {
                    playerWeight.setDirty(true);
                }
            }
        }

        @Override
        public void dataChanged(@Nonnull AbstractContainerMenu container, int slotId, int value) {
            // Not needed for weight tracking
        }
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (event.getEntity().level().isClientSide()) return;
        
        Player player = event.getEntity();
        int containerId = event.getContainer().containerId;
        AbstractContainerMenu menu = event.getContainer();
        
        LOGGER.info("Container opened: {} (type: {}) for player {}", 
            containerId, 
            menu.getClass().getName(), 
            player.getName().getString());
        
        if (openContainers.add(containerId)) {
            // Add our container listener to track slot changes
            menu.addSlotListener(new WeightContainerListener(player, menu));
            
            // Only do a full scan if this is a new container type, not just a vanilla container
            boolean isSophisticatedContainer = menu.getClass().getName().contains("sophisticatedbackpacks");
            
            if (isSophisticatedContainer) {
                // For sophisticated backpacks containers, we need to ensure backpack handlers are properly registered
                BackpackWeightHandlerManager.scanPlayerForBackpacks(player);
            }
            
            // Mark player weight as dirty when container is opened
            IPlayerWeight playerWeight = PlayerWeightProvider.getPlayerWeight(player);
            if (playerWeight != null) {
                playerWeight.setDirty(true);
            }
        }
    }

    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (event.getEntity().level().isClientSide()) return;
        
        Player player = event.getEntity();
        int containerId = event.getContainer().containerId;
        
        LOGGER.info("Container closed: {} (type: {}) for player {}", 
            containerId, 
            event.getContainer().getClass().getName(), 
            player.getName().getString());
        
        if (openContainers.remove(containerId)) {
            // Invalidate container caches and recalculate weight
            invalidateContainerCaches(event.getContainer(), player);
            
            // Ensure backpack handlers are properly registered
            BackpackWeightHandlerManager.scanPlayerForBackpacks(player);
            
            // Mark player weight as dirty
            IPlayerWeight playerWeight = PlayerWeightProvider.getPlayerWeight(player);
            if (playerWeight != null) {
                playerWeight.setDirty(true);
            }
        }
    }

    /**
     * Recursively invalidates caches for all containers in the given container
     */
    private static void invalidateContainerCaches(AbstractContainerMenu container, Player player) {
        // Process all slots in the container
        for (int i = 0; i < container.slots.size(); i++) {
            ItemStack stack = container.getSlot(i).getItem();
            if (!stack.isEmpty() && WeightCalculator.isContainer(stack)) {
                ContainerWeightHelper.invalidateCache(stack, player.level().registryAccess());
                
                // If this container has an item handler, process its contents recursively
                IItemHandler handler = stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.ITEM);
                if (handler != null) {
                    for (int j = 0; j < handler.getSlots(); j++) {
                        ItemStack nestedStack = handler.getStackInSlot(j);
                        if (!nestedStack.isEmpty() && WeightCalculator.isContainer(nestedStack)) {
                            ContainerWeightHelper.invalidateCache(nestedStack, player.level().registryAccess());
                        }
                    }
                }
            }
        }
    }
} 