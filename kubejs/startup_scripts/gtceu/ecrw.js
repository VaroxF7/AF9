// AF9 - the Evaporation Condensation Retaining Well (ECRW): a shallow round pond that boils dilute fluids
// down and keeps the condensate, the replacement for Mekanism's evaporation plant and the evaporation GregTech
// had a while back. Machine logic: af9-core EvaporationWellMachine (plain GregTech behaviour) and the rake arm
// that turns while a recipe runs (af9-core EcrwArmRender, wired below). Recipes: server_scripts/mods/gtceu/ecrw.js.
// Spec: docs/ecrw.md
//
// The structure is generated, not listed: a 15 x 15 pond, its centre at 7, rows bottom -> top, aisles back ->
// front (the controller's, in the front wall, last). From the ground up:
//   * the floor: a disc of solid steel casing;
//   * the wall: a plain ring on its edge, 3 high, glass at the waterline; every steel block of it may be a hatch;
//   * the pier: a 3 x 3 drive column up the middle, capped where the walkway lands;
//   * the walkway: a half bridge of steel frames from the back rim to the pier, with rails a block above it;
//   * the rim: a ring of steel frames along the wall's top.
// The rake itself is no blocks: the render draws it, turning on the pier while a recipe runs.
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

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }

    const ECRW_SIZE = 15         // the pond's width and depth, its centre at 7
    const ECRW_HEIGHT = 6
    const ECRW_CENTRE = 7
    // The design: the block at x, y, z (y up, z towards the front) as a letter of the pattern. S controller,
    // B floor, W wall (hatches), G the glass waterline, C the drive pier, F walkway, rim and pier cap.
    const ecrwCell = (x, y, z) => {
        const dx = x - ECRW_CENTRE, dz = z - ECRW_CENTRE
        const r = Math.sqrt(dx * dx + dz * dz)
        const cheb = Math.max(Math.abs(dx), Math.abs(dz))
        // the controller, in the front wall at the waterline's foot
        if (x === ECRW_CENTRE && z === ECRW_SIZE - 1 && y === 1) return 'S'
        // the floor: a disc under everything
        if (y === 0) return r <= 7.5 ? 'B' : ' '
        // the walkway: a half bridge from the back rim to the pier, rails a block above it (before the pier:
        // the deck crosses the column's top)
        if (y === 4 && x === ECRW_CENTRE && z <= ECRW_CENTRE) return 'F'
        if (y === 5 && (x === ECRW_CENTRE - 1 || x === ECRW_CENTRE + 1) && z <= ECRW_CENTRE) return 'F'
        if (y === 5 && x === ECRW_CENTRE && z === ECRW_CENTRE) return 'F'
        // the pier: a 3 x 3 drive column up the middle
        if (cheb <= 1 && y >= 1 && y <= 4) return 'C'
        // the wall: a plain ring on its edge, glass at the waterline
        if (r >= 6.4 && r <= 7.6 && y >= 1 && y <= 3) return y === 2 ? 'G' : 'W'
        // the rim: a ring of frames along the wall's top
        if (r >= 6.4 && r <= 7.6 && y === 4) return 'F'
        return ' '
    }
    // forEach, not for: KubeJS's engine (Rhino) keeps a const in a loop body at its first pass's value, so loop bodies
    // use callbacks (a const in a callback is new each call; lint rule S3). Array(n).fill(0), not Array.from({ length: n }):
    // Rhino's Array.from leaves n holes, and forEach skips holes
    const ecrwAisles = () => {
        const aisles = []
        Array(ECRW_SIZE).fill(0).forEach((_, z) => {
            const rows = []
            Array(ECRW_HEIGHT).fill(0).forEach((_, y) => {
                let row = ''
                Array(ECRW_SIZE).fill(0).forEach((_, x) => { row += ecrwCell(x, y, z) })
                rows.push(row)
            })
            aisles.push(rows)
        })
        return aisles
    }

    event.create('ecrw', 'multiblock')
        .langValue('Evaporation Condensation Retaining Well')
        .machine(holder => new $EvaporationWellMachine(holder))
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('evaporation')])
        // perfect overclocks
        .recipeModifiers([GTRecipeModifiers.OC_PERFECT])
        .appearanceBlock(GTBlocks.CASING_STEEL_SOLID)
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.ecrw.tooltip', 4))
        .pattern(definition => {
            // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count)); the glass
            // waterline stays glass, everything else of the wall may be one
            const wall = Predicates.blocks(GTBlocks.CASING_STEEL_SOLID.get())
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(4, 2))
                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1))
            let pattern = FactoryBlockPattern.start()
            ecrwAisles().forEach(aisle => { pattern = pattern.aisle(aisle) })
            return pattern
                .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                .where(' ', Predicates.any())
                .where('W', wall)
                .where('B', Predicates.blocks(GTBlocks.CASING_STEEL_SOLID.get()))
                .where('G', Predicates.blocks('gtceu:tempered_glass'))
                .where('C', Predicates.blocks('gtceu:tungstensteel_gearbox'))
                .where('F', Predicates.blocks('gtceu:steel_frame'))
                .build()
        })
        // the controller: solid steel with the assembly line face, and the clarifier bridge on the pier (pond centre 7 back, bridge 2.5 above the controller's floor, 5.4 long)
        .model($EcrwModels.workableCasingWithEcrwArm('gtceu:block/casings/solid/machine_casing_solid_steel',
            'gtceu:block/multiblock/assembly_line', 7, 2.5, 5.4))
        .hasBER(true)
})
