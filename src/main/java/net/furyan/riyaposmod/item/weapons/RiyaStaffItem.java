package net.furyan.riyaposmod.item.weapons;


import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.antlr.v4.runtime.misc.MultiMap;


import java.util.UUID;

import static io.redspace.ironsspellbooks.registries.ComponentRegistry.CASTING_IMPLEMENT;

public class RiyaStaffItem extends Item {

    public RiyaStaffItem(Properties pProperties) {
        super(pProperties.component(CASTING_IMPLEMENT, Unit.INSTANCE)/*.component(MULTIHAND_WEAPON, Unit.INSTANCE)*/);
    }









    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }

    @Override
    public boolean isEnchantable(ItemStack pStack) {
        return true;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 20;
    }

    public boolean hasCustomRendering(){
        return false;
    }
}
