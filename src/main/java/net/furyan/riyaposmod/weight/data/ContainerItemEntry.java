package net.furyan.riyaposmod.weight.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ContainerItemEntry(float weight, float baseCapacityBonus, int slots, float slotMultiplier, boolean dynamic) {
    public static final ContainerItemEntry DEFAULT = new ContainerItemEntry(1.0f, 0.0f, 0, 0.0f, false);
    
    public static final Codec<ContainerItemEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.FLOAT.fieldOf("weight").forGetter(ContainerItemEntry::weight),
        Codec.FLOAT.fieldOf("base_capacity_bonus").forGetter(ContainerItemEntry::baseCapacityBonus),
        Codec.INT.fieldOf("slots").forGetter(ContainerItemEntry::slots),
        Codec.FLOAT.optionalFieldOf("slot_multiplier", 0.0f).forGetter(ContainerItemEntry::slotMultiplier),
        Codec.BOOL.optionalFieldOf("dynamic", false).forGetter(ContainerItemEntry::dynamic)
    ).apply(instance, ContainerItemEntry::new));
    
    public float getCapacityBonus() {
        return baseCapacityBonus + (dynamic ? slots * slotMultiplier : 0);
    }
} 