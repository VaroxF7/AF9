package com.af9.core;

import com.af9.core.registry.AF9TagPrefixes;
import com.af9.core.space.AF9Space;

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

    @Override
    public void initializeAddon() {}

    @Override
    public String addonModId() {
        return AF9Core.MOD_ID;
    }

    @Override
    public void registerTagPrefixes() {
        AF9TagPrefixes.init();
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
