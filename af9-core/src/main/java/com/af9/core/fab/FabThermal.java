package com.af9.core.fab;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.WorkableTieredMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;

import net.minecraft.network.chat.Component;

import java.util.Set;

/**
 * The heat of AF9's thermal fab modes (calcination, CVD, crystal growth): the recipes carry the temperature they
 * need as GT's blast furnace does ("ebf_temp").
 * <p>
 * The Electric Blast Furnace keeps GT's own rules: the coil's temperature (plus 100 K per tier above MV) decides
 * what it runs. The single-block Arc Furnace has no coil, so it reaches the temperature of its tier's coil instead:
 * MV 1800 K, then 900 K more per tier (HV 2700 K, EV 3600 K, IV 4500 K, LuV 5400 K). A recipe that needs more heat
 * than that does not run there.
 */
public final class FabThermal {

    /** The thermal fab modes: they carry a temperature, like the blast furnace's recipes. */
    private static final Set<String> THERMAL_TYPES = Set.of("fab_calcination", "fab_cvd", "fab_crystal_growth");

    private FabThermal() {}

    /** The temperature a single-block furnace of that tier reaches (the matching coil's). */
    public static int temperatureOf(int tier) {
        return 1800 + 900 * (tier - GTValues.MV);
    }

    public static boolean isThermal(GTRecipeType type) {
        return type != null && type.registryName.getNamespace().equals("gtceu")
                && THERMAL_TYPES.contains(type.registryName.getPath());
    }

    /**
     * The heat a single-block furnace has: a thermal fab recipe that needs more temperature than the machine's tier
     * reaches does not run in it. The machine's own recipes have no temperature and are not touched.
     */
    public static final RecipeModifier TIER_TEMPERATURE = (machine, recipe) -> {
        if (!isThermal(recipe.recipeType)) return ModifierFunction.IDENTITY;
        if (!(machine instanceof WorkableTieredMachine tiered)) return ModifierFunction.IDENTITY;
        if (recipe.data.contains("ebf_temp") && recipe.data.getInt("ebf_temp") > temperatureOf(tiered.getTier())) {
            return ModifierFunction.cancel(Component.translatable("gtceu.recipe_modifier.coil_temperature_too_low"));
        }
        return ModifierFunction.IDENTITY;
    };
}
