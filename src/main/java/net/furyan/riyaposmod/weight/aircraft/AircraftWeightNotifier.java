package net.furyan.riyaposmod.weight.aircraft;

import immersive_aircraft.entity.InventoryVehicleEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AircraftWeightNotifier {

    private static final Map<UUID, Integer> riyaposmod$lastAircraftThresholds = new HashMap<>();

    private static int getThresholdLevel(float percentUsed) {
        if (percentUsed > 1.0f) return 3; // Overloaded
        if (percentUsed > 0.9f) return 2; // Very Heavy / Almost Overloaded
        if (percentUsed > 0.5f) return 1; // Heavy
        return 0; // Normal / Not Heavy
    }

    public static void sendCurrentThresholdMessage(InventoryVehicleEntity aircraft, ServerPlayer player, float percentUsed) {
        MutableComponent messageComponent;
        ChatFormatting chatColor;
        int threshold = getThresholdLevel(percentUsed);

        switch (threshold) {
            case 3:
                messageComponent = Component.literal("Your aircraft is overloaded and can barely fly.");
                chatColor = ChatFormatting.DARK_RED;
                break;
            case 2:
                messageComponent = Component.literal("Your aircraft is almost overloaded. Engine Power is reduced and fuel consumption is increased.");
                chatColor = ChatFormatting.RED;
                break;
            case 1:
                messageComponent = Component.literal("Your aircraft is currently heavy. Engine Power is reduced and fuel consumption is increased.");
                chatColor = ChatFormatting.GOLD;
                break;
            default: // case 0
                messageComponent = Component.literal("Your aircraft is not heavy.");
                chatColor = ChatFormatting.GREEN;
                break;
        }
        player.sendSystemMessage(messageComponent.withStyle(chatColor));
    }

    public static void notifyThresholdCrossed(InventoryVehicleEntity aircraft, float percentUsed) {
        if (aircraft.level().isClientSide()) return; 

        if (!(aircraft instanceof AircraftUuidAccessor uuidAccessor)) {
            return;
        }
        UUID aircraftUUID = uuidAccessor.riyaposmod$getUniqueId();
        if (aircraftUUID == null) {
            System.err.println("[AircraftWeightNotifier] ERROR: Server aircraft has null UUID in notifyThresholdCrossed: " + aircraft.getId());
            return;
        }

        int newThreshold = getThresholdLevel(percentUsed);
        Integer lastThreshold = riyaposmod$lastAircraftThresholds.get(aircraftUUID);

        if (lastThreshold == null || lastThreshold != newThreshold) {
            riyaposmod$lastAircraftThresholds.put(aircraftUUID, newThreshold);

            if (lastThreshold == null && newThreshold == 0) { 
                return;
            }
             if (lastThreshold != null) { 
                MutableComponent messageComponent = null;
                ChatFormatting chatColor = ChatFormatting.WHITE;

                switch (newThreshold) {
                    case 3: 
                        messageComponent = Component.literal("Aircraft is now OVERLOADED! Flight is nearly impossible.");
                        chatColor = ChatFormatting.DARK_RED;
                        break;
                    case 2: 
                        messageComponent = Component.literal("Aircraft is now VERY HEAVY.");
                        chatColor = ChatFormatting.RED;
                        break;
                    case 1: 
                        messageComponent = Component.literal("Aircraft is now HEAVY.");
                        chatColor = ChatFormatting.GOLD;
                        break;
                    case 0: 
                        if (lastThreshold > 0) {
                            messageComponent = Component.literal("Aircraft is no longer heavy.");
                            chatColor = ChatFormatting.GREEN;
                        }
                        break;
                }

                if (messageComponent != null) {
                    Entity controllingPassenger = aircraft.getControllingPassenger();
                    if (controllingPassenger instanceof ServerPlayer serverPlayer) {
                        serverPlayer.sendSystemMessage(messageComponent.withStyle(chatColor));
                    }
                }
            }
        }
    }
}