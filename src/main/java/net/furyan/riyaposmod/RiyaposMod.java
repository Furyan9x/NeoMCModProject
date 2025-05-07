package net.furyan.riyaposmod;

import net.furyan.riyaposmod.client.events.ItemTooltipHandler;
import net.furyan.riyaposmod.commands.BenchmarkWeightCommand;
import net.furyan.riyaposmod.commands.DumpItemsCommand;
import net.furyan.riyaposmod.faction.commands.FactionCommands;
import net.furyan.riyaposmod.network.ModNetworking;
import net.furyan.riyaposmod.registries.CreativeTabRegistry;
import net.furyan.riyaposmod.registries.FactionAttachmentRegistry;
import net.furyan.riyaposmod.registries.ItemRegistry;
import net.furyan.riyaposmod.registries.WeightAttachmentRegistry;
import net.furyan.riyaposmod.faction.FactionRegistry;
import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import org.slf4j.Logger;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.furyan.riyaposmod.datagen.WeightDataProvider;
import net.furyan.riyaposmod.weight.WeightSystemManager;
import net.furyan.riyaposmod.weight.data.WeightDataManager;



// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(RiyaposMod.MOD_ID)
public class RiyaposMod {
    public static final String MOD_ID = "riyaposmod";
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private static final WeightDataManager WEIGHT_DATA = new WeightDataManager();

    public RiyaposMod(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onConfigLoad);

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
        NeoForge.EVENT_BUS.addListener(this::onDataPackReload);

        // Register the network system
        modEventBus.register(ModNetworking.class);

        // Get Registries
        ItemRegistry.register(modEventBus);
        CreativeTabRegistry.register(modEventBus);
        FactionRegistry.register(modEventBus);
        FactionAttachmentRegistry.register(modEventBus);
        WeightAttachmentRegistry.register(modEventBus);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register weight data reload listener
        NeoForge.EVENT_BUS.addListener(this::onAddReloadListeners);
        modEventBus.addListener(this::onGatherData);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // Register to handle server stopping events for cleanup
        NeoForge.EVENT_BUS.addListener(WeightSystemManager::onServerStopping);
    }

    private void onConfigLoad(ModConfigEvent.Loading event) {
        
        }

    private void registerCommands(RegisterCommandsEvent evt) {
        // Register faction commands
        FactionCommands.register(evt.getDispatcher());
        DumpItemsCommand.register(evt.getDispatcher());
        // Register the weight system benchmark command
        BenchmarkWeightCommand.register(evt.getDispatcher());
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
    private void onDataPackReload(OnDatapackSyncEvent event) {
        
    }

    private void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(WEIGHT_DATA);
    }
    
    private void onGatherData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput packOutput = gen.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        
        gen.addProvider(event.includeServer(), new WeightDataProvider(
            packOutput,
            lookupProvider,
            event.getExistingFileHelper()
        ));
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Log that the server is starting
        LOGGER.info("Server starting, initializing faction system");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {


        @SubscribeEvent
        private static void registerDataMapTypes(RegisterDataMapTypesEvent event) {
            
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)  {
            NeoForge.EVENT_BUS.register(ItemTooltipHandler.class);

        }

    }
}
