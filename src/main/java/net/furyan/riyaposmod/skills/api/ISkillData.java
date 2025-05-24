package net.furyan.riyaposmod.skills.api;

// import net.furyan.riyaposmod.skills.core.SkillEntry; // Commented out, SkillEntry is an inner class of SkillData
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.neoforged.neoforge.common.util.INBTSerializable;

// import java.util.Map; // Commented out

/**
 * Interface for player skill data.
 * Implementations of this interface will store and manage player skill levels and experience.
 */
public interface ISkillData extends INBTSerializable<CompoundTag> {

    int getSkillLevel(String skillName);
    
    void setSkillLevel(String skillName, int level);

    /**
     * Gets the current experience points towards the next level for the specified skill.
     * @param skillName The name of the skill.
     * @return Experience points towards the next level.
     */
    int getSkillExp(String skillName);

    /**
     * Gets the total accumulated experience points for the specified skill.
     * @param skillName The name of the skill.
     * @return Total accumulated experience points.
     */
    long getSkillTotalExp(String skillName);

    // Map<String, SkillEntry> getSkills(); // Commented out - SkillEntry is an inner class of SkillData

    /**
     * Sets the experience towards the next level for a skill.
     * This primarily recalculates total XP based on current level and this new XP within the level.
     * Prefer addSkillExp for normal XP gain, or setSkillTotalExp for direct total XP manipulation.
     */
    void setSkillExp(String skillName, int amount);

    /**
     * Sets the total accumulated experience for a skill and recalculates level and current XP towards next level.
     * @param skillName The name of the skill.
     * @param totalExperience The total experience points.
     */
    void setSkillTotalExp(String skillName, long totalExperience);

    /**
     * Adds experience to the specified skill, handling level ups.
     * @param skillName The name of the skill.
     * @param amount The amount of experience to add.
     */
    void addSkillExp(String skillName, int amount);

    /**
     * Serializes the skill data to NBT.
     * @param provider The HolderLookup.Provider for serialization.
     * @return A CompoundTag containing the skill data.
     */
    CompoundTag serializeNBT(HolderLookup.Provider provider);

    /**
     * Deserializes skill data from NBT.
     * @param provider The HolderLookup.Provider for deserialization.
     * @param nbt The CompoundTag to read data from.
     */
    void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt);

} 