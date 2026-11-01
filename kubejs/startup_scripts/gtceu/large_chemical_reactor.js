// AF9 - Large Chemical Reactor: a new structure for GT's multiblock, an 11 x 3 x 3 barrel (Janne's build, chemreac.nbt). Only the
// structure changes: the machine, its recipes, fab modes, parallel and overclock rules stay GT's and AF9's, and the controller
// keeps its Inert PTFE casing look. The original's minimum of 10 Inert PTFE casings stays, and so does the rule that the heating coil
// (any type, as before) is only there for the structure: GT's LCR has no coil bonus.
//
//   x = 0     the Inconel 625 end plate (an H: a bar on each side, joined across the middle)
//   x = 1     a ring of Inert PTFE casing
//   x = 2..7  the barrel: a row of six heating coils down the middle, Inconel 718 casing round them, and Inconel 718 slopes in the
//             four long corners (the chamfer of the original's framed slopes)
//   x = 8     a ring of Inert PTFE casing
//   x = 9     the controller in the front, PTFE pipe casings above and below the empty middle, casings round
//   x = 10    the last ring of Inert PTFE casing
// Hatches go in the Inert PTFE casings only, as before (energy, the recipe type's own item / fluid hatches, maintenance). The
// slopes (af9:inconel_718_slope) match in any state, so they are placed like stairs; the preview page shows them in the states of
// the original build. Textures: tools/textures/lcr_casings.py.
//
// It runs after the registries are filled, like the Void Miner MK1 swap (void_mining.js): the definition is GT's.

const $LcrRegistries = Java.loadClass('com.gregtechceu.gtceu.api.registry.GTRegistries')
const $LcrResourceLocation = Java.loadClass('net.minecraft.resources.ResourceLocation')
const $LcrShapeInfo = Java.loadClass('com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo')
const $LcrSlope = Java.loadClass('com.af9.core.registry.SlopeBlock')
const $LcrDirection = Java.loadClass('net.minecraft.core.Direction')
const $LcrHalf = Java.loadClass('net.minecraft.world.level.block.state.properties.Half')

// aisles back -> front (the controller's last), each with its rows bottom -> top, 11 columns from the end plate (x = 0) to the
// controller's end (x = 10). R Inconel 625 casing, X Inert PTFE casing, B Inconel 718 casing, L Inconel 718 slope, C heating coil,
// P PTFE pipe casing, S controller, # air.
const LCR_AISLES = [
    ['RXLLLLLLXXX', 'RXBBBBBBXXX', 'RXLLLLLLXXX'],
    ['#XBBBBBBXPX', 'RXCCCCCCX#X', '#XBBBBBBXPX'],
    ['RXLLLLLLXXX', 'RXBBBBBBXSX', 'RXLLLLLLXXX']
]

// the preview page: the same, the slopes in their states (a controller facing north: the wall of each slope faces the barrel's
// middle; a top slope in the lower row, a bottom one in the upper) and one of each hatch in the front aisle beside the
// controller (columns 8 and 10: I item in, O item out, F fluid in, H fluid out, E energy in, M maintenance)
//   a top/north  b bottom/north  c top/south  d bottom/south
const LCR_PREVIEW_AISLES = [
    ['RXaaaaaaXXX', 'RXBBBBBBXXX', 'RXbbbbbbXXX'],
    ['#XBBBBBBXPX', 'RXCCCCCCX#X', '#XBBBBBBXPX'],
    ['RXccccccIXE', 'RXBBBBBBOSM', 'RXddddddHXF']
]

StartupEvents.postInit(event => {
    try {
        const lcr = $LcrRegistries.MACHINES.get(new $LcrResourceLocation('gtceu', 'large_chemical_reactor'))
        const slopeState = (half, facing) => Block.getBlock('af9:inconel_718_slope').defaultBlockState()
            .setValue($LcrSlope.HALF, half).setValue($LcrSlope.FACING, facing)

        // GT's own parts, as in the original: the casings (at least 10) take the hatches of the recipe types and a maintenance
        // hatch; the coil is any heating coil
        const casing = () => Predicates.blocks('gtceu:inert_machine_casing').setMinGlobalLimited(10)
        const abilities = () => Predicates.autoAbilities(lcr.getRecipeTypes())
            .or(Predicates.autoAbilities(true, false, false))
        const pattern = () => {
            let builder = FactoryBlockPattern.start()
            LCR_AISLES.forEach(aisle => { builder = builder.aisle(aisle) })
            return builder
                .where('S', Predicates.controller(Predicates.blocks(lcr.get())))
                .where('X', casing().or(abilities()))
                .where('R', Predicates.blocks('af9:inconel_625_casing'))
                .where('B', Predicates.blocks('af9:inconel_718_casing'))
                .where('L', Predicates.blocks('af9:inconel_718_slope'))
                .where('P', Predicates.blocks('gtceu:ptfe_pipe_casing'))
                .where('C', Predicates.heatingCoils())
                .where('#', Predicates.air())
                .build()
        }
        let cached = null
        lcr.setPatternFactory(() => {
            if (cached === null) cached = pattern()
            return cached
        })

        lcr.setShapes(() => {
            let builder = FactoryBlockPattern.start()
            LCR_PREVIEW_AISLES.forEach(aisle => { builder = builder.aisle(aisle) })
            const preview = builder
                .where('S', Predicates.controller(Predicates.blocks(lcr.get())))
                .where('X', Predicates.blocks('gtceu:inert_machine_casing'))
                .where('R', Predicates.blocks('af9:inconel_625_casing'))
                .where('B', Predicates.blocks('af9:inconel_718_casing'))
                .where('P', Predicates.blocks('gtceu:ptfe_pipe_casing'))
                .where('C', Predicates.blocks('gtceu:cupronickel_coil_block'))
                .where('a', Predicates.states(slopeState($LcrHalf.TOP, $LcrDirection.NORTH)))
                .where('b', Predicates.states(slopeState($LcrHalf.BOTTOM, $LcrDirection.NORTH)))
                .where('c', Predicates.states(slopeState($LcrHalf.TOP, $LcrDirection.SOUTH)))
                .where('d', Predicates.states(slopeState($LcrHalf.BOTTOM, $LcrDirection.SOUTH)))
                .where('I', Predicates.abilities(PartAbility.IMPORT_ITEMS).setPreviewCount(1))
                .where('O', Predicates.abilities(PartAbility.EXPORT_ITEMS).setPreviewCount(1))
                .where('F', Predicates.abilities(PartAbility.IMPORT_FLUIDS).setPreviewCount(1))
                .where('H', Predicates.abilities(PartAbility.EXPORT_FLUIDS).setPreviewCount(1))
                .where('E', Predicates.abilities(PartAbility.INPUT_ENERGY).setPreviewCount(1))
                .where('M', Predicates.abilities(PartAbility.MAINTENANCE).setPreviewCount(1))
                .where('#', Predicates.air())
                .build()
            return [new $LcrShapeInfo(preview.getPreview(Array(LCR_PREVIEW_AISLES.length).fill(1)))]
        })
    } catch (e) {
        console.error('AF9 Large Chemical Reactor: could not replace the structure: ' + e)
    }
})
