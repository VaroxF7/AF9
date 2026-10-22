package com.af9.core.machine;

import com.gregtechceu.gtceu.api.machine.feature.IMachineFeature;

/**
 * The Bio Vat as its renderer sees it ({@link com.af9.core.client.render.BioVatRender}): runs on the client, from
 * synced state.
 */
public interface IBioVatMachine extends IMachineFeature {

    /** Whether the vat exists (the structure is formed): the culture is drawn then. */
    boolean isVatFormed();

    /** Whether a run goes on now: the culture bubbles then, is still otherwise. */
    boolean isVatActive();

    /** How full the vat is, 0-1000 (of the fluid hatches' capacity); the culture stands 0-2 blocks high with it. */
    int getVatFill();

    /** Registry id of the fluid the vat shows (its colour is the fluid's), or an empty string when it holds none. */
    String getVatFluid();
}
