// AF9 - the Dimensionally Transcendent Plasma Forge (DTPF), after GTNH's: a vast hall that forges plasma into matter.
// Machine logic: af9-core PlasmaForgeMachine (the running-time ramp: the longer it runs without a pause, the cheaper and
// faster it forges). Recipes: server_scripts/mods/gtceu/dtpf.js. Spec: docs/dtpf.md
//
// The structure is AF9's own, not GTNH's: a 33 x 27 x 33 cathedral of plasma. Rows bottom -> top, aisles back -> front
// (the controller's, in the front wall, last). From the outside in:
//   * the foundation: a disc of stable casing with inlaid rings of coils and torus casing, reaching under the towers;
//   * the wall: a ring of fusion casing four blocks high with eight arches and a crenellated top; every block of it may
//     be a hatch;
//   * four towers at the corners, 17 high, of plasma-green casing with coil bands, glass slits and a spire;
//   * the torus: a ring of containment 23 blocks across and a 5 block tube, its shell of mk3 fusion casing with coil
//     bands every 45 degrees and glass panes in its crown and floor, hung at the middle of the hall;
//   * four spokes of mk2 casing from the torus to the column, four pylons from the torus to the floor;
//   * the column: the plasma chamber, 7 across and 25 high, mk2 casing with glass windows and coil rings, capped by a
//     spire and a halo ring of glass and coils on four struts.
// The cell function below is the whole design; the aisles are built from it.

const $PlasmaForgeMachine = Java.loadClass('com.af9.core.machine.PlasmaForgeMachine')

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    // plasma and matter in, forged matter and fluids out
    event.create('plasma_forge')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(6, 6, 6, 3)
        .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.ARC)
})

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }

    const DTPF_SIZE = 33        // the hall's width and depth, its centre at 16
    const DTPF_HEIGHT = 27
    const DTPF_CENTRE = 16
    // The design: the block at x, y, z (y up, z towards the front) as a letter of the pattern. S controller, B foundation
    // and pylons, W wall (hatches), F column and spokes, G glass, K coils, T torus, H towers and spires; a space is nothing.
    const dtpfCell = (x, y, z) => {
        const dx = x - DTPF_CENTRE, dz = z - DTPF_CENTRE
        const r = Math.sqrt(dx * dx + dz * dz)
        const deg = (Math.atan2(dz, dx) * 180 / Math.PI + 360) % 360
        // near a multiple of `step` degrees, within `width`
        const sector = (step, width) => { const m = deg % step; return Math.min(m, step - m) <= width }
        // the controller: in the front wall, three up
        if (x === DTPF_CENTRE && z === DTPF_SIZE - 1 && y === 3) return 'S'
        // the four corner towers
        const tx = Math.abs(dx), tz = Math.abs(dz)
        if (tx >= 11.5 && tx <= 14.5 && tz >= 11.5 && tz <= 14.5 && y >= 1 && y <= 17) {
            const ex = tx - 13, ez = tz - 13
            const edge = Math.max(Math.abs(ex), Math.abs(ez))
            if (y <= 16) {
                if (edge > 1.5) return ' '
                if (edge === 1) {
                    return (y % 4 === 0) ? 'K' : (y % 4 === 2 && (Math.abs(ex) === 0 || Math.abs(ez) === 0)) ? 'G' : 'H'
                }
                return ' '
            }
            return edge <= 1 ? 'G' : ' '
        }
        if (tx >= 12 && tx <= 14 && tz >= 12 && tz <= 14 && y >= 18 && y <= 20) {
            const edge = Math.max(Math.abs(tx - 13), Math.abs(tz - 13))
            if (edge <= 0) return y === 19 ? 'G' : 'K'
            return ' '
        }
        // the central column: the plasma chamber
        if (y >= 1 && y <= 25 && r <= 3.6) {
            if (y === 25) return 'H'
            if (r >= 2.4) {
                if (y === 6 || y === 12 || y === 18 || y === 23) return 'K'
                if ((y % 4 === 2 || y % 4 === 3) && sector(45, 13)) return 'G'
                return 'F'
            }
            if (y === 24) return 'H'
            return ' '
        }
        if (y === 26 && r <= 1.5) return 'H'
        // the torus, major radius 11.5 and tube radius 2.4 round y = 12: a shell with coil bands and glass panes
        const tor = Math.sqrt((r - 11.5) * (r - 11.5) + (y - 12) * (y - 12))
        if (tor >= 1.45 && tor <= 2.5) {
            if (sector(45, 5) && (y - 12) * (y - 12) <= 2.6) return 'K'
            if (sector(22.5, 6.5) && Math.abs(y - 12) >= 1.6) return 'G'
            return 'T'
        }
        // the four spokes, along the axes, from the column to the torus: a tube round y = 12
        if ((Math.abs(dx) <= 2 || Math.abs(dz) <= 2) && r > 3.6 && r < 9.0) {
            const along = Math.abs(dx) <= 2 && Math.abs(dz) > 2 ? Math.abs(dx) : Math.abs(dz)
            const off = Math.sqrt(along * along + (y - 12) * (y - 12))
            if (Math.min(Math.abs(dx), Math.abs(dz)) <= 2 && off >= 1.0 && off <= 1.9) return 'F'
        }
        // the four pylons under the torus, on the diagonals
        const px = Math.abs(dx) - 8.1, pz = Math.abs(dz) - 8.1
        if (y >= 1 && y <= 10 && Math.sqrt(px * px + pz * pz) <= 1.9) return (y % 3 === 0) ? 'K' : 'B'
        // the halo ring over the column and its four struts
        if (y === 21 && r >= 6 && r <= 7.2) return sector(45, 4) ? 'K' : 'G'
        if (y === 21 && r > 3.6 && r < 6 && (Math.abs(dx) <= 0.5 || Math.abs(dz) <= 0.5)) return 'F'
        // the wall ring with its arches, and its crenellated top
        if (r >= 15.3 && r <= 16.7 && y >= 1 && y <= 5) {
            const front = dz > 14
            const arch = sector(45, 6) && !(front && Math.abs(dx) <= 3) && y <= 3 && !(deg % 90 < 1 || deg % 90 > 89)
            if (arch) return ' '
            if (y === 5) return (Math.round(deg / 5.6) % 2 === 0) ? 'W' : ' '
            return 'W'
        }
        // the foundation: the disc, reaching under the towers, with inlaid rings, and the column's plinth
        if (y === 0 && tx >= 11.5 && tx <= 14.5 && tz >= 11.5 && tz <= 14.5) return 'B'
        if (y === 0 && r <= 16.8) return (r >= 8.6 && r <= 9.5) ? 'K' : (r >= 12.6 && r <= 13.2) ? 'T' : 'B'
        if (y === 1 && r > 3.6 && r <= 5.2) return 'B'
        return ' '
    }
    // forEach, not for: KubeJS's engine (Rhino) keeps a const in a loop body at its first pass's value, so loop bodies
    // use callbacks (a const in a callback is new each call; lint rule S3)
    const dtpfAisles = () => {
        const aisles = []
        Array.from({ length: DTPF_SIZE }).forEach((_, z) => {
            const rows = []
            Array.from({ length: DTPF_HEIGHT }).forEach((_, y) => {
                let row = ''
                Array.from({ length: DTPF_SIZE }).forEach((_, x) => { row += dtpfCell(x, y, z) })
                rows.push(row)
            })
            aisles.push(rows)
        })
        return aisles
    }

    event.create('dtpf', 'multiblock')
        .langValue('Dimensionally Transcendent Plasma Forge')
        .machine(holder => new $PlasmaForgeMachine(holder))
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('plasma_forge')])
        // FORGE_GATE: only a recipe the hatches can fully supply; RAMP: the longer it runs, the cheaper and faster
        // (af9-core); perfect overclocks
        .recipeModifiers([$PlasmaForgeMachine.FORGE_GATE, $PlasmaForgeMachine.RAMP, GTRecipeModifiers.OC_PERFECT])
        .appearanceBlock(() => Block.getBlock('gtceu:fusion_casing'))
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.dtpf.tooltip', 6))
        .pattern(definition => {
            // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count)); any block of
            // the wall may be one
            const wall = Predicates.blocks('gtceu:fusion_casing')
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(4, 2))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(8, 2))
                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(4, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(4, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(4, 1))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1))
            let pattern = FactoryBlockPattern.start()
            dtpfAisles().forEach(aisle => { pattern = pattern.aisle(aisle) })
            return pattern
                .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                .where(' ', Predicates.any())
                .where('W', wall)
                .where('B', Predicates.blocks('gtceu:stable_machine_casing'))
                .where('F', Predicates.blocks('gtceu:fusion_casing_mk2'))
                .where('T', Predicates.blocks('gtceu:fusion_casing_mk3'))
                .where('G', Predicates.blocks('gtceu:fusion_glass'))
                .where('K', Predicates.blocks('gtceu:superconducting_coil'))
                .where('H', Predicates.blocks('gtceu:high_temperature_smelting_casing'))
                .build()
        })
        // the controller: fusion casing with GTNH's DTPF face (kubejs assets, overlay_front*)
        .workableCasingModel('gtceu:block/casings/fusion/fusion_casing', 'gtceu:block/multiblock/dtpf')
})
