package com.af9.core.compat.powah.client;

import com.af9.core.AF9Core;
import com.af9.core.compat.powah.OrbMk2;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

/**
 * The Energizing Orb Mk2 as a catalyst of Powah's energizing category in JEI: its recipes (all of Powah's, and the HV
 * components of the pack) are listed under it, and it is listed as what makes them. Without Powah this does nothing.
 */
@JeiPlugin
public class OrbMk2JeiPlugin implements IModPlugin {

    @Override
    @SuppressWarnings("removal")
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(AF9Core.MOD_ID, "orb_mk2");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        if (!ModList.get().isLoaded("powah")) return;
        registration.addRecipeCategories(new OrbMk2JeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (!ModList.get().isLoaded("powah") || Minecraft.getInstance().level == null) return;
        registration.addRecipes(OrbMk2JeiCategory.TYPE,
                OrbMk2JeiCategory.recipes(Minecraft.getInstance().level.getRecipeManager().getRecipes()));
    }

    /**
     * The recipes of this pack that run in the Mk2 are hidden from Powah's page (the plain orb is not where they are made,
     * and its page cannot show their counts): only Powah's own recipes stay there, the pack's are on the Mk2's page.
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        if (!ModList.get().isLoaded("powah") || Minecraft.getInstance().level == null) return;
        try {
            RecipeType powahType = (RecipeType) Class.forName("owmii.powah.compat.jei.energizing.EnergizingCategory")
                    .getField("TYPE").get(null);
            java.util.List<Object> hidden = new java.util.ArrayList<>();
            for (var recipe : Minecraft.getInstance().level.getRecipeManager().getRecipes()) {
                if (recipe instanceof com.af9.core.compat.powah.OrbCounts
                        && AF9Core.MOD_ID.equals(recipe.getId().getNamespace())) {
                    hidden.add(recipe);
                }
            }
            if (!hidden.isEmpty()) runtime.getRecipeManager().hideRecipes(powahType, hidden);
        } catch (ReflectiveOperationException | LinkageError e) {
            AF9Core.LOGGER.warn("Energizing Orb Mk2: could not hide the pack's orb recipes from Powah's page: {}", e.toString());
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (!ModList.get().isLoaded("powah")) return;
        registration.addRecipeCatalyst(new ItemStack(OrbMk2.BLOCK.get()), OrbMk2JeiCategory.TYPE);
        try {
            // Powah's own category type, so that the recipes are the ones Powah registered
            Object type = Class.forName("owmii.powah.compat.jei.energizing.EnergizingCategory").getField("TYPE").get(null);
            registration.addRecipeCatalyst(new ItemStack(OrbMk2.BLOCK.get()), (RecipeType<?>) type);
        } catch (ReflectiveOperationException | LinkageError e) {
            AF9Core.LOGGER.warn("Energizing Orb Mk2: Powah's JEI category not found, no catalyst registered: {}", e.toString());
        }
    }
}
