package com.af9.core.startup.machines;

import com.af9.core.startup.AF9Machines;

import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

/**
 * AF9 - The lithography process around the print: wafer clean-up, the finer coatings.
 * The machines and their recipe types here; items, materials and behaviour: AF9 Core (registry/AF9Items,
 * registry/AF9Materials, LithoMachine); recipes: server_scripts/mods/gtceu/litho_process.js. Spec:
 * docs/semiconductor-factory.md §18
 *
 * ---- Coater Track ----
 * The spin-coat track in front of the exposure tool (a real fab's track does the coating and the developing; here the line
 * keeps the developing): HMDS prime, bottom anti-reflective coat, resist spun on, topcoat, soft bake. It turns a blank
 * wafer into the coated wafer of its node, and what it spins off is spent solvent. Plain GT machine logic, no AF9 Core class.
 */
public final class LithoProcess {

    private LithoProcess() {}

    public static void register() {
        // 3 x 3 x 3, plascrete like the other lithography machines: the front with the controller, then the track itself
        // (the spin chuck, a gearbox on the floor, with the dispense lines over it and windows to the sides, around the one
        // air block the wafer rides in), then the back with the hotplate. Aisles front -> back, rows bottom -> top; hatches
        // on any plascrete.

        AF9Machines.multiblock("wafer_coater", holder -> new WorkableElectricMultiblockMachine(holder), b -> b
                .langValue("Coater Track")
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeTypes(AF9Machines.type("wafer_coating"))
                .recipeModifiers(GTRecipeModifiers.PARALLEL_HATCH, GTRecipeModifiers.OC_NON_PERFECT)
                .appearanceBlock(AF9Machines.blockSupplier("gtceu:plascrete"))
                .tooltips(AF9Machines.lines("af9.wafer_coater.tooltip.", 4))
                .pattern(definition -> FactoryBlockPattern.start(RelativeDirection.LEFT, RelativeDirection.UP,
                    RelativeDirection.BACK)
                    .aisle("CCC", "CMC", "CCC") // front with the controller
                    .aisle("FRF", "W#W", "CPC") // fan filters + spin chuck, the track, dispense lines
                    .aisle("CCC", "CHC", "CCC") // back with the hotplate
                    .where('M', Predicates.controller(Predicates.blocks(definition.get())))
                    // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count))
                    .where('C', AF9Machines.blocks("gtceu:plascrete")
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(6, 1))
                        .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1, 0))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
                    .where('P', AF9Machines.blocks("af9:plascrete_pipe_casing"))       // chemical dispense lines
                    .where('F', AF9Machines.blocks("af9:plascrete_filter_casing"))     // fan filter units
                    .where('R', AF9Machines.blocks("gtceu:stainless_steel_gearbox"))      // spin chuck
                    .where('H', AF9Machines.blocks("gtceu:heatproof_machine_casing"))     // hotplate
                    .where('W', AF9Machines.blocks("gtceu:cleanroom_glass"))              // windows of the track
                    .where('#', Predicates.air())                                        // the track
                    .build())
                .workableCasingModel(AF9Machines.rl("gtceu:block/casings/cleanroom/plascrete"), AF9Machines.rl("gtceu:block/multiblock/network_switch")));
    }
}
