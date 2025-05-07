package net.furyan.riyaposmod.mixin;

import immersive_aircraft.entity.EngineVehicle;
import immersive_aircraft.entity.InventoryVehicleEntity;
import net.furyan.riyaposmod.weight.aircraft.AircraftUuidAccessor;
import net.furyan.riyaposmod.weight.aircraft.AircraftWeightHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mixin(value = EngineVehicle.class, remap = false) // remap = false as Immersive Aircraft is a mod
public abstract class EngineVehicleMixin {

    @Shadow public abstract float getEnginePower();

    @Unique
    private static final Map<UUID, Long> riyaposmod$lastEngineLogTime = new HashMap<>();
    @Unique
    private static final long RIYAPOSMOD_LOG_COOLDOWN_MS = 5000; // Log every 5 seconds per aircraft

    @Inject(method = "getEnginePower", at = @At("RETURN"), cancellable = true)
    private void riyaposmod$modifyEnginePower(CallbackInfoReturnable<Float> cir) {
        EngineVehicle self = (EngineVehicle)(Object)this;
        String side = self.level().isClientSide() ? "CLIENT" : "SERVER";

        if (self instanceof InventoryVehicleEntity aircraftEntity && aircraftEntity instanceof AircraftUuidAccessor uuidAccessor) {
            String identifier = BuiltInRegistries.ENTITY_TYPE.getKey(aircraftEntity.getType()).toString();
            float percentUsed = AircraftWeightHandler.getCapacityPercent(identifier, aircraftEntity);
            float performanceModifier = AircraftWeightHandler.getPerformanceModifier(percentUsed);
            
            float originalPower = cir.getReturnValue();
            cir.setReturnValue(originalPower * performanceModifier);

            UUID aircraftUUID = uuidAccessor.riyaposmod$getUniqueId();
            if (aircraftUUID != null) {
                long currentTime = System.currentTimeMillis();
                long lastLogTime = riyaposmod$lastEngineLogTime.getOrDefault(aircraftUUID, 0L);

                if (currentTime - lastLogTime > RIYAPOSMOD_LOG_COOLDOWN_MS) {
                    System.out.printf("[RiyaposMixin/%s/EnginePower] Aircraft: %s (UUID: %s), %%Used: %.2f, PerfMod: %.2f, OrigP: %.2f, NewP: %.2f%n", 
                        side, identifier, aircraftUUID.toString(), percentUsed, performanceModifier, originalPower, originalPower * performanceModifier);
                    riyaposmod$lastEngineLogTime.put(aircraftUUID, currentTime);
                }
            }
        }
    }

    @Inject(method = "getFuelConsumption", at = @At("RETURN"), cancellable = true)
    private void riyaposmod$modifyFuelConsumption(CallbackInfoReturnable<Float> cir) {
        EngineVehicle self = (EngineVehicle)(Object)this;
        String side = self.level().isClientSide() ? "CLIENT" : "SERVER";

        if (self instanceof InventoryVehicleEntity aircraftEntity && aircraftEntity instanceof AircraftUuidAccessor uuidAccessor) {
            String identifier = BuiltInRegistries.ENTITY_TYPE.getKey(aircraftEntity.getType()).toString();
            float percentUsed = AircraftWeightHandler.getCapacityPercent(identifier, aircraftEntity);
            float fuelModifier = AircraftWeightHandler.getFuelConsumptionModifier(percentUsed);

            float originalConsumption = cir.getReturnValue();
            cir.setReturnValue(originalConsumption * fuelModifier);

            UUID aircraftUUID = uuidAccessor.riyaposmod$getUniqueId();
             if (aircraftUUID != null) { 
                long currentTime = System.currentTimeMillis();
                long lastLogTime = riyaposmod$lastEngineLogTime.getOrDefault(aircraftUUID, 0L);
                boolean canLogFuel = currentTime - lastLogTime > RIYAPOSMOD_LOG_COOLDOWN_MS;

                if (canLogFuel) { 
                    System.out.printf("[RiyaposMixin/%s/FuelConsumption] Aircraft: %s (UUID: %s), %%Used: %.2f, FuelMod: %.2f, OrigC: %.2f, NewC: %.2f%n", 
                        side, identifier, aircraftUUID.toString(), percentUsed, fuelModifier, originalConsumption, originalConsumption * fuelModifier);
                    riyaposmod$lastEngineLogTime.put(aircraftUUID, currentTime); 
                }
            }
        }
    }
} 