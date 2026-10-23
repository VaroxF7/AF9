package com.af9.core.fab;

import com.af9.core.AF9Core;

import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.SimpleTieredMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.GTMachines;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The Wiremill's Spooling mode: the {@code gtceu:spooling} recipe type (kubejs/startup_scripts/gtceu/spooling.js) is
 * added to every Wiremill as a second mode, so GT's mode tab switches between drawing wire and winding spools.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1; GT uses it too
public final class SpoolingMode {

    private SpoolingMode() {}

    public static void install() {
        GTRecipeType spooling = GTRegistries.RECIPE_TYPES.get(new ResourceLocation("gtceu", "spooling"));
        if (spooling == null) {
            AF9Core.LOGGER.warn("Recipe type gtceu:spooling not found - is the AF9 KubeJS startup script loaded?");
            return;
        }
        for (MachineDefinition definition : GTMachines.WIREMILL) {
            if (definition == null) continue;
            List<GTRecipeType> all = new ArrayList<>(Arrays.asList(definition.getRecipeTypes()));
            if (all.contains(spooling)) continue;
            all.add(spooling);
            // the mode is added after registration: without an icon its EMI / JEI category shows a barrier
            if (spooling.getIconSupplier() == null) spooling.setIconSupplier(definition::asStack);
            definition.setRecipeTypes(all.toArray(new GTRecipeType[0]));
            // the slots of the biggest mode (the fork's patch 0014)
            definition.setEditableUI(
                    SimpleTieredMachine.createEditableUI(definition.getId(), definition.getRecipeTypes()));
        }
    }
}
