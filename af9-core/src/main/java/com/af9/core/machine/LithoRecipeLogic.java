package com.af9.core.machine;

import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

/**
 * Recipe logic that reports every finished print back to the line, so it can count wafer packages per mode.
 */
public class LithoRecipeLogic extends RecipeLogic {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(LithoRecipeLogic.class,
            RecipeLogic.MANAGED_FIELD_HOLDER);

    private final PhotolithographyLineMachine line;

    public LithoRecipeLogic(PhotolithographyLineMachine line) {
        super(line);
        this.line = line;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onRecipeFinish() {
        // capture before super.onRecipeFinish() can replace it with the next recipe
        GTRecipe finished = lastOriginRecipe;
        super.onRecipeFinish();
        if (finished != null) {
            line.recordPrinted(finished);
        }
    }
}
