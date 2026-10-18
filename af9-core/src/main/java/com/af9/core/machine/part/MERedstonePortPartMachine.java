package com.af9.core.machine.part;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IRedstoneSignalMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;

import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.Direction;

import org.jetbrains.annotations.Nullable;

/**
 * ME Redstone Port: a part of the ME Resonance Fabricator that outputs the grade of its Budding Certus as a redstone
 * signal (0 nothing, 1 Certus Block, 2 Chipped ... 5 Exquisite). Copied from TFG's {@code MEAssemblerRedstonePort}
 * (TerraFirmaGreg-Team/Core-Modern, LGPL-3.0; see CREDITS-textures.md).
 */
public class MERedstonePortPartMachine extends MultiblockPartMachine implements IRedstoneSignalMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            MERedstonePortPartMachine.class, MultiblockPartMachine.MANAGED_FIELD_HOLDER);

    /** Registered by the port's definition (KubeJS), taken by the Fabricator's pattern. */
    public static final PartAbility ME_REDSTONE_PORT = new PartAbility("me_redstone_port");

    @Persisted
    private int signal = 0;

    public MERedstonePortPartMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    public void trySetSignal(int newSignal) {
        if (newSignal == signal) return;
        signal = newSignal;
        updateSignal();
    }

    @Override
    public int getOutputSignal(@Nullable Direction side) {
        return signal;
    }

    @Override
    public int getOutputDirectSignal(Direction direction) {
        return signal;
    }

    @Override
    public int getAnalogOutputSignal() {
        return signal;
    }

    @Override
    public boolean canConnectRedstone(Direction side) {
        return true;
    }
}
