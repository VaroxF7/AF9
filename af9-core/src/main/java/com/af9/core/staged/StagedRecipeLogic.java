package com.af9.core.staged;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.ActionResult;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.utils.FluidStackHashStrategy;
import com.gregtechceu.gtceu.utils.ItemStackHashStrategy;

import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;

import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Runs staged recipes one step at a time: only recipes carrying {@code af9_staged} data are ever considered, the
 * first matching step starts the craft, and each finished step chains into the next. A step only matches while the
 * machine holds exactly its inputs (programmed circuits aside) — anything more fails the step, so the player feeds
 * the craft stage by stage instead of dumping everything in at once.
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

    /** The running craft's steps, in order; empty when idle. */
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
        return !stagedSteps.isEmpty();
    }

    public int getStageIndex() {
        return stagedIndex;
    }

    public int getStageCount() {
        return stagedSteps.size();
    }

    public long getCraftsCompleted() {
        return craftsCompleted;
    }

    public List<GTRecipe> getStagedSteps() {
        return stagedSteps;
    }

    /** Aborts the running craft (the console's cancel button); already-consumed inputs are not refunded. */
    public void cancelStagedCraft() {
        resetRecipeLogic();
    }

    @Override
    public Iterator<GTRecipe> searchRecipe() {
        if (!stagedSteps.isEmpty()) {
            if (stagedIndex < 0 || stagedIndex >= stagedSteps.size()) return Collections.emptyIterator();
            return Collections.singleton(stagedSteps.get(stagedIndex)).iterator();
        }
        // after a reload the steps are gone but the persisted step knows its root
        if (lastRecipe != null && StagedRecipes.isStep(lastRecipe) && restoreSteps(lastRecipe)) {
            return Collections.singleton(stagedSteps.get(stagedIndex)).iterator();
        }
        // fresh craft: the first staged recipe whose first step matches
        for (GTRecipe root : StagedRecipes.allRecipes(machine.getRecipeType())) {
            if (!StagedRecipeData.isStaged(root)) continue;
            List<GTRecipe> steps = StagedRecipes.getSteps(root);
            if (steps.isEmpty()) continue;
            if (matchRecipe(steps.get(0)).isSuccess()) {
                return Collections.singleton(steps.get(0)).iterator();
            }
        }
        return Collections.emptyIterator();
    }

    @Override
    public void setupRecipe(GTRecipe recipe) {
        // a computed step carries the root's tag as a stamp: only a non-step is a root
        GTRecipe root = !StagedRecipes.isStep(recipe) && StagedRecipeData.isStaged(recipe) ? recipe : null;
        if (root == null && StagedRecipes.isStep(recipe)) {
            root = StagedRecipes.findRoot(machine.getRecipeType(), recipe.data.getString(StagedRecipes.KEY_ROOT));
        }
        if (root != null) {
            List<GTRecipe> steps = StagedRecipes.getSteps(root);
            if (!steps.isEmpty()) {
                stagedSteps = steps;
                stagedRootRecipe = root;
                stagedRoot = root.id == null ? "" : root.id.toString();
                stagedIndex = StagedRecipes.isStep(recipe)
                        ? Math.max(0, Math.min(steps.size() - 1, recipe.data.getInt(StagedRecipes.KEY_STAGE)))
                        : 0;
                if (!StagedRecipes.isStep(recipe)) {
                    // a fresh start names the root: run its first step (already modified, if it came that way)
                    recipe = stagedSteps.get(stagedIndex);
                }
                // otherwise the passed (possibly overclocked) step runs as-is
            } else {
                clearStaged();
            }
        } else {
            // not a staged recipe: this machine never yields one, but never carry stale steps
            clearStaged();
        }
        super.setupRecipe(recipe);
    }

    @Override
    public void onRecipeFinish() {
        machine.afterWorking();
        if (lastRecipe == null) return;

        boolean finishedLast = false;
        if (StagedRecipes.isStep(lastRecipe)) {
            if (stagedSteps.isEmpty() && !restoreSteps(lastRecipe)) {
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
            setStatus(Status.SUSPEND);
            consecutiveRecipes = 0;
            progress = 0;
            duration = 0;
            isActive = false;
            lastRecipe = null;
            return;
        }

        if (finishedLast) {
            // the craft is done: clear first, then offer the same craft again when its first step still matches
            craftsCompleted++;
            GTRecipe root = stagedRootRecipe;
            String rootId = stagedRoot;
            clearStaged();
            if (root == null) root = StagedRecipes.findRoot(machine.getRecipeType(), rootId);
            if (root != null) {
                List<GTRecipe> steps = StagedRecipes.getSteps(root);
                if (!steps.isEmpty()) {
                    GTRecipe retry = machine.fullModifyRecipe(steps.get(0));
                    if (retry != null && checkRecipe(retry).isSuccess()) {
                        setupRecipe(retry);
                        return;
                    }
                }
            }
        } else if (!stagedSteps.isEmpty()) {
            // the next step, modified like every other run (overclocks and all)
            GTRecipe next = machine.fullModifyRecipe(stagedSteps.get(stagedIndex));
            if (next != null && checkRecipe(next).isSuccess()) {
                setupRecipe(next);
                return;
            }
        }

        setStatus(Status.IDLE);
        lastRecipe = null;
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

    @Override
    public void interruptRecipe() {
        clearStaged();
        super.interruptRecipe();
    }

    @Override
    public void resetRecipeLogic() {
        clearStaged();
        super.resetRecipeLogic();
    }

    /** Rebuilds the in-memory sequence from a persisted step; false when its root recipe is gone. */
    private boolean restoreSteps(GTRecipe step) {
        GTRecipe root = StagedRecipes.findRoot(machine.getRecipeType(),
                step.data.getString(StagedRecipes.KEY_ROOT));
        if (root == null) {
            clearStaged();
            return false;
        }
        List<GTRecipe> steps = StagedRecipes.getSteps(root);
        if (steps.isEmpty()) {
            clearStaged();
            return false;
        }
        stagedSteps = steps;
        stagedRootRecipe = root;
        stagedRoot = root.id == null ? "" : root.id.toString();
        stagedIndex = Math.max(0, Math.min(steps.size() - 1, step.data.getInt(StagedRecipes.KEY_STAGE)));
        return true;
    }

    private void clearStaged() {
        stagedSteps = List.of();
        stagedRootRecipe = null;
        stagedRoot = "";
        stagedIndex = -1;
    }
}
