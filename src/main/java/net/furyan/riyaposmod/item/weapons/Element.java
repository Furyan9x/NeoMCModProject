package net.furyan.riyaposmod.item.weapons;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;

public enum Element {

    FIRE(AttributeRegistry.FIRE_SPELL_POWER),
    ICE(AttributeRegistry.ICE_SPELL_POWER),
    LIGHTNING(AttributeRegistry.LIGHTNING_SPELL_POWER);

    private final Holder<Attribute> spellPowerAttribute;

    Element(Holder<Attribute> attribute) {
        this.spellPowerAttribute = attribute;
    }

    public Holder<Attribute> getSpellPowerAttribute() {
        return spellPowerAttribute;
    }
}
