package dev.emi.emi.registry;

import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import net.minecraft.world.inventory.MenuType;

import java.util.List;
import java.util.Map;

/** Compile-time stand-in for EMI's recipe filler (not shipped): only its handler table, which AF9 Core reorders. */
public final class EmiRecipeFiller {

    public static Map<MenuType<?>, List<EmiRecipeHandler<?>>> handlers;

    private EmiRecipeFiller() {}
}
