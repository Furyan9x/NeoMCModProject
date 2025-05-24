package net.furyan.riyaposmod.skills.capability;

import net.furyan.riyaposmod.skills.api.ISkillData;
import net.furyan.riyaposmod.skills.core.SkillData;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.Nullable;


public class SkillDataProvider implements ICapabilityProvider<Entity, Void, ISkillData> {

    private final ISkillData skills = new SkillData();


    @Nullable
    @Override
    public ISkillData getCapability(Entity entity, @Nullable Void context) {
        return skills;
    }
} 