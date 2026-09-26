package com.af9.core.machine;

import com.gregtechceu.gtceu.api.machine.feature.IMachineFeature;

/**
 * A machine with a glowing light ring while it works, like GT's Fusion Reactor
 * ({@link com.af9.core.client.render.LightRingRender}). Both methods run on the client, from synced state.
 */
public interface ILightRingMachine extends IMachineFeature {

    /** Whether the ring is lit now (it fades out over a few seconds after this turns false). */
    boolean isRingLit();

    /** Ring colour (RGB; the ring pulses between it and white). */
    int getRingColor();
}
