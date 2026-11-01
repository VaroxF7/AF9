package com.af9.core.startup.machines;

import com.af9.core.startup.AF9Machines;

import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

/**
 * AF9 - Asteroid fission: the new way to uranium and plutonium. Spec: docs/asteroid-fission.md
 *
 * Brannerite, a uranium-titanium ore, generates only in the Asteroid Field (af9-core: dimension af9:asteroid_field,
 * reached with an Ad Astra rocket; the rocks are AsteroidFieldFeature). The ore becomes yellowcake, the yellowcake the
 * fuel rods of the Extreme Reactors reactor (server_scripts/mods/extremereactors/er_bridge.js), which turns them into
 * steam (GT's Large Steam Turbines and Extreme Reactors' turbines make power from it) and spent rods, which are
 * reprocessed into plutonium. The old ways to uranium and plutonium are closed (server_scripts/mods/gtceu/
 * asteroid_fission.js); the plutonium goes into the Fusion Reactor Mk1 (fusion_reactor.js).
 *
 * What lives where:
 *   this file                                   the side machines' recipe types and structures
 *   server_scripts/mods/gtceu/asteroid_fission  ore veins, the uranium chain, the steam turbine, tags
 *   server_scripts/mods/gtceu/rockets.js        gregified rockets, the propellant
 *   af9-core                                    the materials and the fuel items (com.af9.core.registry), the asteroids
 *                                               and the layer of their ores (com.af9.core.space), RadiationWatch,
 *                                               Extreme Reactors
 *
 * ---- The fission side machines (the reactor itself is Extreme Reactors') ----
 */
public final class AsteroidFission {

    private AsteroidFission() {}

    public static void register() {
        // ---- Heat Exchanger: the hot FLiBe of an Extreme Reactors reactor gives its heat to water ----
        // A 3 x 3 x 5 tower of the reactor's own vocabulary: stable titanium vessel, the titanium pipe casing of the
        // coolant channels as the column the salt climbs. Hatches on any casing, maximums only. HV: 480 EU/t.

        AF9Machines.multiblock("heat_exchanger", holder -> new CoilWorkableElectricMultiblockMachine(holder), b -> b
                .langValue("Heat Exchanger")
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeTypes(AF9Machines.type("heat_exchanger"))
                .recipeModifiers(GTRecipeModifiers.OC_PERFECT)
                .appearanceBlock(GTBlocks.CASING_TITANIUM_STABLE)
                .tooltips(AF9Machines.lines("af9.heat_exchanger.tooltip.", 3))
                .pattern(definition -> FactoryBlockPattern.start()
                    .aisle("XXX", "XXX", "XXX", "XXX", "XXX")
                    .aisle("XXX", "XPX", "XPX", "XPX", "XXX")
                    .aisle("XXX", "XXX", "XSX", "XXX", "XXX")
                    .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                    // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count))
                    .where('X', Predicates.blocks(GTBlocks.CASING_TITANIUM_STABLE.get())
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(2, 2))
                        .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(2, 2))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
                    .where('P', Predicates.blocks(GTBlocks.CASING_TITANIUM_PIPE.get()))
                    .build())
                .workableCasingModel(AF9Machines.rl("gtceu:block/casings/solid/machine_casing_stable_titanium"),
                    AF9Machines.rl("gtceu:block/multiblock/large_chemical_reactor")));

        // ---- Spent Fuel Pool: cooling decladding for every spent rod, the passive bulk path ----

        AF9Machines.multiblock("spent_fuel_pool", holder -> new CoilWorkableElectricMultiblockMachine(holder), b -> b
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeTypes(AF9Machines.type("spent_fuel_pool"))
                .recipeModifiers(GTRecipeModifiers.OC_PERFECT)
                .appearanceBlock(GTBlocks.CASING_STAINLESS_CLEAN)
                .tooltips(AF9Machines.lines("af9.spent_fuel_pool.tooltip.", 3))
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
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(1, 1))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
                    .where('#', Predicates.air())
                    .build())
                .workableCasingModel(AF9Machines.rl("gtceu:block/casings/solid/machine_casing_clean_stainless_steel"),
                    AF9Machines.rl("gtceu:block/multiblock/large_chemical_reactor")));

        // ---- Fuel Fabrication Bay: dusts straight to clad rods and blankets, one passive step ----

        AF9Machines.multiblock("fuel_fabrication_bay", holder -> new CoilWorkableElectricMultiblockMachine(holder), b -> b
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeTypes(AF9Machines.type("fuel_fabrication"))
                .recipeModifiers(GTRecipeModifiers.OC_PERFECT)
                .appearanceBlock(GTBlocks.CASING_STAINLESS_CLEAN)
                .tooltips(AF9Machines.lines("af9.fuel_fabrication_bay.tooltip.", 3))
                .pattern(definition -> FactoryBlockPattern.start()
                    .aisle("XXX", "XXX", "XXX", "XXX")
                    .aisle("XXX", "X#X", "X#X", "XXX")
                    .aisle("XSX", "X#X", "X#X", "XXX")
                    .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                    // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count))
                    .where('X', Predicates.blocks(GTBlocks.CASING_STAINLESS_CLEAN.get())
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(1, 1))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(1, 1))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
                    .where('#', Predicates.air())
                    .build())
                .workableCasingModel(AF9Machines.rl("gtceu:block/casings/solid/machine_casing_clean_stainless_steel"),
                    AF9Machines.rl("gtceu:block/multiblock/assembly_line")));
    }
}
