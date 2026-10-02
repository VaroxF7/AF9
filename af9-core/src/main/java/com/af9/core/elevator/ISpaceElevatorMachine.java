package com.af9.core.elevator;

import com.gregtechceu.gtceu.api.machine.feature.IMachineFeature;

/**
 * A Space Elevator as its renderer sees it ({@link com.af9.core.client.render.SpaceElevatorRender}): runs on the client, from
 * synced state.
 */
public interface ISpaceElevatorMachine extends IMachineFeature {

    /** Whether the structure is formed (then the platform turns on its cable). */
    boolean isElevatorFormed();

    /** Whether a mining run is on (the platform's lights are brighter and it turns a little faster). */
    boolean isElevatorWorking();
}
