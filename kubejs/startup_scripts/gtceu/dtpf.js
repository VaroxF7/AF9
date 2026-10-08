// AF9 - the Dimensionally Transcendent Plasma Forge (DTPF), after GTNH's: a vast hall that forges plasma into matter.
// Machine logic: af9-core PlasmaForgeMachine (the running-time ramp: the longer it runs without a pause, the cheaper and
// faster it forges). Recipes: server_scripts/mods/gtceu/dtpf.js. Spec: docs/dtpf.md
//
// The structure is AF9's own, not GTNH's: a 37 x 29 x 37 square hall of beams. Rows bottom -> top, aisles back -> front
// (the controller's, in the front gatehouse, last). From the ground up:
//   * the foundation: a square of stable casing with diagonals and two square rings of coils and casing inlaid, a plinth;
//   * the wall: a square ring of fusion casing four blocks high round its edge, a gate in the middle of each side (the
//     front one closed round the controller) framed by a gatehouse of two columns and a lintel with a coil keystone;
//     every block of the wall may be a hatch;
//   * four corner pylons, 5 x 5 shafts with coil bands and glass slits, a 3 x 3 neck and a spire 27 high;
//   * the plasma core in the middle: a 9 x 9 cuboid, 16 high, of fusion casing with glass windows and coil rings, a
//     plasma bar of glass in its middle, a crown and a needle;
//   * the supports: from every pylon two diagonal struts cross on their way to the core, one from the top of the pylon to
//     the foot of the core, one from its waist to the core's crown;
//   * two square rings of beams hung between the struts at the core's middle, on four spokes from the core;
//   * the top: a frame of beams joining the pylon tops, and a diagonal cross of beams over the core to its needle.
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
    // The design: the block at x, y, z (y up, z towards the front) as a letter of the pattern. S controller, B foundation,
    // W wall (hatches), F beams and struts, G glass, K coils, T rings and caps, H pylons and gatehouses; a space is nothing.
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
        // the controller, in the front gatehouse's wall
        if (x === DTPF_CENTRE && z === DTPF_SIZE - 1 && y === 2) return 'S'
        // ---- the four corner pylons: a 5 x 5 shaft, a 3 x 3 neck, a spire
        const ex = ax - 15, ez = az - 15, pe = Math.max(Math.abs(ex), Math.abs(ez))
        if (pe <= 2 && y >= 1 && y <= 27) {
            if (y <= 16) {
                if (pe < 2) return ' '
                if (y % 5 === 0) return 'K'
                return (Math.abs(ex) === 2 && Math.abs(ez) === 2) ? 'H' : ((y % 5 === 2 || y % 5 === 3) && (ex === 0 || ez === 0)) ? 'G' : 'H'
            }
            if (y <= 22) { return pe <= 1 ? (pe === 1 ? ((y % 3 === 0) ? 'K' : 'H') : ' ') : ' ' }
            if (pe === 0) return (y <= 24) ? 'H' : (y % 2 === 0 ? 'K' : 'G')
            return ' '
        }
        // ---- the diagonal supports: two struts a corner, crossing: top of the pylon down to the core's foot, and the pylon's
        // waist up to the core's crown
        let d1 = distSeg(ax, y, az, 15, 21, 15, 5, 5, 5), d2 = distSeg(ax, y, az, 15, 9, 15, 4, 20, 4)
        if (d1[0] <= 1.45 || d2[0] <= 1.45) {
            const d = d1[0] <= 1.45 ? d1 : d2
            return (d[0] > 0.9 && Math.floor(d[1] * 9) % 3 === 0) ? 'K' : 'F'
        }
        // ---- the central plasma core: a 9 x 9 cuboid with windows, coil rings and a plasma bar in its middle
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
        if (y === 19 && cheb <= 2) return 'T'
        if (ax === 0 && az === 0 && y >= 20 && y <= 26) return y % 2 === 0 ? 'K' : 'G'
        // ---- the top frame joining the pylons, and the cross of diagonal beams over the core
        if (y >= 21 && y <= 22 && cheb <= 16 && ((az >= 14 && az <= 16 && ax <= 13) || (ax >= 14 && ax <= 16 && az <= 13))) {
            return ((ax + az) % 6 === 0 && y === 22) ? 'K' : 'F'
        }
        if (y === 25 && cheb <= 14 && Math.abs(ax - az) <= 1) return Math.abs(ax - az) === 0 && (ax % 4 === 0) ? 'K' : 'F'
        if (y === 26 && cheb <= 12 && Math.abs(ax - az) === 0 && cheb > 2) return 'T'
        // ---- two square rings of beams, hung between the struts, with spokes to the core
        if ((y === 11 || y === 12) && cheb === 9 && ax <= 9 && az <= 9) return 'T'
        if ((y === 12 || y === 13) && cheb === 12) return 'T'
        if (y === 12 && (ax === 0 || az === 0) && cheb >= 5 && cheb <= 8) return 'F'
        // ---- the gatehouses: columns and a lintel round every gate, the front one round the controller
        const side = Math.max(ax, az)
        const alongSide = ax >= az ? az : ax        // the offset along the wall
        if (cheb >= 17 && cheb <= 18 && alongSide >= 3 && alongSide <= 4 && y >= 1 && y <= 8) return 'H'
        if (cheb >= 17 && cheb <= 18 && alongSide <= 4 && y >= 8 && y <= 9) return (alongSide === 0 && y === 9) ? 'K' : 'F'
        // ---- the wall: a square ring four high on the foundation's edge; every block of it may be a hatch
        if (cheb === 18 && y >= 1 && y <= 4) {
            const front = dz > 0 && az >= ax
            if (!front && alongSide <= 2 && y <= 3) return ' '       // the gates (the front one is closed)
            return (y === 4 && alongSide % 4 === 0) ? 'K' : 'W'
        }
        // ---- the foundation: a stepped square with inlaid diagonals and rings, and the core's plinth
        if (y === 0 && cheb <= 18) {
            if (ax === az && cheb >= 6 && cheb <= 15) return 'K'
            if (cheb === 12 || cheb === 6) return 'T'
            return 'B'
        }
        if (y === 1 && cheb <= 7 && cheb >= 5) return 'B'
        if (y === 1 && cheb < 5 && cheb >= 0) return 'B'
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
