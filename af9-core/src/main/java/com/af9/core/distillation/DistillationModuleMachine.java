package com.af9.core.distillation;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.ActionResult;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ParallelHatchPartMachine;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * A Distillation Tower Module: one of the three towers of the {@link DistillationFacilityMachine}, a small multiblock of its own
 * (a 3 x 3 column with a kanthal coil at its foot, a hollow shaft and a muffler hatch on top, the Distillation Tower's shape).
 * <p>
 * The module is where the recipe is set and where it runs from: its own mode tab (Distillation Tower, Distillation, Cryogenic
 * Rectification) and its own recipe lock ({@link #lockRecipe}), at most one Parallel Hatch in its casings and its muffler hatch. What
 * it runs on is the facility's: the facility's energy hatches (it overclocks to their voltage, perfectly), its fluid and item
 * hatches for the inputs and the outputs, its rotor in the tower's row ({@link DistillationFacilityMachine#rotorOf}) for speed and
 * energy, and its built-in cleanroom for the clean-room recipes. A module without a formed facility runs nothing.
 * <p>
 * With nothing locked a module takes whatever recipe the facility's inputs match (so three towers fed one fluid are three
 * parallels of the same recipe); a locked module only ever runs that recipe, so the three towers can run three different ones.
 * <p>
 * Made in KubeJS ({@code startup_scripts/gtceu/distillation_facility.js}, {@code gtceu:distillation_module}).
 */
public class DistillationModuleMachine extends WorkableElectricMultiblockMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            DistillationModuleMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    /** The pack allows 64 parallels at most on a machine: the three towers of a facility share them. */
    public static final int TOWER_MAX_PARALLEL = 64 / 3;

    /**
     * The tower's parallel hatch, capped at {@link #TOWER_MAX_PARALLEL} so the facility's three towers together stay within
     * the pack's 64 parallels. Replaces GT's Parallel Hatch modifier.
     */
    public static final RecipeModifier TOWER_PARALLEL = (machine, recipe) -> {
        if (!(machine instanceof DistillationModuleMachine module)) {
            return RecipeModifier.nullWrongType(DistillationModuleMachine.class, machine);
        }
        int hatch = 1;
        for (var part : module.getParts()) {
            if (part instanceof ParallelHatchPartMachine parallel) hatch = Math.max(hatch, parallel.getCurrentParallel());
        }
        int limit = Math.min(hatch, TOWER_MAX_PARALLEL);
        if (limit <= 1) return ModifierFunction.IDENTITY;
        // by the inputs only: the outputs go into the facility's buffer, not into hatches that must have room for them now
        int parallels = ParallelLogic.getMaxByInput(module, recipe, limit, Collections.emptyList());
        if (parallels == 0) return ModifierFunction.NULL;
        if (parallels == 1) return ModifierFunction.IDENTITY;
        return ModifierFunction.builder().modifyAllContents(ContentModifier.multiplier(parallels))
                .eutMultiplier(parallels).parallels(parallels).build();
    };

    /**
     * The facility's part in every run: only while the facility is formed and switched on, with a rotor spinning in the tower's
     * row; the rotor makes the run faster and cheaper ({@link DistillationFacilityMachine.RotorState}).
     */
    public static final RecipeModifier FACILITY = (machine, recipe) -> {
        if (!(machine instanceof DistillationModuleMachine module)) {
            return RecipeModifier.nullWrongType(DistillationModuleMachine.class, machine);
        }
        return module.facilityModifier(recipe);
    };

    /** The facility's block position as a long, {@link Long#MIN_VALUE} for none. */
    @Persisted
    private long facility = Long.MIN_VALUE;
    @Persisted
    @DescSynced
    private boolean connected;
    /** The id of the recipe this tower is locked to, empty for none. */
    @Persisted
    @DescSynced
    private String pinned = "";
    /** Whether the facility's hatches are in this module's handler lists. */
    private boolean linked;

    public DistillationModuleMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) {
        return new ModuleLogic(this);
    }

    /** Re-modify every run: the rotor spins up, so each run gets the bonus of its moment. */
    @Override
    public boolean alwaysTryModifyRecipe() {
        return true;
    }

    //////////////////////////////////////
    // ******** The facility ********//
    //////////////////////////////////////

    /** The facility this module stands in, null while it is not connected (or not formed). */
    public DistillationFacilityMachine parent() {
        if (!connected || facility == Long.MIN_VALUE || getLevel() == null) return null;
        return MetaMachine.getMachine(getLevel(), BlockPos.of(facility)) instanceof DistillationFacilityMachine machine &&
                machine.isFormed() ? machine : null;
    }

    /** The facility found this module in its structure; true if that is news. */
    public boolean connect(long facilityPos) {
        if (connected && facility == facilityPos) return false;
        connected = true;
        facility = facilityPos;
        markDirty();
        return true;
    }

    /** The facility broke or does not have this module in its structure any more. */
    public void disconnect() {
        if (!connected) return;
        connected = false;
        facility = Long.MIN_VALUE;
        markDirty();
        setCleanroom(null);
    }

    public boolean isConnected() {
        return connected;
    }

    /** Whether the facility's hatches are in this module's handler lists (the facility relinks the ones that are not). */
    public boolean isLinked() {
        return linked;
    }

    /**
     * The module's hatches are the facility's: the energy hatches (so the module overclocks to their voltage), the fluid and
     * item hatches of the inputs and of the outputs. What the module has of its own (the parallel hatch, the muffler) stays.
     */
    @Override
    protected void collectRecipeHandlers() {
        super.collectRecipeHandlers();
        linked = false;
        DistillationFacilityMachine parent = parent();
        if (parent != null) {
            for (IO io : new IO[] { IO.IN, IO.OUT, IO.BOTH }) {
                var lists = parent.getCapabilitiesProxy().get(io);
                if (lists == null) continue;
                for (var list : new ArrayList<>(lists)) {
                    addHandlerList(list);
                    // a change in the facility's hatch wakes this module's recipe search, as one of its own parts would
                    traitSubscriptions.add(list.subscribe(recipeLogic::updateTickSubscription));
                }
            }
            linked = true;
        }
        if (isFormed()) {
            // the energy container is made once when the structure forms, before the facility's hatches are in: make it again
            energyContainer = getEnergyContainer();
            tier = GTUtil.getFloorTierByVoltage(getMaxVoltage());
        }
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        linked = false;
    }

    //////////////////////////////////////
    // ******** The recipe *********//
    //////////////////////////////////////

    /** The recipe this tower is locked to, empty for none. */
    public String pinnedRecipe() {
        return pinned;
    }

    /** Locks the tower to the recipe it runs (or ran last). */
    public void lockRecipe() {
        GTRecipe last = getRecipeLogic().getLastRecipe();
        if (last == null || last.id == null) return;
        pinned = last.id.toString();
        markDirty();
        getRecipeLogic().updateTickSubscription();
    }

    /**
     * Sets the tower to this recipe, from the recipe viewer's "+": its mode becomes the recipe's type and its recipe lock the recipe;
     * false if the tower does not run that type.
     */
    public boolean pinTo(GTRecipe recipe) {
        if (recipe.id == null) return false;
        int mode = Arrays.asList(getRecipeTypes()).indexOf(recipe.recipeType);
        if (mode < 0) return false;
        if (mode != getActiveRecipeType()) setActiveRecipeType(mode);
        pinned = recipe.id.toString();
        markDirty();
        getRecipeLogic().resetRecipeLogic();
        getRecipeLogic().updateTickSubscription();
        return true;
    }

    public void unlockRecipe() {
        if (pinned.isEmpty()) return;
        pinned = "";
        markDirty();
        getRecipeLogic().updateTickSubscription();
    }

    private ModifierFunction facilityModifier(GTRecipe recipe) {
        DistillationFacilityMachine parent = parent();
        if (parent == null) return ModifierFunction.cancel(Component.translatable("af9.distillation.module.no_facility"));
        if (!parent.isWorkingEnabled()) {
            return ModifierFunction.cancel(Component.translatable("af9.distillation.module.facility_off"));
        }
        if (parent.needsMaintenance()) {
            return ModifierFunction.cancel(Component.translatable("af9.distillation.module.maintenance"));
        }
        // Filtered Distillation: the fab chemistry's columns run only in a filtered facility (the plain distillation does not)
        if (recipe.recipeType.registryName.getPath().startsWith("fab_") && !parent.isFiltered()) {
            return ModifierFunction.cancel(Component.translatable("af9.distillation.module.not_filtered"));
        }
        DistillationFacilityMachine.RotorState rotor = parent.rotorOf(this);
        if (!rotor.ok()) return ModifierFunction.cancel(rotor.problem());
        return ModifierFunction.builder()
                .durationMultiplier(1.0 / rotor.speed())
                .eutMultiplier(rotor.energy())
                .build()
                .andThen(parent::maintain);
    }

    /** The rotor of the tower's row spins while the tower works (and wears, as in a turbine). */
    @Override
    public boolean onWorking() {
        boolean result = super.onWorking();
        DistillationFacilityMachine parent = parent();
        if (parent != null) parent.spinRotor(this);
        return result;
    }

    /** Runs the recipe the module is locked to, if it is locked to one; any recipe the hatches match otherwise. */
    private static class ModuleLogic extends RecipeLogic {

        ModuleLogic(IRecipeLogicMachine machine) {
            super(machine);
        }

        @Override
        public @NotNull Iterator<GTRecipe> searchRecipe() {
            String pin = getMachine() instanceof DistillationModuleMachine module ? module.pinnedRecipe() : "";
            if (pin.isEmpty()) return super.searchRecipe();
            return machine.getRecipeType().searchRecipe(machine, recipe -> recipe.id != null &&
                    recipe.id.toString().equals(pin));
        }

        /** A run starts when the hatches have its inputs and the facility's output buffer has room (not the hatches' room for the outputs). */
        @Override
        protected ActionResult matchRecipe(GTRecipe recipe) {
            DistillationFacilityMachine parent = getMachine() instanceof DistillationModuleMachine module ?
                    module.parent() : null;
            if (parent == null) return super.matchRecipe(recipe);
            if (!machine.hasCapabilityProxies()) return ActionResult.FAIL_NO_CAPABILITIES;
            if (!parent.bufferHasRoom()) {
                return ActionResult.fail(Component.translatable("af9.distillation.buffer_full"), null, null);
            }
            ActionResult inputs = RecipeHelper.handleRecipe(machine, recipe, IO.IN, recipe.inputs,
                    Collections.emptyMap(), false, true);
            if (!inputs.isSuccess()) return inputs;
            return RecipeHelper.matchTickRecipe(machine, recipe);
        }

        /** A finished run's outputs go into the facility's buffer, which feeds its output hatches. */
        @Override
        protected ActionResult handleRecipeIO(GTRecipe recipe, IO io) {
            if (io == IO.OUT && getMachine() instanceof DistillationModuleMachine module) {
                DistillationFacilityMachine parent = module.parent();
                if (parent != null) {
                    parent.bufferOutputs(recipe);
                    return ActionResult.SUCCESS;
                }
            }
            return super.handleRecipeIO(recipe, io);
        }
    }

    //////////////////////////////////////
    // *********** Screen ***********//
    //////////////////////////////////////

    @Override
    public void addDisplayText(List<Component> textList) {
        super.addDisplayText(textList);
        if (!isFormed()) return;
        DistillationFacilityMachine parent = parent();
        if (parent == null) {
            textList.add(Component.translatable("af9.distillation.module.no_facility").withStyle(ChatFormatting.RED));
            return;
        }
        DistillationFacilityMachine.RotorState rotor = parent.rotorOf(this);
        if (rotor.ok()) {
            textList.add(Component.translatable("af9.distillation.module.rotor", rotor.material(),
                    percent(rotor.spin())));
            textList.add(Component.translatable("af9.distillation.module.bonus", percent(rotor.speed() - 1),
                    percent(1 - rotor.energy())));
        } else {
            textList.add(rotor.problem().copy().withStyle(ChatFormatting.RED));
        }
        if (pinned.isEmpty()) {
            textList.add(ComponentPanelWidget.withButton(Component.translatable("af9.distillation.module.lock"), "lock"));
        } else {
            textList.add(Component.translatable("af9.distillation.module.locked", pinned));
            textList.add(ComponentPanelWidget.withButton(Component.translatable("af9.distillation.module.unlock"),
                    "unlock"));
        }
    }

    private static String percent(double fraction) {
        return String.format(Locale.ROOT, "%.0f%%", fraction * 100);
    }

    @Override
    public void handleDisplayClick(String componentData, ClickData clickData) {
        if (clickData.isRemote) return;
        if ("lock".equals(componentData)) lockRecipe();
        else if ("unlock".equals(componentData)) unlockRecipe();
    }
}
