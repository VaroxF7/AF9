package com.af9.core.mixin.powah;

import java.util.List;

import com.af9.core.compat.powah.OrbCounts;
import com.af9.core.compat.powah.OrbRecipes;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import owmii.powah.lib.logistics.inventory.RecipeWrapper;

/**
 * Powah's Energizing Orb recipe with counted ingredients: it carries the counts ({@link OrbCounts}) and matches the orb's
 * slots against them. Powah's method keeps its own name (it specialises the recipe's container type), so it is
 * {@code matches} in the game; the target is Powah's class by name, without Powah on the compile path (stubs only).
 */
@Pseudo
@Mixin(targets = "owmii.powah.block.energizing.EnergizingRecipe", remap = false)
public abstract class OrbRecipeMixin implements OrbCounts {

    @Unique
    private int[] af9$counts = new int[0];

    @Override
    public int[] af9Counts() {
        return af9$counts;
    }

    @Override
    public void af9SetCounts(int[] counts) {
        af9$counts = counts;
    }

    @Inject(method = "matches(Lowmii/powah/lib/logistics/inventory/RecipeWrapper;Lnet/minecraft/world/level/Level;)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void af9$matches(RecipeWrapper inv, Level level, CallbackInfoReturnable<Boolean> cir) {
        List<Ingredient> ingredients = ((Recipe<?>) (Object) this).getIngredients();
        cir.setReturnValue(OrbRecipes.matches(ingredients, af9$counts, inv));
    }
}
