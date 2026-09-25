package com.af9.core.machine;

import com.af9.core.machine.console.ConsoleWidget;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.network.chat.Component;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Supercooling Cryostat (structure and recipes in KubeJS). HV: needs 4A of HV (two 2A hatches) to run at all, perfect
 * overclocks above that. Two modes: dense cooling (a gas is compressed and chilled into its dense liquid, the
 * intermediate) and supercooling (the dense liquid goes back in and comes out supercooled, rated at an effective
 * {@value #SUPERCOOLED_KELVIN} K). The supercooled fluids are what the coolant hatches accept.
 */
public class SupercoolerMachine extends ProcessMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(SupercoolerMachine.class,
            ProcessMachine.MANAGED_FIELD_HOLDER);

    /** Effective temperature the supercooling mode is rated at (the fluids themselves sit at 1 K). */
    public static final int SUPERCOOLED_KELVIN = -5000;
    /** Amps of HV the cryostat needs, like the recipes draw them. */
    public static final int AMPERAGE = 4;
    public static final long MIN_EUT = (long) GTValues.VA[GTValues.HV] * AMPERAGE;
    private static final int[] COLORS = { 0xFF7DD3FC, 0xFFA78BFA };

    public SupercoolerMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public String titleKey() {
        return "af9.supercooling_cryostat.console.title";
    }

    @Override
    public int modeColor(int index) {
        return COLORS[Math.max(0, Math.min(COLORS.length - 1, index))];
    }

    @Override
    public long minimumEUt() {
        return MIN_EUT;
    }

    private boolean isSupercooling() {
        return getRecipeType() != null && getRecipeType().registryName.getPath().equals("supercooling");
    }

    @Override
    public List<Component> infoLines() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("af9.supercooling_cryostat.console.gate",
                ConsoleWidget.compact(MIN_EUT)));
        GTRecipe recipe = getRecipeLogic().isWorking() ? getRecipeLogic().getLastRecipe() : null;
        FluidStack in = firstFluid(recipe, true);
        FluidStack out = firstFluid(recipe, false);
        if (!in.isEmpty() && !out.isEmpty()) {
            int from = in.getFluid().getFluidType().getTemperature(in);
            int to = isSupercooling() ? SUPERCOOLED_KELVIN : out.getFluid().getFluidType().getTemperature(out);
            double fraction = getRecipeLogic().getDuration() <= 0 ? 0 :
                    (double) getRecipeLogic().getProgress() / getRecipeLogic().getDuration();
            lines.add(Component.translatable("af9.supercooling_cryostat.console.chamber",
                    Math.round(from + (to - from) * fraction)));
            lines.add(Component.translatable("af9.supercooling_cryostat.console.target", from, to));
        } else {
            lines.add(Component.translatable("af9.supercooling_cryostat.console.target_idle",
                    isSupercooling() ? SUPERCOOLED_KELVIN : 20));
        }
        return lines;
    }

    private static FluidStack firstFluid(GTRecipe recipe, boolean input) {
        if (recipe == null) return FluidStack.EMPTY;
        var contents = (input ? recipe.inputs : recipe.outputs).getOrDefault(FluidRecipeCapability.CAP, List.of());
        for (Content content : contents) {
            if (content.content instanceof FluidIngredient ingredient && ingredient.getStacks().length > 0) {
                return ingredient.getStacks()[0];
            }
        }
        return FluidStack.EMPTY;
    }
}
