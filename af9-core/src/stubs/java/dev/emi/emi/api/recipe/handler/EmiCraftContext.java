package dev.emi.emi.api.recipe.handler;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Compile-time stand-in for EMI's craft context (not shipped): only what AF9 Core calls. */
public abstract class EmiCraftContext<T extends AbstractContainerMenu> {

    public abstract AbstractContainerScreen<T> getScreen();
}
