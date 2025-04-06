package net.furyan.riyaposmod.item.weapons;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import io.redspace.ironsspellbooks.item.weapons.IronsWeaponTier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class RiyaStaffTier implements IronsWeaponTier {


    private final double elementalBonus;

    public double getElementalBonus() {
        return elementalBonus;
    }

    public static RiyaStaffTier BEGINNER = new RiyaStaffTier(0, 2,
            -3,
            new AttributeContainer(AttributeRegistry.MANA_REGEN, .10, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
            new AttributeContainer(AttributeRegistry.SPELL_POWER, .10, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    public static RiyaStaffTier NOVICE = new RiyaStaffTier(.10, 2,
            -3,
            new AttributeContainer(AttributeRegistry.MANA_REGEN, .15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
            new AttributeContainer(AttributeRegistry.COOLDOWN_REDUCTION, .10, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
            new AttributeContainer(AttributeRegistry.SPELL_POWER, .15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    public static RiyaStaffTier ADEPT = new RiyaStaffTier(.20, 4,
            -3,
            new AttributeContainer(AttributeRegistry.MANA_REGEN, .20, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
            new AttributeContainer(AttributeRegistry.COOLDOWN_REDUCTION, .15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
            new AttributeContainer(AttributeRegistry.SPELL_POWER, .20, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    public static RiyaStaffTier MASTER = new RiyaStaffTier(.30, 6,
            -3,
            new AttributeContainer(AttributeRegistry.MANA_REGEN, .25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
            new AttributeContainer(AttributeRegistry.COOLDOWN_REDUCTION, .20, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
            new AttributeContainer(AttributeRegistry.SPELL_POWER, .25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    public static RiyaStaffTier LEGENDARY = new RiyaStaffTier(.50, 10,
            -2.5f,
            new AttributeContainer(AttributeRegistry.MANA_REGEN, .50, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
            new AttributeContainer(AttributeRegistry.COOLDOWN_REDUCTION, .30, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
            new AttributeContainer(AttributeRegistry.SPELL_POWER, .50, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));



    float damage;
    float speed;
    AttributeContainer[] attributes;





    public RiyaStaffTier(double elementalBonus, float damage, float speed, AttributeContainer... attributes) {
        this.elementalBonus = elementalBonus;
        this.damage = damage;
        this.speed = speed;
        this.attributes = attributes;
    }

    @Override
    public float getSpeed() {
        return speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return damage;
    }

    public AttributeContainer[] getAdditionalAttributes() {
        return this.attributes;
    }

    public AttributeContainer getElementalAttribute(Element element) {
        return new AttributeContainer(
                element.getSpellPowerAttribute(),
                this.elementalBonus,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );
    }
}
