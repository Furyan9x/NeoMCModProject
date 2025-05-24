package net.furyan.riyaposmod.client.events;

import net.furyan.riyaposmod.RiyaposMod;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.Deque;
import java.util.LinkedList;

@EventBusSubscriber(modid = RiyaposMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class SkillDisplayTickHandler {

    public static final int MAX_MESSAGES_VISIBLE = 3;
    private static final long MESSAGE_DURATION_MS = 3000; // How long a message stays fully visible
    private static final long MESSAGE_FADE_IN_MS = 200;
    private static final long MESSAGE_FADE_OUT_MS = 500; // How long it takes to fade out
    private static final long XP_CONSOLIDATION_WINDOW_MS = 1000; // Consolidate XP for the same skill if gained within this time

    // Using a Deque to easily add to front and remove from back if needed
    private static final Deque<DisplayEntry> activeMessages = new LinkedList<>();

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().player == null || Minecraft.getInstance().isPaused()) {
            return;
        }
        tickDownMessages();
    }

    private static void tickDownMessages() {
        long currentTime = System.currentTimeMillis();
        activeMessages.removeIf(entry -> entry.isExpired(currentTime));
    }

    public static void addXpGainMessage(String skillId, int xpAmount) {
        Minecraft.getInstance().execute(() -> {
            long currentTime = System.currentTimeMillis();
            boolean consolidated = false;
            // Iterate from most recent, try to consolidate
            for (DisplayEntry entry : activeMessages) { // LinkedList iteration is fine for this size
                if (entry instanceof XpGainDisplayEntry xpEntry &&
                    xpEntry.skillId.equals(skillId) &&
                    (currentTime - xpEntry.creationTime) < XP_CONSOLIDATION_WINDOW_MS &&
                    !xpEntry.isFadingOut(currentTime)) {
                    xpEntry.addXp(xpAmount);
                    xpEntry.resetTimers(currentTime);
                    consolidated = true;
                    // Move to front to make it "most recent" again for rendering order
                    activeMessages.remove(xpEntry);
                    activeMessages.addFirst(xpEntry);
                    break;
                }
            }

            if (!consolidated) {
                if (activeMessages.size() >= MAX_MESSAGES_VISIBLE) {
                    activeMessages.removeLast(); // Remove oldest if full
                }
                activeMessages.addFirst(new XpGainDisplayEntry(skillId, xpAmount, currentTime));
            }
        });
    }

    public static Deque<DisplayEntry> getActiveMessages() {
        return activeMessages;
    }

    // --- Inner classes for message entries ---

    public abstract static class DisplayEntry {
        protected final String skillId;
        protected long creationTime; // When it was created or last consolidated
        protected long displayStartTime; // When it actually starts fading in (accounts for queue)

        protected DisplayEntry(String skillId, long creationTime) {
            this.skillId = skillId;
            this.creationTime = creationTime;
            this.displayStartTime = creationTime; // Default, can be adjusted if queued
        }

        public void resetTimers(long currentTime) {
            this.creationTime = currentTime;
            this.displayStartTime = currentTime;
        }

        public boolean isExpired(long currentTime) {
            return currentTime > displayStartTime + MESSAGE_FADE_IN_MS + MESSAGE_DURATION_MS + MESSAGE_FADE_OUT_MS;
        }

        public boolean isFadingOut(long currentTime) {
            return currentTime > displayStartTime + MESSAGE_FADE_IN_MS + MESSAGE_DURATION_MS;
        }

        public float getAlpha(long currentTime) {
            long timeSinceDisplayStart = currentTime - displayStartTime;
            if (timeSinceDisplayStart < 0) return 0f; // Not yet time to display

            if (timeSinceDisplayStart < MESSAGE_FADE_IN_MS) {
                return (float) timeSinceDisplayStart / MESSAGE_FADE_IN_MS; // Fade in
            } else if (timeSinceDisplayStart < MESSAGE_FADE_IN_MS + MESSAGE_DURATION_MS) {
                return 1.0f; // Fully visible
            } else if (timeSinceDisplayStart < MESSAGE_FADE_IN_MS + MESSAGE_DURATION_MS + MESSAGE_FADE_OUT_MS) {
                // Fade out
                return 1.0f - (float) (timeSinceDisplayStart - (MESSAGE_FADE_IN_MS + MESSAGE_DURATION_MS)) / MESSAGE_FADE_OUT_MS;
            } else {
                return 0.0f; // Expired or fully faded out
            }
        }
        
        public String getSkillNameFormatted() {
            if (skillId == null || skillId.isEmpty()) return "";
            return skillId.substring(0, 1).toUpperCase() + skillId.substring(1);
        }

        public abstract Component getMessage();
    }

    public static class XpGainDisplayEntry extends DisplayEntry {
        private int xpAmount;

        public XpGainDisplayEntry(String skillId, int xpAmount, long creationTime) {
            super(skillId, creationTime);
            this.xpAmount = xpAmount;
        }

        public void addXp(int additionalXp) {
            this.xpAmount += additionalXp;
        }

        @Override
        public Component getMessage() {
            return Component.literal(String.format("+%d %s", xpAmount, getSkillNameFormatted()));
        }
    }
} 