package com.af9.core.startup.machines;

import com.af9.core.machine.AF9MachineModels;
import com.af9.core.machine.IndustrialCutterMachine;
import com.af9.core.startup.AF9Machines;

import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

/**
 * AF9 - Industrial Cutting Machine: GTNH's Industrial Cutting Machine (GT5-Unofficial, MTEIndustrialCuttingMachine,
 * LGPL-3.0; credits in CREDITS-textures.md, licence in licenses/) copied into GTCEu: the same 9 x 4 x 3 structure with
 * its purple Cutting Factory Frame casings, frame boxes (tungsten carbide here for GTNH's tantalum carbide), a glass
 * window in the front and the back, and in the middle the animated saw blade with the original sprites and spin-up
 * (af9-core SawBladeRender). It replaces GTCEu's Large Cutting Saw (gtceu:large_cutter, removed in the fork) and runs
 * both of that machine's modes: the Cutting Machine's recipes (gtceu:cutter) and the Lathe's (gtceu:lathe), switched
 * with GT's mode tab. Controller logic: af9-core IndustrialCutterMachine.
 *
 * Rows bottom -&gt; top, aisles back -&gt; front (the controller's last); GTNH's slices, reversed. The controller sits in the
 * front, third column of the shape, one row above the floor; the blade is centred 2.5 columns along from it, 1 block
 * behind it, at the floor's top (af9-core renders it: back 1, side 2.5, up 0, 3 x 3 blocks). The hatches go in the
 * casings 'D' (maximums only, never a required count: setMaxGlobalLimited(max, preview count)). GTNH's black steel
 * sheet metal is a steel frame here. A space in GTNH's shape is anything ('#').
 */
public final class IndustrialCutter {

    private IndustrialCutter() {}

    public static void register() {
        AF9Machines.multiblock("industrial_cutting_machine", holder -> new IndustrialCutterMachine(holder), b -> b
                .langValue("Industrial Cutting Machine")
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeTypes(AF9Machines.type("cutter"), AF9Machines.type("lathe"))
                // a parallel hatch multiplies the cuts; perfect overclocks
                .recipeModifiers(GTRecipeModifiers.PARALLEL_HATCH, GTRecipeModifiers.OC_PERFECT)
                .appearanceBlock(AF9Machines.blockSupplier("af9:cutter_casing"))
                .tooltips(AF9Machines.lines("af9.industrial_cutting_machine.tooltip.", 3))
                .pattern(definition -> FactoryBlockPattern.start()
                    .aisle("#DDDDDDDB", "#DDAAAADB", "#DDAAAADB", "##DBBBBB#")
                    .aisle("CDCCCCCC#", "CDB####C#", "CD#####C#", "BCCDDDDD#")
                    .aisle("#DDDDDDDB", "#DKAAAADB", "#DDAAAADB", "##DBBBBB#")
                    .where('K', Predicates.controller(Predicates.blocks(definition.get())))
                    .where('A', AF9Machines.blocks("gtceu:laminated_glass"))
                    .where('B', AF9Machines.blocks("gtceu:tungsten_carbide_frame"))
                    .where('C', AF9Machines.blocks("gtceu:steel_frame"))
                    .where('D', AF9Machines.blocks("af9:cutter_casing")
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1, 0))
                        .or(Predicates.abilities(PartAbility.MUFFLER).setMaxGlobalLimited(1, 0))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
                    .where('#', Predicates.any())
                    .build())
                // Cutting Factory Frame casings with GTNH's controller face, and the saw blade (af9-core SawBladeRender)
                .model(AF9MachineModels.workableCasingWithSawBlade(AF9Machines.rl("af9:block/cutter_casing"),
                    AF9Machines.rl("gtceu:block/multiblock/industrial_cutter"), 1, 2.5f, 0, 3))
                .hasBER(true));
    }
}
