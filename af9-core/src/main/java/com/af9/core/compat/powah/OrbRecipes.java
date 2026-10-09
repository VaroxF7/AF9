package com.af9.core.compat.powah;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Counted ingredients for Powah's Energizing Orb. Powah's orb takes one item per slot and one ingredient per item; a
 * recipe of this pack may say {@code {"item": "gtceu:steel_rod", "count": 4}} and the orb then wants four of them in a
 * slot (up to a stack). All of it is plain logic on stacks and ingredients: the mixins in {@code com.af9.core.mixin.powah}
 * and {@link OrbInteraction} call it.
 */
public final class OrbRecipes {

    private static final int MAX_COUNT = 64;

    private OrbRecipes() {}

    /** The counts of a recipe's {@code ingredients} array, in the order of the ingredients Powah keeps (it drops empty ones). */
    public static int[] readCounts(JsonArray elements) {
        List<Integer> counts = new ArrayList<>();
        for (JsonElement element : elements) {
            if (Ingredient.fromJson(element).isEmpty()) continue;
            counts.add(countOf(element));
        }
        int[] out = new int[counts.size()];
        for (int i = 0; i < out.length; i++) out[i] = counts.get(i);
        return out;
    }

    private static int countOf(JsonElement element) {
        JsonObject object = element.isJsonObject() ? element.getAsJsonObject() : null;
        if (object == null && element.isJsonArray() && !element.getAsJsonArray().isEmpty() &&
                element.getAsJsonArray().get(0).isJsonObject()) {
            object = element.getAsJsonArray().get(0).getAsJsonObject();
        }
        if (object == null || !object.has("count")) return 1;
        return Math.max(1, Math.min(MAX_COUNT, object.get("count").getAsInt()));
    }

    /** How many items ingredient {@code index} takes. */
    public static int required(int[] counts, int index) {
        return counts != null && index >= 0 && index < counts.length ? Math.max(1, counts[index]) : 1;
    }

    /**
     * The orb's contents against a recipe: every filled input slot (1..) takes one ingredient it matches and holds at least
     * that ingredient's count, every ingredient is taken, and nothing is in the output slot (0: a finished product waits to be
     * collected). Powah's own check, with the counts.
     */
    public static boolean matches(List<Ingredient> ingredients, int[] counts, Container inv) {
        if (inv.getContainerSize() > 0 && !inv.getItem(0).isEmpty()) return false;
        List<Integer> free = new ArrayList<>();
        for (int i = 0; i < ingredients.size(); i++) free.add(i);
        for (int slot = 1; slot < inv.getContainerSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack.isEmpty()) continue;
            if (take(ingredients, counts, free, stack) < 0) return false;
        }
        return free.isEmpty();
    }

    /**
     * What stays in the orb's slots after a craft: each filled input slot gives up its ingredient's count (a hopper may have
     * filled a slot with more). Index 0 is left to the caller (the product goes there).
     */
    public static ItemStack[] consume(List<Ingredient> ingredients, int[] counts, ItemStack[] slots) {
        ItemStack[] out = new ItemStack[slots.length];
        List<Integer> free = new ArrayList<>();
        for (int i = 0; i < ingredients.size(); i++) free.add(i);
        for (int slot = 0; slot < slots.length; slot++) {
            ItemStack stack = slots[slot];
            out[slot] = ItemStack.EMPTY;
            if (slot == 0 || stack.isEmpty()) continue;
            int index = take(ingredients, counts, free, stack);
            if (index < 0) continue;
            ItemStack rest = stack.copy();
            rest.shrink(required(counts, index));
            out[slot] = rest.isEmpty() ? ItemStack.EMPTY : rest;
        }
        return out;
    }

    /** Removes from {@code free} the first ingredient the stack satisfies and returns its index; -1 for none. */
    private static int take(List<Ingredient> ingredients, int[] counts, List<Integer> free, ItemStack stack) {
        for (int k = 0; k < free.size(); k++) {
            int index = free.get(k);
            if (ingredients.get(index).test(stack) && stack.getCount() >= required(counts, index)) {
                free.remove(k);
                return index;
            }
        }
        return -1;
    }

    /** A copy of the ingredient whose stacks carry the count (what a recipe viewer shows). Only for concrete, client-side lists. */
    public static Ingredient withCount(Ingredient ingredient, int count) {
        if (count <= 1) return ingredient;
        ItemStack[] items = ingredient.getItems();
        if (items.length == 0) return ingredient;
        ItemStack[] counted = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) {
            counted[i] = items[i].copy();
            counted[i].setCount(Math.min(count, Math.max(1, counted[i].getMaxStackSize())));
        }
        return Ingredient.of(counted);
    }
}
