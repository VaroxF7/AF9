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
//   this file                                   the reactor's recipe type and its structure
//   server_scripts/mods/gtceu/asteroid_fission  ore veins, the uranium chain, the reactor's recipes, steam turbine, tags
//   server_scripts/mods/gtceu/rockets.js        gregified rockets, the propellant
//   af9-core                                    the materials and the fuel items (com.af9.core.registry), the asteroids
//                                               and the layer of their ores (com.af9.core.space), RadiationWatch,
//                                               Extreme Reactors

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
