package com.af9.core.mixin.powah;

import java.util.List;

import com.af9.core.compat.powah.OrbCounts;
import com.af9.core.compat.powah.OrbRecipes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import owmii.powah.block.energizing.EnergizingRecipe;
import owmii.powah.lib.logistics.inventory.Inventory;

/**
 * The Energizing Orb with counted ingredients: its input slots hold a stack (Powah's slot limit is one), and a finished craft
 * takes the recipe's counts out of them instead of emptying the orb.
 */
@Pseudo
@Mixin(targets = "owmii.powah.block.energizing.EnergizingOrbTile", remap = false)
public abstract class OrbTileMixin {

    @Shadow
    private EnergizingRecipe recipe;

    @Inject(method = "getSlotLimit(I)I", at = @At("HEAD"), cancellable = true, remap = false)
    private void af9$stackSlots(int index, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(64);
    }

    @Redirect(method = "fillEnergy(J)J", at = @At(value = "INVOKE",
            target = "Lowmii/powah/lib/logistics/inventory/Inventory;clear()V"), remap = false)
    private void af9$takeCounts(Inventory inventory) {
        if (recipe == null) {
            inventory.clear();
            return;
        }
        List<Ingredient> ingredients = ((Recipe<?>) (Object) recipe).getIngredients();
        int[] counts = ((OrbCounts) (Object) recipe).af9Counts();
        ItemStack[] slots = new ItemStack[inventory.getSlots()];
        for (int i = 0; i < slots.length; i++) slots[i] = inventory.getStackInSlot(i);
        ItemStack[] rest = OrbRecipes.consume(ingredients, counts, slots);
        // one change notice for all of it (Powah puts the product into slot 0 right after, which checks the orb again)
        inventory.setSendUpdates(false);
        for (int i = 0; i < rest.length; i++) inventory.setStackInSlot(i, rest[i]);
        inventory.setSendUpdates(true);
    }
}
