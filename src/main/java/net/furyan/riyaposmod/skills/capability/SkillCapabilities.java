package net.furyan.riyaposmod.skills.capability;

import net.furyan.riyaposmod.RiyaposMod;
import net.furyan.riyaposmod.skills.api.ISkillData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import org.jetbrains.annotations.Nullable;

public class SkillCapabilities {

    public static final EntityCapability<ISkillData, @Nullable Void> PLAYER_SKILLS =
            EntityCapability.createVoid(ResourceLocation.fromNamespaceAndPath(RiyaposMod.MOD_ID, "player_skills"), ISkillData.class);

    private SkillCapabilities() {}

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerEntity(
            SkillCapabilities.PLAYER_SKILLS,
            EntityType.PLAYER,
            new SkillDataProvider() // This provider handles NBT and gives the ISkillData instance
        );
    }
} 