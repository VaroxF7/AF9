package com.af9.core.compat.jei;

import com.af9.core.AF9Core;
import com.af9.core.distillation.DistillationModuleMachine;
import com.af9.core.network.AF9Network;
import com.af9.core.network.PinTowerRecipePacket;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.category.GTRecipeCategory;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.integration.jei.recipe.GTRecipeJEICategory;

import com.lowdragmc.lowdraglib.gui.modular.ModularUIContainer;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * The recipe viewer's "+" for the Distillation Tower Module with JEI: with a tower's screen open, a click on the "+" of a recipe
 * that the tower runs sets the tower to that recipe (its mode and its recipe lock, {@link DistillationModuleMachine#pinTo}); no
 * ingredients are asked for or moved. The EMI side is {@code DistillationEmiPlugin}. JEI loads this class only when it is installed.
 */
@JeiPlugin
public class DistillationJeiPlugin implements IModPlugin {

    @Override
    @SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(AF9Core.MOD_ID, "distillation_tower");
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        var definition = GTRegistries.MACHINES.get(new ResourceLocation("gtceu", "distillation_module"));
        if (definition == null) return;
        List<GTRecipeType> towerTypes = Arrays.asList(definition.getRecipeTypes());
        for (GTRecipeCategory category : GTRegistries.RECIPE_CATEGORIES) {
            if (!category.shouldRegisterDisplays() || !towerTypes.contains(category.getRecipeType())) continue;
            RecipeType<GTRecipe> type = GTRecipeJEICategory.TYPES.apply(category);
            registration.addRecipeTransferHandler(new TowerHandler(type, registration.getTransferHelper()), type);
        }
    }

    private static final class TowerHandler implements IRecipeTransferHandler<ModularUIContainer, GTRecipe> {

        private final RecipeType<GTRecipe> type;
        private final IRecipeTransferHandlerHelper helper;

        TowerHandler(RecipeType<GTRecipe> type, IRecipeTransferHandlerHelper helper) {
            this.type = type;
            this.helper = helper;
        }

        @Override
        public Class<? extends ModularUIContainer> getContainerClass() {
            return ModularUIContainer.class;
        }

        @Override
        public Optional<MenuType<ModularUIContainer>> getMenuType() {
            return Optional.empty();
        }

        @Override
        public RecipeType<GTRecipe> getRecipeType() {
            return type;
        }

        @Nullable
        @Override
        public IRecipeTransferError transferRecipe(ModularUIContainer container, GTRecipe recipe,
                                                   IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer,
                                                   boolean doTransfer) {
            if (!(container.getModularUI().holder instanceof IMachineBlockEntity holder) ||
                    !(holder.getMetaMachine() instanceof DistillationModuleMachine)) {
                return helper.createUserErrorWithTooltip(Component.translatable("af9.distillation.module.not_a_tower"));
            }
            if (recipe.id == null) {
                return helper.createUserErrorWithTooltip(Component.translatable("af9.distillation.module.no_recipe_id"));
            }
            if (doTransfer) AF9Network.CHANNEL.sendToServer(new PinTowerRecipePacket(recipe.id));
            return null;
        }
    }
}
