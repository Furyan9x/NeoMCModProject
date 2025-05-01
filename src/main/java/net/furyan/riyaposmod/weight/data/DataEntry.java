package net.furyan.riyaposmod.weight.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record DataEntry(float weight) {
    public static final DataEntry DEFAULT = new DataEntry(1.0f);
    
    public static final Codec<DataEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.FLOAT.fieldOf("weight").forGetter(DataEntry::weight)
    ).apply(instance, DataEntry::new));
} 