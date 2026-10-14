// AF9 - Atmospheric Simulation Room: a pressure room after GT's Cleanroom that holds a Lunar
// atmosphere anywhere. The controller in the ceiling centre houses the Atmospheric Simulator;
// XPS and NVM circuits build in its lunar air. Recipes: server_scripts/mods/gtceu/atmospheric_simulation.js
//
// 31 x 6 x 31 room (a step past the cleanroom's 15 max), aisles back -> front, rows bottom -> top.
// The middle is open air (AF9 Core watches it for unsuited players); the controller sits
// ceiling-centre, a 1 x 2 door stands front-bottom-centre. The pattern is generated below (like the
// DTPF's cell function, in a loop: back wall closed, front wall with door, hollow middles).

const $AtmosphereRoomMachine = Java.loadClass('com.af9.core.machine.AtmosphereRoomMachine')

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    // seven item inputs: the NVM mainframe lean takes processor, rack, dies, both 7 nm
    // calibration chips and wire (server atmospheric_simulation.js, Stage 5)
    event.create('atmospheric_simulation')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(7, 1, 2, 0)
        .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.ASSEMBLER)
})

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }

    event.create('atmospheric_simulation_room', 'multiblock')
        .machine(holder => new $AtmosphereRoomMachine(holder))
        .langValue('Atmospheric Simulation Room')
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('atmospheric_simulation')])
        .recipeModifiers([GTRecipeModifiers.OC_PERFECT])
        .appearanceBlock(GTBlocks.CASING_STAINLESS_CLEAN)
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.atmospheric_simulation_room.tooltip', 4))
        .pattern(definition => {
            // 31 wide, 6 high, 31 deep: floor + ceiling solid, glass bands in the walls, hollow middle.
            // Col 15 is the centre (controller, door). Rows bottom -> top, aisles back -> front.
            // Back wall (z 0) is closed; front wall (z 30) carries the 1x2 door and the ceiling-centre
            // controller. Middle slices are hollow: stainless edge, glass band, 27 air, glass, stainless.
            const W = 31
            const H = 6
            const D = 31
            const C = 15
            // The design: the block at x, y, z (y up, z towards the front) as a letter of the pattern.
            // S controller, X stainless (or a hatch), G glass, # air.
            const simCell = (x, y, z) => {
                if (x === C && y === H - 1 && z === D - 1) return 'S'
                if (y === 0 || y === H - 1) return 'X'
                if (z === 0) {
                    if (y === H - 2) return 'X'
                    if (x === 0 || x === W - 1) return 'X'
                    if (x === 1 || x === W - 2) return 'G'
                    return 'X'
                }
                if (z === D - 1) {
                    if (y === 1 || y === 2) return x === C ? '#' : 'X'
                    return 'X'
                }
                if (y === H - 2) {
                    if (x === 0 || x === W - 1) return 'X'
                    return '#'
                }
                if (x === 0 || x === W - 1) return 'X'
                if (x === 1 || x === W - 2) return 'G'
                return '#'
            }
            // forEach, not for: KubeJS's engine (Rhino) keeps a const in a loop body at its first pass's value,
            // so loop bodies use callbacks (lint rule S3). Array(n).fill(0), not Array.from({ length: n }):
            // Rhino's Array.from leaves n holes, and forEach skips holes.
            const simAisles = () => {
                const aisles = []
                Array(D).fill(0).forEach((_, z) => {
                    const rows = []
                    Array(H).fill(0).forEach((_, y) => {
                        let row = ''
                        Array(W).fill(0).forEach((_, x) => { row += simCell(x, y, z) })
                        rows.push(row)
                    })
                    aisles.push(rows)
                })
                return aisles
            }
            let pattern = FactoryBlockPattern.start()
            simAisles().forEach(aisle => { pattern = pattern.aisle(aisle) })
            return pattern
                .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count))
                .where('X', Predicates.blocks(GTBlocks.CASING_STAINLESS_CLEAN.get())
                    .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                    .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                    .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(1, 1))
                    .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(2, 1))
                    .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(1, 1))
                    .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
                .where('G', Predicates.blocks('gtceu:tempered_glass'))
                .where('#', Predicates.air())
                .build()
        })
        .workableCasingModel('gtceu:block/casings/solid/machine_casing_clean_stainless_steel',
            'gtceu:block/multiblock/assembly_line')
})
