package com.af9.core.machine;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import org.jetbrains.annotations.NotNull;

/**
 * The High Temperature Precision Fabricator (TerraFirmaGreg's boule machine): GT's electric multiblock, a steel
 * firebox and gearbox hall with the Precision Fabricator Input and Output Ports as its only hatches. It batches its
 * crafts from the start (batch mode is the only way it runs: no parallel hatch) and overclocks perfectly; both
 * modifiers are set on the definition (startup_scripts/gtceu/precision_fabricator.js).
 */
public class PrecisionFabricatorMachine extends WorkableElectricMultiblockMachine {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            PrecisionFabricatorMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    public PrecisionFabricatorMachine(IMachineBlockEntity holder) {
        super(holder);
        this.batchEnabled = true;
    }

    @Override
    public @NotNull ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }
}
