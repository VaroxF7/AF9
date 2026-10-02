// AF9 - Computation arrays: the Computer Rack and the two N1 computation arrays. Behaviour: af9-core
// com.af9.core.compute (cards: ComputeCard, the arrays: ComputationArrayMachine). Recipes:
// server_scripts/mods/gtceu/computation.js. Spec: docs/computation.md §2
//
// N1 Computation Array (MV, 3x3x6): eight MV Computer Racks (Tube and Silicon cards).
// N1 Supercomputer Array (LuV, 2x4 across, 7 to 30 long): four racks in each slice between the end slices, heat
// vents over and under them.
// The arrays run while switched on and fed: energy for their cards, coolant (Coolant Hatches) for the heat. No recipes.
// Their computation leaves through a Computation Transmitter Hatch (GT's Optical Fiber Cable).

const $ComputationArrayMachine = Java.loadClass('com.af9.core.compute.ComputationArrayMachine')
const $ComputerRack = Java.loadClass('com.af9.core.compute.ComputerRackPartMachine')
const $ComputeCoolantHatch = Java.loadClass('com.af9.core.machine.part.CoolantHatchPartMachine')
const $ComputeRelativeDirection = Java.loadClass('com.gregtechceu.gtceu.api.pattern.util.RelativeDirection')

// Server Casing: the MV computer's shell (N1 Computation Array)
StartupEvents.registry('block', event => {
    event.create('server_casing')
        .displayName('Server Casing')
        .soundType('metal')
        .hardness(5)
        .resistance(6)
        .requiresTool(true)
        .tagBlock('minecraft:mineable/pickaxe')
})

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }

    // Computer Rack: four card slots. MV takes Tube and Silicon cards, LuV every card. The MV rack is a machine hull
    // with a rack front; the LuV rack is GT's computer casing with a panel on all four sides (GT's HPCA component
    // face), no front, so the supercomputer's walls show the panels however the racks were placed.
    event.create('computer_rack', 'custom')
        .tiers(GTValues.MV, GTValues.LuV)
        .machine((holder, tier) => new $ComputerRack(holder, tier))
        .definition((tier, builder) => {
            builder
                .langValue(`${GTValues.VN[tier]} Computer Rack`)
                .abilities($ComputerRack.COMPUTER_RACK)
                ['tooltips(net.minecraft.network.chat.Component[])'](tooltips(
                    tier >= GTValues.LuV ? 'af9.computer_rack.luv.tooltip' : 'af9.computer_rack.mv.tooltip', 2))
            if (tier >= GTValues.LuV) {
                builder.rotationState(RotationState.NONE)
                    ['simpleModel(net.minecraft.resources.ResourceLocation)']('af9:block/machine/part/computer_rack_luv')
            } else {
                builder.rotationState(RotationState.ALL)
                    ['overlayTieredHullModel(net.minecraft.resources.ResourceLocation)'](
                        'af9:block/machine/part/computer_rack')
            }
        })

    // N1 Computation Array: 3x3x6. The racks sit in the middle row of the four inner slices, left and right, a cooling
    // pipe between them; hatches on any casing.
    event.create('n1_computation_array', 'multiblock')
        .langValue('N1 Computation Array')
        .machine(holder => new $ComputationArrayMachine(holder, $ComputationArrayMachine.ARRAY))
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.DUMMY_RECIPES])
        .appearanceBlock(() => Block.getBlock('kubejs:server_casing'))
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.n1_computation_array.tooltip', 5))
        .pattern(definition => FactoryBlockPattern.start()
            .aisle('CCC', 'CCC', 'CCC')
            .aisle('CCC', 'RPR', 'CCC')
            .aisle('CCC', 'RPR', 'CCC')
            .aisle('CCC', 'RPR', 'CCC')
            .aisle('CCC', 'RPR', 'CCC')
            .aisle('CCC', 'CSC', 'CCC')
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            .where('R', $ComputerRack.racks(true))                 // MV racks only
            .where('P', Predicates.blocks('gtceu:steel_pipe_casing'))
            .where('C', Predicates.blocks('kubejs:server_casing')
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities($ComputeCoolantHatch.COOLANT_INPUT).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.COMPUTATION_DATA_TRANSMISSION).setMaxGlobalLimited(1, 0)))
            .build())
        .workableCasingModel('kubejs:block/server_casing', 'gtceu:block/multiblock/data_bank')

    // N1 Supercomputer Array: 2 wide, 4 high, 7 to 30 long. Every slice between the two end slices holds four racks
    // (the two middle rows, left and right) between heat vents (the bottom and top rows); the end slices are computer
    // casings, where the hatches go, the controller second from the bottom. Aisles front (controller) -> back: the
    // controller comes before the repeatable slices, so GT's auto-build places the structure right behind it (with the
    // controller after them GT started the build 29 blocks back, at their longest).
    event.create('n1_supercomputer_array', 'multiblock')
        .langValue('N1 Supercomputer Array')
        .machine(holder => new $ComputationArrayMachine(holder, $ComputationArrayMachine.SUPERCOMPUTER))
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.DUMMY_RECIPES])
        .appearanceBlock(GTBlocks.COMPUTER_CASING)
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.n1_supercomputer_array.tooltip', 5))
        .pattern(definition => FactoryBlockPattern.start($ComputeRelativeDirection.LEFT, $ComputeRelativeDirection.UP,
            $ComputeRelativeDirection.BACK)
            .aisle('CC', 'SC', 'CC', 'CC')                           // front end: the controller
            .aisle('VV', 'RR', 'RR', 'VV').setRepeatable(5, 28)      // vents, two rows of racks, vents
            .aisle('CC', 'CC', 'CC', 'CC')                           // back end
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            .where('R', $ComputerRack.racks(false))                // MV or LuV racks
            .where('V', Predicates.blocks('gtceu:computer_heat_vent'))
            .where('C', Predicates.blocks('gtceu:computer_casing')
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(4, 1))
                .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1, 0))
                .or(Predicates.abilities($ComputeCoolantHatch.COOLANT_INPUT).setMaxGlobalLimited(4, 1))
                .or(Predicates.abilities(PartAbility.COMPUTATION_DATA_TRANSMISSION).setMaxGlobalLimited(1, 0)))
            .build())
        .workableCasingModel('gtceu:block/casings/hpca/computer_casing/back', 'gtceu:block/multiblock/hpca')
})
