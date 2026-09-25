package com.af9.core;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * AF9 Core settings, {@code config/af9-common.toml}.
 */
public final class AF9Config {

    public static final ForgeConfigSpec SPEC;
    /** Whether wafers and chips of creative-mode players contaminate too (spectators never). */
    public static final ForgeConfigSpec.BooleanValue CONTAMINATE_IN_CREATIVE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Wafers and chips in a player's inventory turn into contaminated ones (unless the player wears",
                "gloves or stands in a clean Cleanroom).").push("contamination");
        CONTAMINATE_IN_CREATIVE = builder
                .comment("Also contaminate the wafers and chips of players in creative mode (spectators never).")
                .define("includeCreative", true);
        builder.pop();
        SPEC = builder.build();
    }

    private AF9Config() {}
}
