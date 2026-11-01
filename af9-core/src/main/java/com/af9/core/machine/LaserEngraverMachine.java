package com.af9.core.machine;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;

/**
 * The Hyper-Intensity Laser Engraver (structure and recipes in KubeJS: {@code af9-core/src/main/java/com/af9/core/startup/machines/Hile.java}): GT's electric
 * multiblock that shows its laser, from the hatch on top down the glass shaft onto the plate, while it works.
 */
public class LaserEngraverMachine extends WorkableElectricMultiblockMachine implements ILaserEngraverMachine {

    public LaserEngraverMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public boolean beamOn() {
        return isFormed() && getRecipeLogic().isWorking();
    }
}
