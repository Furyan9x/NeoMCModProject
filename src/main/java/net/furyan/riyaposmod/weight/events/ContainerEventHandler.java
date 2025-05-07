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
import net.neoforged.neoforge.capabilities.Capabilities;
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

        public WeightContainerListener(Player player, AbstractContainerMenu menu) {
            this.player = player;
            this.menu = menu;
        }

        @Override
        public void slotChanged(@Nonnull AbstractContainerMenu container, int slotId, @Nonnull ItemStack stack) {
            if (player.level().isClientSide() || container.containerId != menu.containerId) return;

            Slot slot = container.getSlot(slotId);
            if (slot == null) {
                LOGGER.warn("slotChanged event triggered for invalid slotId {} in container {}", slotId, container.containerId);
                return;
            }
            ItemStack oldStack = slot.getItem();

            boolean changed = !ItemStack.matches(oldStack, stack);
            if (!changed) return;

            LOGGER.trace("Slot {} changed in container {} for player {}: {} -> {}",
                slotId, container.containerId, player.getName().getString(), oldStack.getItem(), stack.getItem());

            boolean oldIsBackpack = BackpackWeightHandlerManager.isSophisticatedBackpack(oldStack);
            boolean newIsBackpack = BackpackWeightHandlerManager.isSophisticatedBackpack(stack);
            boolean oldIsContainer = WeightCalculator.isContainer(oldStack);
            boolean newIsContainer = WeightCalculator.isContainer(stack);

            // If a backpack was added or removed
            if (oldIsBackpack != newIsBackpack) { // More precise check
                LOGGER.debug("Backpack added/removed in slot change (Slot {}). Scheduling scan.", slotId);
                // Schedule scan for next tick
                 player.level().getServer().tell(new net.minecraft.server.TickTask(
                     player.level().getServer().getTickCount() + 1,
                     () -> BackpackWeightHandlerManager.scanPlayerForBackpacks(player)
                 ));
                 // If a *new* backpack appeared, attempt an immediate scan too
                 // This might help catch looted/moved backpacks slightly faster
                 if (!oldIsBackpack && newIsBackpack) {
                      LOGGER.debug("New backpack detected in slot {}, performing immediate scan attempt.", slotId);
                      BackpackWeightHandlerManager.scanPlayerForBackpacks(player);
                 }
            }

            if (oldIsContainer) {
                ContainerWeightHelper.invalidateCache(oldStack, player.level().registryAccess());
            }
            if (newIsContainer) {
                ContainerWeightHelper.invalidateCache(stack, player.level().registryAccess());
            }

            IPlayerWeight playerWeight = PlayerWeightProvider.getPlayerWeight(player);
            if (playerWeight != null) {
                playerWeight.setDirty(true);
            }
        }

        @Override
        public void dataChanged(@Nonnull AbstractContainerMenu container, int dataSlot, int value) {
            // Typically less relevant for weight, but could mark dirty if needed
        }
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (event.getEntity().level().isClientSide()) return;
        
        Player player = event.getEntity();
        AbstractContainerMenu menu = event.getContainer();
        int containerId = menu.containerId;
        
        LOGGER.info("Container opened: {} (type: {}) for player {}", 
            containerId, 
            menu.getClass().getName(), 
            player.getName().getString());
        
        if (openContainers.add(containerId)) {
            menu.addSlotListener(new WeightContainerListener(player, menu));
            
            LOGGER.debug("Container opened. Scheduling scan for player {}.", player.getName().getString());
            player.level().getServer().tell(new net.minecraft.server.TickTask(
                 player.level().getServer().getTickCount() + 1,
                 () -> BackpackWeightHandlerManager.scanPlayerForBackpacks(player)
             ));
            
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
        AbstractContainerMenu menu = event.getContainer();
        int containerId = menu.containerId;
        
        LOGGER.info("Container closed: {} (type: {}) for player {}", 
            containerId, 
            menu.getClass().getName(), 
            player.getName().getString());
        
        if (openContainers.remove(containerId)) {
            LOGGER.debug("Container closed. Scheduling scan for player {}.", player.getName().getString());
            player.level().getServer().tell(new net.minecraft.server.TickTask(
                 player.level().getServer().getTickCount() + 1,
                 () -> BackpackWeightHandlerManager.scanPlayerForBackpacks(player)
             ));
            
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
        LOGGER.warn("invalidateContainerCaches called on container close - potentially redundant if slotChanged works correctly.");
        for (int i = 0; i < container.slots.size(); i++) {
            ItemStack stack = container.getSlot(i).getItem();
            if (!stack.isEmpty() && WeightCalculator.isContainer(stack)) {
                ContainerWeightHelper.invalidateCache(stack, player.level().registryAccess());
                
                IItemHandler handler = stack.getCapability(Capabilities.ItemHandler.ITEM);
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