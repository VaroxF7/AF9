package com.af9.core.blast;

import com.af9.core.AF9Core;

import com.gregtechceu.gtceu.api.block.ICoilType;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifierList;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.machines.GTMultiMachines;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;

/**
 * Boule Melting: a second machine mode for GT's Electric Blast Furnace (recipe type gtceu:boule_melting, defined in
 * kubejs/startup_scripts/gtceu/boule_melting.js). The Czochralski pullers that grow the substrate boules: melt
 * charges, a seed crystal and a crucible, under a protective gas, at the EBF's coil temperature and overclocks.
 * <p>
 * The Endion coils give this mode a bonus (on top of their temperature): Endion Coil Blocks run it 25 % faster with up
 * to 2 parallels, Resonant Endion Coil Blocks twice as fast with up to 4 parallels.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1; GT uses it too
public final class BouleMelting {

    public static final String TYPE = "boule_melting";
    /** Coil type names = the coil blocks' registry paths (KubeJS gtceu:coil blocks). */
    public static final String ENDION_COIL = "endion_coil_block";
    public static final String RESONANT_ENDION_COIL = "resonant_endion_coil_block";
    public static final double ENDION_DURATION = 0.75;
    public static final int ENDION_PARALLEL = 2;
    public static final double RESONANT_DURATION = 0.5;
    public static final int RESONANT_PARALLEL = 4;

    /** Endion coil bonus in boule melting; before GT's EBF overclock, so the overclock sees the parallel EU/t. */
    public static final RecipeModifier COIL_BONUS = (machine, recipe) -> {
        if (!isBouleMelting(recipe.recipeType) ||
                !(machine instanceof CoilWorkableElectricMultiblockMachine coilMachine)) {
            return ModifierFunction.IDENTITY;
        }
        ICoilType coil = coilMachine.getCoilType();
        double duration;
        int maxParallel;
        if (RESONANT_ENDION_COIL.equals(coil.getName())) {
            duration = RESONANT_DURATION;
            maxParallel = RESONANT_PARALLEL;
        } else if (ENDION_COIL.equals(coil.getName())) {
            duration = ENDION_DURATION;
            maxParallel = ENDION_PARALLEL;
        } else {
            return ModifierFunction.IDENTITY;
        }
        int parallels = ParallelLogic.getParallelAmount(machine, recipe, maxParallel);
        if (parallels <= 1) {
            return ModifierFunction.builder().durationMultiplier(duration).build();
        }
        return ModifierFunction.builder()
                .modifyAllContents(ContentModifier.multiplier(parallels))
                .eutMultiplier(parallels)
                .parallels(parallels)
                .durationMultiplier(duration)
                .build();
    };

    private BouleMelting() {}

    public static boolean isBouleMelting(GTRecipeType type) {
        return type != null && type.registryName.getNamespace().equals("gtceu") &&
                type.registryName.getPath().equals(TYPE);
    }

    /** Adds the mode and the coil bonus to GT's EBF, and the temperature to the mode's EMI/JEI pages. */
    public static void install() {
        GTRecipeType boule = GTRegistries.RECIPE_TYPES.get(new ResourceLocation("gtceu", TYPE));
        if (boule == null) {
            AF9Core.LOGGER.warn("Recipe type gtceu:{} not found - is the AF9 KubeJS startup script loaded?", TYPE);
            return;
        }
        MultiblockMachineDefinition ebf = GTMultiMachines.ELECTRIC_BLAST_FURNACE;
        GTRecipeType[] types = ebf.getRecipeTypes();
        if (!Arrays.asList(types).contains(boule)) {
            GTRecipeType[] extended = Arrays.copyOf(types, types.length + 1);
            extended[types.length] = boule;
            ebf.setRecipeTypes(extended);
            RecipeModifier original = ebf.getRecipeModifier();
            ebf.setRecipeModifier(original == null ? COIL_BONUS : new RecipeModifierList(COIL_BONUS, original));
        }
        // GT gives a recipe type the icon of the machine that registers it; this mode was added afterwards, so without
        // this its EMI/JEI category shows a barrier
        if (boule.getIconSupplier() == null) boule.setIconSupplier(ebf::asStack);
        // rendered as plain labels, so the texts must not contain '%'
        boule.addDataInfo(data -> Component.translatable("af9.recipe.boule_temperature", data.getInt("ebf_temp"))
                .getString());
        boule.addDataInfo(data -> {
            ICoilType coil = ICoilType.getMinRequiredType(data.getInt("ebf_temp"));
            if (coil == null || coil.getMaterial().isNull()) return "";
            return Component.translatable("af9.recipe.fab_coil",
                    Component.translatable(coil.getMaterial().getUnlocalizedName())).getString();
        });
        boule.addDataInfo(data -> Component.translatable("af9.recipe.boule_endion_bonus").getString());
    }
}
