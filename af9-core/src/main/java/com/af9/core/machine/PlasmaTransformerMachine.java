package com.af9.core.machine;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;

/**
 * The Plasma Transformer (structure and recipe type in
 * {@code kubejs/startup_scripts/gtceu/plasma_transformer.js}): condenses superstate plasmas to
 * solid plates at XPS (Star Matter Plasma Steel for the NVM boards). Plain GT behaviour; the
 * DTPF forges the superstate, this condenses it.
 */
public class PlasmaTransformerMachine extends WorkableElectricMultiblockMachine {

    public PlasmaTransformerMachine(IMachineBlockEntity holder) {
        super(holder);
    }
}
