package com.af9.core.staged;

import com.af9.core.AF9Core;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.content.Content;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Turns an authored staged recipe into the per-step recipes the machine actually runs: step {@code i} needs the
 * global inputs plus its own layer's, carries its own duration, and only the last step has outputs. Every step is
 * stamped with its root id and stage index ({@link #KEY_ROOT}, {@link #KEY_STAGE}) so the logic can rebuild the
 * sequence after a reload from the persisted step alone.
 * <p>
 * Steps are computed once per recipe object and cached in a {@link WeakHashMap}: a datapack reload creates new
 * recipe objects, so stale entries simply stop being used and are collected.
 */
public final class StagedRecipes {

    /** Step marker in a computed step's {@code data}. */
    public static final String KEY_IS_STEP = "af9_is_step";
    /** The root recipe's id, stamped into every computed step. */
    public static final String KEY_ROOT = "af9_root";
    /** The 0-based stage index, stamped into every computed step. */
    public static final String KEY_STAGE = "af9_stage";

    private static final Map<GTRecipe, List<GTRecipe>> CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private StagedRecipes() {}

    public static boolean isStep(GTRecipe recipe) {
        return recipe != null && recipe.data != null && recipe.data.getBoolean(KEY_IS_STEP);
    }

    /** The steps of a staged root recipe, in order; empty when the recipe is not staged or is malformed. */
    public static List<GTRecipe> getSteps(GTRecipe root) {
        // computed steps carry the root's tag as a stamp: they are never roots themselves
        if (!StagedRecipeData.isStaged(root) || isStep(root)) return List.of();
        return CACHE.computeIfAbsent(root, StagedRecipes::computeSteps);
    }

    /** Every recipe of a type, whatever category KubeJS filed it under. */
    public static Set<GTRecipe> allRecipes(GTRecipeType type) {
        Set<GTRecipe> all = new LinkedHashSet<>();
        if (type == null) return all;
        for (Set<GTRecipe> set : type.getCategoryMap().values()) {
            if (set != null) all.addAll(set);
        }
        return all;
    }

    /** The staged root recipe with this id, or null. */
    public static GTRecipe findRoot(GTRecipeType type, String rootId) {
        if (rootId == null || rootId.isEmpty()) return null;
        for (GTRecipe recipe : allRecipes(type)) {
            if (recipe.id != null && rootId.equals(recipe.id.toString()) && StagedRecipeData.isStaged(recipe)) {
                return recipe;
            }
        }
        return null;
    }

    private static List<GTRecipe> computeSteps(GTRecipe root) {
        StagedRecipeData.Info info = StagedRecipeData.read(root.data);
        List<Content> items = root.inputs.getOrDefault(ItemRecipeCapability.CAP, List.of());
        List<Content> fluids = root.inputs.getOrDefault(FluidRecipeCapability.CAP, List.of());
        if (info.steps().isEmpty() || !indicesValid(info, items.size(), fluids.size())) {
            AF9Core.LOGGER.warn("Staged recipe {} has no usable layers, ignoring its staging",
                    root.id);
            return List.of();
        }
        List<GTRecipe> steps = new ArrayList<>();
        for (int i = 0; i < info.steps().size(); i++) {
            StagedRecipeData.Step layer = info.steps().get(i);
            GTRecipe step = root.copy();
            // copy() shares the data tag: stamp a private copy so the registry recipe stays clean
            step.data = root.data.copy();
            step.data.putBoolean(KEY_IS_STEP, true);
            step.data.putString(KEY_ROOT, root.id.toString());
            step.data.putInt(KEY_STAGE, i);
            step.setId(root.id.withSuffix("/stage" + (i + 1)));
            step.inputs.clear();
            putAll(step.inputs, ItemRecipeCapability.CAP, select(items, info.globalItems(), layer.items()));
            putAll(step.inputs, FluidRecipeCapability.CAP, select(fluids, info.globalFluids(), layer.fluids()));
            // any other input capability (none of the demo recipes have one) runs on every step
            for (var entry : root.inputs.entrySet()) {
                var cap = entry.getKey();
                if (cap != ItemRecipeCapability.CAP && cap != FluidRecipeCapability.CAP) {
                    step.inputs.put(cap, new ArrayList<>(entry.getValue()));
                }
            }
            // energy and every other tick input run on all steps; outputs only on the last one
            step.tickInputs.replaceAll((cap, contents) -> new ArrayList<>(contents));
            boolean last = i == info.steps().size() - 1;
            if (last) {
                step.outputs.replaceAll((cap, contents) -> new ArrayList<>(contents));
                step.tickOutputs.replaceAll((cap, contents) -> new ArrayList<>(contents));
            } else {
                step.outputs.clear();
                step.tickOutputs.clear();
            }
            step.duration = layer.duration() > 0 ? layer.duration() : root.duration;
            steps.add(step);
        }
        return List.copyOf(steps);
    }

    private static <T> void putAll(Map<RecipeCapability<?>, List<Content>> map,
            RecipeCapability<T> cap, List<Content> contents) {
        if (!contents.isEmpty()) map.put(cap, contents);
    }

    private static List<Content> select(List<Content> all, List<Integer> global, List<Integer> layer) {
        List<Content> out = new ArrayList<>();
        for (int index : global) out.add(all.get(index));
        for (int index : layer) out.add(all.get(index));
        return out;
    }

    private static boolean indicesValid(StagedRecipeData.Info info, int items, int fluids) {
        for (int index : info.globalItems()) if (index < 0 || index >= items) return false;
        for (int index : info.globalFluids()) if (index < 0 || index >= fluids) return false;
        for (StagedRecipeData.Step layer : info.steps()) {
            if (layer.items().isEmpty() && layer.fluids().isEmpty()) return false;
            for (int index : layer.items()) if (index < 0 || index >= items) return false;
            for (int index : layer.fluids()) if (index < 0 || index >= fluids) return false;
        }
        return true;
    }
}
