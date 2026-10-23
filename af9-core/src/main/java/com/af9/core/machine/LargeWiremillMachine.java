package com.af9.core.machine;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

/**
 * The Large Wiremill (gtceu:large_wiremill, GCYM's Large Wire Factory rebuilt): a plain GT electric multiblock running
 * the Wiremill's recipes (and its Spooling mode) with a wire drawing machine in the glass chamber that turns while a
 * recipe runs ({@link com.af9.core.client.render.WireDrawRender}). Only the formed / working state is read by the render.
 */
public class LargeWiremillMachine extends WorkableElectricMultiblockMachine implements IWiremillMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            LargeWiremillMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    public LargeWiremillMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public boolean wiremillFormed() {
        return isFormed();
    }

    @Override
    public boolean wiremillRunning() {
        return isFormed() && getRecipeLogic().isWorking();
    }
}
