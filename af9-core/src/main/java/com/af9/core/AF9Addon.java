package com.af9.core;

import com.af9.core.registry.AF9DimensionMarkers;
import com.af9.core.registry.AF9TagPrefixes;
import com.af9.core.space.AF9Space;
import com.af9.core.staged.StagedCovers;

import com.gregtechceu.gtceu.api.addon.GTAddon;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;

import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

/**
 * AF9 Core as a GregTech addon: GregTech finds this class by its annotation and calls it while it sets itself up.
 * The materials are registered in GregTech's material phase ({@link com.af9.core.registry.AF9Materials}), which is an
 * event of its own; the Ultrdense Plate is a TagPrefix ({@link AF9TagPrefixes}), which GregTech sets up at another
 * point, through the hooks below.
 */
@GTAddon
public class AF9Addon implements IGTAddon {

    @Override
    public GTRegistrate getRegistrate() {
        return AF9Core.REGISTRATE;
    }

    /**
     * GregTech calls this right after it has registered its own dimension markers and closed their registry, still
     * inside its own start-up: the one place the registry opens for AF9's markers.
     */
    @Override
    public void initializeAddon() {
        AF9DimensionMarkers.bind();
    }

    @Override
    public String addonModId() {
        return AF9Core.MOD_ID;
    }

    @Override
    public void registerTagPrefixes() {
        AF9TagPrefixes.init();
    }

    /**
     * GregTech calls this while it builds {@code GTCovers} and before it freezes the cover registry, so the step
     * detector cover has to be registered here: registering it from the cover item's own class load (an item
     * RegisterEvent) is already too late.
     */
    @Override
    public void registerCovers() {
        StagedCovers.init();
    }

    @Override
    public void addRecipes(Consumer<FinishedRecipe> provider) {
        AF9TagPrefixes.addRecipes(provider);
    }

    @Override
    public void registerWorldgenLayers() {
        AF9Space.registerWorldgenLayers();
    }
}
