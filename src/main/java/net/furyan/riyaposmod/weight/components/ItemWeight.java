package net.furyan.riyaposmod.weight.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Immutable record to store an item's weight.
 */
public record ItemWeight(float weight) {
    /**
     * Codec for persisting to NBT/JSON.
     */
    public static final Codec<WeightComponents.ItemWeight> CODEC = RecordCodecBuilder.create(inst ->
            inst.group(
                    Codec.FLOAT.fieldOf("weight").forGetter(WeightComponents.ItemWeight::weight)
            ).apply(inst, WeightComponents.ItemWeight::new)
    );

    /**
     * StreamCodec for network synchronization.
     */
    public static final StreamCodec<ByteBuf, WeightComponents.ItemWeight> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, WeightComponents.ItemWeight::weight,
            WeightComponents.ItemWeight::new
    );
}
