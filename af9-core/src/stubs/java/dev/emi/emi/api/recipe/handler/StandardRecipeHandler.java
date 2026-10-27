package dev.emi.emi.api.recipe.handler;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.widget.Widget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.List;

/** Compile-time stand-in for EMI's standard recipe handler (not shipped): the defaults are EMI's, here only so that it compiles. */
public interface StandardRecipeHandler<T extends AbstractContainerMenu> extends EmiRecipeHandler<T> {

    @Override
    default boolean canCraft(EmiRecipe recipe, EmiCraftContext<T> context) {
        return false;
    }

    @Override
    default boolean craft(EmiRecipe recipe, EmiCraftContext<T> context) {
        return false;
    }

    @Override
    default void render(EmiRecipe recipe, EmiCraftContext<T> context, List<Widget> widgets, GuiGraphics draw) {}
}
