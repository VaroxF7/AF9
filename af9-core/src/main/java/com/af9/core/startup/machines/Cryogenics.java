package com.af9.core.startup.machines;

import com.af9.core.common.AF9Modifiers;
import com.af9.core.machine.SupercoolerMachine;
import com.af9.core.machine.part.CoolantHatchPartMachine;
import com.af9.core.startup.AF9Machines;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

import net.minecraft.network.chat.Component;

/**
 * AF9 - Cryogenics: the Supercooling Cryostat and the Coolant Hatch. Spec: docs/semiconductor-factory.md
 *
 * Supercooling Cryostat (HV): needs 4A of HV (two energy hatches) to run at all, perfect overclocks above that.
 * Two modes, one after the other:
 *   dense cooling   gas -&gt; dense liquid (compressed and chilled; the intermediate)
 *   supercooling    the dense liquid goes back in -&gt; supercooled fluid, rated at an effective -5000 K
 * Supercooled hydrogen, argon, xenon and endion (the End's noble gas, boule_melting.js) are the only fluids the
 * Coolant Hatch takes; the Particle Accelerator and the Orbital Lithography Station take their fluids only through it.
 * (GT fluids cannot be below 0 K, so the supercooled fluids sit at 1 K; the -5000 K is the cryostat's rating.)
 * Machine behaviour: AF9 Core (SupercoolerMachine, CoolantHatchPartMachine). Recipes: server_scripts/mods/gtceu/
 * cryogenics.js
 */
public final class Cryogenics {

    private Cryogenics() {}

    public static void register() {
        // Cold box: frostproof shell, PTFE heat-exchanger coils, two-stage compressors, a cold chamber behind windows.
        // 5 x 5 x 5, aisles back -> front (controller), rows bottom -> top.

        AF9Machines.multiblock("supercooling_cryostat", holder -> new SupercoolerMachine(holder), b -> b
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeTypes(AF9Machines.type("dense_cooling"), AF9Machines.type("supercooling"))
                // POWER_GATE: only runs when the hatches supply the recipe's full 4A of HV; perfect overclocks above that
                .recipeModifiers(AF9Modifiers.POWER_GATE, GTRecipeModifiers.OC_PERFECT)
                .appearanceBlock(GTBlocks.CASING_ALUMINIUM_FROSTPROOF)
                .tooltips(AF9Machines.lines("af9.supercooling_cryostat.tooltip.", 7))
                .pattern(definition -> FactoryBlockPattern.start()
                    .aisle("FFFFF", "FFFFF", "FFFFF", "FFFFF", "FFFFF")
                    .aisle("FFFFF", "FPPPF", "FPKPF", "FPPPF", "FFFFF")
                    .aisle("FFFFF", "TP#PT", "TK#KT", "TP#PT", "FFFFF")
                    .aisle("FFFFF", "FPPPF", "FPKPF", "FPPPF", "FFFFF")
                    .aisle("FFFFF", "FFFFF", "FFSFF", "FFFFF", "FFFFF")
                    .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                    // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count))
                    .where('F', Predicates.blocks(GTBlocks.CASING_ALUMINIUM_FROSTPROOF.get())
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 2))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
                    .where('P', Predicates.blocks(GTBlocks.CASING_POLYTETRAFLUOROETHYLENE_PIPE.get())) // heat exchanger
                    .where('K', AF9Machines.blocks("gtceu:stainless_steel_gearbox"))                     // compressors
                    .where('T', AF9Machines.blocks("gtceu:tempered_glass"))
                    .where('#', Predicates.air())                                                       // cold chamber
                    .build())
                .workableCasingModel(AF9Machines.rl("gtceu:block/casings/solid/machine_casing_frost_proof"),
                    AF9Machines.rl("gtceu:block/multiblock/vacuum_freezer")));

        // Coolant Hatch: an input hatch for supercooled fluids only, HV-UHV. Tank: 1000 mB x 2^tier (8,000 mB at HV).

        AF9Machines.tiered("coolant_hatch", new int[] { GTValues.HV, GTValues.EV, GTValues.IV, GTValues.LuV, GTValues.ZPM, GTValues.UV,
            GTValues.UHV },
                (holder, tier, tankScaling) -> new CoolantHatchPartMachine(holder, tier),
                (tier, builder) -> builder
                        .langValue("" + GTValues.VN[tier] + " Coolant Hatch")
                        .rotationState(RotationState.ALL)
                        .colorOverlayTieredHullModel("overlay_pipe_in_emissive", (String) null, "overlay_fluid_hatch")
                        .abilities(PartAbility.IMPORT_FLUIDS, CoolantHatchPartMachine.COOLANT_INPUT)
                        .tooltips(Component.translatable("af9.coolant_hatch.tooltip.0"),
                            Component.translatable("af9.coolant_hatch.tooltip.1"),
                            Component.translatable("gtceu.universal.tooltip.fluid_storage_capacity", "" + (1000 << tier))));
    }
}
