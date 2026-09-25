package com.af9.core.compat.emi;

import com.af9.core.litho.LithoMode;

import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.registries.ForgeRegistries;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Lists every mode's wafer package in EMI, right after the plain package item, so players can click it and see the
 * mode recipe that prints it and the cutter recipe that dices it.
 * <p>
 * Only packages: EMI compares them by the printing node (the version and transistor count differ with the line that
 * printed them). Chips keep EMI's default (NBT ignored), because GT's circuit recipes take plain chips and a
 * tag-aware comparison would hide those usages for printed chips.
 */
@EmiEntrypoint
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public class AF9EmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        // each mode's package becomes its own entry; version, transistors and any other NBT are ignored
        Comparison byNode = Comparison.compareData(stack -> {
            CompoundTag tag = stack.getNbt();
            return tag != null && tag.contains(LithoMode.TAG, CompoundTag.TAG_COMPOUND) ?
                    Integer.valueOf(tag.getCompound(LithoMode.TAG).getInt(LithoMode.TAG_NODE)) : null;
        });

        // per package item, the entry the next variant goes after (starts at the plain package)
        Map<Item, EmiStack> insertAfter = new HashMap<>();
        for (String path : LithoMode.PACKAGES) {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("kubejs", path));
            if (item == null || item == Items.AIR) continue;
            registry.setDefaultComparison(item, byNode);
            insertAfter.put(item, EmiStack.of(item));
        }

        // take the variants from the recipes that print them, so they always match exactly; modes in order
        RecipeManager recipes = registry.getRecipeManager();
        // a recipe lists its package twice (guaranteed + bonus), add each item/mode once
        Set<String> added = new HashSet<>();
        for (LithoMode mode : LithoMode.values()) {
            GTRecipeType type = GTRegistries.RECIPE_TYPES.get(new ResourceLocation("gtceu", mode.recipeTypeId()));
            if (type == null) continue;
            for (GTRecipe recipe : recipes.getAllRecipesFor(type)) {
                for (Content content : recipe.getOutputContents(ItemRecipeCapability.CAP)) {
                    if (!(content.content instanceof Ingredient ingredient)) continue;
                    for (ItemStack stack : ingredient.getItems()) {
                        EmiStack previous = insertAfter.get(stack.getItem());
                        LithoMode printed = LithoMode.fromStack(stack);
                        if (previous == null || printed == null) continue;
                        if (!added.add(ForgeRegistries.ITEMS.getKey(stack.getItem()) + "/" + printed.id)) continue;
                        EmiStack variant = EmiStack.of(stack.copyWithCount(1));
                        registry.addEmiStackAfter(variant, previous);
                        insertAfter.put(stack.getItem(), variant);
                    }
                }
            }
        }
    }
}
