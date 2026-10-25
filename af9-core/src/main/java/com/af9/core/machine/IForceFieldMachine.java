package com.af9.core.machine;

import com.gregtechceu.gtceu.api.machine.feature.IMachineFeature;

/**
 * A machine with a force field in its middle ({@link com.af9.core.client.render.ForceFieldRender}): runs on the client,
 * from synced state.
 */
public interface IForceFieldMachine extends IMachineFeature {

    /** Whether the field is up (the structure formed and the field charged): it is drawn then. */
    boolean isFieldFormed();

    /** How strong the field is, 0 to 1. */
    float getFieldLevel();

    /** Whether the injectors are pumping: the field takes the osmium plasma and the stabilisers' power. */
    boolean isFieldInjecting();

    /** Whether a recipe is running: the field burns hard then, and only glows while the hall just stands. */
    boolean isFieldBurning();
}
