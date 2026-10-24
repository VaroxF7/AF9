package com.af9.core.machine;

import com.gregtechceu.gtceu.api.machine.feature.IMachineFeature;

/**
 * A machine with a force field in its middle ({@link com.af9.core.client.render.ForceFieldRender}): runs on the client,
 * from synced state.
 */
public interface IForceFieldMachine extends IMachineFeature {

    /** Whether the structure is formed: the field is drawn then. */
    boolean isFieldFormed();

    /** Whether a recipe is running: the field burns hard then, and only glows while the hall just stands. */
    boolean isFieldBurning();
}
