package com.af9.core.fab;

import com.af9.core.AF9Core;

import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.SimpleTieredMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifierList;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.machines.GCYMMachines;
import com.gregtechceu.gtceu.common.data.machines.GTMultiMachines;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * AF9's semiconductor chemistry (the fab_* recipe types, defined in
 * kubejs/startup_scripts/gtceu/fab_machines.js) as modes of the normal GT machines. The machines are the ones the
 * chemistry belongs to, the modes are what the chemistry does:
 *
 * <table>
 * <tr><th>Machine</th><th>Fab modes</th></tr>
 * <tr><td>Chemical Reactor, Large Chemical Reactor</td><td>synthesis, blending, wet processing</td></tr>
 * <tr><td>Distillery</td><td>fractionation, purification</td></tr>
 * <tr><td>Distillation Facility</td><td>distillation, cryogenic rectification (its own modes, see distillation_facility.js)</td></tr>
 * <tr><td>Electrolyzer, Large Electrolysis Chamber</td><td>electrolysis, electrofluorination</td></tr>
 * <tr><td>Arc Furnace, Electric Blast Furnace</td><td>calcination, CVD, crystal growth</td></tr>
 * </table>
 *
 * The modes are GT recipe types, so GT's mode tab (the machine's side tab) switches between them. The single blocks
 * get their slots from all their modes (the fork's patch 0014), so every mode has room for its inputs and outputs.
 * <p>
 * A mode has to fit the machine it runs in: the (former) Distillation Tower had one fluid input and no item input, so the
 * cuts (which take a circuit) stay in the Distillery, and the column recipes (one fluid in, up to five out) run in
 * the tower.
 * <p>
 * The thermal modes keep their heat: the Electric Blast Furnace with GT's blast furnace rules (the coil's
 * temperature), the Arc Furnace with the temperature of its tier ({@link FabThermal}).
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1; GT uses it too
public final class FabModes {

    private FabModes() {}

    public static void install() {
        // GT's tiered machines are one array with a hole for every tier that has no machine (ULV, and the tiers above
        // UHV while the high tiers are off), so the loops below skip the empty ones
        // chemistry
        GTRecipeType synthesis = type("fab_synthesis");
        GTRecipeType blending = type("fab_blending");
        GTRecipeType wetProcessing = type("fab_wet_processing");
        // separation
        GTRecipeType distillation = type("fab_distillation");
        GTRecipeType cryogenic = type("fab_cryogenic_rectification");
        GTRecipeType fractionation = type("fab_fractionation");
        GTRecipeType purification = type("fab_purification");
        // electrochemistry
        GTRecipeType electrolysis = type("fab_electrolysis");
        GTRecipeType electrofluorination = type("fab_electrofluorination");
        // thermal
        GTRecipeType calcination = type("fab_calcination");
        GTRecipeType cvd = type("fab_cvd");
        GTRecipeType crystalGrowth = type("fab_crystal_growth");

        // the chemistry of the chemical reactor, big and small
        for (MachineDefinition definition : GTMachines.CHEMICAL_REACTOR) {
            if (definition == null) continue;
            modes(definition, synthesis, blending, wetProcessing);
        }
        modes(GTMultiMachines.LARGE_CHEMICAL_REACTOR, synthesis, blending, wetProcessing);

        // the separations of the distillery and the distillation tower: the tower runs a whole column at once (it has
        // one fluid input and no item input, so the cuts, which take a circuit, stay in the distillery)
        for (MachineDefinition definition : GTMachines.DISTILLERY) {
            if (definition == null) continue;
            modes(definition, fractionation, purification);
        }
        // (the Distillation Tower is gone, the Distillation Facility runs the same three modes: it registers them itself)
        facilityIcon();

        // the electrochemistry of the electrolyzer and the large electrolysis chamber
        for (MachineDefinition definition : GTMachines.ELECTROLYZER) {
            if (definition == null) continue;
            modes(definition, electrolysis, electrofluorination);
        }
        modes(GCYMMachines.LARGE_ELECTROLYZER, electrolysis, electrofluorination);

        // the heat of the furnaces: the blast furnace keeps GT's rules, the arc furnace has the heat of its tier
        for (MachineDefinition definition : GTMachines.ARC_FURNACE) {
            if (definition == null) continue;
            modes(definition, calcination, cvd, crystalGrowth);
            heatOfTier(definition);
        }
        modes(GTMultiMachines.ELECTRIC_BLAST_FURNACE, calcination, cvd, crystalGrowth);
    }

    /** GT's distillation recipe type took its icon from the Distillation Tower: give it the Distillation Facility's. */
    private static void facilityIcon() {
        MachineDefinition facility = GTRegistries.MACHINES.get(new ResourceLocation("gtceu", "distillation_facility"));
        GTRecipeType distillationRecipes = GTRegistries.RECIPE_TYPES.get(new ResourceLocation("gtceu", "distillation_tower"));
        if (facility == null || distillationRecipes == null) return;
        if (distillationRecipes.getIconSupplier() == null) distillationRecipes.setIconSupplier(facility::asStack);
    }

    /** The recipe type of the KubeJS startup script, or null when the script did not load. */
    private static GTRecipeType type(String path) {
        GTRecipeType type = GTRegistries.RECIPE_TYPES.get(new ResourceLocation("gtceu", path));
        if (type == null) {
            AF9Core.LOGGER.warn("Recipe type gtceu:{} not found - is the AF9 KubeJS startup script loaded?", path);
        }
        return type;
    }

    /** Adds the modes to the machine, and gives the machine's GUI the slots of its biggest mode. */
    private static void modes(MachineDefinition definition, GTRecipeType... types) {
        if (definition == null) return;
        List<GTRecipeType> all = new ArrayList<>(Arrays.asList(definition.getRecipeTypes()));
        boolean changed = false;
        for (GTRecipeType type : types) {
            if (type == null || all.contains(type)) continue;
            all.add(type);
            changed = true;
            // GT gives a recipe type the icon of the machine that registers it; these modes are added afterwards, so
            // without this their EMI / JEI category shows a barrier
            if (type.getIconSupplier() == null) type.setIconSupplier(definition::asStack);
        }
        if (!changed) return;
        definition.setRecipeTypes(all.toArray(new GTRecipeType[0]));
        if (!(definition instanceof MultiblockMachineDefinition)) {
            // a single block: its slots and its GUI are the ones of the biggest mode (the fork's patch 0014)
            definition.setEditableUI(
                    SimpleTieredMachine.createEditableUI(definition.getId(), definition.getRecipeTypes()));
        }
    }

    /** The single-block furnace reaches the temperature of its tier: a thermal recipe above it does not run. */
    private static void heatOfTier(MachineDefinition definition) {
        RecipeModifier original = definition.getRecipeModifier();
        definition.setRecipeModifier(original == null ? FabThermal.TIER_TEMPERATURE :
                new RecipeModifierList(FabThermal.TIER_TEMPERATURE, original));
    }
}
