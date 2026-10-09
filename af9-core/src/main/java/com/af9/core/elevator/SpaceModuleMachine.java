package com.af9.core.elevator;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;

import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * A Space Elevator Mining Module as its own small multiblock, as GTNH's modules are (gtnhintergalactic's
 * TileEntityModuleBase): a controller that stands in one of the tower's module slots, with its own structure (the casings
 * above and below it, where the slot's hatches go) and its own screen. It does not work on its own: the elevator it stands
 * in finds it when the tower forms and <b>connects</b> it ({@link #connect}), telling it whether the motors power it. A
 * module the motors do not power (a tier above the motors', or more modules than the motors have slots for) stays dark, one that
 * is flies {@link SpaceElevatorMachine#MODULE_EXPEDITIONS its tier's} expeditions at once in the elevator's runs.
 * <p>
 * Made in KubeJS ({@code startup_scripts/gtceu/space_elevator.js}, {@code gtceu:space_mining_module_mk1} to {@code mk3}); the tier
 * is the machine's own.
 */
public class SpaceModuleMachine extends WorkableElectricMultiblockMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(SpaceModuleMachine.class,
            WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    /** Module tier, 1 to 3. */
    private final int tier;
    /** Set by the elevator this module stands in; server side. */
    @Persisted
    private boolean connected;
    @Persisted
    private boolean powered;
    @Persisted
    private int motorTier;
    @Persisted
    private int slots;

    public SpaceModuleMachine(IMachineBlockEntity holder, int tier) {
        super(holder);
        this.tier = tier;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    public int getModuleTier() {
        return tier;
    }

    /** Expeditions this module flies at once when it is powered (GTNH's parallels). */
    public int expeditions() {
        return SpaceElevatorMachine.MODULE_EXPEDITIONS[tier - 1];
    }

    /**
     * The elevator found this module in its structure: the motors it has (their tier), the slots they power and whether this
     * module is one of those they power.
     */
    public void connect(int motorTier, int slots, boolean powered) {
        if (connected && powered == this.powered && motorTier == this.motorTier && slots == this.slots) return;
        connected = true;
        this.powered = powered;
        this.motorTier = motorTier;
        this.slots = slots;
        markDirty();
    }

    /** The elevator broke or does not have this module in its structure any more. */
    public void disconnect() {
        if (!connected) return;
        connected = false;
        powered = false;
        motorTier = 0;
        slots = 0;
        markDirty();
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isPowered() {
        return connected && powered;
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        textList.add(Component.translatable("af9.space_module.title", SpaceElevatorMachine.mark(tier)));
        if (!isFormed()) {
            textList.add(Component.translatable("af9.space_module.not_formed"));
            return;
        }
        if (!connected) {
            textList.add(Component.translatable("af9.space_module.no_elevator"));
            return;
        }
        textList.add(Component.translatable("af9.space_module.motors", SpaceElevatorMachine.mark(motorTier), slots));
        if (powered) {
            textList.add(Component.translatable("af9.space_module.powered", expeditions()));
        } else if (tier > motorTier) {
            textList.add(Component.translatable("af9.space_module.needs_motors", SpaceElevatorMachine.mark(tier)));
        } else {
            textList.add(Component.translatable("af9.space_module.no_slot"));
        }
        super.addDisplayText(textList);
    }
}
