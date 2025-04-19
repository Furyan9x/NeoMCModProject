package net.furyan.riyaposmod.item.weapons;


import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;


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

//    public boolean hasCustomRendering(){
//        return false;
//    }
}
