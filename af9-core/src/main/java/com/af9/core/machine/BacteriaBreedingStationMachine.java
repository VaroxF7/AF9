package com.af9.core.machine;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;

/**
 * The Bacteria Breeding Station (structure and recipe type in
 * {@code kubejs/startup_scripts/gtceu/bacteria_breeding.js}): breeds biochaff-fed bacterial cultures
 * for the Water Filtration Plant's LuV/ZPM biosorption modes. Plain GT behaviour; the filtration
 * plant's high modes only run on what this grows.
 */
public class BacteriaBreedingStationMachine extends WorkableElectricMultiblockMachine {

    public BacteriaBreedingStationMachine(IMachineBlockEntity holder) {
        super(holder);
    }
}
