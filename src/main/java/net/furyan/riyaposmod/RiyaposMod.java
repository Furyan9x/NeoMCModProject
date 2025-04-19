package net.furyan.riyaposmod;


import net.furyan.riyaposmod.client.events.ItemTooltipHandler;
import net.furyan.riyaposmod.network.ModNetworking;
import net.furyan.riyaposmod.registries.CreativeTabRegistry;
import net.furyan.riyaposmod.registries.FactionAttachmentRegistry;
import net.furyan.riyaposmod.registries.ItemRegistry;
import net.furyan.riyaposmod.registries.WeightAttachmentRegistry;
import net.furyan.riyaposmod.faction.FactionRegistry;
import net.furyan.riyaposmod.faction.commands.FactionCommands;
import net.furyan.riyaposmod.weight.WeightDataGenerator;
import net.furyan.riyaposmod.weight.WeightRegistry;

import com.mojang.logging.LogUtils;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;


// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(RiyaposMod.MOD_ID)
public class RiyaposMod {
    public static final String MOD_ID = "riyaposmod";
    private static final Logger LOGGER = LogUtils.getLogger();


    public RiyaposMod(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);

        // Register the network system
        modEventBus.register(ModNetworking.class);



        //Get Registries
        ItemRegistry.register(modEventBus);
        CreativeTabRegistry.register(modEventBus);
        FactionRegistry.register(modEventBus);
        FactionAttachmentRegistry.register(modEventBus);
        WeightRegistry.register(modEventBus);
        WeightAttachmentRegistry.register(modEventBus);

        // Register weight data generator
        WeightDataGenerator.register();

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);


        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void registerCommands(RegisterCommandsEvent evt) {
        // Register faction commands
        FactionCommands.register(evt.getDispatcher());
        LOGGER.info("Registered faction commands");

        //ClientCommands.register(evt.getDispatcher());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Faction data is now handled by the attachment system
        event.enqueueWork(() -> {
            LOGGER.info("Faction system initialized via attachment system");

            // Initialize network event handlers
            ModNetworking.init();
            LOGGER.info("Faction network system initialized");
        });
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)  {

    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Log that the server is starting
        LOGGER.info("Server starting, initializing faction system");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {




        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)  {
            NeoForge.EVENT_BUS.register(ItemTooltipHandler.class);

        }

    }
}
