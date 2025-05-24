package net.furyan.riyaposmod.network;

import com.mojang.logging.LogUtils;
import net.furyan.riyaposmod.RiyaposMod;
import net.furyan.riyaposmod.faction.capability.IPlayerFaction;
import net.furyan.riyaposmod.faction.capability.PlayerFactionProvider;
import net.furyan.riyaposmod.network.packet.JoinFactionPacket;
import net.furyan.riyaposmod.network.packet.SyncFactionDataPacket;
import net.furyan.riyaposmod.network.packet.SyncWeightDataPacket;
import net.furyan.riyaposmod.network.packet.ClientboundSkillUpdatePacket;
import net.furyan.riyaposmod.weight.capability.IPlayerWeight;
import net.furyan.riyaposmod.weight.capability.PlayerWeightProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Handles network communication for the mod.
 * This class provides methods for synchronizing data between server and client.
 */

public class ModNetworking {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Batching system for faction data changes
    private static final Map<UUID, Long> pendingSyncs = new ConcurrentHashMap<>();

    // Batching system for weight data changes
    private static final Map<UUID, Long> pendingWeightSyncs = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final long BATCH_DELAY_MS = 500; // Delay between batches in milliseconds
    private static boolean initialized = false;

    /**
     * Registers all packet handlers.
     * This is called automatically during mod initialization.
     *
     * @param event The register payload handler event
     */
    @SubscribeEvent
    public static void registerMessages(final RegisterPayloadHandlersEvent event) {
        LOGGER.info("Registering network packets for {}", RiyaposMod.MOD_ID);
        final PayloadRegistrar registrar = event.registrar(RiyaposMod.MOD_ID);

        // Register the faction sync packet (server to client only)
        registrar.playToClient(
            SyncFactionDataPacket.TYPE,
            SyncFactionDataPacket.STREAM_CODEC,
            SyncFactionDataPacket::handle
        );

        // Register the weight sync packet (server to client only)
        registrar.playToClient(
            SyncWeightDataPacket.TYPE,
            SyncWeightDataPacket.STREAM_CODEC,
            SyncWeightDataPacket::handle
        );

        // Register the skills update packet (server to client only)
        registrar.playToClient(
            ClientboundSkillUpdatePacket.TYPE,
            ClientboundSkillUpdatePacket.STREAM_CODEC,
            ClientboundSkillUpdatePacket::handle
        );

        // Register client to server packets
        registrar.playToServer(
                JoinFactionPacket.TYPE,
                JoinFactionPacket.STREAM_CODEC,
                JoinFactionPacket::handle
        );
    }

    /**
     * Initializes the network event handlers.
     * This should be called during mod initialization.
     */
    public static void init() {
        if (initialized) return;

        // Register event handlers
        NeoForge.EVENT_BUS.addListener(ModNetworking::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(ModNetworking::onPlayerChangedDimension);
        NeoForge.EVENT_BUS.addListener(ModNetworking::onPlayerRespawn);

        // Start the scheduler for batched updates
        scheduler.scheduleAtFixedRate(ModNetworking::processPendingSyncs, BATCH_DELAY_MS, BATCH_DELAY_MS, TimeUnit.MILLISECONDS);
        scheduler.scheduleAtFixedRate(ModNetworking::processPendingWeightSyncs, BATCH_DELAY_MS, BATCH_DELAY_MS, TimeUnit.MILLISECONDS);

        initialized = true;
        LOGGER.info("Initialized network systems for faction and weight data");
    }

    /**
     * Event handler for player login.
     * Synchronizes faction and weight data when a player logs in.
     *
     * @param event The player logged in event
     */
    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            LOGGER.debug("Player logged in: {}, syncing faction and weight data", serverPlayer.getName().getString());
            syncToClient(serverPlayer);
            syncWeightToClient(serverPlayer);
        }
    }

    /**
     * Event handler for dimension change.
     * Synchronizes faction and weight data when a player changes dimension.
     *
     * @param event The player changed dimension event
     */
    private static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            LOGGER.debug("Player changed dimension: {}, syncing faction and weight data", serverPlayer.getName().getString());
            syncToClient(serverPlayer);
            syncWeightToClient(serverPlayer);
        }
    }

    /**
     * Event handler for player respawn.
     * Synchronizes faction and weight data when a player respawns.
     *
     * @param event The player respawn event
     */
    private static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            LOGGER.debug("Player respawned: {}, syncing faction and weight data", serverPlayer.getName().getString());
            syncToClient(serverPlayer);
            syncWeightToClient(serverPlayer);
        }
    }


    /**
     * Processes pending faction synchronization requests.
     * This method is called periodically by the scheduler.
     */
    private static void processPendingSyncs() {
        try {
            long currentTime = System.currentTimeMillis();

            // Process all pending syncs that are ready
            pendingSyncs.entrySet().removeIf(entry -> {
                UUID playerId = entry.getKey();
                Long scheduledTime = entry.getValue();

                if (currentTime >= scheduledTime) {
                    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                    if (server != null) {
                        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                        if (player != null && player.isAlive()) {
                            syncFactionDataToClient(player, PlayerFactionProvider.getPlayerFaction(player));
                        }
                    }
                    return true; // Remove from pending syncs
                }
                return false; // Keep in pending syncs
            });
        } catch (Exception e) {
            LOGGER.error("Error processing pending faction data syncs: {}", e.getMessage());
        }
    }

    /**
     * Processes pending weight synchronization requests.
     * This method is called periodically by the scheduler.
     */
    private static void processPendingWeightSyncs() {
        try {
            long currentTime = System.currentTimeMillis();

            // Process all pending weight syncs that are ready
            pendingWeightSyncs.entrySet().removeIf(entry -> {
                UUID playerId = entry.getKey();
                Long scheduledTime = entry.getValue();

                if (currentTime >= scheduledTime) {
                    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                    if (server != null) {
                        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                        if (player != null && player.isAlive()) {
                            syncWeightDataToClient(player, PlayerWeightProvider.getPlayerWeight(player));
                        }
                    }
                    return true; // Remove from pending syncs
                }
                return false; // Keep in pending syncs
            });
        } catch (Exception e) {
            LOGGER.error("Error processing pending weight data syncs: {}", e.getMessage());
        }
    }

    /**
     * Schedules a faction data sync for the specified player.
     * This method batches sync requests to reduce network traffic.
     *
     * @param player The player to sync data for
     */
    public static void syncToClient(ServerPlayer player) {
        if (player == null) return;

        // Schedule the sync for BATCH_DELAY_MS milliseconds from now
        pendingSyncs.put(player.getUUID(), System.currentTimeMillis() + BATCH_DELAY_MS);
    }
    /**
     * Sends a packet from the client to the server to join a faction.
     * This should be called from client-side code, such as a GUI.
     *
     * @param factionId The ID of the faction to join
     */
    public static void sendJoinFactionPacket(String factionId) {
        JoinFactionPacket packet = new JoinFactionPacket(factionId);
        PacketDistributor.sendToServer(packet);
    }

    /**
     * Sends faction data to a player immediately.
     * This should be called when immediate synchronization is required.
     *
     * @param player The player to send the data to
     * @param factionData The faction data to send
     */
    public static void syncFactionDataToClient(ServerPlayer player, IPlayerFaction factionData) {
        if (player == null || factionData == null) {
            LOGGER.warn("Attempted to sync faction data with null player or data");
            return;
        }


        try {
            // Validate faction data
            CompoundTag tag = factionData.serializeNBT();
            if (tag == null) {
                LOGGER.warn("Invalid faction data for player {}: null NBT data", player.getName().getString());
                return;
            }

            // Log the synchronization event
            LOGGER.debug("Synchronizing faction data for player {}: faction={}, reputations={}",
                    player.getName().getString(),
                    factionData.getFactionId().orElse("none"),
                    tag.contains("Reputation") ? tag.getCompound("Reputation") : "none");

            // Create and send the packet
            SyncFactionDataPacket packet = new SyncFactionDataPacket(factionData);
            PacketDistributor.sendToPlayer(player, packet);
        } catch (Exception e) {
            LOGGER.error("Error synchronizing faction data for player {}: {}", player.getName().getString(), e.getMessage());
        }
    }

    /**
     * Schedules a weight data sync for the specified player.
     * This method batches sync requests to reduce network traffic.
     *
     * @param player The player to sync data for
     */
    public static void syncWeightToClient(ServerPlayer player) {
        if (player == null) return;

        // Schedule the sync for BATCH_DELAY_MS milliseconds from now
        pendingWeightSyncs.put(player.getUUID(), System.currentTimeMillis() + BATCH_DELAY_MS);
    }

    /**
     * Sends weight data to a player immediately.
     * This should be called when immediate synchronization is required.
     *
     * @param player The player to send the data to
     * @param weightData The weight data to send
     */
    public static void syncWeightDataToClient(ServerPlayer player, IPlayerWeight weightData) {
        if (player == null || weightData == null) {
            LOGGER.warn("Attempted to sync weight data with null player or data");
            return;
        }

        try {
            // Get the HolderLookup.Provider from the player's level
            HolderLookup.Provider provider = player.level().registryAccess();
            // Validate weight data
            CompoundTag tag = weightData.serializeNBT(provider);
            if (tag == null) {
                LOGGER.warn("Invalid weight data for player {}: null NBT data", player.getName().getString());
                return;
            }

            // Log the synchronization event
            LOGGER.debug("Synchronizing weight data for player {}: current={}, max={}",
                    player.getName().getString(),
                    weightData.getCurrentWeight(),
                    weightData.getMaxCapacity());

            // Create and send the packet
            SyncWeightDataPacket packet = new SyncWeightDataPacket(weightData, provider);
            PacketDistributor.sendToPlayer(player, packet);
        } catch (Exception e) {
            LOGGER.error("Error synchronizing weight data for player {}: {}", player.getName().getString(), e.getMessage());
        }
    }

    /**
     * Sends a generic packet to a specific player.
     * @param packet The packet to send.
     * @param player The player to send the packet to.
     */
    public static void sendToPlayer(CustomPacketPayload packet, ServerPlayer player) {
        if (player == null || packet == null) {
            LOGGER.warn("Attempted to send packet with null player or packet type");
            return;
        }
        PacketDistributor.sendToPlayer(player, packet);
        // Use packet.type().id() to get the ResourceLocation for logging
        LOGGER.debug("Sent packet {} to player {}", packet.type().id(), player.getName().getString());
    }
}
