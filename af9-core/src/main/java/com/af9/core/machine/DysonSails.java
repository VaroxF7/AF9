package com.af9.core.machine;

/**
 * The sails of the Dyson Swarm, lowest tier first: ids ({@code af9:<id>}) and the share of the swarm's base yield each
 * one gives, percent. The yield of the swarm is the sum of its sails' yields (DysonSwarmMachine).
 */
public final class DysonSails {

    public static final String[] IDS = { "allthemodium_sail", "unobtainium_alloy_sail",
            "chromodynium_star_matter_tritan_alloy_sail" };
    /** Allthemodium gives the base yield; the star matter alloy 3.5 times as much. */
    public static final int[] PERCENT = { 100, 200, 350 };

    private DysonSails() {}
}
