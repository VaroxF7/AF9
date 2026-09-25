package com.af9.core.fab;

import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

/**
 * Tells the machine which recipe just finished before GT re-modifies it for the next run, so the changeover purge
 * is applied to the first run of a new product only (the machines return true from alwaysTryModifyRecipe).
 */
public class FabRecipeLogic extends RecipeLogic {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(FabRecipeLogic.class,
            RecipeLogic.MANAGED_FIELD_HOLDER);

    private final IFabMachine fab;

    public FabRecipeLogic(IFabMachine machine) {
        super(machine);
        this.fab = machine;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onRecipeFinish() {
        GTRecipe finished = lastRecipe;
        if (finished != null) {
            fab.onFabRecipeFinished(finished);
        }
        super.onRecipeFinish();
    }
}
