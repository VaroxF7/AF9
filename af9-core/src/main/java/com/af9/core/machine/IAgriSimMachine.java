package com.af9.core.machine;

import com.gregtechceu.gtceu.api.machine.feature.IMachineFeature;

/**
 * The Agriculture Simulation Bed as its renderer sees it ({@link com.af9.core.client.render.AgriSimRender}): runs on
 * the client, from synced state.
 */
public interface IAgriSimMachine extends IMachineFeature {

    /** Whether the bed exists (the structure is formed). */
    boolean isAgriFormed();

    /** Registry id of the seed the running recipe grows, or an empty string while the bed stands idle. */
    String getAgriSeed();

    /** How far the crop in the running recipe has grown, 0-1000 (the recipe's progress; per run when batching). */
    int getAgriGrowth();
}
