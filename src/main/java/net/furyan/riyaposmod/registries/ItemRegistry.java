package net.furyan.riyaposmod.registries;


import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.item.weapons.ExtendedSwordItem;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.item.weapons.StaffItem;
import net.furyan.riyaposmod.item.weapons.Element;
import net.furyan.riyaposmod.item.weapons.RiyaStaffItem;
import io.redspace.ironsspellbooks.util.ItemPropertiesHelper;
import net.furyan.riyaposmod.RiyaposMod;
import net.furyan.riyaposmod.item.weapons.RiyaStaffTier;
import net.furyan.riyaposmod.util.helpers.AttributeHelper;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Map;
import java.util.UUID;

public class ItemRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, RiyaposMod.MOD_ID);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }


    //Staves
    public static final DeferredHolder<Item, Item> STARTERSTAFF = ITEMS.register("starter_staff",
            () -> new StaffItem(ItemPropertiesHelper.equipment(1).attributes(ExtendedSwordItem.createAttributes(RiyaStaffTier.BEGINNER))));

    public static final DeferredHolder<Item, Item> FIRESTAFF_ONE = ITEMS.register("firestaff_one",
            () -> new RiyaStaffItem(Element.FIRE, ItemPropertiesHelper.equipment(1).attributes(ExtendedSwordItem.createAttributes(
                            AttributeHelper.withElement(RiyaStaffTier.NOVICE, Element.FIRE, RiyaStaffTier.NOVICE.getElementalBonus())))));

    public static final DeferredHolder<Item, Item> ICESTAFF_ONE = ITEMS.register("icestaff_one",
            () -> new StaffItem(ItemPropertiesHelper.equipment(1).attributes(ExtendedSwordItem.createAttributes(RiyaStaffTier.NOVICE))));
    public static final DeferredHolder<Item, Item> LIFESTAFF_ONE = ITEMS.register("lifestaff_one",
            () -> new StaffItem(ItemPropertiesHelper.equipment(1).attributes(ExtendedSwordItem.createAttributes(RiyaStaffTier.NOVICE))));
    public static final DeferredHolder<Item, Item> ELDRITCHSTAFF_ONE = ITEMS.register("eldritchstaff_one",
            () -> new StaffItem(ItemPropertiesHelper.equipment(1).attributes(ExtendedSwordItem.createAttributes(RiyaStaffTier.NOVICE))));
    public static final DeferredHolder<Item, Item> LIGHTNINGSTAFF_ONE = ITEMS.register("lightningstaff_one",
            () -> new StaffItem(ItemPropertiesHelper.equipment(1).attributes(ExtendedSwordItem.createAttributes(RiyaStaffTier.NOVICE))));
    public static final DeferredHolder<Item, Item> NATURESTAFF_ONE = ITEMS.register("naturestaff_one",
            () -> new StaffItem(ItemPropertiesHelper.equipment(1).attributes(ExtendedSwordItem.createAttributes(RiyaStaffTier.NOVICE))));
    public static final DeferredHolder<Item, Item> WATERSTAFF_ONE = ITEMS.register("waterstaff_one",
            () -> new StaffItem(ItemPropertiesHelper.equipment(1).attributes(ExtendedSwordItem.createAttributes(RiyaStaffTier.NOVICE))));
    public static final DeferredHolder<Item, Item> BLOODSTAFF_ONE = ITEMS.register("bloodstaff_one",
            () -> new StaffItem(ItemPropertiesHelper.equipment(1).attributes(ExtendedSwordItem.createAttributes(RiyaStaffTier.NOVICE))));
    public static final DeferredHolder<Item, Item> EVOCATIONSTAFF_ONE = ITEMS.register("evocationstaff_one",
            () -> new StaffItem(ItemPropertiesHelper.equipment(1).attributes(ExtendedSwordItem.createAttributes(RiyaStaffTier.NOVICE))));


}


