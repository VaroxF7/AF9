package com.af9.core.fab;

import com.af9.core.machine.fab.FabMultiblockMachine;
import com.af9.core.machine.fab.FabTieredMachine;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.List;

/**
 * Recipe modifiers of the fab machines (used from kubejs/startup_scripts/gtceu/fab_machines.js). Order in a machine:
 * structure parallel, GT's parallel hatch, coil discount, overclock, batch, purge last (its time and fluid must not
 * be multiplied or overclocked).
 */
public final class FabModifiers {

    private FabModifiers() {}

    /**
     * Product changeover: if the machine last finished a different recipe, this run also takes the family's purge
     * fluid and its purge time. Without purge fluid in the inputs the recipe does not start.
     */
    public static final RecipeModifier PURGE = (machine, recipe) -> {
        if (!(machine instanceof IFabMachine fab) || !fab.needsPurge(recipe)) return ModifierFunction.IDENTITY;
        FabFamily family = fab.getFabFamily();
        return modified -> withPurge(modified, family);
    };

    /** Parallels from the structure: tray / cell layers, or the ULPA filter bonus of the SMC reactor. */
    public static final RecipeModifier STRUCTURE_PARALLEL = (machine, recipe) -> {
        if (!(machine instanceof FabMultiblockMachine fab) || !fab.isFormed()) return ModifierFunction.IDENTITY;
        int limit = fab.getStructureParallel();
        if (limit <= 1) return ModifierFunction.IDENTITY;
        int parallels = ParallelLogic.getParallelAmount(machine, recipe, limit);
        if (parallels <= 1) return ModifierFunction.IDENTITY;
        return ModifierFunction.builder()
                .modifyAllContents(ContentModifier.multiplier(parallels))
                .eutMultiplier(parallels)
                .parallels(parallels)
                .build();
    };

    /** Heated reactor jacket: 5 % less EU/t per coil tier above cupronickel (at most half). */
    public static final RecipeModifier COIL_DISCOUNT = (machine, recipe) -> {
        if (!(machine instanceof CoilWorkableElectricMultiblockMachine coil) || coil.getCoilTier() <= 0) {
            return ModifierFunction.IDENTITY;
        }
        return ModifierFunction.builder()
                .eutMultiplier(Math.max(0.5, 1.0 - 0.05 * coil.getCoilTier()))
                .build();
    };

    /** Single-block furnaces reach a fixed temperature per voltage tier (the matching coil's). */
    public static final RecipeModifier TIER_TEMPERATURE = (machine, recipe) -> {
        if (!(machine instanceof FabTieredMachine fab)) {
            return RecipeModifier.nullWrongType(FabTieredMachine.class, machine);
        }
        if (recipe.data.contains("ebf_temp") && recipe.data.getInt("ebf_temp") > fab.getMaxTemperature()) {
            return ModifierFunction.NULL;
        }
        return ModifierFunction.IDENTITY;
    };

    /** GT's blast furnace overclock (coil temperature bonus), for the thermal multiblock. */
    public static final RecipeModifier THERMAL_OVERCLOCK = GTRecipeModifiers::ebfOverclock;

    static GTRecipe withPurge(GTRecipe recipe, FabFamily family) {
        List<Fluid> fluids = family.purgeFluids();
        if (fluids.isEmpty()) return recipe;
        GTRecipe purged = recipe.copy();
        List<Content> contents = new ArrayList<>(purged.inputs.getOrDefault(FluidRecipeCapability.CAP, List.of()));
        contents.add(new Content(FluidIngredient.of(fluids, family.purgeAmount, null),
                ChanceLogic.getMaxChancedValue(), ChanceLogic.getMaxChancedValue(), 0));
        purged.inputs.put(FluidRecipeCapability.CAP, contents);
        purged.duration = recipe.duration + family.purgeTicks;
        return purged;
    }
}
