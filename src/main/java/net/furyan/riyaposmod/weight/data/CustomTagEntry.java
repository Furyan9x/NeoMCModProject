package net.furyan.riyaposmod.weight.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record CustomTagEntry(float weight) {
    public static final CustomTagEntry DEFAULT = new CustomTagEntry(1.0f);
    
    public static final Codec<CustomTagEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.FLOAT.fieldOf("weight").forGetter(CustomTagEntry::weight)
    ).apply(instance, CustomTagEntry::new));
} 