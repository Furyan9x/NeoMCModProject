package net.furyan.riyaposmod.weight.events;

import com.mojang.logging.LogUtils;
import net.furyan.riyaposmod.weight.capability.IPlayerWeight;
import net.furyan.riyaposmod.weight.capability.PlayerWeightProvider;
import net.furyan.riyaposmod.weight.data.WeightDataManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Handles lazy weight recalculation on server ticks.
 * This improves performance by batching weight updates and preventing
 * multiple recalculations in the same tick.
 */
@EventBusSubscriber(modid = "riyaposmod")
public class WeightTickHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<UUID> playersToUpdate = new HashSet<>();
    
    /**
     * Marks a player for weight update on the next server tick
     */
    public static void markForUpdate(ServerPlayer player) {
        playersToUpdate.add(player.getUUID());
    }
    
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof ServerPlayer)) return;
        
        // --- Optimization: Clear per-tick item weight cache at the start of each tick ---
        WeightDataManager.clearPerTickWeightCache();
        
        // Process all players marked for update
        if (!playersToUpdate.isEmpty()) {
            LOGGER.debug("Processing {} players marked for weight update", playersToUpdate.size());
            
            // Create a copy to avoid concurrent modification
            Set<UUID> toProcess = new HashSet<>(playersToUpdate);
            playersToUpdate.clear();
            
            for (UUID playerId : toProcess) {
                ServerPlayer player = event.getEntity().getServer().getPlayerList().getPlayer(playerId);
                if (player != null) {
                    IPlayerWeight playerWeight = PlayerWeightProvider.getPlayerWeight(player);
                    if (playerWeight != null && playerWeight.isDirty()) {
                        float oldWeight = playerWeight.getCurrentWeight();
                        float newWeight = playerWeight.calculateWeight(player);
                        
                        LOGGER.debug("Updated weight for player {}: {} -> {}", 
                            player.getName().getString(), oldWeight, newWeight);
                            
                        playerWeight.setDirty(false);
                    }
                }
            }
        }
    }
} 