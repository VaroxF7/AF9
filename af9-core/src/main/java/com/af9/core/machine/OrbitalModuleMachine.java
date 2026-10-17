package com.af9.core.machine;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDisplayUIMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.client.model.machine.MachineRenderState;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/**
 * A module of the Orbital Lithography Array Mk2, a small multiblock of its own that stands in one of the crown's module
 * slots ({@link OrbitalLithographyMachine#modules()}), as the Space Elevator's Mining Modules do
 * ({@link com.af9.core.elevator.SpaceModuleMachine}). It runs no recipe: the Array finds it when it forms, notes it
 * ({@link #link}) and gives its bonus ({@link Kind}) to every print while the module's own structure stands. A slot holds
 * any kind; modules of one kind stack.
 * <p>
 * Made in KubeJS ({@code startup_scripts/gtceu/orbital_modules.js}, {@code gtceu:orbital_module_<kind>}).
 */
public class OrbitalModuleMachine extends MultiblockControllerMachine implements IDisplayUIMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            OrbitalModuleMachine.class, MultiblockControllerMachine.MANAGED_FIELD_HOLDER);

    /**
     * The kinds and what each module of a kind gives the prints of the Array it stands in (every further module of the
     * kind multiplies again): run time, EU/t of the recipes, the auxiliary power (the start-up and standby draw), the
     * parallels it adds; and how many of the kind an Array takes.
     */
    public enum Kind {
        /** Parallel Processing Computer: parallels, as many as the energy supplied allows (the Mk2 has no parallel hatch). */
        PARALLEL(1, 1, 1, 10, 6),
        /** Energy Efficiency Module: 5% off the energy of the recipes (not the auxiliary power). */
        EFFICIENCY(1, 0.95, 1, 0, 6),
        /** Endion Evaporation Annihilation Stack: a supercooler, 10% faster recipes. */
        EVAPORATION(0.90, 1, 1, 0, 2),
        /** Stabilisation Sequencer: half the auxiliary power, running and idle. */
        SEQUENCER(1, 1, 0.5, 0, 1);

        public final double time, energy, aux;
        public final int parallels, max;

        Kind(double time, double energy, double aux, int parallels, int max) {
            this.time = time;
            this.energy = energy;
            this.aux = aux;
            this.parallels = parallels;
            this.max = max;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** The kind of a module block id ({@code orbital_module_<kind>}), null for any other block. */
        public static Kind ofBlock(String path) {
            if (!path.startsWith(OrbitalLithographyMachine.MODULE_PREFIX)) return null;
            String id = path.substring(OrbitalLithographyMachine.MODULE_PREFIX.length());
            for (Kind kind : values()) {
                if (kind.id().equals(id)) return kind;
            }
            return null;
        }
    }

    /** The kind with this id ({@code chrono}, ...), for the startup script. */
    public static Kind kindOf(String id) {
        return Kind.valueOf(id.toUpperCase(Locale.ROOT));
    }

    private final Kind kind;
    /** The Array this module stands in, as a block position, {@link Long#MIN_VALUE} for none. */
    @Persisted
    @DescSynced
    private long station = Long.MIN_VALUE;

    public OrbitalModuleMachine(IMachineBlockEntity holder, Kind kind) {
        super(holder);
        this.kind = kind;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    public Kind getKind() {
        return kind;
    }

    /** The Array noted this module in its structure. */
    public void link(long stationPos) {
        if (station == stationPos) return;
        station = stationPos;
        markDirty();
        showState();
    }

    /** The Array broke or does not have this module any more. */
    public void unlink() {
        if (station == Long.MIN_VALUE) return;
        station = Long.MIN_VALUE;
        markDirty();
        showState();
    }

    public boolean isLinked() {
        return station != Long.MIN_VALUE && stationMachine() != null;
    }

    /** The Array this module stands in, null while it is not linked (or not loaded). */
    public OrbitalLithographyMachine stationMachine() {
        if (station == Long.MIN_VALUE || getLevel() == null) return null;
        return MetaMachine.getMachine(getLevel(), BlockPos.of(station)) instanceof OrbitalLithographyMachine machine &&
                machine.isFormed() ? machine : null;
    }

    /** Whether the module gives its bonus: its own structure stands and it is part of a formed Array. */
    public boolean isActive() {
        return isFormed() && isLinked();
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        showState();
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        showState();
    }

    /** The lit overlay (the module's "active" texture) while it gives its bonus. */
    private void showState() {
        if (getLevel() == null || getLevel().isClientSide) return;
        MachineRenderState state = getRenderState();
        if (!state.hasProperty(GTMachineModelProperties.RECIPE_LOGIC_STATUS)) return;
        MachineRenderState next = state.setValue(GTMachineModelProperties.RECIPE_LOGIC_STATUS,
                isActive() ? RecipeLogic.Status.WORKING : RecipeLogic.Status.IDLE);
        if (next != state) setRenderState(next);
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        textList.add(Component.translatable("af9.orbital_module." + kind.id() + ".name"));
        if (!isFormed()) {
            textList.add(Component.translatable("af9.orbital_module.unformed"));
            return;
        }
        textList.add(Component.translatable(isLinked() ? "af9.orbital_module.linked" : "af9.orbital_module.alone"));
        textList.add(Component.translatable("af9.orbital_module." + kind.id() + ".bonus"));
    }
}
