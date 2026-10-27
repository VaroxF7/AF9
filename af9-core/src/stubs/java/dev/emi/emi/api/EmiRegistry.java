package dev.emi.emi.api;

import java.util.function.Predicate;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/** Compile-time stand-in for EMI's registry (not shipped): only what AF9 Core calls. */
public interface EmiRegistry {

    void removeRecipes(Predicate<EmiRecipe> predicate);

    <T extends AbstractContainerMenu> void addRecipeHandler(MenuType<T> type, EmiRecipeHandler<T> handler);
}
