package com.af9.core.compat.powah.client;

import com.af9.core.AF9Core;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import net.minecraft.resources.ResourceLocation;

/**
 * Keeps the pack's orb recipes (the GT shaping, {@code af9:powah/*}) off the plain orb's page in EMI: Powah's EMI plugin lists
 * every recipe of its type under the plain Energizing Orb, but these run in the Mk2 only ({@code OrbTileMixin}). Powah's own
 * recipes stay. EMI loads this class only when it is installed.
 */
@EmiEntrypoint
public class OrbMk2EmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.removeRecipes(recipe -> {
            ResourceLocation id = recipe.getId();
            return id != null && AF9Core.MOD_ID.equals(id.getNamespace()) && id.getPath().startsWith("powah/");
        });
    }
}
