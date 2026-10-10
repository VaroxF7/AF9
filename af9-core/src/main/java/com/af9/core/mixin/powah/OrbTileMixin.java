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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import owmii.powah.block.energizing.EnergizingRecipe;
import owmii.powah.lib.logistics.energy.Energy;
import owmii.powah.lib.logistics.inventory.Inventory;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.af9.core.compat.powah.OrbMk2Tile;

/**
 * The Energizing Orb with counted ingredients: its input slots hold a stack (Powah's slot limit is one), and a finished craft
 * takes the recipe's counts out of them instead of emptying the orb.
 */
@Pseudo
@Mixin(targets = "owmii.powah.block.energizing.EnergizingOrbTile", remap = false)
public abstract class OrbTileMixin {

    @Shadow
    private EnergizingRecipe recipe;
    @Shadow
    private Energy buffer;
    @Shadow
    private boolean containRecipe;

    @Shadow
    private void checkRecipe() {}

    /** Set when a craft finished inside fillEnergy: Powah zeroes the energy buffer after it, so the orb looks again. */
    @Unique
    private boolean af9$crafted;

    /** A recipe with counted ingredients runs in the Mk2 only: the plain orb drops it again after its own look for a recipe. */
    @Inject(method = "checkRecipe()V", at = @At("TAIL"), remap = false)
    private void af9$countedOnlyInMk2(CallbackInfo ci) {
        if (recipe == null || !containRecipe || (Object) this instanceof OrbMk2Tile) return;
        if (!((OrbCounts) (Object) recipe).af9Counted()) return;
        buffer.setCapacity(0L);
        buffer.setStored(0L);
        buffer.setTransfer(0L);
        containRecipe = false;
        recipe = null;
    }

    @Inject(method = "getSlotLimit(I)I", at = @At("HEAD"), cancellable = true, remap = false)
    private void af9$stackSlots(int index, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(64);
    }

    /** The finished product joins the output slot instead of replacing it: crafts stack up to 64 until collected. */
    @Redirect(method = "fillEnergy(J)J", at = @At(value = "INVOKE",
            target = "Lowmii/powah/lib/logistics/inventory/Inventory;setStackInSlot(ILnet/minecraft/world/item/ItemStack;)V"),
            remap = false)
    private void af9$stackProduct(Inventory inventory, int slot, ItemStack result) {
        af9$crafted = true;
        inventory.setStackInSlot(slot, slot == 0 ? OrbRecipes.addProduct(inventory.getStackInSlot(0), result) : result);
    }

    /**
     * Powah ends a craft by zeroing the energy buffer, after the inventory change already looked for the next recipe. With
     * products stacking in the output slot the same inputs can match again, so the buffer is set up for it here: without
     * this the orb sat at no capacity after its first craft and did nothing more.
     */
    @Inject(method = "fillEnergy(J)J", at = @At("TAIL"), remap = false)
    private void af9$nextCraft(long amount, CallbackInfoReturnable<Long> cir) {
        if (!af9$crafted) return;
        af9$crafted = false;
        checkRecipe();
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
        boolean[] nc = ((OrbCounts) (Object) recipe).af9Nc();
        ItemStack[] slots = new ItemStack[inventory.getSlots()];
        for (int i = 0; i < slots.length; i++) slots[i] = inventory.getStackInSlot(i);
        ItemStack[] rest = OrbRecipes.consume(ingredients, counts, nc, slots);
        // one change notice for all of it (Powah puts the product into slot 0 right after, which checks the orb again)
        inventory.setSendUpdates(false);
        for (int i = 0; i < rest.length; i++) inventory.setStackInSlot(i, rest[i]);
        inventory.setSendUpdates(true);
    }
}
