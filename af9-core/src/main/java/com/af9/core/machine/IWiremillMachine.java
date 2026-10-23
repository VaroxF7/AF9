package com.af9.core.machine;

import com.gregtechceu.gtceu.api.machine.feature.IMachineFeature;

/**
 * The Large Wiremill as its renderer sees it ({@link com.af9.core.client.render.WireDrawRender}): runs on the client,
 * from synced state.
 */
public interface IWiremillMachine extends IMachineFeature {

    /** Whether the structure is formed: the wire drawing machine is drawn then. */
    boolean wiremillFormed();

    /** Whether the structure is formed and a recipe is running: the flyer turns and the wire feeds then. */
    boolean wiremillRunning();
}
