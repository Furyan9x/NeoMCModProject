package net.furyan.riyaposmod.mixin;

import com.talhanation.smallships.world.entity.ship.BriggEntity;
import com.talhanation.smallships.world.entity.ship.CogEntity;
import com.talhanation.smallships.world.entity.ship.ContainerShip;
import com.talhanation.smallships.world.entity.ship.DrakkarEntity;
import com.talhanation.smallships.world.entity.ship.GalleyEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.furyan.riyaposmod.weight.ships.ContainerShipAccessor;
import net.furyan.riyaposmod.weight.ships.ShipWeightHandler;
import net.furyan.riyaposmod.weight.ships.ShipWeightNotifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

@Mixin(ContainerShip.class)
public abstract class ContainerShipMixin implements ContainerShipAccessor {
    private static final Logger LOGGER = LoggerFactory.getLogger("ShipWeightMixin");
    private static long lastLogTime = 0;

    @Shadow public abstract float getContainerModifier();
    
    @Unique
    private java.util.UUID riyaposmod$uniqueId;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstruct(CallbackInfo ci) {
        if (this.riyaposmod$uniqueId == null) {
            this.riyaposmod$uniqueId = java.util.UUID.randomUUID();
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void saveUniqueId(CompoundTag tag, CallbackInfo ci) {
        tag.putUUID("RiyaposmodUniqueId", this.riyaposmod$uniqueId);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void loadUniqueId(CompoundTag tag, CallbackInfo ci) {
        if (tag.hasUUID("RiyaposmodUniqueId")) {
            this.riyaposmod$uniqueId = tag.getUUID("RiyaposmodUniqueId");
        } else {
            this.riyaposmod$uniqueId = java.util.UUID.randomUUID();
        }
    }

    public java.util.UUID riyaposmod$getUniqueId() {
        return this.riyaposmod$uniqueId;
    }

    @Inject(method = "getContainerModifier", at = @At("RETURN"), cancellable = true)
    private void modifyContainerModifier(CallbackInfoReturnable<Float> cir) {
        ContainerShip ship = (ContainerShip)(Object)this;
        String shipType = getShipType(ship);
        float percentUsed = ShipWeightHandler.getCapacityPercent(shipType, ship);
        float weightModifier = ShipWeightHandler.getSpeedModifier(percentUsed);
        
        // Convert from multiplier (1.0 - 0.0) to percentage reduction (0 - 100)
        float additionalModifier = (1.0f - weightModifier) * 100f;
        
        // Original modifier + our weight-based modifier
        float finalModifier = cir.getReturnValue() + additionalModifier;
        
        // Debug logging
        long now = System.currentTimeMillis();
        if (now - lastLogTime >= 10_000L) {
            LOGGER.debug("[ShipWeight] Type={} Load={}%, Base modifier: {}, Weight modifier: {}%, Final: {}%",
                    shipType, String.format("%.1f", percentUsed * 100), 
                    cir.getReturnValue(), additionalModifier, finalModifier);
            lastLogTime = now;
        }
        
        cir.setReturnValue(finalModifier);
    }
    
    private String getShipType(ContainerShip containerShip) {
        if (containerShip instanceof CogEntity) return CogEntity.ID;
        if (containerShip instanceof BriggEntity) return BriggEntity.ID;
        if (containerShip instanceof GalleyEntity) return GalleyEntity.ID;
        if (containerShip instanceof DrakkarEntity) return DrakkarEntity.ID;
        return "unknown";
    }

    @Inject(method = "openCustomInventoryScreen", at = @At("RETURN"))
    private void onOpenCustomInventoryScreen(Player player, CallbackInfo ci) {
        if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            ShipWeightNotifier.sendCurrentThresholdMessage((ContainerShip)(Object)this, serverPlayer);
        }
    }
}
