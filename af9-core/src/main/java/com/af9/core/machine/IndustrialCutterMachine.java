package com.af9.core.machine;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

/**
 * The Industrial Cutting Machine: the Cutting Machine's recipes in a walk-in housing, with a big saw blade behind a
 * glass window that spins while a recipe runs ({@link com.af9.core.client.render.SawBladeRender}). The machine itself
 * is a plain GT electric multiblock; the blade is only drawn from its formed / working state.
 */
public class IndustrialCutterMachine extends WorkableElectricMultiblockMachine implements ISawMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            IndustrialCutterMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    public IndustrialCutterMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public boolean sawFormed() {
        return isFormed();
    }

    @Override
    public boolean sawSpinning() {
        return isFormed() && getRecipeLogic().isWorking();
    }
}
