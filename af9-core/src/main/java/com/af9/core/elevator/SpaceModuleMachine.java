package com.af9.core.elevator;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableEnergyContainer;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import java.util.List;

/**
 * A Space Elevator Mining Module as its own small multiblock, as GTNH's modules are (gtnhintergalactic's
 * TileEntityModuleBase / TileEntityModuleMiner): a controller that stands in one of the tower's module slots, with its own
 * structure (the casings above and below it, where the slot's hatches go), its own drone, its own hatches and its own screen. It
 * <b>flies the missions</b> ({@link SpaceMissionMachine}), {@link SpaceElevatorMachine#MODULE_EXPEDITIONS its tier's} expeditions
 * at once, with the hydrogen and coolant of its hatches and the ore or fluid going to its hatches.
 * <p>
 * The elevator it stands in connects it when the tower forms ({@link #connect}), telling it whether the motors power it, and
 * charges its energy buffer every tick ({@link SpaceElevatorMachine}): the module takes the energy of its expeditions from that
 * buffer, as GTNH's modules do. A module the motors do not power (a tier above the motors', or more modules than the motors have
 * slots for) stays dark.
 * <p>
 * Made in KubeJS ({@code startup_scripts/gtceu/space_elevator.js}, {@code gtceu:space_mining_module_mk1} to {@code mk3}); the tier
 * is the machine's own.
 */
public class SpaceModuleMachine extends SpaceMissionMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(SpaceModuleMachine.class,
            SpaceMissionMachine.MANAGED_FIELD_HOLDER);

    /**
     * The energy buffer: a recipe's energy is taken from it, the elevator fills it. Room for this many ticks of the biggest
     * mission a module flies (8 expeditions of a Mk-IV drone, 32 A of ZPM each).
     */
    private static final long MAX_EUT = 8L * 32L * 122_880L;
    private static final long BUFFER = MAX_EUT * 40L;

    /** Module tier, 1 to 3. */
    private final int tier;
    @Persisted
    public final NotifiableEnergyContainer buffer;
    /** Set by the elevator this module stands in. */
    @Persisted
    @DescSynced
    private boolean connected;
    @Persisted
    @DescSynced
    private boolean powered;
    @Persisted
    @DescSynced
    private int motorTier;
    @Persisted
    @DescSynced
    private int slots;
    /** The elevator's block position as a long, {@link Long#MIN_VALUE} for none. */
    @Persisted
    @DescSynced
    private long elevator = Long.MIN_VALUE;

    public SpaceModuleMachine(IMachineBlockEntity holder, int tier) {
        super(holder);
        this.tier = tier;
        // it takes energy from the elevator only: nothing flows in from a cable (no side takes energy), what a recipe
        // takes comes out of the stored energy
        buffer = NotifiableEnergyContainer.receiverContainer(this, BUFFER, 1L, 1L);
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

    //////////////////////////////////////
    // ********* The elevator *********//
    //////////////////////////////////////

    /** The elevator this module stands in, null while it is not connected (or not loaded). */
    public SpaceElevatorMachine parent() {
        if (!connected || elevator == Long.MIN_VALUE || getLevel() == null) return null;
        return MetaMachine.getMachine(getLevel(), BlockPos.of(elevator)) instanceof SpaceElevatorMachine machine ?
                machine : null;
    }

    /**
     * The elevator found this module in its structure: where it is, the motors it has (their tier), the slots they power and
     * whether this module is one of those they power.
     */
    public void connect(long elevatorPos, int motorTier, int slots, boolean powered) {
        if (connected && elevatorPos == elevator && powered == this.powered && motorTier == this.motorTier &&
                slots == this.slots) {
            return;
        }
        connected = true;
        elevator = elevatorPos;
        this.powered = powered;
        this.motorTier = motorTier;
        this.slots = slots;
        markDirty();
    }

    /**
     * The outputs of a module are the tower's: its ore and its fluid go to the output buses and hatches of the elevator it
     * stands in, in one place whatever the number of modules. Where the tower has none the module keeps its own.
     */
    @Override
    protected void collectRecipeHandlers() {
        super.collectRecipeHandlers();
        SpaceElevatorMachine parent = parent();
        if (parent == null) return;
        var lists = parent.getCapabilitiesProxy().get(IO.OUT);
        if (lists != null && !lists.isEmpty()) {
            var shared = new java.util.ArrayList<>(lists);
            getCapabilitiesProxy().remove(IO.OUT);
            getCapabilitiesFlat().remove(IO.OUT);
            for (var list : shared) addHandlerList(list);
        }
        // and its inputs are the tower's too: the hydrogen and the coolant (and the items) sit in the tower's hatches, in one
        // place whatever the number of modules; what the module has of its own stays
        for (IO io : new IO[] { IO.IN, IO.BOTH }) {
            var inputs = parent.getCapabilitiesProxy().get(io);
            if (inputs == null || inputs.isEmpty()) continue;
            for (var list : new java.util.ArrayList<>(inputs)) {
                // the tower's buses and hatches, not the slots of its own screen (the drone and the circuit of a module are
                // set on the module)
                if (isTraitListOf(list, parent)) continue;
                var own = getCapabilitiesProxy().get(list.getHandlerIO());
                if (own == null || !own.contains(list)) {
                    addHandlerList(list);
                    // a change in the tower's hatch wakes this module's recipe search, as one of its own parts would
                    traitSubscriptions.add(list.subscribe(recipeLogic::updateTickSubscription));
                }
            }
        }
        // the module's own slots and the tower's buses in one group (see SpaceMissionMachine#collectRecipeHandlers)
        alignInputGroups();
    }

    /** Whether a handler list is made of traits of a machine itself (not of one of its parts). */
    private static boolean isTraitListOf(com.gregtechceu.gtceu.api.machine.trait.RecipeHandlerList list,
                                         com.gregtechceu.gtceu.api.machine.MetaMachine machine) {
        for (var handlers : list.getHandlerMap().values()) {
            for (var handler : handlers) {
                if (handler instanceof com.gregtechceu.gtceu.api.machine.trait.MachineTrait trait &&
                        trait.getMachine() == machine) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The elevator broke or does not have this module in its structure any more. */
    public void disconnect() {
        if (!connected) return;
        connected = false;
        powered = false;
        motorTier = 0;
        slots = 0;
        elevator = Long.MIN_VALUE;
        markDirty();
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isPowered() {
        return connected && powered;
    }

    /** Room in the energy buffer. */
    public long energyRoom() {
        return buffer.getEnergyCapacity() - buffer.getEnergyStored();
    }

    /** The elevator puts energy in the buffer. */
    public void receiveEnergy(long amount) {
        buffer.changeEnergy(amount);
    }

    //////////////////////////////////////
    // ********* The mission **********//
    //////////////////////////////////////

    /** What the elevator can give at once: what its hatches carry. */
    @Override
    public long getAvailableEUt() {
        SpaceElevatorMachine parent = parent();
        return !isPowered() || parent == null ? 0 : parent.getAvailableEUt();
    }

    /** A module overclocks to the voltage of its tower's hatches. */
    @Override
    public long overclockVoltage() {
        SpaceElevatorMachine parent = parent();
        return !isPowered() || parent == null ? 0 : parent.getOverclockVoltage();
    }

    @Override
    public boolean isSkyClear() {
        SpaceElevatorMachine parent = parent();
        return parent != null && parent.isSkyClear();
    }

    @Override
    public int getExpeditions() {
        return isPowered() ? expeditions() : 0;
    }

    @Override
    public int getMotorTier() {
        return connected ? motorTier : 0;
    }

    @Override
    public int getModules() {
        SpaceElevatorMachine parent = parent();
        return parent == null ? 0 : parent.getModules();
    }

    @Override
    public int getPoweredModules() {
        SpaceElevatorMachine parent = parent();
        return parent == null ? 0 : parent.getPoweredModules();
    }

    @Override
    public int getTopModule() {
        return connected ? tier : 0;
    }

    @Override
    public boolean isExtended() {
        SpaceElevatorMachine parent = parent();
        return parent != null && parent.isExtended();
    }

    @Override
    public void setExtended(boolean extended) {
        SpaceElevatorMachine parent = parent();
        if (parent != null) parent.setExtended(extended);
    }

    @Override
    public float climberHeight(float partialTick) {
        SpaceElevatorMachine parent = parent();
        return parent == null ? 0F : parent.climberHeight(partialTick);
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        super.addDisplayText(textList);
    }
}
