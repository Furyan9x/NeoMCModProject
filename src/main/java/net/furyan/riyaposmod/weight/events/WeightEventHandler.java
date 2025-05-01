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
    
    // Cooldown for block break handling
    private static final Map<UUID, Long> lastBlockBreakTime = new ConcurrentHashMap<>();
    private static final long BLOCK_BREAK_COOLDOWN_TICKS = 20;

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

        // Get potential weight after pickup
        ItemStack pickupStack = event.getItemEntity().getItem();
        float singleWeight = WeightCalculator.getWeight(pickupStack);
        
        // Add container contents weight if applicable
        if (WeightCalculator.isContainer(pickupStack)) {
            singleWeight += ContainerWeightHelper.getContainerWeight(pickupStack, player.level().registryAccess());
            
            // If it's a sophisticated backpack, register a handler for it
            if (BackpackWeightHandlerManager.isSophisticatedBackpack(pickupStack)) {   
                BackpackWeightHandlerManager.handleBackpackPickup(pickupStack, player);
            }
        }
        
        int count = pickupStack.getCount();
        float prospectiveWeight = cap.getCurrentWeight() + singleWeight * count;
        float maxCap = cap.getMaxCapacity();
        double prospectivePct = (maxCap <= 0) ? Double.POSITIVE_INFINITY : prospectiveWeight / maxCap;

        // Check against CRITICAL threshold
        if (prospectivePct >= EncumbranceLevel.CRITICAL.getThreshold()) {
            long now = System.currentTimeMillis();
            UUID playerId = player.getUUID();
            long lastTime = lastMessageTimestamps.getOrDefault(playerId, 0L);
            long cooldownMillis = COOLDOWN_SECONDS * 1_000L;

            // only send if cooldown has expired
            if (now - lastTime >= cooldownMillis) {
                player.sendSystemMessage(Component.literal("You are too encumbered to pick that up!"));
                lastMessageTimestamps.put(playerId, now);
            }

            event.setCanPickup(TriState.FALSE);
            return;
        }
        // Allow pickup and mark weight dirty
        cap.setDirty(true);
        }
    
    /* --- Crafting Events --- */
    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        Player player = event.getEntity();
        ItemStack craftedItem = event.getCrafting();
        
        if (player.level().isClientSide() || craftedItem.isEmpty()) {
            return;
        }
        
        // Check if the crafted item is a container
        if (WeightCalculator.isContainer(craftedItem)) {
            // Calculate the weight immediately
            ContainerWeightHelper.getContainerWeight(craftedItem, player.level().registryAccess());
            
            // If it's a sophisticated backpack, register a handler for it
            if (BackpackWeightHandlerManager.isSophisticatedBackpack(craftedItem)) {
                BackpackWeightHandlerManager.handleBackpackPickup(craftedItem, player);
            }
            
            // Mark player weight as dirty to force recalculation
            IPlayerWeight weightCap = PlayerWeightProvider.getPlayerWeight(player);
            if (weightCap != null) {
                weightCap.setDirty(true);
            }
        }
    }
    
    /* --- Block Break Events --- */
    
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        // Handle backpack blocks being broken
        if (event.getLevel().isClientSide()) return;
        
        Player player = event.getPlayer();
        Block block = event.getState().getBlock();
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        
        // Only process if it's a backpack block
        if (!blockId.toString().startsWith("sophisticatedbackpacks")) {
            return;
        }
        
        BlockPos pos = event.getPos();
        LOGGER.info("Backpack block broken by player: {} at {}", player.getName().getString(), pos);
        
        // Mark player weight as dirty to force recalculation
        IPlayerWeight weightCap = PlayerWeightProvider.getPlayerWeight(player);
        if (weightCap != null) {
            weightCap.setDirty(true);
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
        if (!(event.getEntity() instanceof Player player)) {
            return; // Only handle players
        }
        
        // Prevent backpacks from being equipped in the chest slot
        if (event.getSlot() == EquipmentSlot.CHEST && !event.getTo().isEmpty()) {
            // Check if the item is in our no_chest_equip tag or is a sophisticated backpack
            if (event.getTo().is(ModTags.Items.NO_CHEST_EQUIP) || 
                BackpackWeightHandlerManager.isSophisticatedBackpack(event.getTo())) {
                
                // Cancel equipping by moving it back to inventory
                player.getInventory().add(event.getTo().copy());
                
                // Clear the slot 
                player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                
                // Notify the player
                player.displayClientMessage(
                    Component.translatable("message.riyaposmod.backpack_chest_slot_restricted"), 
                    true);
                
                LOGGER.info("Prevented player {} from equipping backpack in chest slot: {}", 
                    player.getName().getString(), event.getTo().getItem());
                
                return; // Skip further processing
            }
        }
        
        // Ignore mainhand equipment changes to avoid excessive recalculations
        if (event.getSlot() == EquipmentSlot.MAINHAND) {
            return;
        }
        
        IPlayerWeight weight = PlayerWeightProvider.getPlayerWeight(player);
        if (!(weight instanceof PlayerWeightImpl weightImpl)) return;
        
        // Update capacity bonus for this slot
        weightImpl.updateEquipmentSlotBonus(player, event.getSlot(), event.getFrom(), event.getTo());
        
        // Handle backpack in armor slot (like chest)
        ItemStack oldStack = event.getFrom();
        ItemStack newStack = event.getTo();
        
        if (!oldStack.isEmpty() && BackpackWeightHandlerManager.isSophisticatedBackpack(oldStack)) {
            BackpackWeightHandlerManager.unregisterHandler(oldStack);
        }
        
        if (!newStack.isEmpty() && BackpackWeightHandlerManager.isSophisticatedBackpack(newStack)) {
            BackpackWeightHandlerManager.handleBackpackPickup(newStack, player);
        }
        
        weightImpl.setDirty(true);
    }

    @SubscribeEvent
    public static void onCurioChange(CurioChangeEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return; // Only handle server-side players
        }

        // Only process slots we care about
        if (!SLOTS_TO_CHECK.contains(event.getIdentifier())) {
            return;
        }

        IPlayerWeight weightCap = PlayerWeightProvider.getPlayerWeight(player);
        // Cast to implementation to access slot bonus methods
        if (!(weightCap instanceof PlayerWeightImpl weightImpl)) return;

        ItemStack fromStack = event.getFrom();
        ItemStack toStack = event.getTo();

        // Unregister handler for the item being removed (if it was a backpack)
        if (!fromStack.isEmpty() && BackpackWeightHandlerManager.isSophisticatedBackpack(fromStack)) {
            LOGGER.info("Backpack removed from curio slot {}: {}", 
                event.getIdentifier(), fromStack.getItem());
            BackpackWeightHandlerManager.unregisterHandler(fromStack);
        }

        // Register handler for the item being added (if it's a backpack)
        if (!toStack.isEmpty() && BackpackWeightHandlerManager.isSophisticatedBackpack(toStack)) {
            LOGGER.info("Backpack added to curio slot {}: {}", 
                event.getIdentifier(), toStack.getItem());
            BackpackWeightHandlerManager.handleBackpackPickup(toStack, player);
        }

        // Use slot-specific update method for better performance
        weightImpl.updateCurioSlotBonus(player, event.getIdentifier(), event.getSlotIndex(), fromStack, toStack);
        weightImpl.setDirty(true); // Ensure weight is recalculated immediately after curio change
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
        
        LOGGER.debug("Player {} logged in. Refreshing weight system.", player.getName().getString());

        // Refresh equipment bonuses
        weightImpl.refreshEquippedItemBonuses(player);
        
        // Scan for backpacks and register handlers
        BackpackWeightHandlerManager.scanPlayerForBackpacks(player);
        
        // Mark dirty to force weight calculation on first tick
        weightImpl.setDirty(true);
    }
    
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        
        LOGGER.debug("Player {} logged out. Unregistering backpack handlers.", player.getName().getString());
        
        // Unregister backpack handlers from player's curios slots
        if (ModList.get().isLoaded("curios")) {
            CuriosApi.getCuriosInventory(player).ifPresent(inventory -> {
                for (String slotType : SLOTS_TO_CHECK) {
                    ICurioStacksHandler slotHandler = inventory.getCurios().get(slotType);
                    if (slotHandler != null) {
                        for (int i = 0; i < slotHandler.getSlots(); i++) {
                            ItemStack stack = slotHandler.getStacks().getStackInSlot(i);
                            if (!stack.isEmpty() && BackpackWeightHandlerManager.isSophisticatedBackpack(stack)) {
                                BackpackWeightHandlerManager.unregisterHandler(stack);
                            }
                        }
                    }
                }
            });
        }
        
        // Clean up player-specific data
        lastMessageTimestamps.remove(player.getUUID());
        lastBlockBreakTime.remove(player.getUUID());
    }
    
    /* --- Interaction Events --- */

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        
        // Handle right-clicking a backpack item
        ItemStack stack = event.getItemStack();
        if (BackpackWeightHandlerManager.isSophisticatedBackpack(stack)) {
            // Force a scan of player inventory on the next tick
            player.level().getServer().tell(new net.minecraft.server.TickTask(
                player.level().getServer().getTickCount() + 1,
                () -> BackpackWeightHandlerManager.scanPlayerForBackpacks(player)
            ));
        }
    }
    
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        
        // Check if the player is sneaking (shift-right click)
        if (!player.isShiftKeyDown()) {
            return;
        }
        
        // Check if the clicked block is a backpack
        Block block = event.getLevel().getBlockState(event.getPos()).getBlock();
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        if (!blockId.toString().startsWith("sophisticatedbackpacks")) {
            return;
        }
        
        // Schedule a scan for backpacks after a delay
        player.level().getServer().tell(new net.minecraft.server.TickTask(
            player.level().getServer().getTickCount() + 3, // Small delay
            () -> {
                LOGGER.info("Scanning for backpacks after shift-right click");
                BackpackWeightHandlerManager.scanPlayerForBackpacks(player);
            }
        ));
    }
}
