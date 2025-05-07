package net.furyan.riyaposmod.mixin;

import immersive_aircraft.entity.InventoryVehicleEntity;
import net.furyan.riyaposmod.weight.aircraft.AircraftWeightHandler;
import net.furyan.riyaposmod.weight.aircraft.AircraftWeightNotifier;
import net.furyan.riyaposmod.weight.aircraft.AircraftUuidAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryVehicleEntity.class)
public class InventoryVehicleEntityMixin implements AircraftUuidAccessor {
    @Unique
    private UUID riyaposmod$uniqueId;
    @Unique
    private static final Map<UUID, Long> riyaposmod$lastAircraftOpenTimestamps = new HashMap<>();
    
    @Inject(method = "<init>", at = @At("RETURN"))
    private void riyaposmod$onConstruct(CallbackInfo ci) {
        InventoryVehicleEntity self = (InventoryVehicleEntity)(Object)this;
        if (!self.level().isClientSide()) { // SERVER-SIDE ONLY for initial assignment
            if (this.riyaposmod$uniqueId == null) {
                this.riyaposmod$uniqueId = UUID.randomUUID();
                System.out.println("[RiyaposMixin/SERVER] Constructor: Assigned NEW UUID: " + this.riyaposmod$uniqueId + " to entity " + self.getId());
            }
        } else {
            // Client-side instance. UUID will be synced via NBT or custom packet if needed by client logic beyond basic rendering.
            // For now, client's riyaposmod$uniqueId may remain null initially.
            // System.out.println("[RiyaposMixin/CLIENT] Constructor: Client instance created for entity " + self.getId());
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void riyaposmod$saveUniqueId(CompoundTag tag, CallbackInfo ci) {
        // This is called server-side when saving.
        if (this.riyaposmod$uniqueId != null) {
            tag.putUUID("RiyaposmodUniqueId", this.riyaposmod$uniqueId);
        } else {
            // Should not happen on server if constructor logic is correct.
            System.err.println("[RiyaposMixin/SERVER/ERROR] Attempting to save null UUID for " + ((InventoryVehicleEntity)(Object)this).getId());
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void riyaposmod$loadUniqueId(CompoundTag tag, CallbackInfo ci) {
        // This is called server-side when loading NBT, and client-side when entity data is synced.
        InventoryVehicleEntity self = (InventoryVehicleEntity)(Object)this;
        String side = self.level().isClientSide() ? "CLIENT" : "SERVER";

        if (tag.hasUUID("RiyaposmodUniqueId")) {
            this.riyaposmod$uniqueId = tag.getUUID("RiyaposmodUniqueId");
            // System.out.println("[RiyaposMixin/" + side + "] Loaded RiyaposmodUniqueId from NBT: " + this.riyaposmod$uniqueId + " for " + self.getId());
        } else {
            // If NBT is missing the ID:
            if (!self.level().isClientSide()) {
                // Assign a new one ONLY if on the server and it was missing (e.g. old entity).
                this.riyaposmod$uniqueId = UUID.randomUUID();
                System.err.println("[RiyaposMixin/SERVER] WARN: NBT missing RiyaposmodUniqueId for " + self.getId() + ", assigning new one: " + this.riyaposmod$uniqueId);
            } else {
                // On client, if NBT from server didn't have it, it will be null.
                // Client should not invent its own persistent UUID here if it's meant to match server.
                // System.out.println("[RiyaposMixin/CLIENT] NBT missing RiyaposmodUniqueId for " + self.getId() + ". Client UUID remains null for now.");
            }
        }
    }

    @Override
    public UUID riyaposmod$getUniqueId() {
        InventoryVehicleEntity self = (InventoryVehicleEntity)(Object)this;
        if (this.riyaposmod$uniqueId == null) {
            if (!self.level().isClientSide()) {
                // Server-side: Should have been set by constructor or NBT. This is an error.
                System.err.println("[RiyaposMixin/SERVER/ERROR] riyaposmod$uniqueId was NULL in getUniqueId for server entity " + self.getId() + ". This is unexpected. Assigning temporary.");
                this.riyaposmod$uniqueId = UUID.randomUUID(); // Server should always have one after init.
            } else {
                // Client-side: If null (e.g. NBT not synced yet or server didn't send one),
                // generate a transient UUID for this client instance. This allows client-side
                // caching in AircraftWeightHandler to function using this temporary ID.
                // This UUID will NOT match the server's authoritative UUID.
                // System.out.println("[RiyaposMixin/CLIENT/INFO] riyaposmod$uniqueId was NULL for client entity " + self.getId() + ". Generating transient UUID for client-side caching.");
                this.riyaposmod$uniqueId = UUID.randomUUID(); 
            }
        }
        return this.riyaposmod$uniqueId;
    }

    @Inject(method = "openInventory", at = @At("RETURN"))
    private void riyaposmod$onOpenInventory(ServerPlayer player, CallbackInfo ci) {
        InventoryVehicleEntity aircraft = (InventoryVehicleEntity)(Object)this;

        if (aircraft.level().isClientSide()) {
            return;
        }

        if (!(aircraft instanceof AircraftUuidAccessor uuidAccessor)) {
            // This should not happen if the interface is correctly applied by this mixin.
            System.err.println("[RiyaposMixin/InventoryOpen] ERROR: Aircraft " + aircraft.getId() + " does not implement AircraftUuidAccessor.");
            return;
        }
        UUID uuid = uuidAccessor.riyaposmod$getUniqueId();
        if (uuid == null) {
            // Server entities should always have a UUID by this point.
            System.err.println("[RiyaposMixin/InventoryOpen] ERROR: Server aircraft " + aircraft.getId() + " has null UUID.");
            return;
        }

        long now = System.currentTimeMillis();
        Long lastOpenTime = riyaposmod$lastAircraftOpenTimestamps.get(uuid);

        // Cooldown check to prevent spam from multiple internal calls or rapid re-opens
        if (lastOpenTime != null && (now - lastOpenTime < 1000)) { // 1-second cooldown
            // System.out.printf("[RiyaposMixin/InventoryOpen] Cooldown hit for UUID: %s. Skipping notification.%n", uuid);
            return;
        } 

        String identifier = BuiltInRegistries.ENTITY_TYPE.getKey(aircraft.getType()).toString();
        // getCapacityPercent will use cached values if available, or calculate if necessary.
        // It also handles calling notifyThresholdCrossed internally.
        float percent = AircraftWeightHandler.getCapacityPercent(identifier, aircraft);
        
        // Send the specific "current status on open" message
        AircraftWeightNotifier.sendCurrentThresholdMessage(aircraft, player, percent);
        
        riyaposmod$lastAircraftOpenTimestamps.put(uuid, now);
    }
}
