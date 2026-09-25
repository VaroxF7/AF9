package com.af9.core.machine;

import com.af9.core.litho.LithoMode;

import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

/**
 * Recipe logic of the lithography machines: when a print finishes it rolls the break chance (see
 * {@link LithoMachine#finishPrint}); a broken print puts out the broken wafer instead.
 */
public class LithoRecipeLogic extends RecipeLogic {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(LithoRecipeLogic.class,
            RecipeLogic.MANAGED_FIELD_HOLDER);

    private final LithoMachine litho;

    public LithoRecipeLogic(LithoMachine litho) {
        super(litho);
        this.litho = litho;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onRecipeFinish() {
        // decide before super.onRecipeFinish() puts out lastRecipe's outputs; the machine re-modifies the next run
        // from the original recipe (alwaysTryModifyRecipe), so the swap only affects this one
        GTRecipe finished = lastRecipe;
        if (finished != null) {
            LithoMode mode = LithoMode.of(finished.recipeType);
            if (mode != null && litho.finishPrint(mode)) {
                lastRecipe = LithoMachine.asBroken(finished, mode);
            }
        }
        super.onRecipeFinish();
    }
}
