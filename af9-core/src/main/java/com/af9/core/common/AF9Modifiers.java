package com.af9.core.common;

import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;

/**
 * Recipe modifiers shared by the AF9 multiblocks (used from the KubeJS startup scripts).
 */
public final class AF9Modifiers {

    private AF9Modifiers() {}

    /** Only starts a recipe when the hatches can supply its full EU/t (see {@link IPowerGated}). */
    public static final RecipeModifier POWER_GATE = (machine, recipe) -> {
        if (!(machine instanceof IPowerGated gated)) {
            return RecipeModifier.nullWrongType(IPowerGated.class, machine);
        }
        if (gated.getAvailableEUt() < RecipeHelper.getRealEUt(recipe).getTotalEU()) return ModifierFunction.NULL;
        return ModifierFunction.IDENTITY;
    };
}
