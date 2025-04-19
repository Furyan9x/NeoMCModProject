package net.furyan.riyaposmod.registries;

import net.furyan.riyaposmod.RiyaposMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Comparator;
import java.util.List;

public class CreativeTabRegistry {

        private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RiyaposMod.MOD_ID);

        public static void register(IEventBus eventBus) {
            TABS.register(eventBus);
        }

        public static final DeferredHolder<CreativeModeTab, CreativeModeTab> STAFF_TAB = TABS.register("riyapos_staves",
                () -> CreativeModeTab.builder().title(Component.translatable("itemGroup." + RiyaposMod.MOD_ID + ".staff_tab"))
                .icon(() -> new ItemStack(net.furyan.riyaposmod.registries.ItemRegistry.STARTERSTAFF.get()))

                .displayItems((enabledFeatures, entries) -> {
                    entries.accept(ItemRegistry.STARTERSTAFF.get());
                            //dynamically add all auto-generated staves in order of tier
                            List<String> tierOrder = List.of("novice", "adept", "master", "legendary");
                            ItemRegistry.STAFFS.entrySet().stream()
                                    .sorted(Comparator.comparingInt(entry -> {
                                        String key = entry.getKey().toLowerCase();
                                        for (int i = 0; i < tierOrder.size(); i++) {
                                            if (key.contains(tierOrder.get(i))) {
                                                return i; }
                                        }
                                        return tierOrder.size(); // Put unrecognized stuff at the end
                                    }))
                                    .forEach(entry -> entries.accept(entry.getValue().get()));
                })
                .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                .build());
    }
