// AF9 - Photolithography Line (Mk1), Photolithography Scanner (Mk2) and Orbital Lithography Station
// Realistic lithography: a coater/developer track feeding a stepper. One exposure mode per wafer substrate (see
// wafers.js): the Mk1 line prints 350, 200 and 100 nm, the Mk2 scanner 80 and 65 nm, the orbital station 50, 20, 7
// and 1 nm (in orbit). A print is always GT's own chip wafer, more of them per blank on a higher substrate.
// Each mode uses the light source its real node used and the resist made for that light:
//   350 nm            mercury-lamp i-line 365 nm             DNQ-novolac resist
//   200 nm            KrF excimer laser 248 nm               chemically amplified PHOST resist
//   100, 80 nm        ArF excimer laser 193 nm (dry)         chemically amplified methacrylate resist
//   65, 50 nm         ArF immersion (water under the lens)   same, + high-k gate from 50 nm
//   20, 7 nm          EUV 13.5 nm (tin plasma), 7 nm high-NA metal-oxide (tin-oxo) EUV resist
//   1 nm              X-ray free-electron laser, in orbit    dry resist cartridges (no spin coating without gravity)
// The line (versions 1-3) and the scanner (versions 1-2) grow like the Assembly Line (longer projection lens, better
// light source); a version runs the modes up to its own and runs lower ones faster and with fewer broken wafers.
// Both machines keep an exposure vacuum (cleanliness 0-100): the pumps raise it while there is energy and no
// maintenance problem, every finished wafer drops it by 10-15, and every print can break: the finer the node and the
// dirtier the vacuum, the likelier. See LithoMachine in af9-core.
// Recipes: server_scripts/mods/gtceu/photolithography.js. Controller behaviour: AF9 Core (af9-core/).

const $PhotolithographyLineMachine = Java.loadClass('com.af9.core.machine.PhotolithographyLineMachine')
const $PhotolithographyScannerMachine = Java.loadClass('com.af9.core.machine.PhotolithographyScannerMachine')
const $OrbitalLithographyMachine = Java.loadClass('com.af9.core.machine.OrbitalLithographyMachine')
const $LithoMachine = Java.loadClass('com.af9.core.machine.LithoMachine')
const $LithoCoolantHatch = Java.loadClass('com.af9.core.machine.part.CoolantHatchPartMachine')
const $LithoAirConditioning = Java.loadClass('com.af9.core.machine.part.AirConditioningHatchPartMachine')
const $LithoRelativeDirection = Java.loadClass('com.gregtechceu.gtceu.api.pattern.util.RelativeDirection')
const $LithoMachineModels = Java.loadClass('com.af9.core.machine.AF9MachineModels')
const $LithoSounds = Java.loadClass('com.af9.core.common.AF9Sounds')

// One recipe type per exposure mode; GT turns them into machine modes. Must stay in sync with
// com.af9.core.litho.LithoMode in af9-core.
// Fluid inputs: developer, rinse water, extreme clean dry air (the coating chemicals, HMDS, resist, BARC and TARC, are the
// Coater Track's), + the etch plasma and excimer laser gas from 200 nm, + ultrapure water (immersion) from 65 nm,
// + hafnium tetrachloride (high-k gate) from 50 nm. EUV (20 and 7 nm) needs no laser gas but molten tin and hydrogen
// for the plasma source. 7 nm prints strange matter and takes Idontknowium plasma too (server_scripts).
GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    // [node, fluid inputs, item inputs]: substrate + reticle, + the ArF Excimer Laser for 80 and 65 nm (the scanner's
    // laser slot) or the EUV Light Source for 20 and 7 nm (both not consumed)
    const lineModes = [['350nm', 3, 2], ['200nm', 5, 2], ['100nm', 5, 2], ['80nm', 5, 3], ['65nm', 6, 3],
        ['50nm', 7, 2], ['20nm', 7, 3], ['7nm', 8, 3]]
    // the orbital station's modes sound like it: a deep hum (af9-core AF9Sounds, vanilla sounds pitched down)
    const orbitalNodes = ['50nm', '20nm', '7nm']
    lineModes.forEach(([node, fluids, items]) => {
        event.create(`lithography_${node}`)
            .category('multiblock')
            .setEUIO('in')
            .setMaxIOSize(items, 2, fluids, 0) // GT's chip wafers + the chanced broken wafer out
            .setSlotOverlay(false, false, true, GuiTextures.LENS_OVERLAY)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
            .setSound(orbitalNodes.includes(node) ? $LithoSounds.ORBITAL_STATION : GTSoundEntries.ELECTROLYZER)
    })
    // chromodynium wafer, reticle, dry resist cartridge; the coolant comes from the station (af9-core)
    event.create('orbital_lithography')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(3, 2, 0, 0)
        .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
        .setSound($LithoSounds.ORBITAL_STATION)
    // plasma atomic soldering (UHV+ circuits, ion-by-ion deposition with plasma solder, no reflow): up to 9 item
    // inputs (the UHV mainframe's 8), 1 out, plasma solder + PBI in. Runs in the Hyper-Intensity Laser Engraver
    // (hile.js).
    event.create('plasma_soldering')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(9, 1, 2, 0)
        .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
        .setSound($LithoSounds.ORBITAL_STATION)
})

// Orbital station layouts, precomputed ONCE at module level and reused by .pattern() for both
// basic and Mk2 (memoized): ORBITAL_BASIC is the 25-wide design, ORBITAL_MK2_SLICES the 73x130x73 Mk2 cell table.
const ORBITAL_BASIC = [
        ['                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','                         ','                         ','        DDDDDDDDD        ','                         ','                         ','                         '],
        ['                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','            C            ','         CCCCCCC         ','      DDDCCCCCCCDDD      ','         CCCCCCC         ','            C            ','                         '],
        ['                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','            C            ','       CC   C   CC       ','    DDDCC  FHF  CCDDD    ','       CC   C   CC       ','            C            ','                         '],
        ['                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','            C            ','     CC     C     CC     ','   DDCC    FHF    CCDD   ','     CC     C     CC     ','                         ','                         '],
        ['                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','                         ','    C       C       C    ','  DDC      FHF      CDD  ','    C               C    ','            L            ','                         '],
        ['                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','                         ','   C        C        C   ','  DC       FHF       CD  ','   C                 C   ','            L            ','                         '],
        ['                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','                         ','   C        C        C   ',' DDC       FHF       CDD ','   C                 C   ','           LLL           ','                         '],
        ['                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','                         ','  C         C         C  ',' DC        FHF        CD ','  C                   C  ','           LLL           ','            O            '],
        ['                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','                         ','  C         C         C  ','DDC        FHF        CDD','  C        F F        C  ','          LLLLL          ','            O            '],
        ['                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','            C            ',' C          C          C ','DC        HHHHH        CD',' C         F F         C ','         LLLLLLL         ','            O            '],
        ['                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','            A            ','            A            ','            A            ','            C            ',' C         CCC         C ','DC       HHHHHHH       CD',' C         FFF         C ','        LLLLLLLLL        ','           OOO           '],
        ['                         ', '            A            ', '            A            ', '            A            ','            A            ', '           AAA           ','           AAA           ','           AA            ','            A            ','            A            ','           CBC           ','           CBC           ','           CBC           ',' C        CCBCC        C ','DCFFFFFFFHHHHHHHFFFFFFFCD',' C      FFFF FFFF      C ','      LLLLLLLLLLLLL      ','          OOOOO          '],
        ['            A            ', '           AAA           ', '           AAA           ', '           AAA           ','           ABA           ', '           ABA           ','           ABA           ','           ABA           ','           ABA           ','          AABAA          ','          ABBBA          ','          ABBBA          ',' CCC     CCBBBCC     CCC ',' CCCCCCCCCCBBBCCCCCCCCCC ','DCHHHHHHHHHHHHHHHHHHHHHCD',' CCC       F F       CCC ',' CC LLLLLLLLLLLLLLLLL CC ','       OOOOOKOOOOO       '],
        ['                         ', '            A            ', '            A            ', '            A            ','            A            ', '           AAA           ','           AAA           ','            AA           ','            A            ','            A            ','           CBC           ','           CBC           ','           CBC           ',' C        CCBCC        C ','DCFFFFFFFHHHHHHHFFFFFFFCD',' C      FFFF FFFF      C ','      LLLLLLLLLLLLL      ','          OOOOO          '],
        ['                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','            A            ','            A            ','            A            ','            C            ',' C         CCC         C ','DC       HHHHHHH       CD',' C         FFF         C ','        LLLLLLLLL        ','           OOO           '],
        ['                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','            C            ',' C          C          C ','DC        HHHHH        CD',' C         F F         C ','         LLLLLLL         ','            O            '],
        ['                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','                         ','  C         C         C  ','DDC        FHF        CDD','  C        F F        C  ','          LLLLL          ','            O            '],
        ['                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','                         ','  C         C         C  ',' DC        FHF        CD ','  C                   C  ','           LLL           ','            O            '],
        ['                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','                         ','   C        C        C   ',' DDC       FHF       CDD ','   C                 C   ','           LLL           ','                         '],
        ['                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','                         ','   C        C        C   ','  DC       FHF       CD  ','   C                 C   ','            L            ','                         '],
        ['                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','                         ','    C       C       C    ','  DDC      FHF      CDD  ','    C               C    ','            L            ','                         '],
        ['                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','            C            ','     CC     C     CC     ','   DDCC    FHF    CCDD   ','     CC     C     CC     ','                         ','                         '],
        ['                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','            C            ','       CC   C   CC       ','    DDDCC  FHF  CCDDD    ','       CC   C   CC       ','            C            ','                         '],
        ['                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','            C            ','         CCCCCCC         ','      DDDCCCCCCCDDD      ','         CCCCCCC         ','            C            ','                         '],
        ['                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','                         ','                         ','        DDDDDDDDD        ','                         ','                         ','                         ']
    ]
// Array Mk2: a tower. A tall white spire of panels (the hatches in its crown, the controller on top, facing up) with
// three rings round it, a big one in the middle and a smaller one above and below, joined to it by beams, and a plasma
// lance (drawn, not built) leaving the open foot; the rings' light rings glow just inside them and turn like the wheels of a station
// (af9-core RING_MK2 / OrbitalLithographyMachine.ringSpecs, which read the same numbers: keep them in step). The
// blocks are the basic station's: sturdy and stress-proof casing for the rings and the shell, non-conducting panels, the
// atomic beams in tungsten carbide frame rails, HSS-G coil light strips, HSS-E frames for the core. Rows bottom -> top as
// ORBITAL_BASIC, the controller on the top row. d below counts levels down from the controller.
const ORBITAL_MK2 = 73          // width of the plane, the tower at its centre (36)
const ORBITAL_MID = 36
const ORBITAL_DEPTH = 130       // levels: the crown at the top, the open foot at the bottom (the last 25 are empty)
const ORBITAL_HALF = 9          // half width of the ring collars, the beams start outside it
const ORBITAL_RINGS = [         // d of the ring's top level, levels thick, outer radius, band width (every ring one band of casing)
    { d: 22, t: 2, r: 26, w: 4 },
    { d: 40, t: 3, r: 36, w: 5 },
    { d: 58, t: 2, r: 18, w: 3 }
]
// the ring's middle level: the collar plate of the tower and the beams out to the ring sit here
const orbitalMid = ring => ring.d + Math.floor(ring.t / 2)
const orbitalCollar = d => ORBITAL_RINGS.some(ring => Math.abs(d - orbitalMid(ring)) <= 1)
// the deck round each ring: the levels above and below its collar plate reach out wide along the slab, over the beams
// (which run at the middle level), as far as the ring's band allows. 17 is the most any tower cell may stand from the axis:
// af9-core hides and redraws a 35 x 35 box (TOWER_BOX)
const orbitalDeck = (d, dx, dz) => ORBITAL_RINGS.some(ring => Math.abs(d - orbitalMid(ring)) === 1
    && Math.abs(dx) <= Math.min(17, ring.r - ring.w - 2) && Math.abs(dz) <= 9 && Math.abs(dx) + Math.abs(dz) * 2 <= 25)
// the set-backs between the rings: a two-level ledge, wider than the body, where the slab steps in
const orbitalLedge = d => d === 36 || d === 37 || d === 54 || d === 55 || d === 72 || d === 73
// the tower's half widths (x, z) d levels down: a crown on a slab-sided upper block, a slimmer waist, the long taper
const orbitalSection = d => {
    if (d <= 20) return [8, 5]
    if (d <= 39) return [9, 5]
    if (d <= 57) return [8, 4]
    if (d <= 80) return [6, 3]
    if (d <= 104) {
        const step = Math.floor((d - 81) / 6)
        return [5 - Math.floor(step / 2), Math.max(1, 3 - Math.floor(step / 3))]
    }
    return [0, 0]
}
// the crown's roof, the same left and right of the controller: the controller's saddle (5 wide) at the top, a dip to
// either side of it, and a tall blade at each edge that falls away towards the front and the back
const orbitalRoof = (dx, dz) => {
    const ax = Math.abs(dx)
    if (ax <= 2) return 0
    if (ax <= 4) return 7
    return Math.max(0, Math.abs(dz) - 3) * 2
}
// what stands out of the tower's walls, '' for nothing: 'F' a fin (a thin plate against the body: one against each
// side of the crown, two pylons down the waist), 'R' a rib (the layered vertical
// panels of the body). None of them reach a ring's middle level (the beams run there).
const orbitalFin = (d, dx, dz) => {
    const sec = orbitalSection(d)
    const ax = Math.abs(dx)
    const az = Math.abs(dz)
    // a plate against each side of the crown, under the blades
    if (d >= 3 && d <= 21 && ax === sec[0] + 1 && az <= 3) return 'F'
    if (d >= 62 && d <= 78 && ax === sec[0] + 1 && az <= 1) return 'F'
    // ribs up the faces of the upper block and of the waist
    if (d >= 21 && d <= 80 && d !== 22 && sec[0] > 4) {
        if (az === sec[1] + 1 && ax <= sec[0] - 2 && dx % 4 === 0) return 'R'
        if (ax === sec[0] + 1 && az <= sec[1] - 2 && dz % 4 === 0) return 'R'
    }
    return ''
}
// is this cell part of the tower's mass (its walls, hollow inside, are what is built)
const orbitalSolid = (d, dx, dz) => {
    if (d < 0) return false
    const ax = Math.abs(dx)
    const az = Math.abs(dz)
    if (orbitalCollar(d) && ax <= ORBITAL_HALF && az <= ORBITAL_HALF && ax + az <= 15) return true
    if (orbitalDeck(d, dx, dz)) return true
    const sec = orbitalSection(d)
    if (sec[0] === 0) return false
    if (orbitalLedge(d) && ax <= sec[0] + 3 && az <= sec[1] + 3 && ax + az <= sec[0] + sec[1] + 1) return true
    if (ax <= sec[0] && az <= sec[1] && ax + az <= sec[0] + sec[1] - 2) return d <= 20 ? d >= orbitalRoof(dx, dz) : true
    return orbitalFin(d, dx, dz) !== ''
}
const orbitalTowerCell = (d, dx, dz) => {
    if (d === 0 && dx === 0 && dz === 0) return 'K'
    if (d > 104) return ' '   // the tower ends at its foot; the plasma lance below it is drawn, not built
    if (Math.abs(dx) > 17 || Math.abs(dz) > 17) return ' '
    // the thruster shaft: the foot's chamber is open at the bottom through a 3 x 3 hole, the lance leaves it
    if (d >= 99 && Math.abs(dx) <= 1 && Math.abs(dz) <= 1) return ' '
    // the centre column: PTFE through the crown (the hatches go on it), HSS-E below it
    if (dx === 0 && dz === 0) return d < 12 ? 'O' : 'A'
    if (!orbitalSolid(d, dx, dz)) return ' '
    const east = orbitalSolid(d, dx + 1, dz)
    const west = orbitalSolid(d, dx - 1, dz)
    const north = orbitalSolid(d, dx, dz + 1)
    const south = orbitalSolid(d, dx, dz - 1)
    const up = orbitalSolid(d - 1, dx, dz)
    const down = orbitalSolid(d + 1, dx, dz)
    if (east && west && north && south && up && down) {
        // inside the walls: a floor every 12 levels
        return d <= 80 && d % 12 === 0 ? 'C' : ' '
    }
    // the crown and the roof: plain PTFE casing all round, the hatches go on it
    if (d <= 10 || (d <= 20 && !up)) return 'O'
    const part = orbitalFin(d, dx, dz)
    if (part === 'R') return 'C'
    if (part === 'F') return !up || !down ? 'F' : 'L'
    // corners and the rims of plates: tungsten carbide frames
    if ((east ? 1 : 0) + (west ? 1 : 0) + (north ? 1 : 0) + (south ? 1 : 0) <= 2) return 'F'
    // walls: stress-proof casing columns with non-conducting panels between, a light strip (HSS-G coil) up each third
    // column, broken every 18 levels
    const along = Math.abs(!north || !south ? dx : dz)
    if (along % 6 === 3 && d % 18 > 2) return 'B'
    // rows of small lit windows across the panels, every 8 levels
    if (d % 8 === 4 && along % 2 === 0 && along % 6 !== 0) return 'B'
    return along % 6 < 3 ? 'L' : 'C'
}
const orbitalRingCell = (d, dx, dz) => {
    let found = ' '
    ORBITAL_RINGS.forEach(ring => {
        if (found !== ' ' || d < ring.d || d >= ring.d + ring.t) return
        const r = Math.sqrt(dx * dx + dz * dz)
        const mid = ring.d + Math.floor(ring.t / 2)
        // the band: sturdy casing, a stress-proof top and a non-conducting outer face
        if (r >= ring.r - ring.w && r <= ring.r + 0.3) {
            found = d === ring.d ? 'L' : r > ring.r - 1 ? 'C' : 'D'
            return
        }
        // the beams out to it: the atomic beam between tungsten carbide frame rails, one level, along the axes
        if (d === mid && Math.max(Math.abs(dx), Math.abs(dz)) > ORBITAL_HALF && r < ring.r - ring.w) {
            if (dx === 0 || dz === 0) found = 'H'
            else if (Math.abs(dx) === 1 || Math.abs(dz) === 1) found = 'F'
        }
    })
    return found
}
const orbitalMk2Cell = (d, dx, dz) => {
    const tower = orbitalTowerCell(d, dx, dz)
    return tower !== ' ' ? tower : orbitalRingCell(d, dx, dz)
}
// forEach, not for: KubeJS's engine (Rhino) keeps a const in a loop body at its first pass's value, so loop
// bodies use callbacks (a const in a callback is new each call; lint rule S3). Array(n).fill(0), not
// Array.from({ length: n }): Rhino's Array.from leaves n holes, and forEach skips holes.
// Precomputed once (73 x 130 x 73 Mk2 cell table); .pattern() reuses this const.
const ORBITAL_MK2_SLICES = (() => {
    const slices = []
    Array(ORBITAL_MK2).fill(0).forEach((_, az) => {
        const aisle = []
        Array(ORBITAL_DEPTH).fill(0).forEach((_, row) => {
            const d = ORBITAL_DEPTH - 1 - row
            const cells = []
            Array(ORBITAL_MK2).fill(0).forEach((_, ax) => {
                cells.push(orbitalMk2Cell(d, ax - ORBITAL_MID, az - ORBITAL_MID))
            })
            aisle.push(cells.join(''))
        })
        slices.push(aisle)
    })
    return slices
})()

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }

    event.create('photolithography_line', 'multiblock')
        .machine(holder => new $PhotolithographyLineMachine(holder))
        .rotationState(RotationState.NON_Y_AXIS)
        // machine modes, in order; switched with GT's mode tab, the console's tiles show the active one
        .recipeTypes(['350nm', '200nm', '100nm'].map(node => GTRecipeTypes.get(`lithography_${node}`)))
        // LITHO_GATE: only prints a mode the line's version allows, when the two energy hatches can supply its EU/t
        // STRIP_BROKEN: the chanced broken wafer is only for the recipe viewers, the break roll decides
        // LITHO_VERSION: faster per version above the mode; perfect overclocks above that; batch mode (GT's: once
        // overclocked below 5 s, several prints in one run, each rolling its own break). No parallel hatch.
        .recipeModifiers([$LithoMachine.LITHO_GATE, $LithoMachine.STRIP_BROKEN,
            $PhotolithographyLineMachine.LITHO_VERSION, GTRecipeModifiers.OC_PERFECT, GTRecipeModifiers.BATCH_MODE])
        .appearanceBlock(() => Block.getBlock('gtceu:plascrete'))
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.photolithography_line.tooltip', 17))
        // 3 wide x 3 high x 10-12 long, built from plascrete like a clean room. Aisles run from the front (controller)
        // to the back (light source): the controller has to come before the repeatable lens aisle, or GT's auto-build
        // (terminal) places the structure off the controller. Each aisle lists its rows bottom -> middle -> top.
        // Versions 1-3 like the Assembly Line's length: 3-5 projection-lens slices, and the light source must allow the
        // version (mercury lamp V1, KrF excimer laser V2, ArF excimer laser V3). One preview page each.
        .pattern(definition => FactoryBlockPattern.start($LithoRelativeDirection.LEFT, $LithoRelativeDirection.UP,
            $LithoRelativeDirection.BACK)
            // --- Coater / developer track ---
            .aisle('III', 'IMI', 'CFC') // cassette station (wafers in and out) + controller
            .aisle('CSC', 'WXW', 'FPF') // HMDS prime and spin coater
            .aisle('CKC', 'CHC', 'FFF') // bake plates (soft bake, post-exposure bake, hard bake) and chill plate
            .aisle('CSC', 'WXW', 'FPF') // developer: TMAH puddle and rinse
            // --- Stepper (exposure tool) ---
            .aisle('CRC', 'WRW', 'CCC') // wafer XY stage
            .aisle('CCC', 'WTW', 'CCC').setRepeatable(3, 5) // projection lens: one slice per version + 2
            .aisle('CCC', 'CRC', 'CCC') // reticle stage
            .aisle('CCC', 'CLC', 'CCC') // light source / illuminator
            .where('M', Predicates.controller(Predicates.blocks(definition.get())))
            // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count))
            .where('I', Predicates.blocks('gtceu:plascrete')
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2, 1)))
            .where('C', Predicates.blocks('gtceu:plascrete')
                // up to two normal 2A hatches = 4A, what every print needs; their voltage decides the modes
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 2))
                // a computation hatch: the optical proximity correction's computation (an extra)
                .or(Predicates.abilities(PartAbility.COMPUTATION_DATA_RECEPTION).setMaxGlobalLimited(1, 0))
                // air cooling: the hatches carry the print's heat (cooling units, see air_conditioning.js)
                .or(Predicates.abilities($LithoAirConditioning.AIR_CONDITIONING).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(8, 1))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
            .where('F', Predicates.blocks('af9:plascrete_filter_casing'))     // fan filter units
            .where('R', Predicates.blocks('gtceu:stainless_steel_gearbox'))      // robots and stages
            .where('S', Predicates.blocks('gtceu:steel_gearbox'))                // spin motors
            .where('X', Predicates.blocks('gtceu:solid_machine_casing'))         // process cups (MV: no PTFE yet)
            .where('P', Predicates.blocks('af9:plascrete_pipe_casing'))       // chemical dispense lines
            .where('H', Predicates.blocks('gtceu:cupronickel_coil_block'))       // hot plates
            .where('K', Predicates.blocks('gtceu:frostproof_machine_casing'))    // aluminium chill plates
            .where('T', Predicates.blocks('gtceu:tempered_glass'))               // lens elements
            .where('W', Predicates.blocks('gtceu:cleanroom_glass'))
            .where('L', Predicates.blocks('gtceu:purple_lamp')                   // mercury i-line lamp (V1)
                .or(Predicates.blocks('af9:krf_excimer_laser'))                 // V2
                .or(Predicates.blocks('af9:arf_excimer_laser')))                // V3
            .build())
        .shapeInfos(definition => $PhotolithographyLineMachine.versionShapes(definition))
        .workableCasingModel('gtceu:block/casings/cleanroom/plascrete',
            'gtceu:block/multiblock/gcym/large_engraving_laser')
        // exposure chamber inside the stepper (af9-core LithoChamberRender): UV fill, wafer, scanning laser
        // and wafer robot on its slide, following the print's progress; centre of the lens (6 behind controller)
        .model($LithoMachineModels.workableCasingWithChamber('gtceu:block/casings/cleanroom/plascrete',
            'gtceu:block/multiblock/gcym/large_engraving_laser', 0, 6))
        .hasBER(true)

    // Photolithography Scanner (Mk2): a step-and-scan tool for 80 and 65 nm, a cleanroom tube 3 x 3 of plascrete, 10
    // long at version 1 and 12 at version 2. Aisles front (controller) -> back, rows bottom -> top: the front with the
    // controller, a cap (a Plascrete Filter Casing over the tube), a window section (cleanroom glass on both sides,
    // stainless steel gearboxes over and under the tube: the wafer stages), the track (Plascrete Pipe Casings under the
    // tube, filter casings over it), the back window run (2 aisles a version: the lens slices), a cap and the back. The
    // tube stays empty. Its ArF excimer laser sits in a slot of its screen, not in the structure. Versions 1-2: 2-3
    // window sections of 4 cleanroom glass. Hatches go on any plascrete. AF9 Core: PhotolithographyScannerMachine.
    event.create('photolithography_scanner', 'multiblock')
        .machine(holder => new $PhotolithographyScannerMachine(holder))
        .langValue('Photolithography Scanner Mk2')
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes(['80nm', '65nm'].map(node => GTRecipeTypes.get(`lithography_${node}`)))
        // the line's modifiers, batch mode included
        .recipeModifiers([$LithoMachine.LITHO_GATE, $LithoMachine.STRIP_BROKEN,
            $PhotolithographyLineMachine.LITHO_VERSION, GTRecipeModifiers.OC_PERFECT, GTRecipeModifiers.BATCH_MODE])
        .appearanceBlock(() => Block.getBlock('gtceu:plascrete'))
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.photolithography_scanner.tooltip', 10))
        .pattern(definition => FactoryBlockPattern.start($LithoRelativeDirection.LEFT, $LithoRelativeDirection.UP,
            $LithoRelativeDirection.BACK)
            .aisle('CCC', 'CMC', 'CCC') // front with the controller
            .aisle('CCC', 'C#C', 'CFC') // cap
            .aisle('CGC', 'W#W', 'CGC') // front window section: the wafer stages
            .aisle('CGC', 'W#W', 'CGC')
            .aisle('CPC', 'C#C', 'CFC') // track: resist and developer lines under the tube
            .aisle('CPC', 'C#C', 'CFC')
            .aisle('CGC', 'W#W', 'CGC').setRepeatable(2, 4) // back window run: 2 aisles a version (the lens slices)
            .aisle('CCC', 'C#C', 'CFC') // cap
            .aisle('CCC', 'CCC', 'CCC') // back
            .where('M', Predicates.controller(Predicates.blocks(definition.get())))
            // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count)); any
            // plascrete of the shell may be one
            .where('C', Predicates.blocks('gtceu:plascrete')
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 2))
                // a computation hatch: the optical proximity correction's computation (an extra)
                .or(Predicates.abilities(PartAbility.COMPUTATION_DATA_RECEPTION).setMaxGlobalLimited(1, 0))
                // air cooling: the hatches carry the print's heat (cooling units, see air_conditioning.js)
                .or(Predicates.abilities($LithoAirConditioning.AIR_CONDITIONING).setMaxGlobalLimited(2, 1))
                // the fibre to its Logistics Hub (af9-core fiber): the hub's buses and hatches feed the scanner through it
                .or(Predicates.abilities(Java.loadClass('com.af9.core.fiber.FiberLinkPortPartMachine').FIBER_LINK).setMaxGlobalLimited(2, 0))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(8, 1))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
            .where('F', Predicates.blocks('af9:plascrete_filter_casing'))     // fan filter units
            .where('G', Predicates.blocks('gtceu:stainless_steel_gearbox'))      // wafer stages
            .where('P', Predicates.blocks('af9:plascrete_pipe_casing'))       // track lines
            .where('W', Predicates.blocks('gtceu:cleanroom_glass'))              // windows: the lens slices
            .where('#', Predicates.air())                                        // the tube
            .build())
        .shapeInfos(definition => $PhotolithographyLineMachine.versionShapes(definition,
            $PhotolithographyLineMachine.MK2))
        .workableCasingModel('gtceu:block/casings/cleanroom/plascrete',
            'gtceu:block/multiblock/gcym/large_engraving_laser')
        // exposure chamber inside the tube (af9-core LithoChamberRender): UV fill, wafer, scanning laser and
        // wafer robot on its slide, following the print's progress; front window section (2.5 behind controller)
        .model($LithoMachineModels.workableCasingWithChamber('gtceu:block/casings/cleanroom/plascrete',
            'gtceu:block/multiblock/gcym/large_engraving_laser', 0, 2.5))
        .hasBER(true)

    // Orbital Lithography Station: a 25 x 25 platform, 18 high. The top deck of inert PTFE casing carries the
    // controller and the hatches; under it lie the stress-proof deck plate, the shock-proof exposure deck (cross
    // beams) in a ring of sturdy casing, non-conducting spokes and rims, HSS-S trusses, and the X-ray undulator mast:
    // an HSS-G coil column in HSS-E frames reaching 12 blocks down. Prints 50, 20, 7 and 1 nm, only in orbit
    // (af9-core OrbitalLithographyMachine). Pattern from the sol_array design, unchanged; rows bottom -> top.
    // While it prints, a light ring like GT's fusion ring glows just inside the rim, at the level of the exposure deck
    // (3 below the controller, radius 9.6: clear of the rim, it only crosses the four beams), in the node's colour.
    // The controller faces up out of the top deck (look down when you place it); the station turns with it in any
    // direction. The pattern is laid out for that: rows along the controller's front (up), aisles along its up; with
    // the controller facing up the blocks sit exactly where the old horizontal controller had them. Coolant: every
    // print draws a supercooled fluid through the coolant hatches (af9-core OrbitalLithographyMachine.COOLANT);
    // 7 and 1 nm draw computation (computation hatch) and need their research (data hatch). While switched on and
    // powered its magnetic field gives the space around it normal gravity (Ad Astra; af9-core OrbitalField).
    // Array Mk2 (extended, a screwdriver on the controller switches sizes like the Space Elevator's screen
    // switch): a 73 x 130 x 73 tower with three spinning rings and a thruster lance below (above, af9-core RINGS_MK2). Only the Mk2 runs plasma atomic soldering (gtceu:plasma_soldering):
    // UHV+ circuits deposited ion-by-ion with plasma solder, in orbit.
    // Layouts are module-level (ORBITAL_BASIC / ORBITAL_MK2_SLICES), precomputed once above.
    const orbitalWheres = (definition, mk2) => {
        const energy = Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(3, 1)
        const laser = Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1, 1)
        const coolant = Predicates.abilities($LithoCoolantHatch.COOLANT_INPUT).setMaxGlobalLimited(2, 1)
        const computation = Predicates.abilities(PartAbility.COMPUTATION_DATA_RECEPTION).setMaxGlobalLimited(1, 1)
        const data = Predicates.abilities(PartAbility.DATA_ACCESS).setMaxGlobalLimited(1, 1)
        const optical = Predicates.abilities(PartAbility.OPTICAL_DATA_RECEPTION).setMaxGlobalLimited(1, 0)
        const fluids = Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(8, 1)
        const itemsIn = Predicates.abilities(PartAbility.IMPORT_ITEMS).setPreviewCount(1)
        const itemsOut = Predicates.abilities(PartAbility.EXPORT_ITEMS).setPreviewCount(1)
        const maintenance = Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)
        // the top deck: PTFE casings and every hatch; the Array Mk2 (and only it) also takes a parallel hatch
        const deck = Predicates.blocks('gtceu:inert_machine_casing').or(energy).or(laser).or(coolant).or(computation)
            .or(data).or(optical).or(fluids).or(itemsIn).or(itemsOut).or(maintenance)
        const parallelHatch = Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1, 0)
        return [
            ['K', Predicates.controller(Predicates.blocks(definition.get()))],
            ['A', Predicates.blocks('gtceu:hsse_frame')],
            // the Mk2's light strips are heating coils of any one tier (af9-core OrbitalLithographyMachine.COILS: the
            // tier is the bonus); the basic station's stay HSS-G
            ['B', mk2 ? Predicates.heatingCoils() : Predicates.blocks('gtceu:hssg_coil_block')],
            ['D', Predicates.blocks('gtceu:sturdy_machine_casing')],
            ['F', Predicates.blocks(mk2 ? 'gtceu:tungsten_carbide_frame' : 'gtceu:hsss_frame')],
            ['H', Predicates.blocks(mk2 ? 'gtceu:atomic_casing' : 'gtceu:shock_proof_cutting_casing')],
            ['L', Predicates.blocks('gtceu:stress_proof_casing')],
            ['C', Predicates.blocks('gtceu:nonconducting_casing')],
            // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview)): the track
            // chemicals through fluid input hatches, the supercooled coolant through coolant hatches, computation
            // (7 and 1 nm) through a computation hatch and the 1 nm research through a data hatch. Every hatch goes
            // on the PTFE casings of the top deck and nowhere else.
            ['O', mk2 ? deck.or(parallelHatch) : deck],
            [' ', Predicates.any()]
        ]
    }
    const orbitalPattern = (definition, slices, mk2) => {
        let pattern = FactoryBlockPattern.start($LithoRelativeDirection.RIGHT, $LithoRelativeDirection.FRONT,
            $LithoRelativeDirection.UP)
        for (let c = 0; c < slices.length; c++) pattern = pattern.aisle(slices[c])
        orbitalWheres(definition, mk2).forEach(([char, predicate]) => { pattern = pattern.where(char, predicate) })
        return pattern.build()
    }

    event.create('orbital_lithography_station', 'multiblock')
        .machine(holder => new $OrbitalLithographyMachine(holder))
        .rotationState(RotationState.ALL)
        .allowExtendedFacing(true)
        .recipeTypes(['lithography_50nm', 'lithography_20nm', 'lithography_7nm', 'orbital_lithography']
            .map(id => GTRecipeTypes.get(id)))
        // LITHO_GATE: only in orbit, with the recipe's full EU/t, a sealed vacuum and (researched prints) a data hatch;
        // STRIP_BROKEN: the break roll decides; COOLANT: adds the coolant (faster with a better one);
        // MK2_GATE: the Mk2's own types only in orbit on the Array Mk2 (extended) once its beams are
        // aligned (the 1 nm prints check the same in the station's own gate); FOCUS: the Mk2's focus bonus (shorter
        // runs; its break-chance share is in the station's machineBreakFactor); COILS: the Mk2's coil tier (shorter runs, less EU/t; break chance and focus in the station);
        // PARALLEL_HATCH: GT's parallel hatch, which only the Array Mk2's structure has a place for (the coolant and
        // everything else scale with the parallels); perfect overclocks (the machine caps them at UXV, af9-core
        // getOverclockTier); then batch mode (the console's switch; af9-core BATCH: a print overclocked below a minute
        // runs as a batch about a minute long, up to 64 prints, the coolant too).
        .recipeModifiers([$LithoMachine.LITHO_GATE, $LithoMachine.STRIP_BROKEN, $OrbitalLithographyMachine.COOLANT,
            $OrbitalLithographyMachine.MK2_GATE, $OrbitalLithographyMachine.FOCUS, $OrbitalLithographyMachine.COILS,
            GTRecipeModifiers.PARALLEL_HATCH, GTRecipeModifiers.OC_PERFECT, $OrbitalLithographyMachine.BATCH])
        .appearanceBlock(() => Block.getBlock('gtceu:inert_machine_casing'))
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.orbital_lithography_station.tooltip', 20))
        .pattern(definition => {
            // GT asks for this once: both sizes are built, the extended one is the machine's own to switch to
            $OrbitalLithographyMachine.setExtendedPattern(orbitalPattern(definition, ORBITAL_MK2_SLICES, true))
            return orbitalPattern(definition, ORBITAL_BASIC)
        })
        .workableCasingModel('gtceu:block/casings/solid/machine_casing_inert_ptfe',
            'gtceu:block/multiblock/fusion_reactor')
        // the same model plus the light ring: centre 3 behind the controller (below, as it faces up), radius 9.6
        // (the Array Mk2's three rings: af9-core reads them through ringSpecs), tube 0.25, lying across the
        // controller's front axis. The numbers live in af9-core (RING_*), which also burns whatever touches the lit
        // ring. Plasma soldering glows hotter (ringGlow 2, af9-core).
        .model($LithoMachineModels.workableCasingWithLightRing('gtceu:block/casings/solid/machine_casing_inert_ptfe',
            'gtceu:block/multiblock/fusion_reactor', $OrbitalLithographyMachine.RING_UP,
            $OrbitalLithographyMachine.RING_BACK, $OrbitalLithographyMachine.RING_RADIUS,
            $OrbitalLithographyMachine.RING_THICKNESS, 'front'))
        .hasBER(true)
        // GT would draw the preview for a controller facing north (the platform on its edge): show it facing up;
        // the basic station's page (the Array Mk2 is too big for a preview)
        .shapeInfos(definition => $OrbitalLithographyMachine.previews(definition))
})
