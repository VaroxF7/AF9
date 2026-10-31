package com.af9.core.distillation;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.content.Content;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The recipes a Distillation Tower Module can be set to, and the ID the player finds each one by: the name of what it mainly makes
 * and a number, the recipe's place in the list of its type ("Naphtha 14"). The list of a type is its recipes in the order of their
 * ids, so the number is the same on the module's screen and on the recipe viewer's page, and on the client and the server.
 */
public final class TowerRecipes {

    private static final Map<RecipeManager, Map<GTRecipeType, List<GTRecipe>>> CACHE = new WeakHashMap<>();

    /** The recipe's ID in its data, for the recipe viewers' page (a line of the page's data info, see FabModes.towerIdLine). */
    public static final String ID_TAG = "af9_recipe_id";

    private TowerRecipes() {}

    /**
     * Writes the ID of every recipe of the types into its data (the recipe viewers' pages read it from there, client side), in the
     * manager's recipes and in the ones the types list for the viewers.
     */
    public static synchronized void tagIds(RecipeManager manager, GTRecipeType[] types) {
        CACHE.remove(manager);
        for (GTRecipeType type : types) {
            List<GTRecipe> list = of(manager, type);
            Map<ResourceLocation, String> ids = new java.util.HashMap<>();
            for (int n = 0; n < list.size(); n++) ids.put(list.get(n).id, nameOf(list.get(n)) + " " + (n + 1));
            for (GTRecipe recipe : list) recipe.data.putString(ID_TAG, ids.get(recipe.id));
            for (var category : type.getCategories()) {
                for (GTRecipe recipe : type.getRecipesInCategory(category)) {
                    String id = recipe.id == null ? null : ids.get(recipe.id);
                    if (id != null) recipe.data.putString(ID_TAG, id);
                }
            }
        }
    }

    /** The recipes of the type, by id (those without an id are left out: they cannot be set). */
    public static synchronized List<GTRecipe> of(RecipeManager manager, GTRecipeType type) {
        return CACHE.computeIfAbsent(manager, m -> new java.util.HashMap<>()).computeIfAbsent(type, t -> {
            List<GTRecipe> list = new ArrayList<>();
            for (GTRecipe recipe : manager.getAllRecipesFor(t)) {
                if (recipe.id != null) list.add(recipe);
            }
            list.sort(Comparator.comparing(recipe -> recipe.id.toString()));
            return list;
        });
    }

    /** All the recipes of the tower's types, a type after the other. */
    public static List<GTRecipe> all(RecipeManager manager, GTRecipeType[] types) {
        List<GTRecipe> all = new ArrayList<>();
        for (GTRecipeType type : types) all.addAll(of(manager, type));
        return all;
    }

    /** The recipe's ID: what it mainly makes and its number in its type, "Naphtha 14"; empty if it has no id. */
    public static String idOf(RecipeManager manager, GTRecipeType type, ResourceLocation id) {
        List<GTRecipe> list = of(manager, type);
        for (int n = 0; n < list.size(); n++) {
            if (list.get(n).id.equals(id)) return nameOf(list.get(n)) + " " + (n + 1);
        }
        return "";
    }

    /** The first fluid the recipe makes, else its first item, else the last part of its id. */
    public static String nameOf(GTRecipe recipe) {
        for (Content content : recipe.outputs.getOrDefault(FluidRecipeCapability.CAP, List.of())) {
            FluidStack[] stacks = FluidRecipeCapability.CAP.of(content.getContent()).getStacks();
            if (stacks.length > 0 && !stacks[0].isEmpty()) return stacks[0].getDisplayName().getString();
        }
        for (Content content : recipe.outputs.getOrDefault(ItemRecipeCapability.CAP, List.of())) {
            ItemStack[] stacks = ItemRecipeCapability.CAP.of(content.getContent()).getItems();
            if (stacks.length > 0 && !stacks[0].isEmpty()) return stacks[0].getHoverName().getString();
        }
        String path = recipe.id == null ? "recipe" : recipe.id.getPath();
        return path.substring(path.lastIndexOf('/') + 1).replace('_', ' ');
    }
}
