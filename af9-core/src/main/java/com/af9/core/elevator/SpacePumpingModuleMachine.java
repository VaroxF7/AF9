package com.af9.core.elevator;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;

/**
 * The Space Elevator's Pumping Module: a module of its own ({@link SpaceModuleMachine}) that stands in a module slot and pumps
 * a planet's fluid. One recipe a fluid ({@code gtceu:space_pumping}, 4A of ZPM, 120 to 360 seconds): the recipe is set in the
 * module (the Mining Drone of the planet's reach in its drone slot, and the programmed circuit that picks the n-th of that
 * drone's fluids, {@link PlanetCatalog#ofDrone}); the run is the tower's: it draws the tower's energy, hydrogen and coolant, only
 * starts while the tower is switched on, and the fluid goes to the fluid output hatches of the tower.
 * <p>
 * It counts as a MK-I module for the motors (any motors power it, as a slot among the others), and flies every drone.
 */
public class SpacePumpingModuleMachine extends SpaceModuleMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            SpacePumpingModuleMachine.class, SpaceModuleMachine.MANAGED_FIELD_HOLDER);

    public SpacePumpingModuleMachine(IMachineBlockEntity holder) {
        super(holder, 1);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    /** The tower's runtime is the module's: nothing starts while the tower is switched off. */
    @Override
    public long getAvailableEUt() {
        SpaceElevatorMachine parent = parent();
        return parent == null || !parent.isWorkingEnabled() ? 0 : super.getAvailableEUt();
    }
}
