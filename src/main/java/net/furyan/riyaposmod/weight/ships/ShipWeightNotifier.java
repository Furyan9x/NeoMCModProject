package net.furyan.riyaposmod.weight.ships;

import com.talhanation.smallships.world.entity.ship.BriggEntity;
import com.talhanation.smallships.world.entity.ship.CogEntity;
import com.talhanation.smallships.world.entity.ship.ContainerShip;
import com.talhanation.smallships.world.entity.ship.DrakkarEntity;
import com.talhanation.smallships.world.entity.ship.GalleyEntity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import java.util.*;

public class ShipWeightNotifier {
    // Map ship UUID to last threshold
    private static final Map<UUID, Integer> lastThresholds = new WeakHashMap<>();

    // Thresholds: 0 = normal, 1 = heavy, 2 = very heavy, 3 = overloaded
    private static int getThreshold(float percentUsed) {
        if (percentUsed >= 1.0f) return 3;
        if (percentUsed > 0.90f) return 2;
        if (percentUsed > 0.50f) return 1;
        return 0;
    }

    public static void notifyThresholdCrossed(ContainerShip ship, float percentUsed) {
        // Only run on server
        if (ship.level().isClientSide()) return;
        UUID uuid = null;
        if (ship instanceof ContainerShipAccessor accessor) {
            uuid = accessor.riyaposmod$getUniqueId();
        }
        if (uuid == null) return;
        int newThreshold = getThreshold(percentUsed);
        Integer last = lastThresholds.get(uuid);
        if (last != null && last == newThreshold) return; // No change
        lastThresholds.put(uuid, newThreshold);
        // Prepare message
        String msg;
        ChatFormatting color;
        switch (newThreshold) {
            case 1:
                msg = "Your ship is now heavily loaded and moves slower!";
                color = ChatFormatting.GOLD;
                break;
            case 2:
                msg = "Your ship is very heavily loaded! Movement is greatly reduced.";
                color = ChatFormatting.RED;
                break;
            case 3:
                msg = "Your ship is overloaded and cannot move!";
                color = ChatFormatting.DARK_RED;
                break;
            default:
                msg = "Your ship is no longer heavily loaded.";
                color = ChatFormatting.GREEN;
        }
        // Notify all players riding the ship
        for (var passenger : ship.getPassengers()) {
            if (passenger instanceof ServerPlayer player) {
                player.sendSystemMessage(Component.literal(msg).withStyle(color));
            }
        }
    }
        private static String getShipType(ContainerShip containerShip) {
            if (containerShip instanceof CogEntity) return CogEntity.ID;
            if (containerShip instanceof BriggEntity) return BriggEntity.ID;
            if (containerShip instanceof GalleyEntity) return GalleyEntity.ID;
            if (containerShip instanceof DrakkarEntity) return DrakkarEntity.ID;
            return "unknown";
        }
    
        public static void sendCurrentThresholdMessage(ContainerShip ship, ServerPlayer player) {
        String shipType = getShipType(ship);
        float percentUsed = ShipWeightHandler.getCapacityPercent(shipType, ship);
        int threshold = getThreshold(percentUsed);
        String msg;
        ChatFormatting color;
        switch (threshold) {
            case 1:
                msg = "Your ship is currently Heavy. Speed is reduced.";
                color = ChatFormatting.GOLD;
                break;
            case 2:
                msg = "Your ship is almost overloaded. Speed is greatly reduced.";
                color = ChatFormatting.RED;
                break;
            case 3:
                msg = "Your ship is currently overloaded and cannot move.";
                color = ChatFormatting.DARK_RED;
                break;
            default:
                msg = "Your ship is not heavy.";
                color = ChatFormatting.GREEN;
        }
        player.sendSystemMessage(Component.literal(msg).withStyle(color));
    }
} 