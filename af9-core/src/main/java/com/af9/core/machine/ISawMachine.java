package com.af9.core.machine;

import com.gregtechceu.gtceu.api.machine.feature.IMachineFeature;

/**
 * The Industrial Cutting Machine as its renderer sees it ({@link com.af9.core.client.render.SawBladeRender}): runs on
 * the client, from synced state.
 */
public interface ISawMachine extends IMachineFeature {

    /** Whether the structure is formed: the saw blade is drawn then. */
    boolean sawFormed();

    /** Whether the structure is formed and a recipe is running: the blade spins then. */
    boolean sawSpinning();
}
