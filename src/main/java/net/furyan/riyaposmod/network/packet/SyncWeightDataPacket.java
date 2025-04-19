package net.furyan.riyaposmod.network.packet;

import net.furyan.riyaposmod.RiyaposMod;
import net.furyan.riyaposmod.weight.capability.IPlayerWeight;
import net.furyan.riyaposmod.weight.capability.PlayerWeightProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import org.jetbrains.annotations.NotNull;
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
 * Packet for synchronizing weight data between server and client.
 * This packet contains all necessary weight data (current weight, max capacity, bonuses).
 */
public class SyncWeightDataPacket implements CustomPacketPayload {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final CompoundTag weightData;
    
    // Threshold for compression (in bytes) - compress data larger than this
    private static final int COMPRESSION_THRESHOLD = 1024;
    
    public static final CustomPacketPayload.Type<SyncWeightDataPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RiyaposMod.MOD_ID, "weight_sync"));
    
    public static final StreamCodec<FriendlyByteBuf, SyncWeightDataPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncWeightDataPacket::write, SyncWeightDataPacket::new);
    
    /**
     * Creates a new packet with the given weight data.
     *
     * @param weightData The weight data to synchronize
     */
    public SyncWeightDataPacket(CompoundTag weightData) {
        this.weightData = weightData;
    }
    
    /**
     * Creates a new packet from the given player weight.
     *
     * @param playerWeight The player weight to synchronize
     */
    public SyncWeightDataPacket(IPlayerWeight playerWeight, HolderLookup.Provider provider) {
        this.weightData = playerWeight.serializeNBT(provider);
    }
    
    /**
     * Creates a new packet from a buffer.
     * This constructor is used for deserialization.
     *
     * @param buf The buffer to read from
     */
    public SyncWeightDataPacket(FriendlyByteBuf buf) {
        CompoundTag wrapper = buf.readNbt();
        if (wrapper != null) {
            // Check if the data is compressed
            if (wrapper.getBoolean("Compressed")) {
                // Decompress the data
                byte[] compressedData = wrapper.getByteArray("CompressedData");
                this.weightData = decompressNBT(compressedData);
                LOGGER.debug("Decompressed weight data packet: {} bytes", compressedData.length);
            } else {
                // Data is not compressed
                this.weightData = wrapper.getCompound("Data");
            }
        } else {
            this.weightData = new CompoundTag();
            LOGGER.error("Received null wrapper tag in weight data packet");
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
        int estimatedSize = weightData.toString().length();
        
        // Create a wrapper tag
        CompoundTag wrapper = new CompoundTag();
        
        // Compress large data
        if (estimatedSize > COMPRESSION_THRESHOLD) {
            // Compress the data
            byte[] compressedData = compressNBT(weightData);
            if (compressedData.length > 0) {
                wrapper.putByteArray("CompressedData", compressedData);
                wrapper.putBoolean("Compressed", true);
                LOGGER.debug("Compressed weight data packet: {} -> {} bytes ({}% reduction)", 
                    estimatedSize, compressedData.length, 
                    Math.round((1 - (double)compressedData.length / estimatedSize) * 100));
            } else {
                // Compression failed, fall back to uncompressed
                wrapper.put("Data", weightData);
                wrapper.putBoolean("Compressed", false);
                LOGGER.warn("Compression failed, sending uncompressed data");
            }
        } else {
            // Small data, no compression needed
            wrapper.put("Data", weightData);
            wrapper.putBoolean("Compressed", false);
        }
        
        // Write the wrapper tag to the buffer
        buf.writeNbt(wrapper);
    }
    
    /**
     * Gets the weight data in this packet.
     *
     * @return The weight data
     */
    public CompoundTag getWeightData() {
        return weightData;
    }
    
    /**
     * Handles this packet on the client side.
     * This method is called when the packet is received on the client.
     *
     * @param packet The packet to handle
     * @param context The payload context
     */
    public static void handle(SyncWeightDataPacket packet, IPayloadContext context) {
        // Make sure we're on the client side
        context.enqueueWork(() -> {
            try {
                // Validate packet
                if (packet == null) {
                    LOGGER.error("Received null weight data packet");
                    return;
                }
                
                // Validate weight data
                CompoundTag weightData = packet.getWeightData();
                if (weightData == null) {
                    LOGGER.error("Received weight data packet with null data");
                    return;
                }
                
                // Log detailed information about the received data
                LOGGER.debug("Received weight data packet: {}", weightData);
                
                // Get the client player
                if (Minecraft.getInstance().player != null) {
                    // Get the player's weight data
                    IPlayerWeight playerWeight = PlayerWeightProvider.getPlayerWeight(Minecraft.getInstance().player);

                    if (playerWeight != null) {
                        // Apply the received data
                        HolderLookup.Provider provider = Minecraft.getInstance().level.registryAccess();
                        playerWeight.deserializeNBT(provider, weightData);
                        LOGGER.debug("Successfully applied weight data to player");
                    } else {
                        LOGGER.error("Player weight capability is null");
                    }
                } else {
                    LOGGER.error("Client player is null");
                }
            } catch (Exception e) {
                LOGGER.error("Error handling weight data packet: {}", e.getMessage(), e);
            }
        });
    }
    
    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
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