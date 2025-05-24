package net.furyan.riyaposmod.skills.core;

import net.furyan.riyaposmod.skills.api.ISkillData;
import net.furyan.riyaposmod.skills.util.SkillConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of ISkillData.
 */
public class SkillData implements ISkillData {

    private static final String SKILLS_TAG = "PlayerSkills";
    private static final String TOTAL_EXP_TAG = "TotalExperience";

    private final Map<String, SkillEntry> skills = new HashMap<>();

    // Static nested class for storing individual skill data
    private static class SkillEntry {
        private long totalExperience;

        public SkillEntry(long totalExperience) {
            this.setTotalExperience(totalExperience);
        }

        public int getLevel() {
            return SkillConstants.getLevelForXP((int)this.totalExperience);
        }

        public long getTotalExperience() {
            return totalExperience;
        }

        public void setTotalExperience(long experience) {
            this.totalExperience = Math.max(0, experience);
            long maxExpForMaxLevel = SkillConstants.getXpForLevel(SkillConstants.MAX_SKILL_LEVEL);
            if (this.totalExperience > maxExpForMaxLevel && getLevel() >= SkillConstants.MAX_SKILL_LEVEL) {
                 this.totalExperience = maxExpForMaxLevel;
            }
        }

        public void addExperience(int amount) {
            if (amount <= 0) return;
            long currentTotalExp = this.totalExperience;
            int currentLevel = getLevel();
            if (currentLevel >= SkillConstants.MAX_SKILL_LEVEL && currentTotalExp >= SkillConstants.getXpForLevel(SkillConstants.MAX_SKILL_LEVEL)) {
                return; 
            }
            this.totalExperience += amount;
            int newLevel = getLevel(); 
            if (newLevel > currentLevel) {
                System.out.println("[SKILLS] Player leveled up a skill from " + currentLevel + " to " + newLevel + ".");
            }
            long maxExpForMaxLevel = SkillConstants.getXpForLevel(SkillConstants.MAX_SKILL_LEVEL);
            if (this.totalExperience > maxExpForMaxLevel && newLevel >= SkillConstants.MAX_SKILL_LEVEL) {
                this.totalExperience = maxExpForMaxLevel;
            }
        }

        public CompoundTag serializeNBT(HolderLookup.Provider provider) {
            CompoundTag nbt = new CompoundTag();
            nbt.putLong(TOTAL_EXP_TAG, this.totalExperience);
            return nbt;
        }
    }

    public SkillData() {
        for (Skills skillEnum : Skills.values()) {
            this.skills.put(skillEnum.getSkillName(), new SkillEntry(SkillConstants.getXpForLevel(SkillConstants.MIN_SKILL_LEVEL)));
        }
    }

    private SkillEntry getOrCreateSkillEntry(String skillName) {
        return skills.computeIfAbsent(skillName, k -> {
            return new SkillEntry(SkillConstants.getXpForLevel(SkillConstants.MIN_SKILL_LEVEL));
        });
    }

    @Override
    public int getSkillLevel(String skillName) {
        return getOrCreateSkillEntry(skillName).getLevel();
    }

    @Override
    public int getSkillExp(String skillName) {
        SkillEntry entry = getOrCreateSkillEntry(skillName);
        long totalExp = entry.getTotalExperience();
        int currentLevel = entry.getLevel();
        if (currentLevel >= SkillConstants.MAX_SKILL_LEVEL) {
            return 0; // No XP towards "next" level if at max
        }
        long xpForCurrentLevel = SkillConstants.getXpForLevel(currentLevel);
        return (int) (totalExp - xpForCurrentLevel);
    }

    @Override
    public void setSkillExp(String skillName, int amount) {
        SkillEntry entry = getOrCreateSkillEntry(skillName);
        int currentLevel = entry.getLevel();
        if (currentLevel >= SkillConstants.MAX_SKILL_LEVEL) return; // Cannot set XP for next level if at max
        
        long xpForCurrentLevel = SkillConstants.getXpForLevel(currentLevel);
        long newTotalExp = xpForCurrentLevel + Math.max(0, amount);
        
        long xpForNextLevel = SkillConstants.getXpForLevel(currentLevel + 1);
        if (newTotalExp >= xpForNextLevel) {
            entry.setTotalExperience(Math.min(newTotalExp, xpForNextLevel -1)); 
        } else {
            entry.setTotalExperience(newTotalExp);
        }
    }

    @Override
    public long getSkillTotalExp(String skillName) {
        return getOrCreateSkillEntry(skillName).getTotalExperience();
    }

    @Override
    public void setSkillTotalExp(String skillName, long totalExperience) {
        getOrCreateSkillEntry(skillName).setTotalExperience(totalExperience);
    }

    @Override
    public void addSkillExp(String skillName, int amount) {
        getOrCreateSkillEntry(skillName).addExperience(amount);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag nbt = new CompoundTag();
        CompoundTag skillsTag = new CompoundTag();
        for (Skills skillEnum : Skills.values()) {
            SkillEntry entry = getOrCreateSkillEntry(skillEnum.getSkillName());
            skillsTag.put(skillEnum.getSkillName(), entry.serializeNBT(provider));
        }
        nbt.put(SKILLS_TAG, skillsTag);
        return nbt;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        for (Skills skillEnum : Skills.values()) {
            skills.computeIfAbsent(skillEnum.getSkillName(), k -> new SkillEntry(SkillConstants.getXpForLevel(SkillConstants.MIN_SKILL_LEVEL)));
        }
        if (nbt.contains(SKILLS_TAG, CompoundTag.TAG_COMPOUND)) {
            CompoundTag skillsTag = nbt.getCompound(SKILLS_TAG);
            for (String skillKey : skillsTag.getAllKeys()) {
                SkillEntry existingEntry = skills.get(skillKey);
                if (existingEntry != null) {
                    CompoundTag skillNbt = skillsTag.getCompound(skillKey);
                    if (skillNbt.contains(TOTAL_EXP_TAG)) {
                        if (skillNbt.getTagType(TOTAL_EXP_TAG) == CompoundTag.TAG_LONG) {
                            existingEntry.setTotalExperience(skillNbt.getLong(TOTAL_EXP_TAG));
                        } else if (skillNbt.getTagType(TOTAL_EXP_TAG) == CompoundTag.TAG_INT) {
                            existingEntry.setTotalExperience(skillNbt.getInt(TOTAL_EXP_TAG));
                        }
                    }
                }
            }
        }
    }

    @Override
    public void setSkillLevel(String skillName, int level) {
        SkillEntry entry = getOrCreateSkillEntry(skillName);
        if (level >= SkillConstants.MIN_SKILL_LEVEL && level <= SkillConstants.MAX_SKILL_LEVEL) {
            entry.setTotalExperience(SkillConstants.getXpForLevel(level));
        } else {
            // Optionally, throw an exception or log a warning for invalid level
            System.err.println("Attempted to set invalid skill level " + level + " for skill " + skillName);
        }
    }
} 