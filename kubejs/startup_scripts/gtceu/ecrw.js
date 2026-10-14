// AF9 - the Evaporation Condensation Retaining Well (ECRW): a shallow round pond that boils dilute fluids
// down and keeps the condensate, the replacement for Mekanism's evaporation plant and the evaporation GregTech
// had a while back. Machine logic: af9-core EvaporationWellMachine (plain GregTech behaviour) and the rake arm
// that turns round the middle while a recipe runs (af9-core EcrwArmRender, wired below; the arm is render-only,
// no part of the pattern). Recipes: server_scripts/mods/gtceu/ecrw.js. Spec: docs/ecrw.md
//
// The structure is the player's own build, restyled block for block: a 13 x 13 pond of GT watertight casing with
// a stainless floor and pier, its centre at 6, rows bottom -> top, aisles back -> front (the controller's, in the
// front wall at its foot, last). From the ground up:
//   * the floor: a disc of stainless casing tucked inside the ring;
//   * the wall: a plain ring on the edge, 2 high, every block of it may be a hatch;
//   * the rim: the same ring once more along the wall's top, plain;
//   * the pier: a plus of stainless casing up the middle, 4 high, that the rendered rake turns round.
// The cell function below is the whole design; the aisles are built from it.

const $EvaporationWellMachine = Java.loadClass('com.af9.core.machine.EvaporationWellMachine')
const $EcrwModels = Java.loadClass('com.af9.core.machine.AF9MachineModels')

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    // dilute fluids in, the boil-off's solids and the condensate out
    event.create('evaporation')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(2, 2, 3, 3)
        .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.CHEMICAL)
})

// ECRW layout, precomputed ONCE at module level and reused by .pattern() (memoized).
// ECRW_AISLES is the 13x4x13 ecrwCell table. Same cell function, just called once.
const ECRW_SIZE = 13         // the pond's width and depth, its centre at 6
const ECRW_HEIGHT = 4
const ECRW_CENTRE = 6
// The design: the block at x, y, z (y up, z towards the front) as a letter of the pattern. S controller,
// B floor, W wall (hatches), R rim, C the plus pier; a space is nothing.
const ecrwCell = (x, y, z) => {
    const dx = x - ECRW_CENTRE, dz = z - ECRW_CENTRE
    const r = Math.sqrt(dx * dx + dz * dz)
    // the controller, in the front wall at its foot
    if (x === ECRW_CENTRE && z === ECRW_SIZE - 1 && y === 0) return 'S'
    // the floor: a disc tucked inside the ring
    if (y === 0) return r <= 5.5 ? 'B' : r <= 6.6 ? 'W' : ' '
    // the pier: a plus up the middle, 4 high
    if ((dx === 0 || dz === 0) && Math.abs(dx) <= 1 && Math.abs(dz) <= 1 && y >= 0 && y <= 3) return 'C'
    // the wall: a plain ring on its edge, 2 high, and the same ring once more for the rim
    if (r >= 5.4 && r <= 6.6 && y === 1) return 'W'
    if (r >= 5.4 && r <= 6.6 && y === 2) return 'R'
    return ' '
}
// forEach, not for: KubeJS's engine (Rhino) keeps a const in a loop body at its first pass's value, so loop bodies
// use callbacks (a const in a callback is new each call; lint rule S3). Array(n).fill(0), not Array.from({ length: n }):
// Rhino's Array.from leaves n holes, and forEach skips holes. Precomputed once; .pattern() reuses this const.
const ECRW_AISLES = (() => {
    const aisles = []
    Array(ECRW_SIZE).fill(0).forEach((_, z) => {
        const rows = []
        Array(ECRW_HEIGHT).fill(0).forEach((_, y) => {
            const cells = []
            Array(ECRW_SIZE).fill(0).forEach((_, x) => { cells.push(ecrwCell(x, y, z)) })
            rows.push(cells.join(''))
        })
        aisles.push(rows)
    })
    return aisles
})()

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }
    // the GCYM casings live in GTBlocks' sister registry: forcing its init while KubeJS is still constructing
    // NPEs GT's own startup, so it loads here, where the registries are up (lint rule S5)
    const $EcrwWatertight = Java.loadClass('com.gregtechceu.gtceu.common.data.GCYMBlocks')

    // Layout is module-level (ECRW_AISLES), precomputed once above.
    event.create('ecrw', 'multiblock')
        .langValue('Evaporation Condensation Retaining Well')
        .machine(holder => new $EvaporationWellMachine(holder))
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('evaporation')])
        // perfect overclocks
        .recipeModifiers([GTRecipeModifiers.OC_PERFECT])
        .appearanceBlock($EcrwWatertight.CASING_WATERTIGHT)
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.ecrw.tooltip', 4))
        .pattern(definition => {
            // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count)); any
            // block of the wall may be one
            const wall = Predicates.blocks($EcrwWatertight.CASING_WATERTIGHT.get())
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(4, 2))
                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1))
            let pattern = FactoryBlockPattern.start()
            ECRW_AISLES.forEach(aisle => { pattern = pattern.aisle(aisle) })
            return pattern
                .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                .where(' ', Predicates.any())
                .where('W', wall)
                .where('R', Predicates.blocks($EcrwWatertight.CASING_WATERTIGHT.get()))
                .where('B', Predicates.blocks(GTBlocks.CASING_STAINLESS_CLEAN.get()))
                .where('C', Predicates.blocks(GTBlocks.CASING_STAINLESS_CLEAN.get()))
                .build()
        })
        // the controller: watertight casing with the assembly line face, and the rake round the pier
        // (pond centre 6 back, its blades scraping the floor)
        .model($EcrwModels.workableCasingWithEcrwArm('gtceu:block/casings/gcym/watertight_casing',
            'gtceu:block/multiblock/assembly_line', 6, 1.3, 4.2))
        .hasBER(true)
})
