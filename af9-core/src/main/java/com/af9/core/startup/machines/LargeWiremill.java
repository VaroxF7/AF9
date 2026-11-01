package com.af9.core.startup.machines;

import com.af9.core.machine.AF9MachineModels;
import com.af9.core.machine.LargeWiremillMachine;
import com.af9.core.startup.AF9Machines;

import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

/**
 * AF9 - Large Wiremill: GCYM's Large Wire Factory (gtceu:large_wiremill, removed in the fork, gtceu-fork patch 0022)
 * rebuilt as a wire drawing machine behind glass: a 9 x 4 x 3 box (pattern by Janne). From the left: the casing end
 * wall (column 0), a glass-walled channel one block wide and four long (columns 1-4: glass on the front, the back and
 * the roof, the machine inside), a plate of steel frames (column 5, rising one block above the roof) with a hole in
 * the middle (the die), and a three-long casing box on the right (columns 6-8) with the controller in its front wall,
 * one block further right than the first sketch put it. The channel's middle row is open (spaces: anything, keep it
 * free): af9-core WireDrawRender draws the wire from the die plate along the axis, through a turning bobbin carrier (six bobbins, a closing die) and onto a
 * take-up spool at the left end wall, with no gap. It runs the Wiremill's recipes and its Spooling mode
 * (SpoolingMode.java), switched with GT's mode tab. Controller logic: af9-core LargeWiremillMachine. Docs:
 * docs/large-wiremill.md.
 *
 * Rows bottom -&gt; top, aisles back -&gt; front (the controller's last), columns left -&gt; right as seen from the front.
 * The casings 'D' take the hatches (maximums only, never a required count).
 */
public final class LargeWiremill {

    private LargeWiremill() {}

    public static void register() {
        AF9Machines.multiblock("large_wiremill", holder -> new LargeWiremillMachine(holder), b -> b
                .langValue("Large Wiremill")
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeTypes(AF9Machines.type("wiremill"), AF9Machines.type("spooling"))
                .recipeModifiers(GTRecipeModifiers.PARALLEL_HATCH, GTRecipeModifiers.OC_NON_PERFECT_SUBTICK,
                    GTRecipeModifiers.BATCH_MODE)
                .appearanceBlock(AF9Machines.blockSupplier("gtceu:stress_proof_casing"))
                .tooltips(AF9Machines.lines("af9.large_wiremill.tooltip.", 4))
                .pattern(definition -> FactoryBlockPattern.start()
                    .aisle("DDDDDDDDD", "DGGGGFDDD", "DGGGGFDDD", "     F   ")
                    .aisle("DDDDDDDDD", "D     DDD", "DGGGGFDDD", "     F   ")
                    .aisle("DDDDDDDDD", "DGGGGFDSD", "DGGGGFDDD", "     F   ")
                    .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                    .where('A', Predicates.air())
                    .where('G', AF9Machines.blocks("gtceu:laminated_glass"))
                    .where('F', AF9Machines.blocks("gtceu:steel_frame"))
                    .where('D', AF9Machines.blocks("gtceu:stress_proof_casing")
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1, 0))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
                    .build())
                // GCYM's stress-proof casing with the controller's face (animated) and the wire drawing machine
                .model(AF9MachineModels.workableCasingWithWireDraw(AF9Machines.rl("gtceu:block/casings/gcym/stress_proof_casing"),
                    AF9Machines.rl("gtceu:block/multiblock/large_wiremill")))
                .hasBER(true));
    }
}
