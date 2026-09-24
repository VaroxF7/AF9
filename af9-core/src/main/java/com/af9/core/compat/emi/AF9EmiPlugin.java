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
import java.util.Map;

/**
 * Lists every lithographed wafer variant in EMI, right after its plain GT wafer, so players can click it and see the
 * mode recipe that prints it and the cutter recipe that dices it.
 * <p>
 * Only wafers: EMI compares them by the {@link LithoMode#TAG} tag. Chips keep EMI's default (NBT ignored), because
 * GT's circuit recipes take plain chips and a tag-aware comparison would hide those usages for printed chips.
 */
@EmiEntrypoint
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public class AF9EmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        // each mode's wafer becomes its own entry; any other NBT is still ignored, like EMI's default
        Comparison byLithoTag = Comparison.compareData(stack -> {
            CompoundTag tag = stack.getNbt();
            return tag != null && tag.contains(LithoMode.TAG, CompoundTag.TAG_COMPOUND) ? tag.getCompound(LithoMode.TAG) :
                    null;
        });

        // per wafer item, the entry the next variant goes after (starts at the plain wafer)
        Map<Item, EmiStack> insertAfter = new HashMap<>();
        for (String path : LithoMode.WAFERS) {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("gtceu", path));
            if (item == null || item == Items.AIR) continue;
            registry.setDefaultComparison(item, byLithoTag);
            insertAfter.put(item, EmiStack.of(item));
        }

        // take the variants from the recipes that print them, so they always match exactly; modes in order
        RecipeManager recipes = registry.getRecipeManager();
        for (LithoMode mode : LithoMode.values()) {
            GTRecipeType type = GTRegistries.RECIPE_TYPES.get(new ResourceLocation("gtceu", mode.recipeTypeId()));
            if (type == null) continue;
            for (GTRecipe recipe : recipes.getAllRecipesFor(type)) {
                for (Content content : recipe.getOutputContents(ItemRecipeCapability.CAP)) {
                    if (!(content.content instanceof Ingredient ingredient)) continue;
                    for (ItemStack stack : ingredient.getItems()) {
                        EmiStack previous = insertAfter.get(stack.getItem());
                        if (previous == null || LithoMode.getLithoTag(stack) == null) continue;
                        EmiStack variant = EmiStack.of(stack.copyWithCount(1));
                        registry.addEmiStackAfter(variant, previous);
                        insertAfter.put(stack.getItem(), variant);
                    }
                }
            }
        }
    }
}
