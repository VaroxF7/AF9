package com.af9.core.startup.machines;

import com.af9.core.startup.AF9Machines;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;

import net.minecraft.network.chat.Component;

/**
 * LV and MV Quadruple Input / Output Hatch. GT's quad hatches start at EV; these are the same part (GT's own
 * FluidHatchPartMachine with four tanks, registered like GT's registerFluidHatches: the quad pipe overlay and BOTH the
 * plain IMPORT_FLUIDS / EXPORT_FLUIDS ability and the 4X one, which is what multiblock patterns ask for) at LV and MV.
 * Recipes: recipes/gen. Ids: gtceu:&lt;lv|mv&gt;_input_hatch_4x, gtceu:&lt;lv|mv&gt;_output_hatch_4x (GT's naming).
 */
public final class QuadHatches {

    private QuadHatches() {}

    public static void register() {
        // GT's initial tank size of a quad hatch (it scales it with the tier itself)
        final int initialCapacity = FluidHatchPartMachine.INITIAL_TANK_CAPACITY_4X;

        AF9Machines.tiered("input_hatch_4x", new int[] { GTValues.LV, GTValues.MV },
                (holder, tier) -> new FluidHatchPartMachine(holder, tier, IO.IN, initialCapacity, 4),
                (tier, builder) -> builder
                        .langValue(GTValues.VN[tier] + " Quadruple Input Hatch")
                        .rotationState(RotationState.ALL)
                        .colorOverlayTieredHullModel("overlay_pipe_in_emissive", "overlay_pipe_4x", "overlay_fluid_hatch")
                        .abilities(PartAbility.IMPORT_FLUIDS, PartAbility.IMPORT_FLUIDS_4X)
                        .tooltips(Component.translatable("gtceu.machine.fluid_hatch.import.tooltip"),
                                Component.translatable("gtceu.universal.tooltip.fluid_storage_capacity_mult", 4,
                                        String.valueOf(initialCapacity << tier))));

        AF9Machines.tiered("output_hatch_4x", new int[] { GTValues.LV, GTValues.MV },
                (holder, tier) -> new FluidHatchPartMachine(holder, tier, IO.OUT, initialCapacity, 4),
                (tier, builder) -> builder
                        .langValue(GTValues.VN[tier] + " Quadruple Output Hatch")
                        .rotationState(RotationState.ALL)
                        .colorOverlayTieredHullModel("overlay_pipe_out_emissive", "overlay_pipe_4x", "overlay_fluid_hatch")
                        .abilities(PartAbility.EXPORT_FLUIDS, PartAbility.EXPORT_FLUIDS_4X)
                        .tooltips(Component.translatable("gtceu.machine.fluid_hatch.export.tooltip"),
                                Component.translatable("gtceu.universal.tooltip.fluid_storage_capacity_mult", 4,
                                        String.valueOf(initialCapacity << tier))));
    }
}
