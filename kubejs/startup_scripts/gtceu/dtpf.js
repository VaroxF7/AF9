// AF9 - the Dimensionally Transcendent Plasma Forge (DTPF), after GTNH's: a vast hall that forges plasma into matter.
// Machine logic: af9-core PlasmaForgeMachine (the running-time ramp: the longer it runs without a pause, the cheaper and
// faster it forges). Recipes: server_scripts/mods/gtceu/dtpf.js. Spec: docs/dtpf.md
//
// The structure is AF9's own, not GTNH's: a 37 x 29 x 37 industrial skeleton, all beams and bracing. Rows bottom -> top,
// aisles back -> front (the controller's, in the front wall, last). From the ground up:
//   * the foundation: a square of stable casing with diagonals and two square rings of coils and casing inlaid;
//   * the wall: a plain low square ring of fusion casing on its edge, a gate in the middle of each side (the front one
//     closed round the controller); every block of it may be a hatch;
//   * four corner pylons: open lattice towers, 5 x 5, of four 2 x 2 posts with ring girders every 6 blocks and X bracing
//     in every face, capped by a flat slab and a coil;
//   * the plasma core in the middle: a 9 x 9 box, 16 high, of fusion casing with glass windows and coil rings, a glass
//     plasma bar up its middle, a flat roof with four exhaust stacks and a hub post;
//   * the supports: from every pylon two diagonal struts cross on their way to the core, one from the top of the pylon to
//     the foot of the core, one from its waist to the core's roof;
//   * two square rings of beams hung between the struts at the core's middle, on four spokes from the core;
//   * the top: box girders joining the pylon tops, and a diagonal cross of beams over the core.
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

    const DTPF_SIZE = 37        // the hall's width and depth, its centre at 18
    const DTPF_HEIGHT = 29
    const DTPF_CENTRE = 18
    // The design: the block at x, y, z (y up, z towards the front) as a letter of the pattern. S controller, B posts and
    // foundation, W wall (hatches), F beams and struts, G glass, K coils, T girders and slabs; a space is nothing.
    // distance from a point to a segment, and how far along it the nearest point is (0-1)
        const distSeg = (px, py, pz, ax, ay, az, bx, by, bz) => {
        const vx = bx - ax, vy = by - ay, vz = bz - az
        const t = Math.max(0, Math.min(1, ((px - ax) * vx + (py - ay) * vy + (pz - az) * vz) / (vx * vx + vy * vy + vz * vz)))
        const cx = ax + vx * t - px, cy = ay + vy * t - py, cz = az + vz * t - pz
        return [Math.sqrt(cx * cx + cy * cy + cz * cz), t]
    }
    const dtpfCell = (x, y, z) => {
        const dx = x - DTPF_CENTRE, dz = z - DTPF_CENTRE
        const ax = Math.abs(dx), az = Math.abs(dz)
        const cheb = Math.max(ax, az)
        // the controller, in the front wall
        if (x === DTPF_CENTRE && z === DTPF_SIZE - 1 && y === 2) return 'S'
        // ---- the four corner pylons: open lattice towers, 5 x 5, flat topped
        const ex = ax - 15, ez = az - 15, pe = Math.max(Math.abs(ex), Math.abs(ez))
        if (pe <= 2 && y >= 1 && y <= 26) {
            if (y >= 24) return y <= 25 ? 'T' : (ex === 0 && ez === 0 ? (y === 26 ? 'K' : ' ') : ' ')
            const post = Math.abs(ex) >= 1 && Math.abs(ez) >= 1                          // the four 2 x 2 posts
            if (post) return (y % 6 === 0) ? 'T' : 'B'
            if (pe === 2) {
                if (y % 6 === 0) return 'T'                                              // the ring girders
                const u = Math.abs(ex) === 2 ? ez : ex, h = y % 6                        // X bracing in every face
                if (Math.abs(u) <= 1 && (u === h - 3 || u === 3 - h)) return 'F'
            }
            return ' '
        }
        // ---- the diagonal supports: two struts a corner, crossing
        const d1 = distSeg(ax, y, az, 15, 21, 15, 5, 5, 5), d2 = distSeg(ax, y, az, 15, 9, 15, 4, 20, 4)
        if (d1[0] <= 1.45 || d2[0] <= 1.45) {
            const d = d1[0] <= 1.45 ? d1 : d2
            return (d[0] > 0.9 && Math.floor(d[1] * 9) % 3 === 0) ? 'K' : 'F'
        }
        // ---- the plasma core: a 9 x 9 box with windows and coil rings, a plasma bar, a flat roof with four exhaust stacks
        if (cheb <= 4 && y >= 2 && y <= 17) {
            if (ax === 0 && az === 0 && y >= 3) return 'G'
            if (y === 2) return 'T'
            if (cheb < 4 && y < 17) return ' '
            if (y === 5 || y === 10 || y === 15 || y === 17) return 'K'
            const along = ax === 4 ? az : ax
            if (along <= 2 && along % 2 === 0 && y % 5 >= 1 && y % 5 <= 3 && y >= 3 && y <= 14) return 'G'
            return 'F'
        }
        if (y === 18 && cheb <= 4) return 'T'
        if (y >= 19 && y <= 22 && ax === 3 && az === 3) return y === 22 ? 'K' : 'F'            // the stacks
        if (ax === 0 && az === 0 && y >= 19 && y <= 24) return y === 24 ? 'K' : 'F'            // the hub post
        // ---- the top: box girders along the pylon tops, and a diagonal cross of beams over the core
        if (y >= 21 && y <= 23 && cheb <= 16 && ((az === 15 && ax <= 13) || (ax === 15 && az <= 13))) {
            const along = az === 15 ? ax : az
            if (y === 21 || y === 23) return 'F'
            return along % 2 === 0 ? 'F' : ' '                                                  // verticals every second block
        }
        if (y === 25 && cheb <= 14 && Math.abs(ax - az) <= 1) return (Math.abs(ax - az) === 0 && ax % 4 === 0) ? 'K' : 'F'
        if (y === 24 && cheb <= 3) return 'T'
        // ---- two square rings of beams, hung between the struts, with spokes to the core
        if ((y === 11 || y === 12) && cheb === 9 && ax <= 9 && az <= 9) return 'T'
        if ((y === 12 || y === 13) && cheb === 12) return 'T'
        if (y === 12 && (ax === 0 || az === 0) && cheb >= 5 && cheb <= 8) return 'F'
        // ---- the wall: a plain low square, gates (no frames) in the middle of the sides, a girder along its top
        if (cheb === 18 && y >= 1 && y <= 3) {
            const alongSide = ax >= az ? az : ax
            const front = dz > 0 && az >= ax
            if (!front && alongSide <= 2 && y <= 2) return ' '
            return (y === 3 && alongSide % 6 === 0) ? 'K' : 'W'
        }
        // ---- the foundation
        if (y === 0 && cheb <= 18) {
            if (ax === az && cheb >= 6 && cheb <= 15) return 'K'
            if (cheb === 12 || cheb === 6) return 'T'
            return 'B'
        }
        if (y === 1 && cheb <= 7) return 'B'
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
                .build()
        })
        // the controller: fusion casing with GTNH's DTPF face (kubejs assets, overlay_front*)
        .workableCasingModel('gtceu:block/casings/fusion/fusion_casing', 'gtceu:block/multiblock/dtpf')
})
