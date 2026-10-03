// AF9 - Asteroid fission: the new way to uranium and plutonium. Spec: docs/asteroid-fission.md
//
// Brannerite, a uranium-titanium ore, generates only in the Asteroid Field (af9-core: dimension af9:asteroid_field,
// reached with an Ad Astra rocket; the rocks are AsteroidFieldFeature). The ore becomes yellowcake, the yellowcake the
// FX-1 Reactor's fuel rods, and the reactor turns the rods, water and a sodium-potassium coolant into supercritical
// steam (GT's Large Steam Turbines and Extreme Reactors' turbines make power from it) and spent rods, which are
// reprocessed into plutonium. The old ways to uranium and plutonium are closed (server_scripts/mods/gtceu/
// asteroid_fission.js); the plutonium goes into the Fusion Reactor Mk1 (fusion_reactor.js).
//
// What lives where:
//   this file                                   materials, items, the layer of the asteroid ores, the reactor's recipe type
//                                               and its structure
//   server_scripts/mods/gtceu/asteroid_fission  ore veins, the uranium chain, the reactor's recipes, steam turbine, tags
//   server_scripts/mods/gtceu/rockets.js        gregified rockets, the propellant
//   af9-core                                    the asteroids (com.af9.core.space), RadiationWatch, Extreme Reactors

// ---- The layer: what GT's ore veins grow into in the Asteroid Field ----
// The rock of the asteroids: the four stones GT has ore blocks for (AsteroidFieldFeature).
GTCEuStartupEvents.registry('gtceu:world_gen_layer', event => {
    event.create('af9_asteroid')
        .targets('minecraft:andesite', 'minecraft:tuff', 'minecraft:basalt', 'minecraft:blackstone')
        .dimensions('af9:asteroid_field')
})

GTCEuStartupEvents.registry('gtceu:material', event => {
    // ---- The ore and the uranium chain ----
    // Formulas only (no components), so GT adds no electrolyzer or centrifuge shortcut past the chain.
    // Brannerite: uranium, titanium and rare earths in one oxide. Crushing gives two crushed ores per ore.
    event.create('brannerite')
        .dust().ore(2, 1)
        .color(0x4a4636).secondaryColor(0xd2c24a)
        .iconSet(GTMaterialIconSet.RADIOACTIVE)
        .formula('(U,Ca,Ce)(Ti,Fe)2O6')
        .radioactiveHazard(0.6)
        .addOreByproducts(GTMaterials.Rutile, GTMaterials.Thorium, GTMaterials.Neodymium)

    // The acid leach of the ore: uranyl sulfate in solution, and the ammonia precipitates it as yellowcake
    event.create('uranyl_sulfate_solution')
        .liquid()
        .color(0xc9d63c)
        .formula('UO2SO4')
    event.create('yellowcake')
        .dust()
        .color(0xe8c51c)
        .iconSet('rough')
        .formula('U3O8')
        .radioactiveHazard(0.8)

    // ---- Spent fuel ----
    // What the reactor leaves in a rod, dissolved in nitric acid; the centrifuge splits plutonium and uranium off.
    event.create('irradiated_fuel')
        .dust()
        .color(0x4b5a22).secondaryColor(0x8cff3c)
        .iconSet(GTMaterialIconSet.RADIOACTIVE)
        .formula('(U,Pu,FP)O2')
        .radioactiveHazard(1.5)
    event.create('spent_fuel_solution')
        .liquid()
        .color(0x7a9a2e)
        .formula('(U,Pu)(NO3)x')

    // ---- The reactor's coolant circuit ----
    // The coolant is GT's own sodium-potassium alloy (NaK, liquid at room temperature). It carries the heat out of the
    // core and gives it to the water in the same machine; the hot alloy goes back to NaK in a Vacuum Freezer.
    event.create('hot_sodium_potassium')
        .liquid(800)
        .color(0xff8a3d)
        .formula('NaK*')
    // Water above the critical point (647 K, 22 MPa): no boiling, so no drying out. 80 EU per mB in a steam turbine
    // (steam: 0.5), 320 FE in Extreme Reactors' (af9-core ExtremeReactorsCompat).
    event.create('supercritical_steam')
        .gas(647)
        .color(0xe6f4ff)
        .formula('H2O*')

    // ---- The propellant of the rockets (server_scripts/mods/gtceu/rockets.js) ----
    // Triethylaluminium, the hypergolic igniter of real rockets (the Merlin's TEA-TEB): aluminium, ethylene and hydrogen
    // in a chemical reactor, MV
    event.create('triethylaluminium')
        .liquid()
        .color(0xd7dbf2)
        .formula('Al(C2H5)3')
    // Hydrogen and oxygen with aluminium powder burning in them and the igniter: more thrust per mB than any of its
    // parts. The only fuel Ad Astra's rockets take in this pack.
    event.create('aluminised_hydrolox')
        .liquid()
        .color(0xf2c96a)
        .formula('H2/O2/Al')
})

// ---- The FX-1 Reactor ----
const $Fx1Reactor = Java.loadClass('com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine')
const $Fx1Direction = Java.loadClass('com.gregtechceu.gtceu.api.pattern.util.RelativeDirection')

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    // a fuel rod in and the spent rod out; water and coolant in, supercritical steam and hot coolant out
    event.create('fx1_reactor')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(1, 1, 2, 2)
        .setProgressBar(GuiTextures.PROGRESS_BAR_BOILER_HEAT, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.BOILER)
})

GTCEuStartupEvents.registry('gtceu:machine', event => {
    // A fission reactor: a stable titanium vessel (5 x 5 x 5) around a core of heatproof casing, the coolant channels
    // (titanium pipe casing) running through it and the one block of air the fuel rod is in. Aisles front -> back,
    // rows bottom -> top; the hatches go on any vessel casing. EV: 1,920 EU/t.
    event.create('fx1_reactor', 'multiblock')
        .langValue('FX-1 Reactor')
        .machine(holder => new $Fx1Reactor(holder))
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('fx1_reactor')])
        .recipeModifiers([GTRecipeModifiers.OC_NON_PERFECT])
        .appearanceBlock(GTBlocks.CASING_TITANIUM_STABLE)
        ['tooltips(net.minecraft.network.chat.Component[])']([0, 1, 2, 3, 4, 5].map(i =>
            Component.translatable(`af9.fx1_reactor.tooltip.${i}`)))
        .pattern(definition => FactoryBlockPattern.start($Fx1Direction.LEFT, $Fx1Direction.UP, $Fx1Direction.BACK)
            .aisle('CCCCC', 'CCCCC', 'CCSCC', 'CCCCC', 'CCCCC') // front with the controller
            .aisle('CCCCC', 'CHHHC', 'CHPHC', 'CHHHC', 'CCCCC')
            .aisle('CCCCC', 'CHPHC', 'CP#PC', 'CHPHC', 'CCCCC') // the core
            .aisle('CCCCC', 'CHHHC', 'CHPHC', 'CHHHC', 'CCCCC')
            .aisle('CCCCC', 'CCCCC', 'CCCCC', 'CCCCC', 'CCCCC') // back
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count))
            .where('C', Predicates.blocks(GTBlocks.CASING_TITANIUM_STABLE.get())
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(3, 2))
                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(4, 2))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
            .where('H', Predicates.blocks(GTBlocks.CASING_INVAR_HEATPROOF.get()))       // the core's shell
            .where('P', Predicates.blocks(GTBlocks.CASING_TITANIUM_PIPE.get()))         // the coolant channels
            .where('#', Predicates.air())                                               // the fuel rod
            .build())
        .workableCasingModel('gtceu:block/casings/solid/machine_casing_stable_titanium',
            'gtceu:block/multiblock/fusion_reactor')
})
