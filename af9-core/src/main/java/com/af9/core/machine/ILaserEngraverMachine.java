package com.af9.core.machine;

import com.gregtechceu.gtceu.api.machine.feature.IMachineFeature;

/**
 * The Hyper-Intensity Laser Engraver as its renderer sees it ({@link com.af9.core.client.render.LaserEngraverRender}): runs on
 * the client, from synced state.
 */
public interface ILaserEngraverMachine extends IMachineFeature {

    /** Whether the structure is formed and a recipe is running: the beam is drawn then. */
    boolean beamOn();
}
