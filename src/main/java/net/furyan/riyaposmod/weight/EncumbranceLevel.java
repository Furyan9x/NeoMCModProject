package net.furyan.riyaposmod.weight;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import java.util.Collections;
import java.util.List;

public enum EncumbranceLevel {
    NORMAL(0.0f, Collections.emptyList()),

    HEAVY(0.90f, List.of(
            new EffectData(MobEffects.MOVEMENT_SLOWDOWN,
                    /*durationTicks=*/200, /*amp=*/1))),

    OVERENCUMBERED(1.10f, List.of(
            new EffectData(MobEffects.MOVEMENT_SLOWDOWN,
                    /*durationTicks=*/200, /*amp=*/1),
            new EffectData(MobEffects.DIG_SLOWDOWN,
                    /*durationTicks=*/200, /*amp=*/0))),

    CRITICAL(1.50f, List.of(
            new EffectData(MobEffects.MOVEMENT_SLOWDOWN,
                    /*durationTicks=*/200, /*amp=*/1),
            new EffectData(MobEffects.DIG_SLOWDOWN,
                    /*durationTicks=*/200, /*amp=*/0),
            new EffectData(MobEffects.WEAKNESS,
                    /*durationTicks=*/200, /*amp=*/1)));

    private final float threshold;
    private final List<EffectData> effects;

    EncumbranceLevel(float threshold, List<EffectData> effects) {
        this.threshold = threshold;
        this.effects = effects;
    }

    public float getThreshold() { return threshold; }
    public List<EffectData> getEffects() { return effects; }

    public static EncumbranceLevel fromPercent(float pct) {
        if (pct >= CRITICAL.threshold) {
            return CRITICAL;
        } else if (pct >= OVERENCUMBERED.threshold) {
            return OVERENCUMBERED;
        } else if (pct >= HEAVY.threshold) {
            return HEAVY;
        } else {
            return NORMAL; // Default if below all other thresholds
        }
    }

    /** Simple record to keep effect‑type, duration, and amplifier together. */
    public record EffectData(Holder<MobEffect> effect, int durationTicks, int amplifier) {}
}
