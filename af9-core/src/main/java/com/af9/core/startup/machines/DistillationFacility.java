package com.af9.core.startup.machines;

import com.af9.core.distillation.DistillationFacilityMachine;
import com.af9.core.distillation.DistillationModuleMachine;
import com.af9.core.pattern.DistillationFacilityPipes;
import com.af9.core.startup.AF9Machines;

import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

/**
 * AF9 - Distillation Facility: the IV Distillation Tower of the pack. A 10 x 7 x 15 walk-through of real GT fluid pipes (Tungsten
 * and Stainless Steel Small Fluid Pipes) round three Distillation Tower Modules, taken 1:1 from Janne's structure file (v3.nbt)
 * block by block, with the file's exact pipe connections (af9-core DistillationFacilityPipes, as in the Chemical Plant): the pattern
 * asks for the exact pipe and the exact connection mask at every pipe spot, and the controller connects the pipes of an
 * auto-built facility itself while it is unformed.
 *
 * The controller stands where the file's green wool stood, in the arch at the front. The hatches go on the Turbine Casing spine
 * that runs down the middle of the floor (up to six energy hatches, one maintenance hatch, fluid and item hatches). The three
 * controllers where the file had Distillation Towers are Distillation Tower Modules (gtceu:distillation_module), small
 * multiblocks of their own: each has its own recipe (mode tab, recipe lock), at most one Parallel Hatch in its casings and its muffler
 * hatch on top, and runs on the facility's hatches. Behind each tower a rotor holder (EV or better) holds the rotor that speeds it up
 * and saves energy. The facility is its own clean room.
 *
 * Modes (the Distillation Tower's three): Distillation Tower, Distillation, Cryogenic Rectification. Perfect overclock, no batch mode.
 * Docs: docs/distillation-facility.md.
 *
 * Rows bottom -&gt; top (the file's y 1 to 7), aisles back -&gt; front (the file's z 0 to 14), columns as in the file's x (0 to 9), left to
 * right as seen from the front. The file stands in the world with the controller facing SOUTH. A space is anything.
 *
 *   S controller (the green wool)         D a Distillation Tower Module           R a rotor holder, EV or better
 *   M Turbine Casing: the hatch spot      U a muffler hatch                       K Kanthal Coil Block
 *   C Clean Casing  c Clean Casing of a tower (one Parallel Hatch a tower may stand here)
 *   I Inert Casing  T Stable Casing  F Stainless Firebox Casing  A air (the tower's shaft)
 *   a b d e f g h   the pipes (see the where() lines)
 */
public final class DistillationFacility {

    private DistillationFacility() {}

    public static void register() {
        // the modes: the Distillation Tower's recipes and the fab chemistry's two separations (af9-core FabModes gives the tower the same)
        // ---- the tower: a module of its own, in the Distillation Tower's shape with a kanthal coil at its foot. Its structure is the
        // casings round it; its recipe is its own, what it runs on (energy, inputs, outputs, rotor, clean room) is the facility's.
        // The tower is 5 or 6 high (the file's towers are 6, 5 and 6): the shaft is 2 or 3 layers.

        AF9Machines.multiblock("distillation_module", holder -> new DistillationModuleMachine(holder), b -> b
                .langValue("Distillation Tower Module")
                .rotationState(RotationState.NON_Y_AXIS)
                .allowFlip(false)
                .recipeTypes(AF9Machines.type("distillation_tower"), AF9Machines.type("fab_distillation"),
                    AF9Machines.type("fab_cryogenic_rectification"))
                // no batch mode: parallels from the hatch, the rotor's speed and energy, a perfect overclock
                .recipeModifiers(DistillationModuleMachine.TOWER_PARALLEL, DistillationModuleMachine.FACILITY, GTRecipeModifiers.OC_PERFECT)
                .appearanceBlock(AF9Machines.blockSupplier("gtceu:clean_machine_casing"))
                .pattern(definition -> FactoryBlockPattern.start(RelativeDirection.RIGHT, RelativeDirection.BACK, RelativeDirection.UP)
                    .aisle("CSC", "CKC", "CCC")
                    .aisle("CCC", "CKC", "CCC")
                    .aisle("CCC", "CAC", "CCC").setRepeatable(2, 3)
                    .aisle("CCC", "CUC", "CCC")
                    .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                    .where('C', AF9Machines.blocks("gtceu:clean_machine_casing")
                        .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1, 0))
                        // the tower's own energy hatch: it powers this tower alone
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(1, 1)))
                    .where('K', AF9Machines.blocks("gtceu:kanthal_coil_block"))
                    .where('A', Predicates.air())
                    .where('U', Predicates.abilities(PartAbility.MUFFLER).setMaxGlobalLimited(1, 1))
                    .build())
                .workableCasingModel(AF9Machines.rl("gtceu:block/casings/solid/machine_casing_clean_stainless_steel"),
                    AF9Machines.rl("gtceu:block/multiblock/distillation_tower")));

        // ---- the facility

        AF9Machines.multiblock("distillation_facility", holder -> new DistillationFacilityMachine(holder), b -> b
                .langValue("Distillation Facility")
                .rotationState(RotationState.NON_Y_AXIS)
                .allowFlip(false)
                // the towers run the recipes; these are the facility's categories in the recipe viewers
                .recipeTypes(AF9Machines.type("distillation_tower"), AF9Machines.type("fab_distillation"),
                    AF9Machines.type("fab_cryogenic_rectification"))
                .recipeModifiers(GTRecipeModifiers.OC_PERFECT)
                // the controller and the hatches on the spine wear the Steel Turbine Casing, the hatches in a tower the clean stainless one
                .appearanceBlock(AF9Machines.blockSupplier("gtceu:steel_turbine_casing"))
                .partAppearance((controller, part, side) -> DistillationFacilityMachine.partAppearance(part))
                .pattern(definition -> FactoryBlockPattern.start()
                    .aisle("   I I    ", "   I I    ", "   III    ", "          ", "          ", "          ", "          ")
                    .aisle("   IMI    ", "   IMI    ", "   III    ", "          ", "          ", "          ", "          ")
                    .aisle("FFFIMITTTT", "lllIIITTTT", "lllIIIa   ", "lllbjbd   ", "lll       ", "lll       ", "lll       ")
                    .aisle("FCCIMITTTT", "DKlIIITTRT", "lKlIIIe   ", "lAlfkfg   ", "lAl       ", "lAl       ", "lUl       ")
                    .aisle("FFFIMITTTT", "lllIIITTTT", "lllIIIe   ", "lllfkfg   ", "lll       ", "lll       ", "lll       ")
                    .aisle("   IMI    ", "   III    ", "   III    ", "          ", "          ", "          ", "          ")
                    .aisle("FFFIMITTTT", "mmmIIITTTT", "mmmIIIe   ", "mmmhkfg   ", "mmmg      ", "mmm       ", "          ")
                    .aisle("FCCIMITTTT", "DKmIIITTRT", "mKmIIIa   ", "mAmbjbd   ", "mAm       ", "mUm       ", "          ")
                    .aisle("FFFIMITTTT", "mmmIIITTTT", "mmmIIIe   ", "mmmfkfg   ", "mmm       ", "mmm       ", "          ")
                    .aisle("   IMI    ", "   III    ", "   III    ", "          ", "          ", "          ", "          ")
                    .aisle("FFFIMITTTT", "nnnIIITTTT", "nnnIIIe   ", "nnnfkfg   ", "nnn       ", "nnn       ", "nnn       ")
                    .aisle("FCCIMITTTT", "DKnIIITTRT", "nKnIIIa   ", "nAn   a   ", "nAnbjbd   ", "nAn       ", "nUn       ")
                    .aisle("FFFIMITTTT", "nnnIIITTTT", "nnnIIIa   ", "nnnbjbd   ", "nnn       ", "nnn       ", "nnn       ")
                    .aisle("   IMI    ", "   ISI    ", "   III    ", "          ", "          ", "          ", "          ")
                    .aisle("   I I    ", "   I I    ", "   III    ", "          ", "          ", "          ", "          ")
                    .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                    .where('D', DistillationFacilityMachine.modules())
                    .where('R', DistillationFacilityMachine.rotorHolders())
                    .where('I', AF9Machines.blocks("gtceu:inert_machine_casing"))
                    .where('T', AF9Machines.blocks("gtceu:stable_machine_casing"))
                    .where('F', AF9Machines.blocks("af9:stainless_firebox_casing"))
                    .where('C', AF9Machines.blocks("gtceu:clean_machine_casing"))
                    // a parallel hatch in a casing of a tower: at most one for each of the three (a predicate of its own for each tower, so an
                    // auto-build puts one in every tower and not all three in the first)
                    .where('l', AF9Machines.blocks("gtceu:clean_machine_casing")
                        .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1, 0))
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(1, 1)))
                    .where('m', AF9Machines.blocks("gtceu:clean_machine_casing")
                        .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1, 0))
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(1, 1)))
                    .where('n', AF9Machines.blocks("gtceu:clean_machine_casing")
                        .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1, 0))
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(1, 1)))
                    .where('K', AF9Machines.blocks("gtceu:kanthal_coil_block"))
                    .where('A', Predicates.air())
                    .where('U', Predicates.abilities(PartAbility.MUFFLER).setMaxGlobalLimited(3, 3))
                    // the hatch spine: the Turbine Casings of the file
                    .where('M', AF9Machines.blocks("gtceu:steel_turbine_casing")
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(6, 0))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(3, 1))
                        // outputs, fluid hatches and item buses together: at most 3 (the pack wants real automation for the facility)
                        .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS, PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(3, 3))
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 0))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
                    // the pipes, with the file's exact connection masks (bit 1 << Direction ordinal, controller facing SOUTH)
                    .where('a', DistillationFacilityPipes.pipe("gtceu:tungsten_small_fluid_pipe", 3))
                    .where('b', DistillationFacilityPipes.pipe("gtceu:tungsten_small_fluid_pipe", 48))
                    .where('d', DistillationFacilityPipes.pipe("gtceu:tungsten_small_fluid_pipe", 17))
                    .where('e', DistillationFacilityPipes.pipe("gtceu:stainless_steel_small_fluid_pipe", 3))
                    .where('f', DistillationFacilityPipes.pipe("gtceu:stainless_steel_small_fluid_pipe", 48))
                    .where('g', DistillationFacilityPipes.pipe("gtceu:stainless_steel_small_fluid_pipe", 17))
                    .where('h', DistillationFacilityPipes.pipe("gtceu:stainless_steel_small_fluid_pipe", 34))
                    // the 9 pipes the file has in a frame (middle of the long runs); the frame is not asked for
                    .where('j', DistillationFacilityPipes.pipe("gtceu:tungsten_small_fluid_pipe", 48, true))
                    .where('k', DistillationFacilityPipes.pipe("gtceu:stainless_steel_small_fluid_pipe", 48, true))
                    .build())
                .workableCasingModel(AF9Machines.rl("gtceu:block/casings/mechanic/machine_casing_turbine_steel"),
                    AF9Machines.rl("gtceu:block/multiblock/distillation_tower")));
    }
}
