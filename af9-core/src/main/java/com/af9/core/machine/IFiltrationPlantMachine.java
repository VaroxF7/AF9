package com.af9.core.machine;

import com.gregtechceu.gtceu.api.machine.feature.IMachineFeature;

/**
 * The Water Filtration Plant as its renderer sees it
 * ({@link com.af9.core.client.render.FiltrationBasinRender}): runs on the client, from synced state.
 */
public interface IFiltrationPlantMachine extends IMachineFeature {

    /** Whether the basin exists (the structure is formed): the water and the debris are drawn then. */
    boolean isBasinFormed();

    /** Whether a filtration run is going now: the skimmer arm sweeps then, parked otherwise. */
    boolean isBasinActive();

    /**
     * Filtration progress 0-1 for the skimmer sweep, {@code partialTick} for smoothness.
     * -1 when nothing runs (the arm is parked, the water is still).
     */
    float getBasinProgress(float partialTick);
}
