package net.furyan.riyaposmod.skills.core;

public enum Skills {
    // Combat Skills
    MELEE("Melee"),
    DEFENCE("Defence"),
    RANGED("Ranged"),
    MAGIC("Magic"),
    // Gathering Skills
    MINING("Mining"),
    WOODCUTTING("Woodcutting"),
    FISHING("Fishing"),
    FARMING("Farming"),
    HERBLORE("Herblore"), // Could also be crafting
    // Artisan/Crafting Skills
    SMITHING("Smithing"),
    CRAFTING("Crafting"), // General crafting
    COOKING("Cooking"),
    ENCHANTING("Enchanting"),
    // Utility/Misc Skills
    AGILITY("Agility"),
    SLAYER("Slayer"),
    THIEVING("Thieving"),
    CONSTRUCTION("Construction"),
    TRANSPORT("Transport"); // For vehicle/mount related activities

    private final String skillName;

    Skills(String skillName) {
        this.skillName = skillName;
    }

    public String getSkillName() {
        return skillName;
    }

    @Override
    public String toString() {
        return skillName;
    }
} 