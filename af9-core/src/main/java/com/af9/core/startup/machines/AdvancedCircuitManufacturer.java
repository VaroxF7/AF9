package com.af9.core.startup.machines;

import com.af9.core.startup.AF9Machines;

import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

/**
 * AF9 - Advanced Circuit Manufacturer: the circuit plant of the Genesis pack (its
 * startup_scripts/Startup/Multiblock Registry/advanced_circuit_manufacturer.js), structure and blocks as they were there.
 * It makes the Pico circuits (server_scripts/mods/gtceu/circuits_af9.js); the controller's recipe is in
 * server_scripts/mods/gtceu/advanced_circuit_manufacturer.js.
 *
 * 17 wide, 5 high, 5 deep: a hall of Large-Scale Assembler Casing in the middle (laminated glass front and back, the
 * controller under the front window), a line of molybdenum disilicide coils with a PTFE pipe casing at its centre and
 * tungsten carbide frames at both ends running through it, tungstensteel gearboxes beside the hall and two wings of
 * nonconducting casing. Rows bottom -&gt; top, aisles back -&gt; front (the controller's last).
 */
public final class AdvancedCircuitManufacturer {

    private AdvancedCircuitManufacturer() {}

    public static void register() {
        AF9Machines.multiblock("advanced_circuit_manufacturer", WorkableElectricMultiblockMachine::new, b -> b
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeTypes(AF9Machines.type("advanced_circuit_manufacturer"))
                // a parallel hatch multiplies the runs; perfect overclocks
                .recipeModifiers(GTRecipeModifiers.PARALLEL_HATCH, GTRecipeModifiers.OC_PERFECT)
                .appearanceBlock(AF9Machines.blockSupplier("gtceu:large_scale_assembler_casing"))
                .tooltips(AF9Machines.lines("af9.advanced_circuit_manufacturer.tooltip.", 4))
                .pattern(definition -> FactoryBlockPattern.start()
                    .aisle("#######C#C#######", "######CCCCC######", "#####CCGGGCC#####", "######CCCCC######", "#######C#C#######")
                    .aisle("######CCCCC######", "###DDDCCCCCDDD###", "#DD###Y###Y###DD#", "###DDDCCCCCDDD###", "######CCCCC######")
                    .aisle("#####CCCCCCC#####", "##DDDD#PBP#DDDD##", "ZZZZPPPPFPPPPZZZZ", "##DDDD#PBP#DDDD##", "#####CCCCCCC#####")
                    .aisle("######CCCCC######", "###DDDCCCCCDDD###", "#DD###Y###Y###DD#", "###DDDCCCCCDDD###", "######CCCCC######")
                    .aisle("#######C#C#######", "######CCKCC######", "#####CCGGGCC#####", "######CCCCC######", "#######C#C#######")
                    .where('K', Predicates.controller(Predicates.blocks(definition.get())))
                    .where('G', AF9Machines.blocks("gtceu:laminated_glass"))
                    .where('Y', AF9Machines.blocks("gtceu:tungstensteel_gearbox"))
                    .where('P', AF9Machines.blocks("gtceu:molybdenum_disilicide_coil_block"))
                    .where('D', AF9Machines.blocks("gtceu:nonconducting_casing"))
                    .where('F', AF9Machines.blocks("gtceu:ptfe_pipe_casing"))
                    .where('Z', AF9Machines.blocks("gtceu:tungsten_carbide_frame"))
                    .where('B', AF9Machines.blocks("gtceu:assembly_line_grating"))
                    // any casing of the hall may be a part; parts have a maximum only, never a required count
                    // (setMaxGlobalLimited(max, preview count)). The Pico recipes draw 300 A of UHV: a laser target hatch
                    // carries that.
                    .where('C', AF9Machines.blocks("gtceu:large_scale_assembler_casing")
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1, 0))
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(4, 2))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(4, 2))
                        .or(Predicates.abilities(PartAbility.COMPUTATION_DATA_RECEPTION).setMaxGlobalLimited(1, 1))
                        .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1, 1))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
                    .where('#', Predicates.any())
                    .build())
                .workableCasingModel(AF9Machines.rl("gtceu:block/casings/gcym/large_scale_assembling_casing"),
                    AF9Machines.rl("gtceu:block/multiblock/fusion_reactor")));
    }
}
