package com.af9.core.startup.machines;

import com.af9.core.machine.LargeElectricImplosionMachine;
import com.af9.core.startup.AF9Machines;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.SimpleTieredMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

/**
 * AF9 - Electric Implosion Compressor: the implosion compressor without explosives. A capacitor bank fires through the
 * work coils and the shock wave that dynamite made is made of current and an acidic vapor (Lunar Acid Vapor, a cut of
 * Lunar Air) that the discharge flashes into plasma. Recipes: server_scripts/mods/gtceu/electric_implosion.js,
 * rocket_hull.js. Spec: docs/electric-implosion.md
 *
 *   single block   HV-LuV, one dust (and one more) in, one vapor, one plate or naquadah dust out
 *   Large Electric Implosion Compressor, 3 x 3 x 3 like GT's Large Chemical Reactor: the Shock Capacitor Casing, five
 *   cupronickel coil blocks as the work coils (the middle of the cross and its four arms) round a titanium frame, the
 *   controller in the middle of the front. Hatches go in the casings. Parallel hatch, perfect overclocks.
 * Both run the recipe type `electric_implosion_compressor`; GT's own Implosion Compressor keeps the explosive recipes.
 * The Large one also holds a capacitor bank (LargeElectricImplosionMachine): charged recipes, one per normal implosion
 * recipe without its explosive, fire from the bank. The naquadah recipe (4 U-235 dust + vapor -&gt; 1 naquadah dust) is the electric implosion's transmutation.
 */
public final class ElectricImplosion {

    private ElectricImplosion() {}

    public static void register() {
        // ---- The single blocks ----

        AF9Machines.tiered("electric_implosion_compressor", new int[] { GTValues.HV, GTValues.EV, GTValues.IV, GTValues.LuV },
                true, tier -> 16000, (holder, tier, tankScaling) -> new SimpleTieredMachine(holder, tier, tankScaling),
                (tier, builder) -> builder
                        .langValue("" + GTValues.VLVH[tier] + " Electric Implosion Compressor " + GTValues.VLVT[tier])
                        .recipeTypes(AF9Machines.type("electric_implosion_compressor"))
                        .recipeModifiers(GTRecipeModifiers.OC_NON_PERFECT)
                        .editableUI(SimpleTieredMachine.EDITABLE_UI_CREATOR.apply(
                            GTCEu.id("electric_implosion_compressor"), AF9Machines.type("electric_implosion_compressor")))
                        .workableTieredHullModel(GTCEu.id("block/machines/electric_implosion_compressor"))
                        .tooltips(AF9Machines.lines("af9.electric_implosion_compressor.tooltip.", 2)));

        // ---- The multiblock ----

        AF9Machines.multiblock("large_electric_implosion_compressor", holder -> new LargeElectricImplosionMachine(holder), b -> b
                .langValue("Large Electric Implosion Compressor")
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeTypes(AF9Machines.type("electric_implosion_compressor"), AF9Machines.type("charged_implosion"))
                .recipeModifiers(GTRecipeModifiers.PARALLEL_HATCH, GTRecipeModifiers.OC_PERFECT)
                .appearanceBlock(AF9Machines.blockSupplier("af9:shock_capacitor_casing"))
                .tooltips(AF9Machines.lines("af9.large_electric_implosion_compressor.tooltip.", 4))
                .pattern(definition -> FactoryBlockPattern.start()
                    .aisle("XXX", "XCX", "XXX")
                    .aisle("XCX", "CPC", "XCX")
                    .aisle("XXX", "XSX", "XXX")
                    .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                    // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count))
                    .where('X', AF9Machines.blocks("af9:shock_capacitor_casing")
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1, 0))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
                    .where('C', AF9Machines.blocks("gtceu:cupronickel_coil_block"))
                    .where('P', AF9Machines.blocks("gtceu:titanium_frame"))
                    .build())
                .workableCasingModel(AF9Machines.rl("af9:block/shock_capacitor_casing"),
                    AF9Machines.rl("gtceu:block/multiblock/large_electric_implosion_compressor")));
    }
}
