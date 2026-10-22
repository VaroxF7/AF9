// AF9 - the Dimensionally Transcendent Plasma Forge (DTPF), after GTNH's: a vast hall that forges plasma into matter.
// Machine logic: af9-core PlasmaForgeMachine (the running-time ramp: the longer it runs without a pause, the cheaper and
// faster it forges). Recipes: server_scripts/mods/gtceu/dtpf.js. Spec: docs/dtpf.md
//
// The structure is AF9's own, not GTNH's: a forge LINE, 21 x 22 x 45, that teaches the process front to back. Its blocks
// are GTNH's three casings of the original (Dimensionally Transcendent, Dimensional Injection, Dimensional Bridge) and two
// coils of AF9's (Plasma Containment, Plasma Resonance), all AF9 Core's blocks (registry/AF9Blocks). Aisles back -> front
// (the controller's, in the front deck, last), rows bottom -> top. From the front:
//   * the injection deck: a low hall, 17 wide, whose front wall of injection casing is the only place for the controller
//     and every hatch; its roof carries the bore;
//   * two feed conduits, along both sides, from the deck to the press, their top row a dashed line of bridge casing that
//     glows while the forge works;
//   * the plasma bore: an octagonal glass-windowed tube, 31 long, with a plasma bar of glass down its axis, through five
//     containment coil rings (resonance coils at their corners) held in a cage of rails and legs;
//   * the press at the back: a 19 x 13 x 8 plated block with a glowing bridge band, the bore's mouth ringed with
//     resonance coil, a hammer head on its roof and two exhaust stacks.
// The cell function below is the whole design; the aisles are built from it. Mk2 is the same hall.

const $PlasmaForgeMachine = Java.loadClass('com.af9.core.machine.PlasmaForgeMachine')
const $PlasmaHeaterMachine = Java.loadClass('com.af9.core.machine.PlasmaHeaterMachine')
const $ForgePlasmaHatch = Java.loadClass('com.af9.core.machine.part.PlasmaHatchPartMachine')

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    // plasma and matter in, forged matter and fluids out (three of each: the recipe page lays out rows of three)
    event.create('plasma_forge')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(3, 3, 3, 3)
        .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.ARC)
    // osmium dust and Endion in, heater plasma out
    event.create('plasma_heating')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(2, 1, 2, 1)
        .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.FURNACE)
})

// DTPF layout, precomputed ONCE at module level and reused by Mk1 + Mk2 .pattern(): DTPF_AISLES is the 21 x 22 x 45
// dtpfCell table. The design: the block at x, y, z (y up, z towards the front) as a letter of the pattern.
//   S controller, W injection casing (hatches), N transcendent casing, B bridge casing (glows), K containment coil,
//   R resonance coil, G fusion glass; a space is nothing.
const DTPF_WIDTH = 21       // x: the hall's width, its axis at 10
const DTPF_HEIGHT = 22      // y
const DTPF_DEPTH = 45       // z: 0 is the press's back, 44 the front wall
const DTPF_AXIS_X = 10
const DTPF_AXIS_Y = 10      // the bore's axis
const DTPF_RING_Z = [11, 16, 21, 26, 31] // the first block of each coil ring (two thick)
const dtpfCell = (x, y, z) => {
    const ax = Math.abs(x - DTPF_AXIS_X)
    const dy = y - DTPF_AXIS_Y, ady = Math.abs(dy)
    const cheb = Math.max(ax, ady)
    // the controller, in the front wall, two up
    if (x === DTPF_AXIS_X && y === 2 && z === DTPF_DEPTH - 1) return 'S'
    // ---- the foundation: a slab with the axis lit down it
    if (y === 0) return (ax === 0 && z >= 8 && z <= 38) ? 'B' : 'N'
    // ---- the injection deck: a low hall, front wall = the hatch wall
    if (z >= 39 && ax <= 8 && y <= 4) {
        if (y === 4) return 'N'                                                              // its roof
        if (z === 44) return 'W'
        if (ax === 8 || z === 39) return 'N'
        return ' '
    }
    // ---- the feed conduits: from the deck to the press, a dashed line of bridge casing along the top
    if (ax === 9 && y >= 1 && y <= 3 && z >= 8 && z <= 39) return (y === 3 && z % 2 === 0) ? 'B' : 'N'
    // ---- the press: a plated block with a bridge band, the bore's mouth ringed with resonance coil
    if (z <= 7 && ax <= 9 && y >= 1 && y <= 13) {
        if (z === 7 && cheb === 4) return 'R'
        if (z === 0 || z === 7 || ax === 9 || y === 1 || y === 13) return y === 6 ? 'B' : 'N'
        return ' '
    }
    // the hammer head on the roof, two exhaust stacks capped with bridge casing
    if (z >= 1 && z <= 6 && ax <= 4 && y >= 14 && y <= 16) return y === 15 && (ax === 4 || z === 1 || z === 6) ? 'B' : 'N'
    if (z >= 1 && z <= 2 && (ax === 7 || ax === 8) && y >= 14 && y <= 20) return y === 20 ? 'B' : 'N'
    // the pier that carries the bore's end plate on the deck's roof
    if (z === 39 && ax === 0 && y >= 5 && y <= 6) return 'N'
    // ---- the plasma bore: an octagonal tube, windows in the gaps between the rings, a plasma bar down its axis
    if (z >= 8 && z <= 39 && cheb <= 3 && ax + ady <= 5) {
        if (z >= 38) return 'N'                                                             // the end plate, two thick
        if (ax === 0 && dy === 0) return 'G'                                                // the plasma bar
        const shell = ax === 3 || ady === 3 || ax + ady === 5
        if (!shell) return ' '
        const gap = DTPF_RING_Z.some(r => z >= r + 2 && z <= r + 4) || z === 9 || z === 36
        return (gap && ((ax === 3 && ady <= 1) || (ady === 3 && ax <= 1))) ? 'G' : 'N'
    }
    // ---- the coil rings (resonance coil at the corners), their spokes to the bore, the rails and legs of the cage
    const ring = DTPF_RING_Z.some(r => z === r || z === r + 1)
    if (z >= 11 && z <= 32 && ax <= 5 && ady <= 5) {
        const corner = ax === 5 && ady === 5
        if (ring) {
            if (cheb === 5) return corner ? 'R' : 'K'
            if (ax === 4 && ady === 0 || ax === 0 && ady === 4) return 'N'                  // spokes
        } else if (corner) {
            return 'N'                                                                      // rails between the rings
        }
    }
    if (ring && z >= 11 && z <= 32 && ax === 5 && y >= 1 && y <= 4) return 'N'              // legs under the rings' feet
    return ' '
}
// forEach, not for: KubeJS's engine (Rhino) keeps a const in a loop body at its first pass's value, so loop bodies
// use callbacks (a const in a callback is new each call; lint rule S3). Array(n).fill(0), not Array.from({ length: n }):
// Rhino's Array.from leaves n holes, and forEach skips holes. Precomputed once; .pattern() reuses this const.
const DTPF_AISLES = (() => {
    const aisles = []
    Array(DTPF_DEPTH).fill(0).forEach((_, z) => {
        const rows = []
        Array(DTPF_HEIGHT).fill(0).forEach((_, y) => {
            const cells = []
            Array(DTPF_WIDTH).fill(0).forEach((_, x) => { cells.push(dtpfCell(x, y, z)) })
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

    // The hall's pattern: one for the Mk1 and the Mk2. Parts have a maximum only, never a required count
    // (setMaxGlobalLimited(max, preview count)); any block of the front wall may be one, nowhere else.
    const dtpfPattern = definition => {
        const wall = Predicates.blocks('af9:dtpf_injection_casing')
            .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(4, 2))
            // its forging recipes draw 100 A of UHV: a laser target hatch carries that
            .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(2, 0))
            .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(8, 2))
            .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(4, 1))
            .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(4, 1))
            .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(4, 1))
            // osmium plasma for the core heat goes through plasma hatches only (never recipe inputs)
            .or(Predicates.abilities($ForgePlasmaHatch.PLASMA_INPUT).setMaxGlobalLimited(2, 1))
            .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1))
        let pattern = FactoryBlockPattern.start()
        DTPF_AISLES.forEach(aisle => { pattern = pattern.aisle(aisle) })
        return pattern
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            .where(' ', Predicates.any())
            .where('W', wall)
            .where('N', Predicates.blocks('af9:dtpf_transcendent_casing'))
            .where('B', Predicates.blocks('af9:dtpf_bridge_casing'))
            .where('K', Predicates.blocks('af9:dtpf_containment_coil'))
            .where('R', Predicates.blocks('af9:dtpf_resonance_coil'))
            .where('G', Predicates.blocks('gtceu:fusion_glass'))
            .build()
    }

    event.create('dtpf', 'multiblock')
        .langValue('Dimensionally Transcendent Plasma Forge')
        .machine(holder => new $PlasmaForgeMachine(holder))
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('plasma_forge')])
        // FORGE_GATE: only a recipe the hatches can fully supply; RAMP: the longer it runs, the cheaper and faster
        // (af9-core); perfect overclocks
        .recipeModifiers([$PlasmaForgeMachine.FORGE_GATE, $PlasmaForgeMachine.RAMP, GTRecipeModifiers.OC_PERFECT])
        .appearanceBlock(() => Block.getBlock('af9:dtpf_injection_casing'))
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.dtpf.tooltip', 6))
        .pattern(dtpfPattern)
        // the controller: injection casing with GTNH's DTPF face (kubejs assets, overlay_front*)
        .workableCasingModel('af9:block/dtpf_injection_casing', 'gtceu:block/multiblock/dtpf')

    // the Mk2: the same hall, half the osmium for four times the amperage.
    // The controller is turned from the Mk1's with the Plasma Heater's heart (server dtpf.js).
    event.create('dtpf_mk2', 'multiblock')
        .langValue('Dimensionally Transcendent Plasma Forge Mk2')
        .machine(holder => new $PlasmaForgeMachine(holder, true))
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('plasma_forge')])
        // FORGE_GATE covers the Mk2 too (readiness plus four times the amperage); RAMP as ever
        .recipeModifiers([$PlasmaForgeMachine.FORGE_GATE, $PlasmaForgeMachine.RAMP, GTRecipeModifiers.OC_PERFECT])
        .appearanceBlock(() => Block.getBlock('af9:dtpf_injection_casing'))
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.dtpf.tooltip', 6))
        .pattern(dtpfPattern)
        .workableCasingModel('af9:block/dtpf_injection_casing', 'gtceu:block/multiblock/dtpf')

        // Plasma Hatch: an input hatch for osmium plasma only, HV-UHV. The forge burns readiness
    // through these; the heater fills them.
    event.create('plasma_hatch', 'custom')
        .tiers(GTValues.HV, GTValues.EV, GTValues.IV, GTValues.LuV, GTValues.ZPM, GTValues.UV,
            GTValues.UHV)
        .machine((holder, tier, tankScaling) => new $ForgePlasmaHatch(holder, tier))
        .definition((tier, builder) => {
            builder
                .langValue(`${GTValues.VN[tier]} Plasma Hatch`)
                .rotationState(RotationState.ALL)
                ['colorOverlayTieredHullModel(java.lang.String,java.lang.String,java.lang.String)'](
                    'overlay_pipe_in_emissive', null, 'overlay_fluid_hatch')
                .abilities(PartAbility.IMPORT_FLUIDS, $ForgePlasmaHatch.PLASMA_INPUT)
                ['tooltips(net.minecraft.network.chat.Component[])']([
                    Component.translatable('af9.plasma_hatch.tooltip.0'),
                    Component.translatable('af9.plasma_hatch.tooltip.1'),
                    Component.translatable('gtceu.universal.tooltip.fluid_storage_capacity', `${1000 << tier}`)])
        })

    // Plasma Heater: the sub multi that ionises osmium dust with Endion to osmium plasma,
    // the forge's heater fuel. 5 x 4 x 5, controller front-bottom-centre.
    event.create('plasma_heater', 'multiblock')
        .machine(holder => new $PlasmaHeaterMachine(holder))
        .langValue('Plasma Heater')
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('plasma_heating')])
        .recipeModifiers([GTRecipeModifiers.OC_PERFECT])
        .appearanceBlock(GTBlocks.CASING_STAINLESS_CLEAN)
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.plasma_heater.tooltip', 3))
        .pattern(definition => FactoryBlockPattern.start()
            .aisle('XXXXX', 'XXXXX', 'XXXXX', 'XXXXX')
            .aisle('XXXXX', 'XX#XX', 'XX#XX', 'XXXXX')
            .aisle('XXXXX', 'XX#XX', 'XX#XX', 'XXXXX')
            .aisle('XXXXX', 'XX#XX', 'XX#XX', 'XXXXX')
            .aisle('XXSXX', 'XX#XX', 'XX#XX', 'XXXXX')
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count))
            .where('X', Predicates.blocks(GTBlocks.CASING_STAINLESS_CLEAN.get())
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
            .where('#', Predicates.air())
            .build())
        .workableCasingModel('gtceu:block/casings/solid/machine_casing_clean_stainless_steel',
            'gtceu:block/multiblock/multi_furnace')
})
