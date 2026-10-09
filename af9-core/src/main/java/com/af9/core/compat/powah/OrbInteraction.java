package com.af9.core.compat.powah;

import java.util.ArrayList;
import java.util.List;

import com.af9.core.AF9Core;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.registries.ForgeRegistries;
import owmii.powah.block.energizing.EnergizingOrbTile;
import owmii.powah.lib.logistics.inventory.Inventory;

/**
 * Putting counted ingredients into Powah's Energizing Orb by hand. Powah's orb takes one item per click; a recipe that wants
 * four steel rods would take four slots. With an item in hand that one of the orb's recipes takes more of than one, a click puts
 * that many into a slot (or tops up the slot that already holds it). Everything else is left to Powah: a click with an item
 * no counted recipe wants, an empty hand (which takes the contents out, one slot at a time) and a finished product waiting.
 */
public final class OrbInteraction {

    private static final ResourceLocation ENERGIZING = new ResourceLocation("powah", "energizing");
    /** The orb has the output in slot 0 and the inputs in slots 1 to 6. */
    private static final int FIRST_INPUT = 1;
    private static final int LAST_INPUT = 6;

    private OrbInteraction() {}

    /** Logs whether the orb's recipes take counts (the mixins apply when Powah's classes load). */
    public static void verify() {
        try {
            Class<?> recipe = Class.forName("owmii.powah.block.energizing.EnergizingRecipe");
            if (OrbCounts.class.isAssignableFrom(recipe)) {
                AF9Core.LOGGER.info("Powah's Energizing Orb takes counted ingredients");
            } else {
                AF9Core.LOGGER.warn("Powah's Energizing Orb recipes were not patched: counted ingredients count as one each");
            }
        } catch (ClassNotFoundException | LinkageError e) {
            AF9Core.LOGGER.warn("Powah's Energizing Orb class not found: {}", e.toString());
        }
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack held = event.getItemStack();
        if (held.isEmpty()) return;
        Level level = event.getLevel();
        BlockEntity entity = level.getBlockEntity(event.getPos());
        if (!(entity instanceof EnergizingOrbTile orb)) return;
        Inventory inventory = orb.getInventory();
        // a finished product waits in slot 0: Powah's own click takes it out
        if (!inventory.getStackInSlot(0).isEmpty()) return;
        Plan plan = plan(level, inventory, held);
        if (plan == null) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        if (level.isClientSide) return;
        ItemStack inserted = held.copy();
        inserted.setCount(plan.add);
        ItemStack current = inventory.getStackInSlot(plan.slot);
        if (current.isEmpty()) {
            inventory.setStackInSlot(plan.slot, inserted);
        } else {
            ItemStack grown = current.copy();
            grown.grow(plan.add);
            inventory.setStackInSlot(plan.slot, grown);
        }
        if (!event.getEntity().getAbilities().instabuild) held.shrink(plan.add);
    }

    /** Which slot gets how many items of the held stack. */
    private record Plan(int slot, int add, int required) {}

    /**
     * The best way to put the held stack into the orb: over all of the orb's recipes that its filled slots still agree with, the
     * insert that takes the most. Null when no counted recipe wants more than one of it (then Powah's click is right).
     */
    private static Plan plan(Level level, Inventory inventory, ItemStack held) {
        RecipeType<?> type = ForgeRegistries.RECIPE_TYPES.getValue(ENERGIZING);
        if (type == null) return null;
        Plan best = null;
        for (Recipe<?> recipe : level.getRecipeManager().getRecipes()) {
            if (recipe.getType() != type || !(recipe instanceof OrbCounts counted)) continue;
            Plan plan = planFor(recipe.getIngredients(), counted.af9Counts(), inventory, held);
            if (plan != null && (best == null || plan.add > best.add)) best = plan;
        }
        return best;
    }

    private static Plan planFor(List<Ingredient> ingredients, int[] counts, Inventory inventory, ItemStack held) {
        List<Integer> free = new ArrayList<>();
        for (int i = 0; i < ingredients.size(); i++) free.add(i);
        int topUpSlot = -1;
        int topUpIndex = -1;
        // every filled slot has to be one of the recipe's ingredients
        for (int slot = FIRST_INPUT; slot <= LAST_INPUT; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            int found = -1;
            for (int k = 0; k < free.size() && found < 0; k++) {
                if (ingredients.get(free.get(k)).test(stack)) found = k;
            }
            if (found < 0) return null;
            int index = free.remove(found);
            if (topUpSlot < 0 && ItemStack.isSameItemSameTags(stack, held) &&
                    stack.getCount() < OrbRecipes.required(counts, index)) {
                topUpSlot = slot;
                topUpIndex = index;
            }
        }
        if (topUpSlot >= 0) {
            int missing = OrbRecipes.required(counts, topUpIndex) - inventory.getStackInSlot(topUpSlot).getCount();
            int add = Math.min(Math.min(missing, held.getCount()),
                    held.getMaxStackSize() - inventory.getStackInSlot(topUpSlot).getCount());
            return add > 0 ? new Plan(topUpSlot, add, OrbRecipes.required(counts, topUpIndex)) : null;
        }
        int emptySlot = -1;
        for (int slot = FIRST_INPUT; slot <= LAST_INPUT && emptySlot < 0; slot++) {
            if (inventory.getStackInSlot(slot).isEmpty()) emptySlot = slot;
        }
        if (emptySlot < 0) return null;
        for (int index : free) {
            if (!ingredients.get(index).test(held)) continue;
            int required = OrbRecipes.required(counts, index);
            // one item is Powah's own click
            return required > 1 ? new Plan(emptySlot, Math.min(required, held.getCount()), required) : null;
        }
        return null;
    }
}
