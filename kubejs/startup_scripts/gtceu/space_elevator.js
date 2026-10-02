// AF9 - The Space Elevator, after GTNH's: a tower on a cable that reaches into space, the pack's renewable ore source from
// ZPM on. Behaviour: af9-core (com.af9.core.elevator.SpaceElevatorMachine, the asteroids; the platform that turns on the
// cable: com.af9.core.client.render.SpaceElevatorRender). Recipes: server_scripts/mods/gtceu/space_elevator.js.
// Spec: docs/space-elevator.md
//
// A Mining Drone (not used up) in an input bus, 50 to 100 buckets of hydrogen and of a supercooled coolant in the fluid hatches
// and 4 to 32 amps of ZPM energy for minutes send an expedition to a random asteroid: the output buses hold its ore,
// tens of stacks of raw ore. The asteroids are made from GT's ore veins; the better the drone, the more of them it reaches.
//
// The tower is 13 x 13 and 26 high (fixed, like GTNH's): a disc of casing as the base, a cone of blue glass round a 3 x 3
// pillar with the cable up its middle. Hatches go in the base. The platform on the cable above it is drawn by af9-core.

const $SpaceElevator = Java.loadClass('com.af9.core.elevator.SpaceElevatorMachine')
const $ElevatorModels = Java.loadClass('com.af9.core.machine.AF9MachineModels')
const $ElevatorDirection = Java.loadClass('com.gregtechceu.gtceu.api.pattern.util.RelativeDirection')

const SE_ROMAN = ['I', 'II', 'III', 'IV']
const SE_SIZE = 13
const SE_HALF = 6
const SE_HEIGHT = 26

StartupEvents.registry('block', event => {
    event.create('space_elevator_base_casing')
        .displayName('Space Elevator Base Casing')
        .soundType('metal')
        .hardness(5)
        .resistance(12)
        .requiresTool(true)
        .tagBlock('minecraft:mineable/pickaxe')
    event.create('space_elevator_support')
        .displayName('Space Elevator Support Structure')
        .soundType('metal')
        .hardness(5)
        .resistance(12)
        .requiresTool(true)
        .tagBlock('minecraft:mineable/pickaxe')
    event.create('space_elevator_glass')
        .displayName('Space Elevator Glass')
        .soundType('glass')
        .hardness(3)
        .resistance(8)
        .notSolid()
        .renderType('translucent')
        .requiresTool(true)
        .tagBlock('minecraft:mineable/pickaxe')
    event.create('space_elevator_cable')
        .displayName('Space Elevator Cable')
        .soundType('metal')
        .hardness(5)
        .resistance(12)
        .lightLevel(0.5)
        .requiresTool(true)
        .tagBlock('minecraft:mineable/pickaxe')
})

StartupEvents.registry('item', event => {
    SE_ROMAN.forEach((roman, i) => {
        event.create(`space_mining_drone_mk${i + 1}`)
            .displayName(`Mining Drone MK-${roman}`)
            .maxStackSize(1)
            .tooltip(`Sent to the asteroids by a Space Elevator: reaches the ores of tier ${i + 1} and below.`)
            .tooltip('Not used up.')
    })
})

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    // a Mining Drone (not used up), hydrogen and the coolant in; the ore is made when a run starts (SpaceElevatorMachine)
    event.create('space_mining')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(1, 1, 2, 0)
        .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.ARC)
})

// One character of one layer of the tower (x across, z deep from the controller's face, y up).
//   S the controller (front of the base, y 0)   F the base disc, hatches allowed   C pillar casing   P support (corners)
//   K the cable up the middle                   G the glass cone
const seChar = (x, y, z) => {
    const dx = x - SE_HALF
    const dz = z - SE_HALF
    const d2 = dx * dx + dz * dz
    // the cone's radius at this height: 5.5 at y 2, narrowing to 2.5 at y 8 (declared here: Rhino keeps a const of a nested
    // block once for the whole script, and this function runs for every block of the tower)
    const radius = 5.5 - (y - 2) * 0.5
    if (y === 0 && x === SE_HALF && z === 0) return 'S'
    if (y <= 1) {
        if (dx === 0 && dz === 0 && y === 1) return 'K'
        return d2 <= 42 ? 'F' : ' '
    }
    if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
        if (dx === 0 && dz === 0) return 'K'
        return dx !== 0 && dz !== 0 ? 'P' : 'C'
    }
    if (y <= 8) {
        // the cone: a shell of glass
        return Math.abs(Math.sqrt(d2) - radius) < 0.6 ? 'G' : ' '
    }
    return ' '
}

const seLayer = y => {
    const rows = []
    for (var z = 0; z < SE_SIZE; z++) {
        var row = ''
        for (var x = 0; x < SE_SIZE; x++) row += seChar(x, y, z)
        rows.push(row)
    }
    return rows
}

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }

    // Space Elevator: aisles bottom -> top (a layer each), rows front -> back, the controller in the front of the base.
    event.create('space_elevator', 'multiblock')
        .langValue('Space Elevator')
        .machine(holder => new $SpaceElevator(holder))
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('space_mining')])
        .recipeModifiers([$SpaceElevator.ASTEROID])
        .appearanceBlock(() => Block.getBlock('kubejs:space_elevator_base_casing'))
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.space_elevator.tooltip', 7))
        .pattern(definition => {
            let pattern = FactoryBlockPattern.start($ElevatorDirection.LEFT, $ElevatorDirection.BACK,
                $ElevatorDirection.UP)
            for (var y = 0; y < SE_HEIGHT; y++) pattern = pattern.aisle(seLayer(y))
            // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count)), in the base
            const parts = Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(4, 2)
                .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(2, 0))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(4, 2))
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(4, 2))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1))
            return pattern
                .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                .where('F', Predicates.blocks('kubejs:space_elevator_base_casing').or(parts))
                .where('C', Predicates.blocks('kubejs:space_elevator_base_casing'))
                .where('P', Predicates.blocks('kubejs:space_elevator_support'))
                .where('G', Predicates.blocks('kubejs:space_elevator_glass'))
                .where('K', Predicates.blocks('kubejs:space_elevator_cable'))
                .where(' ', Predicates.any())
                .build()
        })
        .workableCasingModel('kubejs:block/space_elevator_base_casing', 'gtceu:block/multiblock/fusion_reactor')
        // the same model plus the platform on the cable: its centre 27 blocks above the controller (just over the top of
        // the pillar) and 6 behind it (the cable's axis), the cable 150 blocks on up from there
        .model($ElevatorModels.workableCasingWithSpaceElevator('kubejs:block/space_elevator_base_casing',
            'gtceu:block/multiblock/fusion_reactor', 27, 6, 150))
        .hasBER(true)
})
