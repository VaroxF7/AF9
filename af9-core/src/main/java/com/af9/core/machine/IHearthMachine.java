package com.af9.core.machine;

import com.gregtechceu.gtceu.api.machine.feature.IMachineFeature;

/**
 * The Sanguinite Hearth Furnace as its renderer sees it ({@link com.af9.core.client.render.HearthTurntableRender}):
 * runs on the client, from synced state.
 */
public interface IHearthMachine extends IMachineFeature {

    /** Whether the structure is formed: the turntables are drawn then. */
    boolean hearthFormed();

    /** Whether the structure is formed and a recipe is running: the turntables turn then. */
    boolean hearthTurning();
}
