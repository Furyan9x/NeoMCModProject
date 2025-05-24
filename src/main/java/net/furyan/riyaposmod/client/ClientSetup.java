package net.furyan.riyaposmod.client;

import net.furyan.riyaposmod.RiyaposMod;
import net.furyan.riyaposmod.client.gui.SkillOverlayGui;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid = RiyaposMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    public static final ResourceLocation SKILL_OVERLAY_RL = ResourceLocation.fromNamespaceAndPath(RiyaposMod.MOD_ID, "skill_overlay");

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        RiyaposMod.LOGGER.info("Registering GUI layers for {}", RiyaposMod.MOD_ID);
        event.registerAboveAll(SKILL_OVERLAY_RL, new SkillOverlayGui());
        // If you want it above a specific vanilla element, you can use:
        // event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), SKILL_OVERLAY_RL, new SkillOverlayGui());
        // Or below:
        // event.registerBelow(VanillaGuiOverlay.CROSSHAIR.id(), SKILL_OVERLAY_RL, new SkillOverlayGui());
    }
} 