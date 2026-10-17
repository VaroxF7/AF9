// AF9 - Quantum Computer: GTNH's TecTech Quantum Computer (GT5-Unofficial MTEQuantumComputer / MTEHatchRack, LGPL-3.0;
// credits in CREDITS-textures.md) as a GT computation source, far beyond the HPCA. Behaviour: AF9 Core
// (com.af9.core.quantum). Recipes: server_scripts/mods/gtceu/quantum_computer.js. Spec: docs/quantum-computer.md
//
// The structure is TecTech's, block for block: a corridor 3 wide and 4 high, one column of it air (where the racks face
// and the vents blow), the other two the computer's wall. Front slice (the controller, 2nd row, middle; hatches go in
// the Computer Casing), a cap, 1 to 13 rack slices (a rack hatch or an Advanced Computer Casing in each of the two middle
// rows of the middle column), a cap, the back slice. Rows bottom -> top, aisles back -> front.
//   S controller   A Quantum Computer Casing or a hatch   B Quantum Computer Casing
//   C Vent Casing (top and bottom row, with air beside it)   D Advanced Casing   E Rack Hatch or Advanced Casing
//   - air          (space) anything

const $QuantumComputer = Java.loadClass('com.af9.core.quantum.QuantumComputerMachine')
const $RackPart = Java.loadClass('com.af9.core.quantum.RackPartMachine')

GTCEuStartupEvents.registry('gtceu:machine', event => {
    // Computer Rack Hatch: four component slots (processors and heat vents), with its own heat. UV, like the computer.
    event.create('computer_rack_hatch', 'custom')
        .tiers(GTValues.UV)
        .machine((holder, tier) => new $RackPart(holder))
        .definition((tier, builder) => {
            builder
                .langValue('Computer Rack Hatch')
                .rotationState(RotationState.ALL)
                .abilities($RackPart.RACK)
                .workableTieredHullModel(GTCEu.id('block/multiblock/computer_rack_hatch'))
                ['tooltips(net.minecraft.network.chat.Component[])']([
                    Component.translatable('af9.computer_rack_hatch.tooltip.0'),
                    Component.translatable('af9.computer_rack_hatch.tooltip.1'),
                    Component.translatable('af9.computer_rack_hatch.tooltip.2')])
        })
})

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }

    event.create('quantum_computer', 'multiblock')
        .machine(holder => new $QuantumComputer(holder))
        .langValue('Quantum Computer')
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeType(GTRecipeTypes.DUMMY_RECIPES)
        .appearanceBlock(() => Block.getBlock('af9:quantum_computer_advanced_casing'))
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.quantum_computer.tooltip', 6))
        .pattern(definition => FactoryBlockPattern.start()
            // back
            .aisle(' AA', ' AA', ' AA', ' AA')
            // cap
            .aisle('-CB', ' DD', ' DD', '-CB')
            // 1 to 13 rack slices
            .aisle('-CB', ' ED', ' ED', '-CB').setRepeatable(1, 13)
            // cap
            .aisle('-CB', ' DD', ' DD', '-CB')
            // front, with the controller
            .aisle(' AA', ' SA', ' AA', ' AA')
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            .where(' ', Predicates.any())
            .where('-', Predicates.air())
            .where('B', Predicates.blocks('af9:quantum_computer_casing'))
            .where('C', Predicates.blocks('af9:quantum_computer_vent_casing'))
            .where('D', Predicates.blocks('af9:quantum_computer_advanced_casing'))
            .where('E', Predicates.blocks('af9:quantum_computer_advanced_casing')
                .or(Predicates.abilities($RackPart.RACK).setMaxGlobalLimited(26, 2)))
            // parts have a maximum only, never a required count (the AF9 convention)
            .where('A', Predicates.blocks('af9:quantum_computer_casing')
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(8, 2))
                .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(2, 0))
                .or(Predicates.abilities(PartAbility.COMPUTATION_DATA_TRANSMISSION).setMaxGlobalLimited(4, 1))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
            .build())
        // the casing with TecTech's computer face (kubejs assets, overlay_front*)
        .workableCasingModel('af9:block/quantum_computer_advanced_casing', 'gtceu:block/multiblock/quantum_computer')
})
