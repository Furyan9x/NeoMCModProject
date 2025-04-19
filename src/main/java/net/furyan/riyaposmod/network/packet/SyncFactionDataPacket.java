package net.furyan.riyaposmod.network.packet;

import net.furyan.riyaposmod.RiyaposMod;
import net.furyan.riyaposmod.faction.capability.IPlayerFaction;
import net.furyan.riyaposmod.faction.capability.PlayerFactionProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


/**
 * Packet for synchronizing faction data between server and client.
 * This packet contains all necessary faction data (faction ID, reputation values).
 */
public class SyncFactionDataPacket implements CustomPacketPayload {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final CompoundTag factionData;

    // Threshold for compression (in bytes) - compress data larger than this
    private static final int COMPRESSION_THRESHOLD = 1024;

    public static final CustomPacketPayload.Type<SyncFactionDataPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RiyaposMod.MOD_ID, "faction_sync"));

    public static final StreamCodec<FriendlyByteBuf, SyncFactionDataPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncFactionDataPacket::write, SyncFactionDataPacket::new);

    /**
     * Creates a new packet with the given faction data.
     *
     * @param factionData The faction data to synchronize
     */
    public SyncFactionDataPacket(CompoundTag factionData) {
        this.factionData = factionData;
    }

    /**
     * Creates a new packet from the given player faction.
     *
     * @param playerFaction The player faction to synchronize
     */
    public SyncFactionDataPacket(IPlayerFaction playerFaction) {
        this.factionData = playerFaction.serializeNBT();
    }

    /**
     * Creates a new packet from a buffer.
     * This constructor is used for deserialization.
     *
     * @param buf The buffer to read from
     */
    public SyncFactionDataPacket(FriendlyByteBuf buf) {
        CompoundTag wrapper = buf.readNbt();
        if (wrapper != null) {
            // Check if the data is compressed
            if (wrapper.getBoolean("Compressed")) {
                // Decompress the data
                byte[] compressedData = wrapper.getByteArray("CompressedData");
                this.factionData = decompressNBT(compressedData);
                LOGGER.debug("Decompressed faction data packet: {} bytes", compressedData.length);
            } else {
                // Data is not compressed
                this.factionData = wrapper.getCompound("Data");
            }
        } else {
            this.factionData = new CompoundTag();
            LOGGER.error("Received null wrapper tag in faction data packet");
        }
    }

    /**
     * Writes this packet to a buffer.
     * This method is used for serialization.
     *
     * @param buf The buffer to write to
     */
    public void write(FriendlyByteBuf buf) {
        // Estimate the size of the data
        int estimatedSize = factionData.toString().length();

        // Create a wrapper tag
        CompoundTag wrapper = new CompoundTag();

        // Compress large data
        if (estimatedSize > COMPRESSION_THRESHOLD) {
            // Compress the data
            byte[] compressedData = compressNBT(factionData);
            if (compressedData.length > 0) {
                wrapper.putByteArray("CompressedData", compressedData);
                wrapper.putBoolean("Compressed", true);
                LOGGER.debug("Compressed faction data packet: {} -> {} bytes ({}% reduction)", 
                    estimatedSize, compressedData.length, 
                    Math.round((1 - (double)compressedData.length / estimatedSize) * 100));
            } else {
                // Compression failed, fall back to uncompressed
                wrapper.put("Data", factionData);
                wrapper.putBoolean("Compressed", false);
                LOGGER.warn("Compression failed, sending uncompressed data");
            }
        } else {
            // Small data, no compression needed
            wrapper.put("Data", factionData);
            wrapper.putBoolean("Compressed", false);
        }

        // Write the wrapper tag to the buffer
        buf.writeNbt(wrapper);
    }

    /**
     * Gets the faction data in this packet.
     *
     * @return The faction data
     */
    public CompoundTag getFactionData() {
        return factionData;
    }

    /**
     * Handles this packet on the client side.
     * This method is called when the packet is received on the client.
     *
     * @param context The payload context
     */
    public static void handle(SyncFactionDataPacket packet, IPayloadContext context) {
        // Make sure we're on the client side
        context.enqueueWork(() -> {
            try {
                // Validate packet
                if (packet == null) {
                    LOGGER.error("Received null faction data packet");
                    return;
                }

                // Validate faction data
                CompoundTag factionData = packet.getFactionData();
                if (factionData == null) {
                    LOGGER.error("Received faction data packet with null data");
                    return;
                }

                // Log detailed information about the received data
                LOGGER.debug("Received faction data packet: {}", factionData);

                // Check if the faction data contains the required fields
                if (!factionData.contains("Reputation")) {
                    LOGGER.warn("Received faction data packet without reputation data");
                    // Continue processing, as the faction ID might still be valid
                }

                // Get the client player
                if (Minecraft.getInstance().player != null) {
                    // Get the player's faction data
                    IPlayerFaction playerFaction = PlayerFactionProvider.getPlayerFaction(Minecraft.getInstance().player);

                    if (playerFaction != null) {
                        // Apply the received data
                        playerFaction.deserializeNBT(factionData);
                        LOGGER.debug("Successfully applied faction data to player");
                    } else {
                        LOGGER.error("Player faction capability is null");
                    }
                } else {
                    LOGGER.error("Client player is null");
                }
            } catch (Exception e) {
                LOGGER.error("Error handling faction data packet: {}", e.getMessage(), e);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Compresses an NBT tag into a byte array using GZIP compression.
     *
     * @param tag The NBT tag to compress
     * @return The compressed byte array, or an empty array if compression failed
     */
    private byte[] compressNBT(CompoundTag tag) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
                DataOutputStream dataOut = new DataOutputStream(gzipOut);
                NbtIo.write(tag, dataOut);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            LOGGER.error("Failed to compress NBT data: {}", e.getMessage());
            return new byte[0];
        }
    }

    /**
     * Decompresses a byte array into an NBT tag using GZIP decompression.
     *
     * @param data The compressed byte array
     * @return The decompressed NBT tag, or an empty tag if decompression failed
     */
    private static CompoundTag decompressNBT(byte[] data) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            try (GZIPInputStream gzipIn = new GZIPInputStream(bais)) {
                DataInputStream dataIn = new DataInputStream(gzipIn);
                return NbtIo.read(dataIn);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to decompress NBT data: {}", e.getMessage());
            return new CompoundTag();
        }
    }
}
