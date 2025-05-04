package net.furyan.riyaposmod.weight.events;

import com.mojang.logging.LogUtils;
import net.furyan.riyaposmod.registries.WeightAttachmentRegistry;
import net.furyan.riyaposmod.util.ModTags;
import net.furyan.riyaposmod.weight.EncumbranceLevel;
import net.furyan.riyaposmod.weight.WeightCalculator;
import net.furyan.riyaposmod.weight.capability.IPlayerWeight;
import net.furyan.riyaposmod.weight.capability.PlayerWeightImpl;
import net.furyan.riyaposmod.weight.capability.PlayerWeightProvider;
import net.furyan.riyaposmod.weight.util.ContainerWeightHelper;
import net.furyan.riyaposmod.weight.util.BackpackWeightHandlerManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.CurioChangeEvent;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Central handler for all weight-related events.
 * Handles player weight calculation, encumbrance effects, 
 * and coordinates with BackpackWeightHandlerManager for backpack events.
 */
@EventBusSubscriber(modid = "riyaposmod")
public class WeightEventHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Cooldown for pickup rejection messages
    private static final long COOLDOWN_SECONDS = 5;
    private static final Map<UUID, Long> lastMessageTimestamps = new ConcurrentHashMap<>();

    // Curios slots that are relevant for weight calculation
    public static final Set<String> SLOTS_TO_CHECK = Set.of("back");
    
    // Cooldown for Item Pickup scans to prevent spam
    private static final long PICKUP_SCAN_COOLDOWN_MS = 100; // 100ms cooldown (2 ticks)
    private static final Map<UUID, Long> lastPickupScanTime = new ConcurrentHashMap<>();
    // Track recently scanned item entity IDs to prevent duplicate scans/logs
    private static final Set<Integer> recentlyScannedItemEntities = ConcurrentHashMap.newKeySet();
    private static final long ITEM_ENTITY_SCAN_MEMORY_MS = 5000; // 5 seconds memory window
    private static final Map<Integer, Long> itemEntityScanTimestamps = new ConcurrentHashMap<>();

    // For inventory change polling
    private static final Map<UUID, Integer> lastBackpackInventoryHash = new ConcurrentHashMap<>();
    private static final int INVENTORY_POLL_INTERVAL_TICKS = 5; // Check every 5 ticks (0.25s)
    private static final Map<UUID, Integer> lastInventoryPollTick = new ConcurrentHashMap<>();

    // For pickup event debounce
    private static final Map<UUID, Integer> lastPickupDirtyTick = new ConcurrentHashMap<>();

    /**
     * Main player tick handler - calculates weight and applies effects
     */
    @SubscribeEvent
    public static void onLivingUpdate(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        // Process only ServerPlayer on server side
        if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // --- Periodic inventory poll for backpack changes (outside containers) ---
        int tickCount = serverPlayer.level().getServer().getTickCount();
        UUID playerId = serverPlayer.getUUID();
        int lastPollTick = lastInventoryPollTick.getOrDefault(playerId, -INVENTORY_POLL_INTERVAL_TICKS);
        if (tickCount - lastPollTick >= INVENTORY_POLL_INTERVAL_TICKS) {
            // Compute a hash of all backpack UUIDs in inventory
            int hash = 1;
            for (int i = 0; i < serverPlayer.getInventory().getContainerSize(); i++) {
                ItemStack stack = serverPlayer.getInventory().getItem(i);
                if (BackpackWeightHandlerManager.isSophisticatedBackpack(stack)) {
                    var uuid = BackpackWeightHandlerManager.getBackpackUUID(stack).orElse(null);
                    if (uuid != null) {
                        hash = 31 * hash + uuid.hashCode();
                    }
                }
            }
            int prevHash = lastBackpackInventoryHash.getOrDefault(playerId, 0);
            if (hash != prevHash) {
                LOGGER.debug("Detected backpack inventory change for player {}. Triggering scan.", serverPlayer.getName().getString());
                BackpackWeightHandlerManager.scanPlayerForBackpacks(serverPlayer);
                lastBackpackInventoryHash.put(playerId, hash);
            }
            lastInventoryPollTick.put(playerId, tickCount);
        }
        // --- End inventory poll ---

        // Skip dead or spectator players
        if (!player.isAlive() || player.isSpectator()) {
            // Optional: You might want to clear effects explicitly here if needed
            return;
        }

        IPlayerWeight cap = PlayerWeightProvider.getPlayerWeight(serverPlayer);
        if (cap == null) {
            LOGGER.warn("PlayerWeight capability not found for {}", serverPlayer.getName().getString());
            return;
        }

        // Recalculate only if capability is marked dirty
        if (cap.isDirty()) {
            cap.calculateWeight(serverPlayer);
            cap.setDirty(false);
        }

        // Calculate encumbrance level
        float maxCap = cap.getMaxCapacity();
        float currentWeightPercent = (maxCap <= 0) ? 0.0f : cap.getCurrentWeight() / maxCap;
        EncumbranceLevel newLevel = EncumbranceLevel.fromPercent(currentWeightPercent);
        EncumbranceLevel previousLevel = cap.getPreviousEncumbranceLevel();

        // Only apply changes if the level has changed
        if (newLevel != previousLevel) {
            LOGGER.info("Player {} Weight%: {} Max Capacity: {} | Level: {} (Prev: {})",
                    serverPlayer.getName().getString(), currentWeightPercent, maxCap, newLevel, previousLevel);
            
            handleLevelTransition(serverPlayer, previousLevel, newLevel);
            cap.setPreviousEncumbranceLevel(newLevel);
        } else {
            // Just refresh effects if the level is stable
            refreshEffectsForLevel(serverPlayer, newLevel);
        }
    }
    
    /* --- Weight Effects Management --- */

    private static void handleLevelTransition(ServerPlayer player, EncumbranceLevel previousLevel, EncumbranceLevel newLevel) {
        if (previousLevel == null) previousLevel = EncumbranceLevel.NORMAL;

        List<EncumbranceLevel.EffectData> prevEffectsData = previousLevel.getEffects();
        List<EncumbranceLevel.EffectData> newEffectsData = newLevel.getEffects();

        Set<Holder<MobEffect>> newEffectsSet = newEffectsData.stream()
                .map(EncumbranceLevel.EffectData::effect)
                .collect(Collectors.toSet());

        // Remove effects from previous level that are not in the new level
        for (EncumbranceLevel.EffectData oldData : prevEffectsData) {
            Holder<MobEffect> effectHolder = oldData.effect();
            if (effectHolder != null && effectHolder.isBound() && !newEffectsSet.contains(effectHolder)) {
                if (player.removeEffect(effectHolder)) {
                    LOGGER.debug("Removed effect {} due to level change.", effectHolder.getRegisteredName());
                }
            }
        }

        // Apply/Update effects for the new level
        applyOrUpdateEffects(player, newEffectsData, "Level Transition");
    }

    private static void refreshEffectsForLevel(ServerPlayer player, EncumbranceLevel level) {
        final int REFRESH_THRESHOLD = 40; // 2 seconds

        for (EncumbranceLevel.EffectData data : level.getEffects()) {
            Holder<MobEffect> effectHolder = data.effect();
            if (effectHolder == null || !effectHolder.isBound()) continue;

            MobEffectInstance currentEffect = player.getEffect(effectHolder);

            // Refresh if missing, wrong amplifier, or low duration
            if (currentEffect == null || 
                currentEffect.getAmplifier() != data.amplifier() || 
                currentEffect.getDuration() < REFRESH_THRESHOLD) {
                
                // Apply with correct parameters
                applyEffectInstance(player, data);
            }
        }
    }

    private static void applyOrUpdateEffects(ServerPlayer player, List<EncumbranceLevel.EffectData> effectsToApply, String context) {
        for (EncumbranceLevel.EffectData data : effectsToApply) {
            applyEffectInstance(player, data);
        }
    }

    private static void applyEffectInstance(ServerPlayer player, EncumbranceLevel.EffectData data) {
        Holder<MobEffect> effectHolder = data.effect();
        if (effectHolder != null && effectHolder.isBound()) {
            player.addEffect(new MobEffectInstance(
                    effectHolder,
                    data.durationTicks(),
                    data.amplifier(),
                    false, // isAmbient
                    false, // showParticles
                    true   // showIcon
            ));
        } else {
            LOGGER.warn("Attempted to apply an unbound or null MobEffect Holder!");
        }
    }
    
    /* --- Item Pickup Events --- */

    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide()) return;

        IPlayerWeight cap = PlayerWeightProvider.getPlayerWeight(player);
        if (cap == null) return;

        ItemStack pickupStack = event.getItemEntity().getItem();
        if (pickupStack.isEmpty()) return; // Nothing to pick up

        boolean isSBBackpack = BackpackWeightHandlerManager.isSophisticatedBackpack(pickupStack);
        float singleWeight = WeightCalculator.getWeight(pickupStack);

        // Add container contents weight if applicable
        if (WeightCalculator.isContainer(pickupStack)) {
            singleWeight += ContainerWeightHelper.getContainerWeight(pickupStack, player.level().registryAccess());

            // If it's a sophisticated backpack, trigger a scan (with cooldown and entity ID tracking)
            if (isSBBackpack) {
                int entityId = event.getItemEntity().getId();
                long now = System.currentTimeMillis();
                long lastScan = lastPickupScanTime.getOrDefault(player.getUUID(), 0L);
                boolean isDuplicateEntity = recentlyScannedItemEntities.contains(entityId);
                // Clean up old IDs
                itemEntityScanTimestamps.entrySet().removeIf(e -> now - e.getValue() > ITEM_ENTITY_SCAN_MEMORY_MS);
                recentlyScannedItemEntities.removeIf(id -> !itemEntityScanTimestamps.containsKey(id));

                if (!isDuplicateEntity && now - lastScan > PICKUP_SCAN_COOLDOWN_MS) {
                    LOGGER.debug("Sophisticated backpack picked up: {} (entityId {}). Scheduling scan (Cooldown Passed, Not Duplicate).", pickupStack.getItem(), entityId);
                    lastPickupScanTime.put(player.getUUID(), now);
                    recentlyScannedItemEntities.add(entityId);
                    itemEntityScanTimestamps.put(entityId, now);
                    // Schedule scan for the next tick
                    player.level().getServer().tell(new net.minecraft.server.TickTask(
                        player.level().getServer().getTickCount() + 1,
                        () -> BackpackWeightHandlerManager.scanPlayerForBackpacks(player)
                    ));
                    // Invalidate cache immediately for the picked up stack
                    ContainerWeightHelper.invalidateCache(pickupStack, player.level().registryAccess());
                } else {
                    LOGGER.trace("Sophisticated backpack picked up: {} (entityId {}). Scan skipped due to cooldown or duplicate.", pickupStack.getItem(), entityId);
                }
            }
        }

        int count = pickupStack.getCount();
        float prospectiveWeight = cap.getCurrentWeight() + singleWeight * count;
        float maxCap = cap.getMaxCapacity();
        double prospectivePct = (maxCap <= 0) ? Double.POSITIVE_INFINITY : prospectiveWeight / maxCap;

        // Check against CRITICAL threshold
        if (prospectivePct >= EncumbranceLevel.CRITICAL.getThreshold()) {
            long nowMsg = System.currentTimeMillis();
            UUID playerId = player.getUUID();
            long lastTime = lastMessageTimestamps.getOrDefault(playerId, 0L);
            long cooldownMillis = COOLDOWN_SECONDS * 1_000L;

            if (nowMsg - lastTime >= cooldownMillis) {
                player.sendSystemMessage(Component.literal("You are too encumbered to pick that up!"));
                lastMessageTimestamps.put(playerId, nowMsg);
            }
            event.setCanPickup(TriState.FALSE);
            return;
        }
        // Allow pickup and mark weight dirty, but debounce to once per tick
        int currentTick = player.level().getServer().getTickCount();
        int lastTick = lastPickupDirtyTick.getOrDefault(player.getUUID(), -1);
        if (currentTick != lastTick) {
            cap.setDirty(true);
            lastPickupDirtyTick.put(player.getUUID(), currentTick);
        }
    }
    
    /* --- Crafting Events --- */
    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        Player player = event.getEntity();
        ItemStack craftedItem = event.getCrafting();
        
        if (player.level().isClientSide() || craftedItem.isEmpty()) {
            return;
        }
        
        // If it's a sophisticated backpack, trigger a scan
        if (BackpackWeightHandlerManager.isSophisticatedBackpack(craftedItem)) {
            LOGGER.debug("Sophisticated backpack crafted: {}. Scheduling scan.", craftedItem.getItem());
            // Schedule scan for the next tick
            player.level().getServer().tell(new net.minecraft.server.TickTask(
                 player.level().getServer().getTickCount() + 1,
                 () -> BackpackWeightHandlerManager.scanPlayerForBackpacks(player)
             ));
             // Invalidate cache immediately for the crafted stack
             ContainerWeightHelper.invalidateCache(craftedItem, player.level().registryAccess());
        }

        // Always mark player weight as dirty after crafting
        IPlayerWeight weightCap = PlayerWeightProvider.getPlayerWeight(player);
        if (weightCap != null) {
            weightCap.setDirty(true);
        }
    }
    
    /* --- Block Break Events --- */
    
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;

        Player player = event.getPlayer();
        Block block = event.getState().getBlock();
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);

        // If a backpack block is broken, it might drop a backpack item.
        // Item pickup event will handle the scan if needed.
        if (blockId.toString().startsWith("sophisticatedbackpacks")) {
            LOGGER.info("Backpack block broken by player: {} at {}", player.getName().getString(), event.getPos());
            // Mark player weight as dirty? No, let pickup handle dirtiness if item is acquired.
            // IPlayerWeight weightCap = PlayerWeightProvider.getPlayerWeight(player);
            // if (weightCap != null) {
            //     weightCap.setDirty(true);
            // }
        }
    }
    
    /* --- Container Events --- */
    
    @SubscribeEvent
   public static void onContainerOpen(PlayerContainerEvent.Open event) {
       Player player = event.getEntity();
       if (player.level().isClientSide()) return;
       
       // Ensure any backpack handlers are properly registered
       BackpackWeightHandlerManager.scanPlayerForBackpacks(player);
   }
        
    
    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        
        // After closing a container, ensure player weight is recalculated
        IPlayerWeight weightCap = PlayerWeightProvider.getPlayerWeight(player);
        if (weightCap != null) {
            weightCap.setDirty(true);
        }
    }
    
    /* --- Equipment Events --- */
    
    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return; // Only handle server-side players
        }

        // Prevent backpacks from being equipped in the chest slot
        if (event.getSlot() == EquipmentSlot.CHEST && !event.getTo().isEmpty()) {
            if (event.getTo().is(ModTags.Items.NO_CHEST_EQUIP) ||
                BackpackWeightHandlerManager.isSophisticatedBackpack(event.getTo())) {

                // Unregister handler for the old ItemStack (chest slot)
                BackpackWeightHandlerManager.getBackpackUUID(event.getTo()).ifPresent(uuid ->
                    BackpackWeightHandlerManager.unregisterHandler(uuid)
                );

                player.getInventory().add(event.getTo().copy());
                player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                player.displayClientMessage(
                    Component.translatable("message.riyaposmod.backpack_chest_slot_restricted"),
                    true);
                LOGGER.info("Prevented player {} from equipping backpack in chest slot: {}",
                    player.getName().getString(), event.getTo().getItem());

                // Invalidate cache for the forcibly moved backpack
                ContainerWeightHelper.invalidateCache(event.getTo(), player.level().registryAccess());
                // Mark player weight as dirty
                IPlayerWeight weightCap = PlayerWeightProvider.getPlayerWeight(player);
                if (weightCap != null) {
                    weightCap.setDirty(true);
                }

                // Schedule scan because item moved back to inventory
                player.level().getServer().tell(new net.minecraft.server.TickTask(
                    player.level().getServer().getTickCount() + 1,
                    () -> BackpackWeightHandlerManager.scanPlayerForBackpacks(player)
                ));
                // Immediately scan as well to ensure handler state is correct
                BackpackWeightHandlerManager.scanPlayerForBackpacks(player);
                return; // Skip further processing
            }
        }

        // Determine if the slot is relevant for capacity calculation (not hands)
        boolean isRelevantSlotForCapacity = event.getSlot() != EquipmentSlot.MAINHAND && event.getSlot() != EquipmentSlot.OFFHAND;

        IPlayerWeight weight = PlayerWeightProvider.getPlayerWeight(player);
        if (!(weight instanceof PlayerWeightImpl weightImpl)) return;

        // Update capacity bonus if the slot is relevant
        if (isRelevantSlotForCapacity) {
            weightImpl.updateEquipmentSlotBonus(player, event.getSlot(), event.getFrom(), event.getTo());
        }
        // Always mark the player's weight as dirty after any equipment change
        weightImpl.setDirty(true);
    }

    @SubscribeEvent
    public static void onCurioChange(CurioChangeEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return; // Only handle server-side players
        }

        // Only process slots we might care about for capacity or backpacks
        if (!SLOTS_TO_CHECK.contains(event.getIdentifier())) {
            // Even if not a backpack slot, other curios might affect capacity
            // Fall through to update capacity bonus, but skip backpack scan if not relevant stack
             IPlayerWeight weightCap = PlayerWeightProvider.getPlayerWeight(player);
             if (weightCap instanceof PlayerWeightImpl weightImpl) {
                 weightImpl.updateCurioSlotBonus(player, event.getIdentifier(), event.getSlotIndex(), event.getFrom(), event.getTo());
                 weightImpl.setDirty(true);
             }
             return;
        }

        IPlayerWeight weightCap = PlayerWeightProvider.getPlayerWeight(player);
        if (!(weightCap instanceof PlayerWeightImpl weightImpl)) return;

        // Update capacity bonus for this slot
        weightImpl.updateCurioSlotBonus(player, event.getIdentifier(), event.getSlotIndex(), event.getFrom(), event.getTo());

        // Check if backpacks were involved
        boolean oldIsBackpack = BackpackWeightHandlerManager.isSophisticatedBackpack(event.getFrom());
        boolean newIsBackpack = BackpackWeightHandlerManager.isSophisticatedBackpack(event.getTo());

        // Trigger a scan if a backpack was involved
        if (oldIsBackpack || newIsBackpack) {
             // Use getIdentifier() for Curio slot identifier
             LOGGER.debug("Curio change involved backpack (Slot Identifier: {}). Scheduling scan.", event.getIdentifier());
             // Schedule scan for the next tick
             player.level().getServer().tell(new net.minecraft.server.TickTask(
                  player.level().getServer().getTickCount() + 1,
                  () -> BackpackWeightHandlerManager.scanPlayerForBackpacks(player)
              ));
        }

        // Always mark dirty after equipment change
        weightImpl.setDirty(true);
    }

    /* --- Player Lifecycle Events --- */
    
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            // Not handling end portal travel here
            return;
        }

        Player originalPlayer = event.getOriginal();
        Player newPlayer = event.getEntity();

        // Get old capability data
        IPlayerWeight oldWeight = originalPlayer.getData(WeightAttachmentRegistry.PLAYER_WEIGHT_ATTACHMENT);

        // Get or create new capability data
        IPlayerWeight newWeight = newPlayer.getData(WeightAttachmentRegistry.PLAYER_WEIGHT_ATTACHMENT);

        // Copy data
        if (oldWeight != null && newWeight != null) {
            CompoundTag nbt = oldWeight.serializeNBT(newPlayer.level().registryAccess());
            newWeight.deserializeNBT(newPlayer.level().registryAccess(), nbt);
            
            // Explicitly mark dirty after clone to ensure recalculation and bonus refresh
            newWeight.setDirty(true); 
            LOGGER.debug("Cloned weight data for player {}", newPlayer.getName().getString());
        } else {
            LOGGER.error("Failed to clone weight capability for player {}", newPlayer.getName().getString());
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        IPlayerWeight weightCap = player.getData(WeightAttachmentRegistry.PLAYER_WEIGHT_ATTACHMENT);
        if (!(weightCap instanceof PlayerWeightImpl weightImpl)) return;
        
        LOGGER.debug("Player {} logged in. Refreshing weight system and scanning for backpacks.", player.getName().getString());

        // Refresh equipment bonuses
        weightImpl.refreshEquippedItemBonuses(player);
        
        // Scan for backpacks immediately on login
        BackpackWeightHandlerManager.scanPlayerForBackpacks(player);
        
        // Mark dirty to force weight calculation and client sync on first tick
        weightImpl.setDirty(true);
    }
    
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        
        LOGGER.debug("Player {} logged out. Cleaning up data.", player.getName().getString());
        
        // Option 1: Clear all handlers associated with this player (complex, see manager class)
        // BackpackWeightHandlerManager.clearPlayerHandlers(player.getUUID());

        // Option 2: Rely on scanPlayerForBackpacks during gameplay and clearAllHandlers on server stop.
        // This is generally safer.

        // Clean up player-specific tracking data
        lastMessageTimestamps.remove(player.getUUID());
    }
    
    /* --- Interaction Events --- */

    // @SubscribeEvent // Remove this event handler entirely
    // public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
    //     // ... removed logic ...
    // }
    
    @SubscribeEvent // Keep RightClickBlock for placed backpacks
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        
        // Handle shift-right clicking a placed backpack block (to pick it up)
        if (!player.isShiftKeyDown()) {
            return;
        }
        
        Block block = event.getLevel().getBlockState(event.getPos()).getBlock();
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        if (!blockId.toString().startsWith("sophisticatedbackpacks")) {
            return;
        }

        LOGGER.debug("Player {} shift-right-clicked backpack block: {}. Scheduling scan.",
             player.getName().getString(), blockId);
        // Picking up happens over several ticks. Scan after a short delay.
        player.level().getServer().tell(new net.minecraft.server.TickTask(
            player.level().getServer().getTickCount() + 3, // Small delay
            () -> {
                BackpackWeightHandlerManager.scanPlayerForBackpacks(player);
                 IPlayerWeight weightCap = PlayerWeightProvider.getPlayerWeight(player);
                 if (weightCap != null) {
                     weightCap.setDirty(true);
                 }
            }
        ));
    }
}
