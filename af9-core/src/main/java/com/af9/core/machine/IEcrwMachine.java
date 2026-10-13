package com.af9.core.machine;

import com.gregtechceu.gtceu.api.machine.feature.IMachineFeature;

/**
 * The Evaporation Condensation Retaining Well as its renderer sees it
 * ({@link com.af9.core.client.render.EcrwArmRender}): runs on the client, from synced state.
 */
public interface IEcrwMachine extends IMachineFeature {

    /** Whether the structure is formed and a recipe is running: the rake arm turns then. */
    boolean rakeSpinning();
}
