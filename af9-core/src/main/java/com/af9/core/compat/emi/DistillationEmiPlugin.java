package com.af9.core.compat.emi;

import com.af9.core.distillation.DistillationModuleMachine;
import com.af9.core.network.AF9Network;
import com.af9.core.network.PinTowerRecipePacket;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import com.lowdragmc.lowdraglib.gui.modular.ModularUIContainer;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.registry.EmiRecipeFiller;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;

import java.util.Arrays;
import java.util.List;

/**
 * The recipe viewer's "+" for the Distillation Tower Module: with a tower's screen open, a click on the "+" of a recipe that
 * the tower runs sets the tower to that recipe (its mode and its recipe lock, {@link DistillationModuleMachine#pinTo}) instead of
 * moving items. EMI loads this class only when it is installed.
 */
@EmiEntrypoint
public class DistillationEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        TowerHandler handler = new TowerHandler();
        registry.addRecipeHandler(ModularUIContainer.MENUTYPE, handler);
        // GT's handler for the machine screens takes every GT recipe: ours goes first, and only answers for a tower's screen
        List<EmiRecipeHandler<?>> handlers = EmiRecipeFiller.handlers.get(ModularUIContainer.MENUTYPE);
        if (handlers != null && handlers.remove(handler)) handlers.add(0, handler);
    }

    private static final class TowerHandler implements StandardRecipeHandler<ModularUIContainer> {

        /** The tower whose screen is open, null if the open screen is another. */
        private static DistillationModuleMachine openTower() {
            if (Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen &&
                    screen.getMenu() instanceof ModularUIContainer menu &&
                    menu.getModularUI().holder instanceof IMachineBlockEntity holder &&
                    holder.getMetaMachine() instanceof DistillationModuleMachine tower) {
                return tower;
            }
            return null;
        }

        private static GTRecipe gtRecipe(EmiRecipe recipe) {
            ResourceLocation id = recipe.getId();
            if (id == null || Minecraft.getInstance().level == null) return null;
            return Minecraft.getInstance().level.getRecipeManager().byKey(id).orElse(null) instanceof GTRecipe gt ? gt : null;
        }

        @Override
        public List<Slot> getInputSources(ModularUIContainer handler) {
            return List.of();
        }

        @Override
        public List<Slot> getCraftingSlots(ModularUIContainer handler) {
            return List.of();
        }

        @Override
        public boolean supportsRecipe(EmiRecipe recipe) {
            DistillationModuleMachine tower = openTower();
            GTRecipe gt = tower == null ? null : gtRecipe(recipe);
            return gt != null && Arrays.asList(tower.getRecipeTypes()).contains(gt.recipeType);
        }

        @Override
        public boolean canCraft(EmiRecipe recipe, EmiCraftContext<ModularUIContainer> context) {
            return true;
        }

        @Override
        public boolean craft(EmiRecipe recipe, EmiCraftContext<ModularUIContainer> context) {
            ResourceLocation id = recipe.getId();
            if (id == null) return false;
            AF9Network.CHANNEL.sendToServer(new PinTowerRecipePacket(id));
            Minecraft.getInstance().setScreen(context.getScreen());
            return true;
        }

        /** Nothing is missing (no items move): no red marks on the recipe's inputs. */
        @Override
        public void render(EmiRecipe recipe, EmiCraftContext<ModularUIContainer> context, List<Widget> widgets,
                           GuiGraphics draw) {}
    }
}
