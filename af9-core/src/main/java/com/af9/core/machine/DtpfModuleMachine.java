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
 * A module of the Dimensionally Transcendent Plasma Forge ({@link PlasmaForgeMachine}): the controller of one of the hall's
 * six module slots. The slot is a floating plate of {@value #CASINGS} Inconel 738 Module Casings behind the middle
 * injection casing of a big ring, with the controller in the middle of it, facing the field. A module runs no recipe: the
 * hall finds it ({@link PlasmaForgeMachine#modules()}), turns it to face outwards, away from the field, links it and gives its bonus
 * ({@link Kind}) to every run while the slot's casings are all in place. An empty slot is simply gone from the structure.
 * <p>
 * Made in KubeJS ({@code startup_scripts/gtceu/dtpf_modules.js}, {@code gtceu:dtpf_module_<kind>}); its recipes are the
 * pack's.
 */
public class DtpfModuleMachine extends MultiblockControllerMachine implements IDisplayUIMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            DtpfModuleMachine.class, MultiblockControllerMachine.MANAGED_FIELD_HOLDER);

    /** The casings round a module's controller. */
    public static final int CASINGS = 12;
    /** The parallels a Parallel Processing Assembly adds (six of them make 61, under the pack's 64). */
    public static final int PARALLEL_STEP = 10;
    /** What the Superheating Element multiplies the field's heat and the recipes' EU/t by. */
    public static final int HEAT_FACTOR = 10;
    public static final double HEAT_ENERGY = 2;
    /** The Celestial Forge's speed: runs take 1 / this as long, each module again. */
    public static final double FORGE_SPEED = 1.5;
    /** The Sequencer holds the field at this share of its strength at least, and burns this share of the plasma. */
    public static final double SEQUENCER_FLOOR = 0.30, SEQUENCER_BURN = 0.5;

    /**
     * The kinds, and how many of each a hall takes: Parallel Processing Assembly (6), Superheating Element (1), Osmic
     * Retention Stabilisation Sequencer (1), Manipulated Celestial Forge (2).
     */
    public enum Kind {
        PARALLEL(6),
        SUPERHEATER(1),
        SEQUENCER(1),
        FORGE(2);

        public final int max;

        Kind(int max) {
            this.max = max;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** The kind of a module block id ({@code dtpf_module_<kind>}), null for any other block. */
        public static Kind ofBlock(String path) {
            if (!path.startsWith(PlasmaForgeMachine.MODULE_PREFIX)) return null;
            String id = path.substring(PlasmaForgeMachine.MODULE_PREFIX.length());
            for (Kind kind : values()) {
                if (kind.id().equals(id)) return kind;
            }
            return null;
        }
    }

    /** The kind with this id ({@code parallel}, ...), for the startup script. */
    public static Kind kindOf(String id) {
        return Kind.valueOf(id.toUpperCase(Locale.ROOT));
    }

    private final Kind kind;
    /** The hall this module stands in, as a block position, {@link Long#MIN_VALUE} for none. */
    @Persisted
    @DescSynced
    private long hall = Long.MIN_VALUE;
    /** Whether the slot's casings are all there and the hall counts it. */
    @DescSynced
    private boolean complete;

    public DtpfModuleMachine(IMachineBlockEntity holder, Kind kind) {
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

    /** The hall noted this module, and whether the casings round it are all in place. */
    public void link(long hallPos, boolean casingsComplete) {
        if (hall == hallPos && complete == casingsComplete) return;
        hall = hallPos;
        complete = casingsComplete;
        markDirty();
        showState();
    }

    /** The hall broke or does not have this module any more. */
    public void unlink() {
        if (hall == Long.MIN_VALUE && !complete) return;
        hall = Long.MIN_VALUE;
        complete = false;
        markDirty();
        showState();
    }

    public boolean isLinked() {
        return hall != Long.MIN_VALUE && hallMachine() != null;
    }

    /** The hall this module stands in, null while it is not linked (or not loaded). */
    public PlasmaForgeMachine hallMachine() {
        if (hall == Long.MIN_VALUE || getLevel() == null) return null;
        return MetaMachine.getMachine(getLevel(), BlockPos.of(hall)) instanceof PlasmaForgeMachine machine &&
                machine.isFormed() ? machine : null;
    }

    /** Whether the module gives its bonus: its casings are all in place and it is part of a formed hall. */
    public boolean isActive() {
        return isFormed() && complete && isLinked();
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
        textList.add(Component.translatable("af9.dtpf_module." + kind.id() + ".name"));
        if (!isFormed()) {
            textList.add(Component.translatable("af9.dtpf_module.unformed"));
            return;
        }
        if (!isLinked()) {
            textList.add(Component.translatable("af9.dtpf_module.alone"));
        } else {
            textList.add(Component.translatable(complete ? "af9.dtpf_module.linked" : "af9.dtpf_module.incomplete",
                    CASINGS));
        }
        textList.add(Component.translatable("af9.dtpf_module." + kind.id() + ".bonus"));
    }
}
