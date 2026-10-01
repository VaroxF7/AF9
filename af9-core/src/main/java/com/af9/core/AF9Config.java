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
    /** Whether AE2 networks with an ME Controller need computation for their channels. */
    public static final ForgeConfigSpec.BooleanValue ME_NEEDS_COMPUTATION;
    /** Channels one CWU/t pays for. */
    public static final ForgeConfigSpec.IntValue ME_CHANNELS_PER_CWUT;
    /** Whether the Photolithography Line and Scanner need Air Conditioning Hatches for their heat load. */
    public static final ForgeConfigSpec.BooleanValue LITHO_AIR_COOLING;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Wafers and chips in a player's inventory turn into contaminated ones (unless the player wears",
                "gloves or stands in a clean Cleanroom).").push("contamination");
        CONTAMINATE_IN_CREATIVE = builder
                .comment("Also contaminate the wafers and chips of players in creative mode (spectators never).")
                .define("includeCreative", true);
        builder.pop();
        builder.comment("The magnetic field of the Orbital Lithography Station: while it is switched on and powered,",
                "everything on the station (and up to 4 blocks above its deck) falls with this gravity instead of",
                "floating in orbit (needs Ad Astra).")
                .push("orbitalField");
        FIELD_GRAVITY = builder
                .comment("Gravity inside the field, 1.0 = Earth (jumps about 1.25 blocks high).",
                        "It only raises gravity, never lowers it.")
                .defineInRange("gravity", 1.0, 0.0, 4.0);
        builder.pop();
        builder.comment("AE2: a network with an ME Controller needs computation (CWU/t) for its channels, brought in",
                "by ME Computation Links (Optical Bus Cable, a GT Computation Transmitter Hatch or GT Optical Fiber",
                "Cable on their back). Short of it, the network gets only the channels its computation pays for (the",
                "devices farthest from the controller lose theirs first). Networks without a controller need none.")
                .push("meComputation");
        ME_NEEDS_COMPUTATION = builder
                .comment("Networks with an ME Controller need computation.")
                .define("enabled", true);
        ME_CHANNELS_PER_CWUT = builder
                .comment("Channels one CWU/t pays for (a network of 128 channels needs 32 CWU/t at 4).")
                .defineInRange("channelsPerCwut", 4, 1, 1024);
        builder.pop();
        builder.comment("The Photolithography Line and Scanner are built as clean rooms and cool with air: a print puts",
                "heat into the chamber and the Air Conditioning Hatches in the structure have to remove it (the",
                "Orbital Lithography Station cools with supercooled fluids instead).")
                .push("lithography");
        LITHO_AIR_COOLING = builder
                .comment("The Line and the Scanner need Air Conditioning Hatches for the heat load of the print.")
                .define("airCooling", true);
        builder.pop();
        SPEC = builder.build();
    }

    private AF9Config() {}
}
