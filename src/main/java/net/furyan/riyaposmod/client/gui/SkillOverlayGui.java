package net.furyan.riyaposmod.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.furyan.riyaposmod.client.events.SkillDisplayTickHandler;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.Deque;
import java.util.Iterator;
import java.util.List;


@OnlyIn(Dist.CLIENT)
public class SkillOverlayGui implements LayeredDraw.Layer {

    private static final int XP_GAIN_Y_START_OFFSET = 60; // Pixels from top for the first XP gain message
    private static final int MESSAGE_SPACING = 15; // Vertical spacing between stacked messages

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) {
            return;
        }

        Font font = mc.font;
        long currentTime = System.currentTimeMillis();
        Deque<SkillDisplayTickHandler.DisplayEntry> activeMessages = SkillDisplayTickHandler.getActiveMessages();

        if (activeMessages.isEmpty()) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int xpGainDisplayCount = 0;

        int screenWidth = mc.getWindow().getGuiScaledWidth();

        Iterator<SkillDisplayTickHandler.DisplayEntry> descendingIterator = activeMessages.descendingIterator();

        while(descendingIterator.hasNext()){
            SkillDisplayTickHandler.DisplayEntry entry = descendingIterator.next();
            float alpha = entry.getAlpha(currentTime);

            if (alpha <= 0.01f) { // Skip nearly invisible messages
                continue;
            }

            Component message = entry.getMessage();
            int textWidth = font.width(message);
            int xPos = (screenWidth - textWidth) / 2;
            int yPos;

            yPos = XP_GAIN_Y_START_OFFSET + (xpGainDisplayCount * MESSAGE_SPACING);
            xpGainDisplayCount++;
            
            if (xpGainDisplayCount > SkillDisplayTickHandler.MAX_MESSAGES_VISIBLE) { 
                continue;
            }

            int color = ((int)(alpha * 255.0f) << 24) | 0xFFFFFF; // White text with calculated alpha
            guiGraphics.drawString(font, message, xPos, yPos, color);
        }

        RenderSystem.disableBlend();
    }

    // Static method to be called from the packet handler or a client event for fireworks
    public static void triggerLevelUpFireworks(Player player) {
        if (player == null || !(player.level() instanceof ClientLevel clientLevel)) return;

        int[] flightDurations = {0, 1, 0, 2};

        for (int flightDuration : flightDurations) {
            ItemStack fireworkStack = new ItemStack(Items.FIREWORK_ROCKET);

            FireworkExplosion.Shape shape = player.getRandom().nextInt(2) == 0 ? FireworkExplosion.Shape.SMALL_BALL : FireworkExplosion.Shape.LARGE_BALL;
            boolean flicker = player.getRandom().nextBoolean();
            boolean trail = player.getRandom().nextBoolean();
            
            IntList colorIntList = new IntArrayList();
            int numColors = player.getRandom().nextInt(3) + 1;
            for (int c = 0; c < numColors; c++) {
                colorIntList.add(java.awt.Color.HSBtoRGB(player.getRandom().nextFloat(), 1.0f, 1.0f));
            }
            IntList fadeColorIntList = new IntArrayList(); // Empty for now

            FireworkExplosion explosion = new FireworkExplosion(shape, colorIntList, fadeColorIntList, trail, flicker);
            Fireworks fireworksComponent = new Fireworks((byte) flightDuration, List.of(explosion));
            fireworkStack.set(DataComponents.FIREWORKS, fireworksComponent);

            double offsetX = (player.getRandom().nextDouble() - 0.5) * player.getBbWidth() * 0.8;
            double offsetZ = (player.getRandom().nextDouble() - 0.5) * player.getBbWidth() * 0.8;
            
            FireworkRocketEntity fireworkRocket = new FireworkRocketEntity(clientLevel, 
                player.getX() + offsetX, 
                player.getY() + player.getEyeHeight() - 0.15, 
                player.getZ() + offsetZ, 
                fireworkStack);
            
            Vec3 lookAngle = player.getLookAngle(); 
            double spreadFactor = 0.3; 
            fireworkRocket.setDeltaMovement(
                lookAngle.x * 0.1 + (player.getRandom().nextDouble() - 0.5) * spreadFactor,
                0.3 + player.getRandom().nextDouble() * 0.2, 
                lookAngle.z * 0.1 + (player.getRandom().nextDouble() - 0.5) * spreadFactor
            );

            clientLevel.addFreshEntity(fireworkRocket);
        }

        // TODO: Add a level up sound effect here if desired: player.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.0F);
    }
} 