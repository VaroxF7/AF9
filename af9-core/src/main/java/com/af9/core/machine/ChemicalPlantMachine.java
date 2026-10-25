package com.af9.core.machine;

import com.af9.core.pattern.ChemicalPlantPipes;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

/**
 * The Chemical Plant: a walk-through of real GT fluid pipes (a 6 x 6 x 4 structure, see {@link ChemicalPlantPipes}) that runs
 * the Chemical Reactor's and the Large Chemical Reactor's recipes. Until it forms, it connects the pipes of the structure
 * to the file's exact connections once a second, so an auto-built plant forms without wrench work. The plant itself is
 * a plain GT electric multiblock.
 */
public class ChemicalPlantMachine extends WorkableElectricMultiblockMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            ChemicalPlantMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    private TickableSubscription repairSubs;

    public ChemicalPlantMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) repairSubs = subscribeServerTick(repairSubs, this::repairTick);
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (repairSubs != null) {
            repairSubs.unsubscribe();
            repairSubs = null;
        }
    }

    private void repairTick() {
        if (isFormed() || getOffsetTimer() % 20 != 0 || getLevel() == null) return;
        if (ChemicalPlantPipes.repair(getLevel(), getPos(), getFrontFacing())) checkPattern();
    }
}
