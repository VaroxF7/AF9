package com.af9.core.machine;

import com.gregtechceu.gtceu.api.GTValues;

/**
 * The sails of the Dyson Swarm, lowest tier first: ids ({@code af9:<id>}) and the yield each one gives, percent of
 * the base sail's. The yield of the swarm is the sum of its sails' yields (DysonSwarmMachine), and the top tier is
 * exactly one amp of UHV, so a full swarm of them gives 10,000 A.
 */
public final class DysonSails {

    public static final String[] IDS = { "allthemodium_sail", "unobtainium_alloy_sail",
            "chromodynium_star_matter_tritan_alloy_sail" };
    /** Allthemodium gives the base yield; the alloy twice, the star matter alloy 3.5 times as much. */
    public static final int[] PERCENT = { 100, 200, 350 };
    /** Display names, by tier. */
    public static final String[] NAMES = { "Allthemodium", "Unobtainium Alloy", "Star Matter Tritan Alloy" };
    /** The percent that is one amp of UHV: the best sail's. */
    public static final int AMP_PERCENT = PERCENT[PERCENT.length - 1];

    private DysonSails() {}

    /** EU/t of sails whose yields add up to the given percent: 350 % is one amp of UHV. */
    public static long euPerTick(long weightedPercent) {
        return weightedPercent * GTValues.V[GTValues.UHV] / AMP_PERCENT;
    }

    /** EU/t of one sail of the tier. */
    public static long euPerSail(int tier) {
        return euPerTick(PERCENT[tier]);
    }
}
