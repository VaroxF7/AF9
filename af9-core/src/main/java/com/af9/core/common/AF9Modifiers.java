package com.af9.core.common;

import com.af9.core.AF9Core;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * Recipe modifiers shared by the AF9 multiblocks (used from the KubeJS startup scripts).
 */
public final class AF9Modifiers {

    private AF9Modifiers() {}

    /** Only starts a recipe when the hatches can supply its full EU/t (see {@link IPowerGated}). */
    public static final RecipeModifier POWER_GATE = (machine, recipe) -> {
        if (!(machine instanceof IPowerGated gated)) {
            return RecipeModifier.nullWrongType(IPowerGated.class, machine);
        }
        if (gated.getAvailableEUt() < RecipeHelper.getRealEUt(recipe).getTotalEU()) return ModifierFunction.NULL;
        return ModifierFunction.IDENTITY;
    };

    /** What the Planetary Satellite's rays are worth: the recipe's fluid is one 12 mB step, a run rolls 1 to 16 of them. */
    public static final int SATELLITE_STEP = 12;

    /**
     * The Planetary Satellite's yield. The recipe's neutronium is {@link #SATELLITE_STEP} mB a run (times the parallels
     * it was multiplied by); every run of them rolls its own steps, and the reflector (the recipe id ends in the
     * reflector's material) sets the range: iridium 12 - 120 mB (1 to 10 steps), osmiridium 36 - 120, unobtainium-vibranium
     * 96 - 120, tritanium 120 - 144 (10 to 12), ceranium 144 - 168, neutronium 168 - 192. Put it after the parallels.
     */
    public static final RecipeModifier SATELLITE_YIELD = (machine, recipe) -> {
        String path = recipe.id == null ? "" : recipe.id.getPath();
        int min;
        int max;
        if (path.endsWith("_neutronium")) {
            min = 14;
            max = 16;
        } else if (path.endsWith("_ceranium")) {
            min = 12;
            max = 14;
        } else if (path.endsWith("_tritanium")) {
            min = 10;
            max = 12;
        } else if (path.endsWith("_unobtainium_vibranium")) {
            min = 8;
            max = 10;
        } else if (path.endsWith("_osmiridium")) {
            min = 3;
            max = 10;
        } else {
            min = 1;
            max = 10;
        }
        return (ModifierFunction) (GTRecipe modified) -> {
            List<Content> fluids = modified.outputs.get(FluidRecipeCapability.CAP);
            if (fluids == null || fluids.isEmpty()) return modified;
            FluidStack[] stacks = FluidRecipeCapability.CAP.of(fluids.get(0).content).getStacks();
            if (stacks.length == 0) return modified;
            int runs = Math.max(1, stacks[0].getAmount() / SATELLITE_STEP);
            RandomSource random = machine.getLevel() != null ? machine.getLevel().getRandom() : RandomSource.create();
            long total = 0;
            for (int i = 0; i < runs; i++) {
                total += SATELLITE_STEP * (long) (min + random.nextInt(max - min + 1));
            }
            GTRecipe result = modified.copy();
            result.outputs.put(FluidRecipeCapability.CAP, List.of(new Content(
                    FluidIngredient.of(new FluidStack(stacks[0], (int) Math.min(total, Integer.MAX_VALUE))),
                    ChanceLogic.getMaxChancedValue(), ChanceLogic.getMaxChancedValue(), 0)));
            return result;
        };
    };

    /**
     * The Planetary Satellite's reflector wears one point a run. Wired to the machine's {@code afterWorking} in the
     * startup script: {@code RecipeLogic.onRecipeFinish} fires that first, exactly once per completed cycle, so the
     * wear counts cycles, not parallel runs (a batch of N costs one point like any other cycle). The recipe holds
     * the reflector as a not-consumed input, so nothing else ever touches it: when its durability is gone the slot
     * empties and the recipe stops matching until a fresh reflector goes in.
     */
    public static void satelliteWear(IRecipeLogicMachine machine) {
        for (IRecipeHandler<?> handler : machine.getCapabilitiesFlat(IO.IN, ItemRecipeCapability.CAP)) {
            if (!(handler instanceof IItemHandlerModifiable slots)) continue;
            for (int slot = 0; slot < slots.getSlots(); slot++) {
                ItemStack stack = slots.getStackInSlot(slot);
                if (!stack.isDamageableItem()) continue;
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (id == null || !AF9Core.MOD_ID.equals(id.getNamespace())
                        || !id.getPath().endsWith("_ray_reflector")) {
                    continue;
                }
                int worn = stack.getDamageValue() + 1;
                if (worn >= stack.getMaxDamage()) {
                    slots.setStackInSlot(slot, ItemStack.EMPTY);
                } else {
                    stack.setDamageValue(worn);
                    slots.setStackInSlot(slot, stack);
                }
                return;
            }
        }
    }
}
