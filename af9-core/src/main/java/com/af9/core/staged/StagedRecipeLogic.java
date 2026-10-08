package com.af9.core.staged;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.ActionResult;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.utils.FluidStackHashStrategy;
import com.gregtechceu.gtceu.utils.ItemStackHashStrategy;

import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;

import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Runs staged recipes one step at a time, the way Star Technology's layered recipes run (the {@code
 * LayeredRecipeLogic} of its GTCEu fork): only recipes carrying {@code af9_staged} data are ever considered, the
 * first matching step starts the craft, and each finished step chains into the next. A step only matches while the
 * machine holds exactly its inputs (programmed circuits aside) — anything more fails the step, so the player feeds
 * the craft stage by stage instead of dumping everything in at once.
 * <p>
 * A craft is modified once, when it starts: the machine's modifier for the first step (overclocks, parallels) is
 * applied to every step, so the whole craft runs on the same overclock and the same parallel count, as it does there
 * (its fork passes the root's modifier down to the layers).
 * <p>
 * The steps live only in memory ({@link #stagedSteps}); what persists is the root id and the stage index, and every
 * computed step carries both stamps, so the sequence rebuilds itself after a reload.
 */
public class StagedRecipeLogic extends RecipeLogic {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(StagedRecipeLogic.class,
            RecipeLogic.MANAGED_FIELD_HOLDER);

    /** Id of the staged root recipe of the running craft, "" when idle. */
    @Persisted
    private String stagedRoot = "";
    /** Index of the running (or next) step, -1 when idle. */
    @Persisted
    private int stagedIndex = -1;
    /** Fully finished staged crafts, for the console. */
    @Persisted
    private long craftsCompleted;

    /** The running craft's steps, modified, in order; empty when idle. */
    private List<GTRecipe> stagedSteps = List.of();
    /** The running craft's root recipe, for the repeat run after the last step. */
    private GTRecipe stagedRootRecipe;

    public StagedRecipeLogic(IRecipeLogicMachine machine) {
        super(machine);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    /** True while a staged craft is loaded (running or waiting for the next step's inputs). */
    public boolean hasStagedCraft() {
        return !craft().isEmpty();
    }

    public int getStageIndex() {
        return stagedIndex;
    }

    public int getStageCount() {
        return craft().size();
    }

    public long getCraftsCompleted() {
        return craftsCompleted;
    }

    public List<GTRecipe> getStagedSteps() {
        return craft();
    }

    /** The craft's last step, the one with the outputs; null when idle. */
    public GTRecipe getLastStep() {
        List<GTRecipe> steps = craft();
        return steps.isEmpty() ? null : steps.get(steps.size() - 1);
    }

    /**
     * The step whose inputs go in next: the one the craft waits for, or the one after the running step; null when
     * idle and on the last step.
     */
    public GTRecipe getNextStep() {
        List<GTRecipe> steps = craft();
        if (stagedIndex < 0 || steps.isEmpty()) return null;
        if (lastRecipe == null) return steps.get(Math.min(stagedIndex, steps.size() - 1));
        return stagedIndex < steps.size() - 1 ? steps.get(stagedIndex + 1) : null;
    }

    /** Ticks of all steps of the craft together. */
    public int getCraftDuration() {
        int total = 0;
        for (GTRecipe step : craft()) total += step.duration;
        return total;
    }

    /** Ticks of the craft done: the finished steps and the running one's progress. */
    public int getCraftProgress() {
        List<GTRecipe> steps = craft();
        int done = getProgress();
        for (int i = 0; i < stagedIndex && i < steps.size(); i++) done += steps.get(i).duration;
        return done;
    }

    /**
     * The step detector cover's signal: the steps begun (the running one counts), so the number of the step to feed
     * next, and 0 again on the last step and while idle.
     */
    public int getCoverRedstoneOutput() {
        int result = stagedIndex + (lastRecipe == null ? 0 : 1);
        if (result == craft().size()) result = 0;
        return Mth.clamp(result, 0, 15);
    }

    /** Aborts the running craft (the console's cancel button); already-consumed inputs are not refunded. */
    public void cancelStagedCraft() {
        resetRecipeLogic();
    }

    @Override
    public Iterator<GTRecipe> searchRecipe() {
        List<GTRecipe> steps = craft();
        if (!steps.isEmpty()) {
            if (stagedIndex < 0 || stagedIndex >= steps.size()) return Collections.emptyIterator();
            return Collections.singleton(steps.get(stagedIndex)).iterator();
        }
        // a fresh craft: every staged recipe whose first step matches
        List<GTRecipe> matches = new ArrayList<>();
        for (GTRecipe root : StagedRecipes.allRecipes(machine.getRecipeType())) {
            if (!StagedRecipeData.isStaged(root)) continue;
            List<GTRecipe> first = StagedRecipes.getSteps(root);
            if (!first.isEmpty() && matchRecipe(first.get(0)).isSuccess()) matches.add(first.get(0));
        }
        return matches.iterator();
    }

    @Override
    public boolean checkMatchedRecipeAvailable(GTRecipe match) {
        GTRecipe run = match;
        GTRecipe root = null;
        List<GTRecipe> fresh = null;
        if (stagedSteps.isEmpty()) {
            // a craft begins: its steps are modified here, all with the first step's modifier
            root = StagedRecipes.findRoot(machine.getRecipeType(), match.data.getString(StagedRecipes.KEY_ROOT));
            fresh = root == null ? null : modifiedSteps(root);
            if (fresh == null) return false;
            run = fresh.get(0);
        }
        // otherwise a later step of the running craft, modified with it

        if (checkRecipe(run).isSuccess()) {
            if (fresh != null) load(root, fresh, 0);
            setupRecipe(run);
            // the machine or its inputs refused it after all: no craft began
            if (fresh != null && lastRecipe != run) clearStaged();
        }
        if (lastRecipe != null && getStatus() == Status.WORKING) {
            lastOriginRecipe = null;
            lastFailedMatches = null;
            return true;
        }
        return false;
    }

    @Override
    public void setupRecipe(GTRecipe recipe) {
        super.setupRecipe(recipe);
        // never run a step again from GT's "same recipe again" shortcut: the next one is chosen in onRecipeFinish
        recipeDirty = true;
    }

    @Override
    public void onRecipeFinish() {
        machine.afterWorking();
        if (lastRecipe == null) return;

        boolean finishedLast = false;
        if (StagedRecipes.isStep(lastRecipe)) {
            // after a reload the running step is all there is: its stamps rebuild the craft
            if (craft().isEmpty() && !restore(lastRecipe.data.getString(StagedRecipes.KEY_ROOT),
                    lastRecipe.data.getInt(StagedRecipes.KEY_STAGE))) {
                clearStaged();
            } else {
                stagedIndex++;
                if (stagedIndex >= stagedSteps.size()) finishedLast = true;
            }
        }

        runAttempt = 0;
        runDelay = 0;
        consecutiveRecipes++;
        handleRecipeIO(lastRecipe, IO.OUT);

        if (suspendAfterFinish) {
            if (finishedLast) {
                craftsCompleted++;
                clearStaged();
            }
            setStatus(Status.SUSPEND);
            consecutiveRecipes = 0;
            progress = 0;
            duration = 0;
            isActive = false;
            lastRecipe = null;
            return;
        }

        if (finishedLast) {
            // the craft is done: clear first, then start the same craft again when its first step still matches
            craftsCompleted++;
            GTRecipe root = stagedRootRecipe;
            String rootId = stagedRoot;
            clearStaged();
            if (root == null) root = StagedRecipes.findRoot(machine.getRecipeType(), rootId);
            List<GTRecipe> again = root == null ? null : modifiedSteps(root);
            if (again != null && checkRecipe(again.get(0)).isSuccess()) {
                load(root, again, 0);
                setupRecipe(again.get(0));
                if (lastRecipe == again.get(0)) return;
                clearStaged();
            }
        } else if (!stagedSteps.isEmpty()) {
            // the next step, already modified with the craft
            GTRecipe next = stagedSteps.get(stagedIndex);
            if (checkRecipe(next).isSuccess()) {
                setupRecipe(next);
                if (lastRecipe == next) return;
            }
        }

        setStatus(Status.IDLE);
        lastRecipe = null; // a step never runs again from lastRecipe
        consecutiveRecipes = 0;
        progress = 0;
        duration = 0;
        isActive = false;
    }

    @Override
    protected ActionResult checkRecipe(GTRecipe recipe) {
        ActionResult normal = super.checkRecipe(recipe);
        if (!normal.isSuccess() || !StagedRecipes.isStep(recipe)) return normal;

        // the step matches, but only when the machine holds nothing beyond it: no pre-loading later steps
        Set<ItemStack> presentItems = new ObjectOpenCustomHashSet<>(
                ItemStackHashStrategy.comparingAllButCount());
        machine.getCapabilitiesFlat(IO.IN, ItemRecipeCapability.CAP).stream()
                .flatMap(handler -> handler.getContents().stream())
                .filter(ItemStack.class::isInstance).map(ItemStack.class::cast)
                .filter(stack -> !stack.isEmpty() && !stack.is(GTItems.PROGRAMMED_CIRCUIT.get()))
                .forEach(presentItems::add);
        for (Content raw : recipe.getInputContents(ItemRecipeCapability.CAP)) {
            Ingredient need = ItemRecipeCapability.CAP.of(raw.getContent());
            presentItems.removeIf(need::test);
        }
        if (!presentItems.isEmpty()) {
            return ActionResult.fail(Component.translatable("af9.recipe.staged.extra_inputs"), null, IO.IN);
        }

        Set<FluidStack> presentFluids = new ObjectOpenCustomHashSet<>(
                FluidStackHashStrategy.comparingAllButAmount());
        machine.getCapabilitiesFlat(IO.IN, FluidRecipeCapability.CAP).stream()
                .flatMap(handler -> handler.getContents().stream())
                .filter(FluidStack.class::isInstance).map(FluidStack.class::cast)
                .filter(stack -> !stack.isEmpty())
                .forEach(presentFluids::add);
        for (Content raw : recipe.getInputContents(FluidRecipeCapability.CAP)) {
            FluidIngredient need = FluidRecipeCapability.CAP.of(raw.getContent());
            presentFluids.removeIf(need::test);
        }
        if (!presentFluids.isEmpty()) {
            return ActionResult.fail(Component.translatable("af9.recipe.staged.extra_inputs"), null, IO.IN);
        }
        return ActionResult.SUCCESS;
    }

    /** A craft that is interrupted (the structure broke) is lost, the running step included. */
    @Override
    public void interruptRecipe() {
        machine.afterWorking();
        setStatus(Status.IDLE);
        progress = 0;
        duration = 0;
        lastRecipe = null;
        clearStaged();
    }

    @Override
    public void resetRecipeLogic() {
        clearStaged();
        super.resetRecipeLogic();
    }

    /**
     * The running craft's steps. After a reload they are gone while the root id and the stage are still there: they
     * are built again from those (and, should the machine not be able to modify them yet, on a later call).
     */
    private List<GTRecipe> craft() {
        if (stagedSteps.isEmpty() && stagedIndex >= 0 && !stagedRoot.isEmpty() && !machine.self().isRemote()) {
            restore(stagedRoot, stagedIndex);
        }
        return stagedSteps;
    }

    /** Rebuilds the craft of a root id at a stage; false when it cannot be (clears it when its recipe is gone). */
    private boolean restore(String rootId, int stage) {
        GTRecipe root = StagedRecipes.findRoot(machine.getRecipeType(), rootId);
        if (root == null) {
            clearStaged();
            return false;
        }
        List<GTRecipe> steps = modifiedSteps(root);
        if (steps == null) return false;
        load(root, steps, Mth.clamp(stage, 0, steps.size() - 1));
        return true;
    }

    /**
     * A root's steps as the machine runs them: each with the machine's modifier for the first step, so one overclock
     * and one parallel count for the whole craft. Null when the machine cannot run it (or it has no steps).
     */
    private List<GTRecipe> modifiedSteps(GTRecipe root) {
        List<GTRecipe> steps = StagedRecipes.getSteps(root);
        if (steps.isEmpty()) return null;
        MetaMachine self = machine.self();
        var limits = machine.getOutputLimits();
        ModifierFunction modifier = self.getDefinition().getRecipeModifier()
                .getModifier(self, RecipeHelper.trimRecipeOutputs(steps.get(0), limits));
        List<GTRecipe> modified = new ArrayList<>(steps.size());
        for (GTRecipe step : steps) {
            GTRecipe result = modifier.apply(RecipeHelper.trimRecipeOutputs(step, limits));
            if (result == null) return null;
            modified.add(result);
        }
        return modified;
    }

    private void load(GTRecipe root, List<GTRecipe> steps, int stage) {
        stagedSteps = steps;
        stagedRootRecipe = root;
        stagedRoot = root.id == null ? "" : root.id.toString();
        stagedIndex = stage;
    }

    private void clearStaged() {
        stagedSteps = List.of();
        stagedRootRecipe = null;
        stagedRoot = "";
        stagedIndex = -1;
    }
}
