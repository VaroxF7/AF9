package com.af9.core.compat.jei;

import com.af9.core.AF9Core;
import com.af9.core.distillation.TowerRecipes;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.category.GTRecipeCategory;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.integration.jei.recipe.GTRecipeJEICategory;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.category.extensions.IRecipeCategoryDecorator;
import mezz.jei.api.registration.IAdvancedRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.List;

/**
 * JEI's page of a recipe the Distillation Tower Module can be set to shows the recipe's ID at its foot, the name of what it mainly
 * makes and a number ("ID: Naphtha 14"): the one the module's selector shows, so a player finds the recipe on the selector by it.
 * JEI loads this class only when it is installed.
 */
@JeiPlugin
public class DistillationJeiPlugin implements IModPlugin {

    @Override
    @SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(AF9Core.MOD_ID, "distillation_tower");
    }

    @Override
    public void registerAdvanced(IAdvancedRegistration registration) {
        var definition = GTRegistries.MACHINES.get(new ResourceLocation("gtceu", "distillation_module"));
        if (definition == null) return;
        List<GTRecipeType> towerTypes = Arrays.asList(definition.getRecipeTypes());
        for (GTRecipeCategory category : GTRegistries.RECIPE_CATEGORIES) {
            if (!category.shouldRegisterDisplays() || !towerTypes.contains(category.getRecipeType())) continue;
            registration.addRecipeCategoryDecorator(GTRecipeJEICategory.TYPES.apply(category), new IdLine());
        }
    }

    private static final class IdLine implements IRecipeCategoryDecorator<GTRecipe> {

        @Override
        public void draw(GTRecipe recipe, IRecipeCategory<GTRecipe> category, IRecipeSlotsView slots, GuiGraphics graphics,
                         double mouseX, double mouseY) {
            Minecraft minecraft = Minecraft.getInstance();
            if (recipe.id == null || minecraft.level == null) return;
            String id = TowerRecipes.idOf(minecraft.level.getRecipeManager(), recipe.recipeType, recipe.id);
            if (id.isEmpty()) return;
            graphics.drawString(minecraft.font, Component.translatable("af9.distillation.module.recipe_id", id), 3,
                    category.getHeight() - 10, -1, true);
        }
    }
}
