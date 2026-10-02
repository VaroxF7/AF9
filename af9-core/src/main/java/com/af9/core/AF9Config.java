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
    /** Whether the Photolithography Line and Scanner need Air Conditioning Hatches for their heat load. */
    public static final ForgeConfigSpec.BooleanValue LITHO_AIR_COOLING;
    /** Whether players get a warning above the hotbar near radioactive material. */
    public static final ForgeConfigSpec.BooleanValue RADIATION_HINTS;

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
        builder.comment("The Photolithography Line and Scanner are built as clean rooms and cool with air: a print puts",
                "heat into the chamber and the Air Conditioning Hatches in the structure have to remove it (the",
                "Orbital Lithography Station cools with supercooled fluids instead).")
                .push("lithography");
        LITHO_AIR_COOLING = builder
                .comment("The Line and the Scanner need Air Conditioning Hatches for the heat load of the print.")
                .define("airCooling", true);
        builder.pop();
        builder.comment("Radiation: GregTech's hazard system poisons players that carry radioactive material without a",
                "hazmat suit. AF9 adds the warning: near radioactive material (in the inventory, dropped, as ore",
                "blocks close by, a running FX-1 Reactor) a message above the hotbar says how strong it is.")
                .push("radiation");
        RADIATION_HINTS = builder
                .comment("Warn above the hotbar when radioactive material is near.")
                .define("hints", true);
        builder.pop();
        SPEC = builder.build();
    }

    private AF9Config() {}
}
