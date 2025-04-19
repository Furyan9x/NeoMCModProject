package net.furyan.riyaposmod.registries;



import io.redspace.ironsspellbooks.api.item.weapons.ExtendedSwordItem;
import io.redspace.ironsspellbooks.item.weapons.StaffItem;
import net.furyan.riyaposmod.item.weapons.Element;
import net.furyan.riyaposmod.item.weapons.RiyaStaffItem;
import io.redspace.ironsspellbooks.util.ItemPropertiesHelper;
import net.furyan.riyaposmod.RiyaposMod;
import net.furyan.riyaposmod.item.weapons.RiyaStaffTier;
import net.furyan.riyaposmod.util.helpers.AttributeHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.Map;

public class ItemRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, RiyaposMod.MOD_ID);
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        registerStaves();
    }

    public static final Map<String, DeferredHolder<Item, Item>> STAFFS = new HashMap<>();

    public static void registerStaves() {
        for (RiyaStaffTier tier : RiyaStaffTier.values()) {
            if (tier == RiyaStaffTier.BEGINNER) {
                continue; // Skip BEGINNER tier from auto-registration
            }
        for (Element element : Element.values()) {
                String registryName = element.name().toLowerCase() + "staff_" + tier.name().toLowerCase();

                DeferredHolder<Item, Item> staff = ITEMS.register(registryName,
                        () -> new RiyaStaffItem(ItemPropertiesHelper.equipment(1)
                                .attributes(ExtendedSwordItem.createAttributes(AttributeHelper.withElement(
                                        tier, element, tier.getElementalBonus())))));

                STAFFS.put(registryName, staff);
            }
        }
    }

    //single beginner staff
    public static final DeferredHolder<Item, Item> STARTERSTAFF = ITEMS.register("starter_staff",
            () -> new StaffItem(ItemPropertiesHelper.equipment(1).attributes(ExtendedSwordItem.createAttributes(RiyaStaffTier.BEGINNER))));

//    public static final DeferredHolder<Item, Item> FIRESTAFF_ONE = ITEMS.register("firestaff_one",
//            () -> new RiyaStaffItem(ItemPropertiesHelper.equipment(1).attributes(ExtendedSwordItem.createAttributes(
//                            AttributeHelper.withElement(RiyaStaffTier.NOVICE, Element.FIRE, RiyaStaffTier.NOVICE.getElementalBonus())))));


}


