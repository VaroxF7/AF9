package com.af9.core.startup.machines;

import com.af9.core.machine.AF9MachineModels;
import com.af9.core.machine.FiltrationPlantMachine;
import com.af9.core.startup.AF9Machines;

import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.common.data.GCYMBlocks;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

/**
 * AF9 - Water Filtration Plant: an open watertight basin that filters dirty water through two
 * strips of Water Filter Casing to distilled water and skims the floating debris off the top.
 * Controller behaviour: AF9 Core (FiltrationPlantMachine). Basin render: FiltrationBasinRender
 * (animated water, floating debris, 3D skimmer arm). Recipes: server_scripts/mods/gtceu/water_filtration.js
 *
 * 10 x 7 x 2 basin (10 deep), aisles back -&gt; front, rows bottom -&gt; top. The floor carries two 1 x 6 strips
 * of Water Filter Casing (F); the 8 x 5 above the floor, one layer high, is open air (the basin the
 * render draws its water in); the controller sits in the middle of the front wall, in the top layer.
 */
public final class WaterFiltration {

    private WaterFiltration() {}

    public static void register() {
        AF9Machines.multiblock("water_filtration_plant", holder -> new FiltrationPlantMachine(holder), b -> b
                .langValue("Water Filtration Plant")
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeTypes(AF9Machines.type("water_filtration"))
                .recipeModifiers(GTRecipeModifiers.PARALLEL_HATCH, GTRecipeModifiers.OC_PERFECT)
                .appearanceBlock(GCYMBlocks.CASING_WATERTIGHT)
                .tooltips(AF9Machines.lines("af9.water_filtration_plant.tooltip.", 4))
                .pattern(definition -> FactoryBlockPattern.start()
                    // back wall
                    .aisle("XXXXXXX", "XXXXXXX")
                    // the basin: floor with the two filter strips (F), open water above
                    .aisle("XXXXXXX", "XAAAAAX")
                    .aisle("XXFXFXX", "XAAAAAX")
                    .aisle("XXFXFXX", "XAAAAAX")
                    .aisle("XXFXFXX", "XAAAAAX")
                    .aisle("XXFXFXX", "XAAAAAX")
                    .aisle("XXFXFXX", "XAAAAAX")
                    .aisle("XXFXFXX", "XAAAAAX")
                    .aisle("XXXXXXX", "XAAAAAX")
                    .aisle("XXXXXXX", "XXXSXXX")
                    .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                    // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count))
                    .where('X', Predicates.blocks(GCYMBlocks.CASING_WATERTIGHT.get())
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(1, 1))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(1, 1))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1, 0))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
                    .where('F', AF9Machines.blocks("af9:water_filter_casing"))
                    .where('A', Predicates.air())
                    .build())
                // watertight casing + LCR overlay, and the basin while it runs: water, debris, skimmer arm.
                // The water is 5 wide and 8 long, centred 4.5 behind the controller, in the controller's layer
                .model(AF9MachineModels.workableCasingWithFiltrationBasin(
                    AF9Machines.rl("gtceu:block/casings/gcym/watertight_casing"),
                    AF9Machines.rl("gtceu:block/multiblock/large_chemical_reactor"), 0.0f, 4.5f, 0.0f, 5.0f, 8.0f))
                .hasBER(true));
    }
}
