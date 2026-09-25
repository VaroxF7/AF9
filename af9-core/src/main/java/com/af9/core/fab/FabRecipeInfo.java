package com.af9.core.fab;

import com.af9.core.AF9Core;
import com.af9.core.machine.fab.FabTieredMachine;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.block.ICoilType;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Adds the furnace temperature to the EMI/JEI pages of the thermal fab modes (the recipes carry it as GT's
 * "ebf_temp", like the blast furnace's), with the coil the multiblock needs and the smallest single block that
 * reaches it.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1; GT uses it too
public final class FabRecipeInfo {

    private static final String[] THERMAL_TYPES = { "fab_calcination", "fab_cvd", "fab_crystal_growth" };
    /** Highest tier of the single-block furnaces (see kubejs/startup_scripts/gtceu/fab_machines.js). */
    private static final int MAX_SINGLE_TIER = GTValues.LuV;

    private FabRecipeInfo() {}

    public static void register() {
        for (String path : THERMAL_TYPES) {
            GTRecipeType type = GTRegistries.RECIPE_TYPES.get(new ResourceLocation("gtceu", path));
            if (type == null) {
                AF9Core.LOGGER.warn("Recipe type gtceu:{} not found - is the AF9 KubeJS startup script loaded?", path);
                continue;
            }
            // rendered as plain labels, so the texts must not contain '%'
            type.addDataInfo(data -> Component.translatable("af9.recipe.fab_temperature", data.getInt("ebf_temp"))
                    .getString());
            type.addDataInfo(data -> {
                ICoilType coil = ICoilType.getMinRequiredType(data.getInt("ebf_temp"));
                if (coil == null || coil.getMaterial().isNull()) return "";
                return Component.translatable("af9.recipe.fab_coil",
                        Component.translatable(coil.getMaterial().getUnlocalizedName())).getString();
            });
            type.addDataInfo(data -> {
                int tier = singleTierFor(data.getInt("ebf_temp"));
                return tier > MAX_SINGLE_TIER ? Component.translatable("af9.recipe.fab_multi_only").getString() :
                        Component.translatable("af9.recipe.fab_single_tier", GTValues.VN[tier]).getString();
            });
        }
    }

    /** Lowest voltage tier whose single-block furnace reaches the temperature. */
    static int singleTierFor(int temperature) {
        int tier = GTValues.MV;
        while (tier <= MAX_SINGLE_TIER && FabTieredMachine.temperatureOf(tier) < temperature) tier++;
        return tier;
    }
}
