package com.af9.core.machine;

import com.gregtechceu.gtceu.api.machine.feature.IOverclockMachine;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.network.chat.Component;

/**
 * The overclock of the recipes that draw several amps at their voltage (the Void Miners MK2 and MK3: 2 and 3 A of the base
 * miner's voltage). GT's own overclock takes the recipe's TOTAL EU/t as its voltage, so two amps of EV count as an IV recipe and an
 * EV or IV hatch overclocks nothing: the machine seemed locked to the recipe's voltage. This one overclocks the voltage of one
 * amp, by the hatches' voltage, and leaves the amps as they are (each overclock: four times the voltage, half the time).
 */
public final class AmperageOverclock {

    public static final RecipeModifier OC = (machine, recipe) -> {
        if (!(machine instanceof IOverclockMachine overclocking)) return ModifierFunction.IDENTITY;
        var eut = RecipeHelper.getRealEUt(recipe);
        if (eut.isEmpty() || eut.voltage() <= 0) return ModifierFunction.IDENTITY;
        int recipeTier = GTUtil.getTierByVoltage(eut.voltage());
        if (recipeTier > overclocking.getMaxOverclockTier()) {
            return ModifierFunction.cancel(Component.translatable("gtceu.recipe_modifier.insufficient_voltage"));
        }
        long maxVoltage = overclocking.getOverclockVoltage();
        int overclocks = GTUtil.getOCTierByVoltage(maxVoltage) - recipeTier;
        if (overclocks <= 0) return ModifierFunction.IDENTITY;
        var result = OverclockingLogic.NON_PERFECT_OVERCLOCK.runOverclockingLogic(
                new OverclockingLogic.OCParams(eut.voltage(), recipe.duration, overclocks, 1), maxVoltage);
        return result.toModifier();
    };

    private AmperageOverclock() {}
}
