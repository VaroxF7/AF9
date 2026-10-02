package com.af9.core.elevator;

import com.gregtechceu.gtceu.api.machine.feature.IMachineFeature;

/**
 * A Space Elevator as its renderer sees it ({@link com.af9.core.client.render.SpaceElevatorRender}): runs on the client, from
 * synced state.
 */
public interface ISpaceElevatorMachine extends IMachineFeature {

    /** Whether the structure is formed (then the cable and the climber on it are drawn). */
    boolean isElevatorFormed();

    /** How far above its rest the climber is now (blocks): 0 between its rides ({@link ClimberRide}). */
    float climberHeight(float partialTick);

    /** How far the climber has turned round the cable (degrees). */
    float climberTurn(float partialTick);
}
