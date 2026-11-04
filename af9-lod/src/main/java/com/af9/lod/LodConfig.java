package com.af9.lod;

import net.minecraftforge.common.ForgeConfigSpec;

/** Client config (config/af9_lod-client.toml). Edited in game through LodConfigScreen. */
public final class LodConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.BooleanValue DEBUG_DRAW;
    public static final ForgeConfigSpec.IntValue MAX_DISTANCE_CHUNKS;
    public static final ForgeConfigSpec.IntValue NEAR_BAND_CHUNKS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        ENABLED = b.comment("Master switch for the GPU LOD renderer").define("enabled", false);
        DEBUG_DRAW = b.comment("Also draw loaded sections through the LOD path (debug, overdraws vanilla terrain)")
                .define("debugDraw", false);
        MAX_DISTANCE_CHUNKS = b.comment("Farthest LOD distance in chunks").defineInRange("maxDistanceChunks", 128, 16, 1024);
        NEAR_BAND_CHUNKS = b.comment("LOD cells closer than this many chunks are left to Embeddium")
                .defineInRange("nearBandChunks", 0, 0, 256);
        SPEC = b.build();
    }

    private LodConfig() {}

    public static boolean drawEnabled() {
        return ENABLED.get() && (DEBUG_DRAW.get() || Boolean.getBoolean("af9lod.debugdraw"));
    }
}
