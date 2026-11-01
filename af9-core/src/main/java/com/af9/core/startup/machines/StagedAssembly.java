package com.af9.core.startup.machines;

import com.af9.core.machine.StagedAssemblyMachine;
import com.af9.core.startup.AF9Machines;

import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

/**
 * AF9 - Staged Assembly: a multiblock that builds its recipes in stages. Spec: this file's tooltips.
 * One recipe carries every stage's inputs; the machine (AF9 Core: StagedAssemblyMachine, StagedRecipeLogic)
 * runs the stages one after the other and only accepts exactly the running stage's inputs, so each stage is
 * fed separately. Recipes: server_scripts/mods/gtceu/staged_assembly.js
 */
public final class StagedAssembly {

    private StagedAssembly() {}

    public static void register() {
        // Assembly hall: solid steel shell with glass bands, the controller front-bottom-centre.
        // 3 x 3 x 4, aisles back -> front (controller), rows bottom -> top.

        AF9Machines.multiblock("staged_assembly", holder -> new StagedAssemblyMachine(holder), b -> b
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeTypes(AF9Machines.type("staged_assembly"))
                .recipeModifiers(GTRecipeModifiers.OC_PERFECT)
                .appearanceBlock(GTBlocks.CASING_STEEL_SOLID)
                .tooltips(AF9Machines.lines("af9.staged_assembly.tooltip.", 4))
                .pattern(definition -> FactoryBlockPattern.start()
                    .aisle("CCC", "CCC", "CCC")
                    .aisle("CCC", "C#C", "CCC")
                    .aisle("CGC", "G#G", "CGC")
                    .aisle("CSC", "CCC", "CCC")
                    .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                    // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count))
                    .where('C', Predicates.blocks(GTBlocks.CASING_STEEL_SOLID.get())
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 2))
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 2))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(1, 1))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
                    .where('G', AF9Machines.blocks("gtceu:tempered_glass"))
                    .where('#', Predicates.air())
                    .build())
                .workableCasingModel(AF9Machines.rl("gtceu:block/casings/solid/machine_casing_solid_steel"),
                    AF9Machines.rl("gtceu:block/multiblock/assembly_line")));
    }
}
