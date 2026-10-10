package mezz.jei.api.registration;

import mezz.jei.api.recipe.RecipeType;
import net.minecraft.world.item.ItemStack;

/** Compile-time stand-in for JEI's interface (not shipped). */
public interface IRecipeCatalystRegistration {

    void addRecipeCatalyst(ItemStack catalyst, RecipeType<?>... recipeTypes);
}
