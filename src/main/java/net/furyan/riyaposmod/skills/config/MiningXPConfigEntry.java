package net.furyan.riyaposmod.skills.config;

// We might use Gson or another JSON library for parsing. 
// For Gson, field names should match JSON keys or use @SerializedName.
// For now, keeping it simple.

public class MiningXPConfigEntry {
    // Making fields public for direct access after parsing, or add getters.
    // Could also make them private and use a deserializer that sets them (e.g. with Gson).
    public String block_id; // Can be a block ResourceLocation string or a tag string (e.g., "#forge:ores")
    public int base_xp;
    // Future fields:
    // public int required_level;
    // public Map<String, Integer> tool_level_bonus; // e.g. {"diamond": 2, "netherite": 3}
    // public double silk_touch_multiplier; 
    // public List<String> required_tools; // e.g. ["pickaxe"]

    public MiningXPConfigEntry() {
        // Default constructor often needed for JSON deserializers
    }

    // Optional: Constructor for manual creation or testing
    public MiningXPConfigEntry(String blockId, int baseXp) {
        this.block_id = blockId;
        this.base_xp = baseXp;
    }

    // Optional: Getters if fields are private
    public String getBlockId() {
        return block_id;
    }

    public int getBaseXp() {
        return base_xp;
    }

    @Override
    public String toString() {
        return "MiningXPConfigEntry{" +
                "block_id='" + block_id + '\'' +
                ", base_xp=" + base_xp +
                // Add future fields here for logging
                '}';
    }
} 