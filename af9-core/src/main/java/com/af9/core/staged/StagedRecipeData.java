package com.af9.core.staged;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * The shape of a staged (multi-step) recipe, as authored from KubeJS: the recipe carries every step's inputs in its
 * normal input lists, and this tag says which content index belongs to which step. Per-step tick inputs are not
 * supported (energy and other tick inputs run on every step); outputs are only ever awarded after the last step.
 * <p>
 * There is deliberately no timeout: Star Technology's fork declares one per layer but never enforces it, and AF9 v1
 * drops it.
 *
 * <pre>
 * af9_staged: {
 *   layers: [ { duration: 200, items: [0, 1], fluids: [] }, ... ],
 *   globalItems: [4],
 *   globalFluids: [0]
 * }
 * </pre>
 *
 * {@code items} / {@code fluids} are indices into the recipe's merged item / fluid input lists; the global lists are
 * required by every step (inputs declared outside the layer list, like the energy or a catalyst fluid).
 */
public final class StagedRecipeData {

    /** The {@code data} key that marks a recipe as staged. */
    public static final String KEY = "af9_staged";

    /** One authored layer: its own duration and its indices into the merged input lists. */
    public record Step(int duration, List<Integer> items, List<Integer> fluids) {}

    /** The whole authored staging: layers plus the globals every step needs. */
    public record Info(List<Step> steps, List<Integer> globalItems, List<Integer> globalFluids) {}

    private StagedRecipeData() {}

    public static boolean isStaged(GTRecipe recipe) {
        return recipe != null && recipe.data != null && recipe.data.contains(KEY);
    }

    /** Reads the staging info; empty steps when the tag is missing or malformed. */
    public static Info read(CompoundTag data) {
        List<Step> steps = new ArrayList<>();
        List<Integer> globalItems = new ArrayList<>();
        List<Integer> globalFluids = new ArrayList<>();
        if (data == null || !data.contains(KEY, Tag.TAG_COMPOUND)) {
            return new Info(steps, globalItems, globalFluids);
        }
        CompoundTag root = data.getCompound(KEY);
        ListTag layers = root.getList("layers", Tag.TAG_COMPOUND);
        for (int i = 0; i < layers.size(); i++) {
            CompoundTag layer = layers.getCompound(i);
            steps.add(new Step(layer.getInt("duration"), ints(layer, "items"), ints(layer, "fluids")));
        }
        globalItems.addAll(ints(root, "globalItems"));
        globalFluids.addAll(ints(root, "globalFluids"));
        return new Info(steps, globalItems, globalFluids);
    }

    /** Writes the tag the KubeJS helper attaches with {@code addData}; also used by the docs example. */
    public static CompoundTag write(List<Step> steps, List<Integer> globalItems, List<Integer> globalFluids) {
        CompoundTag root = new CompoundTag();
        ListTag layers = new ListTag();
        for (Step step : steps) {
            CompoundTag layer = new CompoundTag();
            layer.putInt("duration", step.duration());
            layer.put("items", ints(step.items()));
            layer.put("fluids", ints(step.fluids()));
            layers.add(layer);
        }
        root.put("layers", layers);
        root.put("globalItems", ints(globalItems));
        root.put("globalFluids", ints(globalFluids));
        return root;
    }

    /**
     * The numbers under a key, whatever list they arrive as: a recipe goes through JSON on its way from KubeJS, and
     * that turns a list of small numbers into a byte array (and one of larger numbers into another kind).
     */
    private static List<Integer> ints(CompoundTag tag, String key) {
        List<Integer> out = new ArrayList<>();
        if (tag.get(key) instanceof CollectionTag<?> list) {
            for (Tag value : list) {
                if (value instanceof NumericTag number) out.add(number.getAsInt());
            }
        }
        return out;
    }

    private static ListTag ints(List<Integer> values) {
        ListTag out = new ListTag();
        for (int value : values) out.add(net.minecraft.nbt.IntTag.valueOf(value));
        return out;
    }
}
