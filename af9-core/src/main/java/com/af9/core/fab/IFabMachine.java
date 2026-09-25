package com.af9.core.fab;

import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

/**
 * What the fab consoles and {@link FabModifiers} need from a fab machine, single-block or multiblock.
 */
public interface IFabMachine extends IRecipeLogicMachine {

    int MAX_MODES = 4;

    FabFamily getFabFamily();

    /** Id of the last finished recipe, "" if none yet. A different recipe next means a changeover purge. */
    String getLastProduct();

    /** True while the running recipe is a changeover (it carries the purge). */
    boolean isPurgeRun();

    /** Called by {@link FabRecipeLogic} right before GT handles the outputs and looks for the next run. */
    void onFabRecipeFinished(GTRecipe recipe);

    boolean isFabFormed();

    long getFabAvailableEUt();

    /** Parallel the structure (or last run) gives, 1 for single blocks. */
    int getFabParallel();

    /** 0 = no clean environment, 1 = cleanroom (ISO 5), 2 = sterile / ULPA filters (ISO 3). */
    int getFabCleanClass();

    /** Working temperature in K (coils or the single block's limit), 0 where heat does not matter. */
    int getFabHeat();

    /** Finished runs per machine mode (index = recipe type order). */
    long[] getFabCounters();

    long getFabPurges();

    boolean hasFabMaintenanceProblems();

    default boolean needsPurge(GTRecipe recipe) {
        String last = getLastProduct();
        return recipe.id != null && !last.isEmpty() && !last.equals(recipe.id.toString());
    }

    /** Switches machine mode the way GT's mode tab does and drops the recipe cached for the old one. */
    default void selectFabMode(int index) {
        if (index < 0 || index >= getRecipeTypes().length || index == getActiveRecipeType()) return;
        setActiveRecipeType(index);
        getRecipeLogic().updateTickSubscription();
        getRecipeLogic().markLastRecipeDirty();
    }
}
