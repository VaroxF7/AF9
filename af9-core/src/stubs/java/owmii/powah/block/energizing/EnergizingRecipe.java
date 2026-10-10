package owmii.powah.block.energizing;

import owmii.powah.lib.logistics.inventory.RecipeWrapper;

/** Compile-time stand-in for Powah's class (not shipped). */
public abstract class EnergizingRecipe implements net.minecraft.world.item.crafting.Recipe<RecipeWrapper> {

    public abstract long getEnergy();
}
