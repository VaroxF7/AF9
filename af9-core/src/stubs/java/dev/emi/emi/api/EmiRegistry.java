package dev.emi.emi.api;

import java.util.function.Predicate;

import dev.emi.emi.api.recipe.EmiRecipe;

/** Compile-time stand-in for EMI's registry (not shipped): only what AF9 Core calls. */
public interface EmiRegistry {

    void removeRecipes(Predicate<EmiRecipe> predicate);
}
