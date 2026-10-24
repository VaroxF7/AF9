// AF9 - Asteroid fission: the new way to uranium and plutonium. Spec: docs/asteroid-fission.md
//
// Brannerite, a uranium-titanium ore, generates only in the Asteroid Field (af9-core: dimension af9:asteroid_field,
// reached with an Ad Astra rocket; the rocks are AsteroidFieldFeature). The ore becomes yellowcake, the yellowcake the
// fuel rods of the Extreme Reactors reactor (server_scripts/mods/extremereactors/er_bridge.js), which turns them into
// steam (GT's Large Steam Turbines and Extreme Reactors' turbines make power from it) and spent rods, which are
// reprocessed into plutonium. The old ways to uranium and plutonium are closed (server_scripts/mods/gtceu/
// asteroid_fission.js); the plutonium goes into the Fusion Reactor Mk1 (fusion_reactor.js).
//
// What lives where:
//   this file                                   the side machines' recipe types and structures
//   server_scripts/mods/gtceu/asteroid_fission  ore veins, the uranium chain, the steam turbine, tags
//   server_scripts/mods/gtceu/rockets.js        gregified rockets, the propellant
//   af9-core                                    the materials and the fuel items (com.af9.core.registry), the asteroids
//                                               and the layer of their ores (com.af9.core.space), RadiationWatch,
//                                               Extreme Reactors

// ---- The fission side machines (the reactor itself is Extreme Reactors') ----
const $FissionMachine = Java.loadClass('com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine')

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    // the molten-salt loop's second half: hot FLiBe and water in, steam and cooled FLiBe out (docs/nuclear-rework.md)
    event.create('heat_exchanger')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(0, 0, 2, 2)
        .setProgressBar(GuiTextures.PROGRESS_BAR_BOILER_HEAT, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.BOILER)
    // cooling decladding: spent rods and water in, decladded dusts out
    event.create('spent_fuel_pool')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(1, 4, 1, 0)
        .setProgressBar(GuiTextures.PROGRESS_BAR_BATH, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.BATH)
    // dusts straight to clad rods in one passive step
    event.create('fuel_fabrication')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(3, 1, 1, 0)
        .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.ASSEMBLER)
})

GTCEuStartupEvents.registry('gtceu:machine', event => {
    // ---- Heat Exchanger: the hot FLiBe of an Extreme Reactors reactor gives its heat to water ----
    // A 3 x 3 x 5 tower of the reactor's own vocabulary: stable titanium vessel, the titanium pipe casing of the
    // coolant channels as the column the salt climbs. Hatches on any casing, maximums only. HV: 480 EU/t.
    event.create('heat_exchanger', 'multiblock')
        .langValue('Heat Exchanger')
        .machine(holder => new $FissionMachine(holder))
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('heat_exchanger')])
        .recipeModifiers([GTRecipeModifiers.OC_PERFECT])
        .appearanceBlock(GTBlocks.CASING_TITANIUM_STABLE)
        ['tooltips(net.minecraft.network.chat.Component[])']([0, 1, 2].map(i =>
            Component.translatable(`af9.heat_exchanger.tooltip.${i}`)))
        .pattern(definition => FactoryBlockPattern.start()
            .aisle('XXX', 'XXX', 'XXX', 'XXX', 'XXX')
            .aisle('XXX', 'XPX', 'XPX', 'XPX', 'XXX')
            .aisle('XXX', 'XXX', 'XSX', 'XXX', 'XXX')
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count))
            .where('X', Predicates.blocks(GTBlocks.CASING_TITANIUM_STABLE.get())
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(2, 2))
                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(2, 2))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
            .where('P', Predicates.blocks(GTBlocks.CASING_TITANIUM_PIPE.get()))
            .build())
        .workableCasingModel('gtceu:block/casings/solid/machine_casing_stable_titanium',
            'gtceu:block/multiblock/large_chemical_reactor')

    // ---- Spent Fuel Pool: cooling decladding for every spent rod, the passive bulk path ----
    event.create('spent_fuel_pool', 'multiblock')
        .machine(holder => new $FissionMachine(holder))
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('spent_fuel_pool')])
        .recipeModifiers([GTRecipeModifiers.OC_PERFECT])
        .appearanceBlock(GTBlocks.CASING_STAINLESS_CLEAN)
        ['tooltips(net.minecraft.network.chat.Component[])']([0, 1, 2].map(i =>
            Component.translatable(`af9.spent_fuel_pool.tooltip.${i}`)))
        .pattern(definition => FactoryBlockPattern.start()
            .aisle('XXX', 'XXX', 'XXX', 'XXX')
            .aisle('XXX', 'X#X', 'X#X', 'XXX')
            .aisle('XSX', 'X#X', 'X#X', 'XXX')
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
        .workableCasingModel('gtceu:block/casings/solid/machine_casing_clean_stainless_steel',
            'gtceu:block/multiblock/large_chemical_reactor')

    // ---- Fuel Fabrication Bay: dusts straight to clad rods and blankets, one passive step ----
    event.create('fuel_fabrication_bay', 'multiblock')
        .machine(holder => new $FissionMachine(holder))
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('fuel_fabrication')])
        .recipeModifiers([GTRecipeModifiers.OC_PERFECT])
        .appearanceBlock(GTBlocks.CASING_STAINLESS_CLEAN)
        ['tooltips(net.minecraft.network.chat.Component[])']([0, 1, 2].map(i =>
            Component.translatable(`af9.fuel_fabrication_bay.tooltip.${i}`)))
        .pattern(definition => FactoryBlockPattern.start()
            .aisle('XXX', 'XXX', 'XXX', 'XXX')
            .aisle('XXX', 'X#X', 'X#X', 'XXX')
            .aisle('XSX', 'X#X', 'X#X', 'XXX')
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
        .workableCasingModel('gtceu:block/casings/solid/machine_casing_clean_stainless_steel',
            'gtceu:block/multiblock/assembly_line')
})
