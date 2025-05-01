package net.furyan.riyaposmod.weight;

import com.mojang.logging.LogUtils;
import net.furyan.riyaposmod.weight.util.BackpackWeightHandlerManager;
import net.furyan.riyaposmod.weight.util.ContainerWeightHelper;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

/**
 * Central manager for the weight system.
 * Handles cleanup and coordination between different components.
 */
public class WeightSystemManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Handles server stopping event to clean up weight system resources
     */
    public static void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("Server stopping, cleaning up weight system resources");
        BackpackWeightHandlerManager.clearAllHandlers();
        ContainerWeightHelper.clearCache();
    }
} 