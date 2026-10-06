package com.af9.core.machine;

import com.af9.core.AF9Core;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.gregtechceu.gtceu.api.GTValues.IV;
import static com.gregtechceu.gtceu.api.GTValues.LuV;

/**
 * MK3 Void Miner (white/silver, LuV tier) - top tier, fastest, maximum parallel outputs.
 */
public class VoidMinerMachineMK3 extends VoidMinerMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            VoidMinerMachineMK3.class, VoidMinerMachine.MANAGED_FIELD_HOLDER);

    /** Highest tier colors - white/silver theme for MK3. */
    private static final int[] COLORS_MK3 = { 0xFFF8FAFC, 0xFFE2E8F0, 0xFFCBD5E1, 0xFF94A3B8 };

    public VoidMinerMachineMK3(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public String titleKey() {
        return "af9.voidminer.mk3.console.title";
    }

    @Override
    public int modeColor(int index) {
        return COLORS_MK3[Math.max(0, Math.min(COLORS_MK3.length - 1, index))];
    }

    @Override
    public int getActiveRecipeType() {
        // MK3 runs at LuV tier base
        return Math.max(LuV, super.getActiveRecipeType());
    }

    /** MK3 has 3x output multiplier */
    @Override
    public int getOutputMultiplier() {
        return 3;
    }

    /** MK3 runs 50% faster */
    @Override
    public int getSpeedMultiplier() {
        return 6; // 6/4 = 1.5x speed
    }
}