package net.furyan.riyaposmod.client.events;

import net.furyan.riyaposmod.weight.WeightCalculator;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@OnlyIn(Dist.CLIENT)
public class ItemTooltipHandler {
    
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        float weight = WeightCalculator.getWeight(stack);
        float bonus = WeightCalculator.getCapacityBonus(stack);
        // Only add tooltip if item has a non-zero weight
        if (weight > 0) {
            // Insert weight tooltip after item name (index 0) but before other tooltips
            event.getToolTip().add(1, Component.translatable("tooltip.riyaposmod.weight", 
                String.format("%.1f", weight)));
        }
        if (bonus > 0) {
            event.getToolTip().add(1, Component.translatable("tooltip.riyaposmod.capacity",
                String.format("%.1f", bonus)));
        }
    }
}