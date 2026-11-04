package com.af9.lod;

import net.minecraftforge.common.ForgeConfigSpec;

/** Client config (config/af9_lod-client.toml). Edited in game through LodConfigScreen. */
public final class LodConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.BooleanValue DEBUG_DRAW;
    public static final ForgeConfigSpec.IntValue MAX_DISTANCE_CHUNKS;
    public static final ForgeConfigSpec.IntValue NEAR_BAND_CHUNKS;
    public static final ForgeConfigSpec.BooleanValue GENERATE_FAR;
    public static final ForgeConfigSpec.IntValue FAR_BUDGET_MS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        ENABLED = b.comment("Master switch for the GPU LOD renderer").define("enabled", false);
        DEBUG_DRAW = b.comment("Also draw loaded sections through the LOD path (debug, overdraws vanilla terrain)")
                .define("debugDraw", false);
        MAX_DISTANCE_CHUNKS = b.comment("Farthest LOD distance in chunks").defineInRange("maxDistanceChunks", 64, 16, 1024);
        NEAR_BAND_CHUNKS = b.comment("LOD cells closer than this many chunks are left to Embeddium")
                .defineInRange("nearBandChunks", 0, 0, 256);
        GENERATE_FAR = b.comment("Singleplayer: load or generate chunks beyond the view distance (low priority) to feed the LOD. "
                + "This writes newly generated chunks into the world save.").define("generateFarChunks", true);
        FAR_BUDGET_MS = b.comment("Max milliseconds per server tick spent loading far chunks").defineInRange("farBudgetMs", 6, 1, 25);
        SPEC = b.build();
    }

    private LodConfig() {}

    public static boolean drawEnabled() {
        return ENABLED.get();
    }

    /** Also draw the already-loaded level-0 sections (overdraws vanilla; debugging only). */
    public static boolean debugDraw() {
        return DEBUG_DRAW.get() || Boolean.getBoolean("af9lod.debugdraw");
    }
}
