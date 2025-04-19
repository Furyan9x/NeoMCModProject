package net.furyan.riyaposmod.util.helpers;

import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import io.redspace.ironsspellbooks.item.weapons.IronsWeaponTier;
import net.furyan.riyaposmod.item.weapons.Element;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.Arrays;

public class AttributeHelper {
    public static AttributeContainer[] mergeAttributes(AttributeContainer[] base, AttributeContainer extra) {
        AttributeContainer[] result = Arrays.copyOf(base, base.length + 1);
        result[base.length] = extra;
        return result;
    }
    public static IronsWeaponTier withElement(IronsWeaponTier base, Element element, double elementalBonus) {
        return new IronsWeaponTier() {
            private final AttributeContainer[] combined = mergeAttributes(
                    base.getAdditionalAttributes(),
                    new AttributeContainer(
                            element.getSpellPowerAttribute(),
                            elementalBonus,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    )
            );

            @Override
            public float getAttackDamageBonus() {
                return base.getAttackDamageBonus();
            }

            @Override
            public float getSpeed() {
                return base.getSpeed();
            }

            @Override
            public AttributeContainer[] getAdditionalAttributes() {
                return combined;
            }
        };
    }
}