package com.af9.core.startup.machines;

import com.af9.core.machine.PlasmaTransformerMachine;
import com.af9.core.startup.AF9Machines;

import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

/**
 * Plasma Transformer: condenses superstate plasmas to solid plates at XPS (Star Matter Plasma Steel for the NVM boards;
 * the DTPF forges the superstate). Recipes: recipes/gen. 3 x 4 x 3 condenser, aisles back -> front, rows bottom -> top.
 * Controller front-bottom-centre.
 */
public final class PlasmaTransformer {

    private PlasmaTransformer() {}

    public static void register() {
        AF9Machines.multiblock("plasma_transformer", PlasmaTransformerMachine::new, b -> b
                .langValue("Plasma Transformer")
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeTypes(AF9Machines.type("plasma_transform"))
                .recipeModifiers(GTRecipeModifiers.OC_PERFECT)
                .appearanceBlock(GTBlocks.CASING_STAINLESS_CLEAN)
                .tooltips(AF9Machines.lines("af9.plasma_transformer.tooltip.", 3))
                .pattern(definition -> FactoryBlockPattern.start()
                        .aisle("XXX", "XXX", "XXX", "XXX")
                        .aisle("XXX", "X#X", "X#X", "XXX")
                        .aisle("XSX", "X#X", "X#X", "XXX")
                        .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                        // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count))
                        .where('X', Predicates.blocks(GTBlocks.CASING_STAINLESS_CLEAN.get())
                                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(1, 1))
                                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(1, 1))
                                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(2, 1))
                                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
                        .where('#', Predicates.air())
                        .build())
                .workableCasingModel(AF9Machines.rl("gtceu:block/casings/solid/machine_casing_clean_stainless_steel"),
                        AF9Machines.rl("gtceu:block/multiblock/assembly_line")));
    }
}
