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
 * MK2 Void Miner (blue, IV tier) - faster, more parallel outputs.
 */
public class VoidMinerMachineMK2 extends VoidMinerMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            VoidMinerMachineMK2.class, VoidMinerMachine.MANAGED_FIELD_HOLDER);

    /** Higher tier colors - blue theme for MK2. */
    private static final int[] COLORS_MK2 = { 0xFF3B82F6, 0xFF60A5FA, 0xFF93C5FD, 0xFFBFDBFE };

    public VoidMinerMachineMK2(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public String titleKey() {
        return "af9.voidminer.mk2.console.title";
    }

    @Override
    public int modeColor(int index) {
        return COLORS_MK2[Math.max(0, Math.min(COLORS_MK2.length - 1, index))];
    }

    @Override
    public int getActiveRecipeType() {
        // MK2 runs at IV tier base
        return Math.max(IV, super.getActiveRecipeType());
    }

    /** MK2 has 2x output multiplier */
    @Override
    public int getOutputMultiplier() {
        return 2;
    }

    /** MK2 runs 25% faster */
    @Override
    public int getSpeedMultiplier() {
        return 4; // 4/3 = ~1.33x speed
    }
}