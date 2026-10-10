package com.af9.core.compat.powah.client;

import com.af9.core.AF9Core;
import com.af9.core.compat.powah.OrbMk2;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
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
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (!ModList.get().isLoaded("powah")) return;
        try {
            // Powah's own category type, so that the recipes are the ones Powah registered
            Object type = Class.forName("owmii.powah.compat.jei.energizing.EnergizingCategory").getField("TYPE").get(null);
            registration.addRecipeCatalyst(new ItemStack(OrbMk2.BLOCK.get()), (RecipeType<?>) type);
        } catch (ReflectiveOperationException | LinkageError e) {
            AF9Core.LOGGER.warn("Energizing Orb Mk2: Powah's JEI category not found, no catalyst registered: {}", e.toString());
        }
    }
}
