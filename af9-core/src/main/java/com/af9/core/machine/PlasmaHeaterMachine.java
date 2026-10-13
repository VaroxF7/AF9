package com.af9.core.machine;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;

/**
 * The Plasma Heater (structure and recipe type in
 * {@code kubejs/startup_scripts/gtceu/dtpf.js}): a small multiblock that ionises osmium dust with
 * Endion to osmium plasma, the only source of the Plasma Forge's heater fuel. Plain GT behaviour;
 * the forge burns what this makes.
 */
public class PlasmaHeaterMachine extends WorkableElectricMultiblockMachine {

    public PlasmaHeaterMachine(IMachineBlockEntity holder) {
        super(holder);
    }
}
