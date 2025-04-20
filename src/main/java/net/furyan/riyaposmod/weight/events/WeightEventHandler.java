package net.furyan.riyaposmod.weight.events;

import com.mojang.logging.LogUtils;
import net.furyan.riyaposmod.weight.EncumbranceLevel;
import net.furyan.riyaposmod.weight.WeightRegistry;
import net.furyan.riyaposmod.weight.capability.IPlayerWeight;
import net.furyan.riyaposmod.weight.capability.PlayerWeightProvider;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@EventBusSubscriber(modid = "riyaposmod")
public class WeightEventHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Field to track the last time the chat message was sent
    // how many seconds between chat messages
    private static final long COOLDOWN_SECONDS = 5;
    // store last message time per player UUID
    private static final Map<UUID, Long> lastMessageTimestamps = new ConcurrentHashMap<>();
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

        // --- Optimized Weight Calculation ---
        // Recalculate if capability is marked dirty OR periodically (e.g., every 5 secs)
        if (cap.isDirty() || serverPlayer.tickCount % 100 == 0) {
            cap.calculateWeight(serverPlayer); // Recalculate from inventory
            cap.setDirty(false); // Reset dirty flag
            // LOGGER.debug("Recalculated weight for {}: {}", serverPlayer.getName().getString(), cap.getCurrentWeight());
        }
        // ---

        // Use getOverencumbrancePercentage for clarity if it's implemented robustly
        // float currentWeightPercent = cap.getOverencumbrancePercentage();
        // Or calculate directly:
        float maxCap = cap.getMaxCapacity();
        float currentWeightPercent = (maxCap <= 0) ? 0.0f : cap.getCurrentWeight() / maxCap;


        EncumbranceLevel newLevel = EncumbranceLevel.fromPercent(currentWeightPercent);
        EncumbranceLevel previousLevel = cap.getPreviousEncumbranceLevel(); // Get previous level from Cap

        // --- Debug Logging ---
        if (newLevel != previousLevel || serverPlayer.tickCount % 200 == 0) { // Log change or every 10 seconds
            LOGGER.info("Player {} Weight%: {} | Level: {} (Prev: {})",
                    serverPlayer.getName().getString(), currentWeightPercent, newLevel, previousLevel);
        }
        // ---

        if (newLevel != previousLevel) {
            // --- Level Changed ---
            LOGGER.info("Encumbrance level changing for {}: {} -> {}", serverPlayer.getName().getString(), previousLevel, newLevel);
            handleLevelTransition(serverPlayer, previousLevel, newLevel);
            cap.setPreviousEncumbranceLevel(newLevel); // Update capability
            // No need to set dirty here unless saving previousLevel requires it explicitly outside normal save cycles
        } else {
            // --- Level Stable ---
            // Check if effects need refreshing (duration low)
            refreshEffectsForLevel(serverPlayer, newLevel);
        }
    }

    /** Handles removing old effects and applying/updating new ones when level changes. */
    private static void handleLevelTransition(ServerPlayer player, EncumbranceLevel previousLevel, EncumbranceLevel newLevel) {
        if (previousLevel == null) previousLevel = EncumbranceLevel.NORMAL; // Safety for first run

        List<EncumbranceLevel.EffectData> prevEffectsData = previousLevel.getEffects();
        List<EncumbranceLevel.EffectData> newEffectsData = newLevel.getEffects();

        Set<Holder<MobEffect>> newEffectsSet = newEffectsData.stream()
                .map(EncumbranceLevel.EffectData::effect)
                .collect(Collectors.toSet());

        // 1. Remove effects from previous level that are NOT in the new level
        for (EncumbranceLevel.EffectData oldData : prevEffectsData) {
            Holder<MobEffect> effectHolder = oldData.effect();
            if (effectHolder != null && effectHolder.isBound() && !newEffectsSet.contains(effectHolder)) {
                if (player.removeEffect(effectHolder)) {
                    LOGGER.debug("Removed effect {} due to level change.", effectHolder.getRegisteredName());
                }
            }
        }

        // 2. Apply/Update effects for the new level
        applyOrUpdateEffects(player, newEffectsData, "Level Transition");
    }

    /** Checks effects for the current stable level and reapplies if duration is low. */
    private static void refreshEffectsForLevel(ServerPlayer player, EncumbranceLevel level) {
        final int REFRESH_THRESHOLD = 40; // Reapply when duration is less than 2 seconds

        for (EncumbranceLevel.EffectData data : level.getEffects()) {
            Holder<MobEffect> effectHolder = data.effect();
            if (effectHolder == null || !effectHolder.isBound()) continue;

            MobEffectInstance currentEffect = player.getEffect(effectHolder);

            // Refresh if missing, wrong amplifier, or low duration
            if (currentEffect == null || currentEffect.getAmplifier() != data.amplifier() || currentEffect.getDuration() < REFRESH_THRESHOLD) {
                // Log reason for refresh
                if (currentEffect == null) {
                    LOGGER.debug("Effect {} missing for stable level {}, reapplying.", effectHolder.getRegisteredName(), level);
                } else if (currentEffect.getAmplifier() != data.amplifier()){
                    LOGGER.warn("Effect {} has wrong amplifier ({}) for stable level {} (expected {}), correcting.", effectHolder.getRegisteredName(), currentEffect.getAmplifier(), level, data.amplifier());
                } else { // Duration must be low
                    LOGGER.debug("Refreshing effect {} duration ({} < {}) for stable level {}.", effectHolder.getRegisteredName(), currentEffect.getDuration(), REFRESH_THRESHOLD, level);
                }

                // Apply with correct parameters
                applyEffectInstance(player, data);
            }
        }
    }

    /** Applies/Updates all effects in the provided list. */
    private static void applyOrUpdateEffects(ServerPlayer player, List<EncumbranceLevel.EffectData> effectsToApply, String context) {
        for (EncumbranceLevel.EffectData data : effectsToApply) {
            applyEffectInstance(player, data);
            // LOGGER.debug("[{}] Applied/Updated effect {} (Amp: {})", context, data.effect().getRegisteredName(), data.amplifier());
        }
    }

    /** Creates and applies a single MobEffectInstance based on EffectData. */
    private static void applyEffectInstance(ServerPlayer player, EncumbranceLevel.EffectData data) {
        Holder<MobEffect> effectHolder = data.effect();
        if (effectHolder != null && effectHolder.isBound()) {
            player.addEffect(new MobEffectInstance(
                    effectHolder,
                    data.durationTicks(), // Use defined duration (e.g., 200)
                    data.amplifier(),
                    false, // isAmbient (usually false for penalties)
                    false, // showParticles (true/false based on preference)
                    true  // showIcon (usually true for penalties)
            ));
        } else {
            LOGGER.warn("Attempted to apply an unbound or null MobEffect Holder!");
        }
    }
    // --- Item Pickup Handling ---
    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide()) return;

        IPlayerWeight cap = PlayerWeightProvider.getPlayerWeight(player);
        if (cap == null) return;

        // Get potential weight *after* pickup
        float singleWeight = WeightRegistry.getWeight(event.getItemEntity().getItem().getItem()); // Ensure WeightRegistry is accessible
        int count = event.getItemEntity().getItem().getCount();
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

        // below CRITICAL, allow pickup and mark weight dirty
        cap.setDirty(true);
    }


    // --- Player Clone Handling ---
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // Keep this check if you only want data transfer on death respawn
        if (!event.isWasDeath()) return;

        Player original = event.getOriginal();
        Player player = event.getEntity(); // The new player instance

        // Get the weight data for both players using your provider
        IPlayerWeight oldCap = PlayerWeightProvider.getPlayerWeight(original);
        IPlayerWeight newCap = PlayerWeightProvider.getPlayerWeight(player); // Get attachment for the new player instance

        if (oldCap != null && newCap != null) {
            // Serialize data from the old instance
            // Use RegistryAccess from the original player's level (or new, usually same context)
            CompoundTag nbtData = oldCap.serializeNBT(original.level().registryAccess());

            // Deserialize the data into the new instance
            // Use RegistryAccess from the new player's level
            // This assumes IPlayerWeight has deserializeNBT(HolderLookup.Provider, CompoundTag)
            newCap.deserializeNBT(player.level().registryAccess(), nbtData);

            // The deserializeNBT implementation should ideally handle setting the 'dirty' flag
            // if the loaded state requires a recalculation (e.g., if currentWeight wasn't saved).
            // If not, you might uncomment this:
            // newCap.setDirty(true);

            LOGGER.debug("Cloned weight data for {}", player.getName().getString());

        } else {
            if (oldCap == null) {
                LOGGER.warn("Could not clone weight data for {} - original data missing.", player.getName().getString());
            }
            if (newCap == null) {
                // This would be concerning, implying the attachment isn't present on the new player
                LOGGER.error("Could not clone weight data for {} - new player data instance missing!", player.getName().getString());
            }
        }
    }

    // Add PlayerEvent.LoadFromFile / PlayerLoggedInEvent handling maybe?
    // To ensure weight is calculated shortly after login if deserializeNBT doesn't trigger it.
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide() && player instanceof ServerPlayer sp) {
            IPlayerWeight playerWeight = PlayerWeightProvider.getPlayerWeight(sp);
            if (playerWeight != null) {
                // Mark dirty on login to force a weight recalc soon,
                // especially if inventory could change while offline.
                playerWeight.setDirty(true);
                LOGGER.debug("Marked weight dirty on login for {}", sp.getName().getString());
            }
        }
    }
}