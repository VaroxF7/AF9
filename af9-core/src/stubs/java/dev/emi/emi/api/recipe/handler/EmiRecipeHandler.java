package dev.emi.emi.api.recipe.handler;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.widget.Widget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/** Compile-time stand-in for EMI's recipe handler (not shipped): only what AF9 Core calls. */
public interface EmiRecipeHandler<T extends AbstractContainerMenu> {

    List<Slot> getInputSources(T handler);

    List<Slot> getCraftingSlots(T handler);

    boolean supportsRecipe(EmiRecipe recipe);

    boolean canCraft(EmiRecipe recipe, EmiCraftContext<T> context);

    boolean craft(EmiRecipe recipe, EmiCraftContext<T> context);

    void render(EmiRecipe recipe, EmiCraftContext<T> context, List<Widget> widgets, GuiGraphics draw);
}
