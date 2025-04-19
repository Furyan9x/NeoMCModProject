package net.furyan.riyaposmod.item.weapons;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;

public enum Element {

    FIRE(AttributeRegistry.FIRE_SPELL_POWER),
    ICE(AttributeRegistry.ICE_SPELL_POWER),
    LIGHTNING(AttributeRegistry.LIGHTNING_SPELL_POWER),
    //WATER();
    HOLY(AttributeRegistry.HOLY_SPELL_POWER),
    NATURE(AttributeRegistry.NATURE_SPELL_POWER),
    ELDRITCH(AttributeRegistry.ELDRITCH_SPELL_POWER),
    EVOCATION(AttributeRegistry.EVOCATION_SPELL_POWER),
    BLOOD(AttributeRegistry.BLOOD_SPELL_POWER);
    //NECROMANCY();

    private final Holder<Attribute> spellPowerAttribute;

    Element(Holder<Attribute> attribute) {
        this.spellPowerAttribute = attribute;
    }

    public Holder<Attribute> getSpellPowerAttribute() {
        return spellPowerAttribute;
    }
}
