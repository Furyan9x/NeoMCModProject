package net.furyan.riyaposmod.skills.util;

import net.furyan.riyaposmod.RiyaposMod;

public class SkillConstants {

    public static final int MAX_SKILL_LEVEL = 100; // Max level is 99 in OSRS, 120 in RS3 for some. We'll use 100 for now.
    public static final int MIN_SKILL_LEVEL = 1;
    public static final String PLAYER_SKILLS_NBT_KEY = RiyaposMod.MOD_ID + "_player_skills_data";

    private static final int[] XP_FOR_LEVEL = new int[MAX_SKILL_LEVEL + 1];

    static {
        generateXpThresholds();
    }

    private static void generateXpThresholds() {
        double totalPoints = 0;
        XP_FOR_LEVEL[0] = 0; // Level 0 doesn't exist, but helps index
        XP_FOR_LEVEL[1] = 0; // Total XP to reach level 1 is 0

        for (int level = 2; level <= MAX_SKILL_LEVEL; level++) {
            // Points needed to advance from (level-1) to level
            double pointsForThisLevel = Math.floor((level - 1) + 300.0 * Math.pow(2.0, (level - 1) / 7.0));
            totalPoints += pointsForThisLevel;
            XP_FOR_LEVEL[level] = (int) Math.floor(totalPoints / 4.0); // Total XP to reach this level
        }
    }

    /**
     * Gets the total experience points required to reach the given skill level.
     * @param level The skill level (1 to MAX_SKILL_LEVEL).
     * @return The total XP needed to attain that level. Returns 0 for level 1.
     *         Returns experience for MAX_SKILL_LEVEL if requested level is higher.
     *         Returns 0 if level is less than 1.
     */
    public static int getXpForLevel(int level) {
        if (level < MIN_SKILL_LEVEL) {
            return 0;
        }
        if (level > MAX_SKILL_LEVEL) {
            return XP_FOR_LEVEL[MAX_SKILL_LEVEL];
        }
        return XP_FOR_LEVEL[level];
    }

    /**
     * Gets the skill level for the given total experience points.
     * @param totalExperience The total accumulated experience.
     * @return The current skill level (1 to MAX_SKILL_LEVEL).
     */
    public static int getLevelForXP(int totalExperience) {
        if (totalExperience <= 0) {
            return MIN_SKILL_LEVEL;
        }
        // Iterate downwards to find the highest level achieved
        for (int level = MAX_SKILL_LEVEL; level >= MIN_SKILL_LEVEL; level--) {
            if (totalExperience >= XP_FOR_LEVEL[level]) {
                return level;
            }
        }
        return MIN_SKILL_LEVEL; // Should not be reached if totalExperience > 0 due to XP_FOR_LEVEL[1] = 0
    }

    // For debugging or displaying XP table
    public static void printXpTable() {
        System.out.println("Level | Total XP Needed");
        System.out.println("------|----------------");
        for (int i = 1; i <= MAX_SKILL_LEVEL; i++) {
            System.out.printf("%-6d| %d%n", i, getXpForLevel(i));
        }
    }
} 