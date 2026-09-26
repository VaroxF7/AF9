package com.af9.core;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * AF9 Core settings, {@code config/af9-common.toml}.
 */
public final class AF9Config {

    public static final ForgeConfigSpec SPEC;
    /** Whether wafers and chips of creative-mode players contaminate too (spectators never). */
    public static final ForgeConfigSpec.BooleanValue CONTAMINATE_IN_CREATIVE;
    /** Gravity (1 = Earth) inside the magnetic field of a running Orbital Lithography Station. */
    public static final ForgeConfigSpec.DoubleValue FIELD_GRAVITY;
    /** How far the field reaches beyond the station, in blocks. */
    public static final ForgeConfigSpec.IntValue FIELD_RANGE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Wafers and chips in a player's inventory turn into contaminated ones (unless the player wears",
                "gloves or stands in a clean Cleanroom).").push("contamination");
        CONTAMINATE_IN_CREATIVE = builder
                .comment("Also contaminate the wafers and chips of players in creative mode (spectators never).")
                .define("includeCreative", true);
        builder.pop();
        builder.comment("The magnetic field of the Orbital Lithography Station: while it is switched on and powered,",
                "everything around it falls with this gravity instead of floating in orbit (needs Ad Astra).")
                .push("orbitalField");
        FIELD_GRAVITY = builder
                .comment("Gravity inside the field, 1.0 = Earth (jumps about 1.25 blocks high).",
                        "It only raises gravity, never lowers it.")
                .defineInRange("gravity", 1.0, 0.0, 4.0);
        FIELD_RANGE = builder
                .comment("How far the field reaches beyond the station's blocks (4 more above the deck).")
                .defineInRange("range", 8, 0, 64);
        builder.pop();
        SPEC = builder.build();
    }

    private AF9Config() {}
}
