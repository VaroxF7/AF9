package com.af9.core.machine;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

/**
 * The Industrial Cutting Machine (GTNH's cutting multiblock, GT5-Unofficial, LGPL-3.0): the Cutting Machine's and the
 * Lathe's recipes in a walk-in housing, with a big saw blade behind a glass window that spins while a recipe runs
 * ({@link com.af9.core.client.render.SawBladeRender}). A plain GT electric multiblock with GT's mode tab switching
 * cutter / lathe; the blade is only drawn from its formed / working state. GTNH picks the blade by the sawblade item
 * in the controller; here the blade follows the energy hatch's tier instead (the same four steps GTNH's sawblades limit
 * hatches to): up to LuV, UV, UEV, above.
 */
public class IndustrialCutterMachine extends WorkableElectricMultiblockMachine implements ISawMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            IndustrialCutterMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    @DescSynced
    private int bladeTier;

    public IndustrialCutterMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        int tier = energyContainer == null ? 0 : GTUtil.getTierByVoltage(energyContainer.getInputVoltage());
        bladeTier = tier <= GTValues.LuV ? 0 : tier <= GTValues.UV ? 1 : tier <= GTValues.UEV ? 2 : 3;
    }

    @Override
    public boolean sawFormed() {
        return isFormed();
    }

    @Override
    public boolean sawSpinning() {
        return isFormed() && getRecipeLogic().isWorking();
    }

    @Override
    public int sawTier() {
        return bladeTier;
    }
}
