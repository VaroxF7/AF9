// AF9 - the Dimensionally Transcendent Plasma Forge (DTPF), after GTNH's: a long hall that forges plasma into matter.
// Machine logic: af9-core PlasmaForgeMachine (the running-time ramp: the longer it runs without a pause, the cheaper and
// faster it forges). Recipes: server_scripts/mods/gtceu/dtpf.js. Spec: docs/dtpf.md
//
// The structure is Janne's own build, 43 long, 13 high, 11 wide, written out below block by block (hand-written aisles, no
// maths), taken 1:1 from the structure he exported from his world. From the front (the controller's end) to the back:
//   * two gear plates, one at each end: a 7 x 7 plate of transcendent casing (the controller in the middle of the front's),
//     set one block in front of a ring of transcendent casing, 11 wide, that is open in the middle where the plate sits;
//     in the ring's middle a square of bridge casing round the end of the beam;
//   * the beam, a line of superconducting coil down the axis, 41 blocks;
//   * three big rings of transcendent casing round the beam (an injection casing above and below, a bridge casing either
//     side of the beam), five apart, and between them four small rings with an injection casing either side of the beam,
//     each with a pair of injection casings a few blocks to either side of it along the beam.
// Every injection casing is a spot for a hatch (the maximums are below); the force field is drawn in the empty middle
// (the model, af9-core ForceFieldRender).

const $PlasmaForgeMachine = Java.loadClass('com.af9.core.machine.PlasmaForgeMachine')
const $ForgePlasmaHatch = Java.loadClass('com.af9.core.machine.part.PlasmaHatchPartMachine')
const $DtpfModels = Java.loadClass('com.af9.core.machine.AF9MachineModels')

// The force field in the hall's middle (af9-core ForceFieldRender): 0 hoops, 1 plasma sheath, 2 helix, 3 lattice. A tube
// round the beam between the two end rings; its size is PlasmaForgeMachine's (FIELD_RADIUS 2.6: the rings' blocks come as
// near as 2.9 blocks to the beam, so it clips nothing; the same tube is what kills whatever touches it).
const DTPF_FIELD_STYLE = 1

// Where the injection casings stand, in the field's frame (x to the controller's left, y up, z blocks behind the
// controller's centre; the controller is at x 5, y 6 of the last plate): the renderer feeds the field with a beam from each.
const dtpfInjectors = () => {
    const spots = []
    DTPF_AISLES.forEach((aisle, z) => aisle.forEach((row, y) => row.split('').forEach((ch, x) => {
        if (ch === 'W') {
            spots.push(`${(5.5 - (x + 0.5)).toFixed(2)},${(y + 0.5 - 6.5).toFixed(2)},${(DTPF_AISLES.length - 1 - z).toFixed(2)}`)
        }
    })))
    return spots.join(';')
}
const dtpfModel = () => $DtpfModels.workableCasingWithForceField('af9:block/dtpf_injection_casing',
    'gtceu:block/multiblock/dtpf', DTPF_FIELD_STYLE, $PlasmaForgeMachine.FIELD_RADIUS, $PlasmaForgeMachine.FIELD_START,
    $PlasmaForgeMachine.FIELD_END, dtpfInjectors())

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    // plasma and matter in, forged matter and fluids out (three of each: the recipe page lays out rows of three)
    event.create('plasma_forge')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(3, 3, 3, 3)
        .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.ARC)
})

// The DTPF, layer by layer: DTPF_AISLES[z] is the z-th plate, back (0) to front (42, the controller's); a plate's rows are
// y, bottom (0) to top (12); a row's characters are x, 0 to 10, the beam at x 5, y 6.
//   S controller, N transcendent casing, W injection casing (hatches), B bridge casing (glows),
//   C beam (superconducting coil); a space is nothing (anything may be there).
//   M module casing (Inconel 738) or nothing, P a module controller or nothing: the six module slots, a plate of 12 M round a P
//   at the top and bottom of each big ring (x 5, y 11 and y 1), behind the middle injection casing.
const DTPF_AISLES = [
    // z 0: the back plate (as the front plate, casing where the controller is)
    [
        '           ',  // y 0
        '           ',  // y 1
        '           ',  // y 2
        '    N N    ',  // y 3
        '   NNNNN   ',  // y 4
        '  NNNNNNN  ',  // y 5
        '   NNNNN   ',  // y 6
        '  NNNNNNN  ',  // y 7
        '   NNNNN   ',  // y 8
        '    N N    ',  // y 9
        '           ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 1: the back ring: a ring of bridge casing round the beam end
    [
        '           ',  // y 0
        '   NN NN   ',  // y 1
        '  NNNNNNN  ',  // y 2
        ' NNN N NNN ',  // y 3
        ' NN     NN ',  // y 4
        'NN  BBB  NN',  // y 5
        'NNN BCB NNN',  // y 6
        'NN  BBB  NN',  // y 7
        ' NN     NN ',  // y 8
        ' NNN N NNN ',  // y 9
        '  NNNNNNN  ',  // y 10
        '   NN NN   ',  // y 11
        '           ',  // y 12
    ],
    // z 2: the beam
    [
        '           ',  // y 0
        '           ',  // y 1
        '           ',  // y 2
        '           ',  // y 3
        '           ',  // y 4
        '           ',  // y 5
        '     C     ',  // y 6
        '           ',  // y 7
        '           ',  // y 8
        '           ',  // y 9
        '           ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 3: the beam
    [
        '           ',  // y 0
        '           ',  // y 1
        '           ',  // y 2
        '           ',  // y 3
        '           ',  // y 4
        '           ',  // y 5
        '     C     ',  // y 6
        '           ',  // y 7
        '           ',  // y 8
        '           ',  // y 9
        '           ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 4: the beam
    [
        '           ',  // y 0
        '           ',  // y 1
        '           ',  // y 2
        '           ',  // y 3
        '           ',  // y 4
        '           ',  // y 5
        '     C     ',  // y 6
        '           ',  // y 7
        '           ',  // y 8
        '           ',  // y 9
        '           ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 5: the beam, an injection casing either side (hatch spots)
    [
        '           ',  // y 0
        '           ',  // y 1
        '           ',  // y 2
        '           ',  // y 3
        '           ',  // y 4
        '           ',  // y 5
        ' W   C   W ',  // y 6
        '           ',  // y 7
        '           ',  // y 8
        '           ',  // y 9
        '           ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 6: the small ring: transcendent casing, an injection casing either side of the beam (hatch spots)
    [
        '           ',  // y 0
        '           ',  // y 1
        '           ',  // y 2
        ' NN     NN ',  // y 3
        'NNN     NNN',  // y 4
        'NN       NN',  // y 5
        'NW   C   WN',  // y 6
        'NN       NN',  // y 7
        'NNN     NNN',  // y 8
        ' NN     NN ',  // y 9
        '           ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 7: the beam, an injection casing either side (hatch spots)
    [
        '           ',  // y 0
        '           ',  // y 1
        '           ',  // y 2
        '           ',  // y 3
        '           ',  // y 4
        '           ',  // y 5
        ' W   C   W ',  // y 6
        '           ',  // y 7
        '           ',  // y 8
        '           ',  // y 9
        '           ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 8: the beam
    [
        '           ',  // y 0
        '           ',  // y 1
        '           ',  // y 2
        '           ',  // y 3
        '           ',  // y 4
        '           ',  // y 5
        '     C     ',  // y 6
        '           ',  // y 7
        '           ',  // y 8
        '           ',  // y 9
        '           ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 9: the beam
    [
        '           ',  // y 0
        '           ',  // y 1
        '           ',  // y 2
        '           ',  // y 3
        '           ',  // y 4
        '           ',  // y 5
        '     C     ',  // y 6
        '           ',  // y 7
        '           ',  // y 8
        '           ',  // y 9
        '           ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 10: the beam, a bridge casing either side, an injection casing above and below (hatch spots)
    [
        '           ',  // y 0
        '           ',  // y 1
        '     W     ',  // y 2
        '           ',  // y 3
        '           ',  // y 4
        '           ',  // y 5
        ' B   C   B ',  // y 6
        '           ',  // y 7
        '           ',  // y 8
        '           ',  // y 9
        '     W     ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 11: the big ring: transcendent casing, a bridge casing either side of the beam, injection casings above and below (hatch spots)
    [
        '  MM   MM  ',  // y 0
        '  MMMPMMM  ',  // y 1
        '    MWM    ',  // y 2
        ' NN     NN ',  // y 3
        'NNN     NNN',  // y 4
        'NN       NN',  // y 5
        'NB   C   BN',  // y 6
        'NN       NN',  // y 7
        'NNN     NNN',  // y 8
        ' NN     NN ',  // y 9
        '    MWM    ',  // y 10
        '  MMMPMMM  ',  // y 11
        '  MM   MM  ',  // y 12
    ],
    // z 12: the beam, a bridge casing either side, an injection casing above and below (hatch spots)
    [
        '           ',  // y 0
        '           ',  // y 1
        '     W     ',  // y 2
        '           ',  // y 3
        '           ',  // y 4
        '           ',  // y 5
        ' B   C   B ',  // y 6
        '           ',  // y 7
        '           ',  // y 8
        '           ',  // y 9
        '     W     ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 13: the beam
    [
        '           ',  // y 0
        '           ',  // y 1
        '           ',  // y 2
        '           ',  // y 3
        '           ',  // y 4
        '           ',  // y 5
        '     C     ',  // y 6
        '           ',  // y 7
        '           ',  // y 8
        '           ',  // y 9
        '           ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 14: the beam
    [
        '           ',  // y 0
        '           ',  // y 1
        '           ',  // y 2
        '           ',  // y 3
        '           ',  // y 4
        '           ',  // y 5
        '     C     ',  // y 6
        '           ',  // y 7
        '           ',  // y 8
        '           ',  // y 9
        '           ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 15: the beam, an injection casing either side (hatch spots)
    [
        '           ',  // y 0
        '           ',  // y 1
        '           ',  // y 2
        '           ',  // y 3
        '           ',  // y 4
        '           ',  // y 5
        ' W   C   W ',  // y 6
        '           ',  // y 7
        '           ',  // y 8
        '           ',  // y 9
        '           ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 16: the small ring: transcendent casing, an injection casing either side of the beam (hatch spots)
    [
        '           ',  // y 0
        '           ',  // y 1
        '           ',  // y 2
        ' NN     NN ',  // y 3
        'NNN     NNN',  // y 4
        'NN       NN',  // y 5
        'NW   C   WN',  // y 6
        'NN       NN',  // y 7
        'NNN     NNN',  // y 8
        ' NN     NN ',  // y 9
        '           ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 17: the beam, an injection casing either side (hatch spots)
    [
        '           ',  // y 0
        '           ',  // y 1
        '           ',  // y 2
        '           ',  // y 3
        '           ',  // y 4
        '           ',  // y 5
        ' W   C   W ',  // y 6
        '           ',  // y 7
        '           ',  // y 8
        '           ',  // y 9
        '           ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 18: the beam
    [
        '           ',  // y 0
        '           ',  // y 1
        '           ',  // y 2
        '           ',  // y 3
        '           ',  // y 4
        '           ',  // y 5
        '     C     ',  // y 6
        '           ',  // y 7
        '           ',  // y 8
        '           ',  // y 9
        '           ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 19: the beam
    [
        '           ',  // y 0
        '           ',  // y 1
        '           ',  // y 2
        '           ',  // y 3
        '           ',  // y 4
        '           ',  // y 5
        '     C     ',  // y 6
        '           ',  // y 7
        '           ',  // y 8
        '           ',  // y 9
        '           ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 20: the beam, a bridge casing either side, an injection casing above and below (hatch spots)
    [
        '           ',  // y 0
        '           ',  // y 1
        '     W     ',  // y 2
        '           ',  // y 3
        '           ',  // y 4
        '           ',  // y 5
        ' B   C   B ',  // y 6
        '           ',  // y 7
        '           ',  // y 8
        '           ',  // y 9
        '     W     ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 21: the big ring: transcendent casing, a bridge casing either side of the beam, injection casings above and below (hatch spots)
    [
        '  MM   MM  ',  // y 0
        '  MMMPMMM  ',  // y 1
        '    MWM    ',  // y 2
        ' NN     NN ',  // y 3
        'NNN     NNN',  // y 4
        'NN       NN',  // y 5
        'NB   C   BN',  // y 6
        'NN       NN',  // y 7
        'NNN     NNN',  // y 8
        ' NN     NN ',  // y 9
        '    MWM    ',  // y 10
        '  MMMPMMM  ',  // y 11
        '  MM   MM  ',  // y 12
    ],
    // z 22: the beam, a bridge casing either side, an injection casing above and below (hatch spots)
    [
        '           ',  // y 0
        '           ',  // y 1
        '     W     ',  // y 2
        '           ',  // y 3
        '           ',  // y 4
        '           ',  // y 5
        ' B   C   B ',  // y 6
        '           ',  // y 7
        '           ',  // y 8
        '           ',  // y 9
        '     W     ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 23: the beam
    [
        '           ',  // y 0
        '           ',  // y 1
        '           ',  // y 2
        '           ',  // y 3
        '           ',  // y 4
        '           ',  // y 5
        '     C     ',  // y 6
        '           ',  // y 7
        '           ',  // y 8
        '           ',  // y 9
        '           ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 24: the beam
    [
        '           ',  // y 0
        '           ',  // y 1
        '           ',  // y 2
        '           ',  // y 3
        '           ',  // y 4
        '           ',  // y 5
        '     C     ',  // y 6
        '           ',  // y 7
        '           ',  // y 8
        '           ',  // y 9
        '           ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 25: the beam, an injection casing either side (hatch spots)
    [
        '           ',  // y 0
        '           ',  // y 1
        '           ',  // y 2
        '           ',  // y 3
        '           ',  // y 4
        '           ',  // y 5
        ' W   C   W ',  // y 6
        '           ',  // y 7
        '           ',  // y 8
        '           ',  // y 9
        '           ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 26: the small ring: transcendent casing, an injection casing either side of the beam (hatch spots)
    [
        '           ',  // y 0
        '           ',  // y 1
        '           ',  // y 2
        ' NN     NN ',  // y 3
        'NNN     NNN',  // y 4
        'NN       NN',  // y 5
        'NW   C   WN',  // y 6
        'NN       NN',  // y 7
        'NNN     NNN',  // y 8
        ' NN     NN ',  // y 9
        '           ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 27: the beam, an injection casing either side (hatch spots)
    [
        '           ',  // y 0
        '           ',  // y 1
        '           ',  // y 2
        '           ',  // y 3
        '           ',  // y 4
        '           ',  // y 5
        ' W   C   W ',  // y 6
        '           ',  // y 7
        '           ',  // y 8
        '           ',  // y 9
        '           ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 28: the beam
    [
        '           ',  // y 0
        '           ',  // y 1
        '           ',  // y 2
        '           ',  // y 3
        '           ',  // y 4
        '           ',  // y 5
        '     C     ',  // y 6
        '           ',  // y 7
        '           ',  // y 8
        '           ',  // y 9
        '           ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 29: the beam
    [
        '           ',  // y 0
        '           ',  // y 1
        '           ',  // y 2
        '           ',  // y 3
        '           ',  // y 4
        '           ',  // y 5
        '     C     ',  // y 6
        '           ',  // y 7
        '           ',  // y 8
        '           ',  // y 9
        '           ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 30: the beam, a bridge casing either side, an injection casing above and below (hatch spots)
    [
        '           ',  // y 0
        '           ',  // y 1
        '     W     ',  // y 2
        '           ',  // y 3
        '           ',  // y 4
        '           ',  // y 5
        ' B   C   B ',  // y 6
        '           ',  // y 7
        '           ',  // y 8
        '           ',  // y 9
        '     W     ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 31: the big ring: transcendent casing, a bridge casing either side of the beam, injection casings above and below (hatch spots)
    [
        '  MM   MM  ',  // y 0
        '  MMMPMMM  ',  // y 1
        '    MWM    ',  // y 2
        ' NN     NN ',  // y 3
        'NNN     NNN',  // y 4
        'NN       NN',  // y 5
        'NB   C   BN',  // y 6
        'NN       NN',  // y 7
        'NNN     NNN',  // y 8
        ' NN     NN ',  // y 9
        '    MWM    ',  // y 10
        '  MMMPMMM  ',  // y 11
        '  MM   MM  ',  // y 12
    ],
    // z 32: the beam, a bridge casing either side, an injection casing above and below (hatch spots)
    [
        '           ',  // y 0
        '           ',  // y 1
        '     W     ',  // y 2
        '           ',  // y 3
        '           ',  // y 4
        '           ',  // y 5
        ' B   C   B ',  // y 6
        '           ',  // y 7
        '           ',  // y 8
        '           ',  // y 9
        '     W     ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 33: the beam
    [
        '           ',  // y 0
        '           ',  // y 1
        '           ',  // y 2
        '           ',  // y 3
        '           ',  // y 4
        '           ',  // y 5
        '     C     ',  // y 6
        '           ',  // y 7
        '           ',  // y 8
        '           ',  // y 9
        '           ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 34: the beam
    [
        '           ',  // y 0
        '           ',  // y 1
        '           ',  // y 2
        '           ',  // y 3
        '           ',  // y 4
        '           ',  // y 5
        '     C     ',  // y 6
        '           ',  // y 7
        '           ',  // y 8
        '           ',  // y 9
        '           ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 35: the beam, an injection casing either side (hatch spots)
    [
        '           ',  // y 0
        '           ',  // y 1
        '           ',  // y 2
        '           ',  // y 3
        '           ',  // y 4
        '           ',  // y 5
        ' W   C   W ',  // y 6
        '           ',  // y 7
        '           ',  // y 8
        '           ',  // y 9
        '           ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 36: the small ring: transcendent casing, an injection casing either side of the beam (hatch spots)
    [
        '           ',  // y 0
        '           ',  // y 1
        '           ',  // y 2
        ' NN     NN ',  // y 3
        'NNN     NNN',  // y 4
        'NN       NN',  // y 5
        'NW   C   WN',  // y 6
        'NN       NN',  // y 7
        'NNN     NNN',  // y 8
        ' NN     NN ',  // y 9
        '           ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 37: the beam, an injection casing either side (hatch spots)
    [
        '           ',  // y 0
        '           ',  // y 1
        '           ',  // y 2
        '           ',  // y 3
        '           ',  // y 4
        '           ',  // y 5
        ' W   C   W ',  // y 6
        '           ',  // y 7
        '           ',  // y 8
        '           ',  // y 9
        '           ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 38: the beam
    [
        '           ',  // y 0
        '           ',  // y 1
        '           ',  // y 2
        '           ',  // y 3
        '           ',  // y 4
        '           ',  // y 5
        '     C     ',  // y 6
        '           ',  // y 7
        '           ',  // y 8
        '           ',  // y 9
        '           ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 39: the beam
    [
        '           ',  // y 0
        '           ',  // y 1
        '           ',  // y 2
        '           ',  // y 3
        '           ',  // y 4
        '           ',  // y 5
        '     C     ',  // y 6
        '           ',  // y 7
        '           ',  // y 8
        '           ',  // y 9
        '           ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 40: the beam
    [
        '           ',  // y 0
        '           ',  // y 1
        '           ',  // y 2
        '           ',  // y 3
        '           ',  // y 4
        '           ',  // y 5
        '     C     ',  // y 6
        '           ',  // y 7
        '           ',  // y 8
        '           ',  // y 9
        '           ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
    // z 41: the front ring: a ring of bridge casing round the beam end
    [
        '           ',  // y 0
        '   NN NN   ',  // y 1
        '  NNNNNNN  ',  // y 2
        ' NNN N NNN ',  // y 3
        ' NN     NN ',  // y 4
        'NN  BBB  NN',  // y 5
        'NNN BCB NNN',  // y 6
        'NN  BBB  NN',  // y 7
        ' NN     NN ',  // y 8
        ' NNN N NNN ',  // y 9
        '  NNNNNNN  ',  // y 10
        '   NN NN   ',  // y 11
        '           ',  // y 12
    ],
    // z 42: the front plate: the controller in the middle
    [
        '           ',  // y 0
        '           ',  // y 1
        '           ',  // y 2
        '    N N    ',  // y 3
        '   NNNNN   ',  // y 4
        '  NNNNNNN  ',  // y 5
        '   NNSNN   ',  // y 6
        '  NNNNNNN  ',  // y 7
        '   NNNNN   ',  // y 8
        '    N N    ',  // y 9
        '           ',  // y 10
        '           ',  // y 11
        '           ',  // y 12
    ],
]

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }

    // The hall's pattern: Parts have a maximum only, never a required count
    // (setMaxGlobalLimited(max, preview count)); any injection casing ('W') may be one, nowhere else.
    // Power: the recipes' own power comes through ONE laser hatch (or one wireless receiver in Primary mode: the machine
    // checks it); every energy hatch is held in Auxiliary mode and only pays the field's stabilisers (10 A of LuV).
    // No maintenance hatch, no coolant.
    const dtpfPattern = definition => {
        const wall = Predicates.blocks('af9:dtpf_injection_casing')
            .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1, 1))
            // auxiliary power for the stabilisers: energy hatches and wireless receivers
            .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(6, 2))
            .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(8, 2))
            .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(4, 1))
            .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(4, 1))
            .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(4, 1))
            // osmium plasma for the field goes through plasma hatches only (never recipe inputs)
            .or(Predicates.abilities($ForgePlasmaHatch.PLASMA_INPUT).setMaxGlobalLimited(2, 1))
        let pattern = FactoryBlockPattern.start()
        DTPF_AISLES.forEach(aisle => { pattern = pattern.aisle(aisle) })
        return pattern
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            .where(' ', Predicates.any())
            .where('W', wall)
            .where('N', Predicates.blocks('af9:dtpf_transcendent_casing'))
            .where('B', Predicates.blocks('af9:dtpf_bridge_casing'))
            .where('C', Predicates.blocks('gtceu:superconducting_coil'))
            // the six module slots (af9-core PlasmaForgeMachine.modules(), dtpf_modules.js): a floating plate of module casing round
            // the module's controller, behind the middle injection casing of each big ring; an empty slot is simply not there
            .where('M', Predicates.blocks('af9:dtpf_module_casing').or(Predicates.air()))
            .where('P', $PlasmaForgeMachine.modules().or(Predicates.air()))
            .build()
    }

    event.create('dtpf', 'multiblock')
        .langValue('Dimensionally Transcendent Plasma Forge')
        .machine(holder => new $PlasmaForgeMachine(holder))
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('plasma_forge')])
        // FORGE_GATE: only a recipe the hatches can fully supply, with the field up; RAMP: the longer it runs, the cheaper
        // and faster (af9-core); perfect overclocks
        .recipeModifiers([$PlasmaForgeMachine.FORGE_GATE, $PlasmaForgeMachine.RAMP, $PlasmaForgeMachine.MODULES, GTRecipeModifiers.OC_PERFECT])
        .appearanceBlock(() => Block.getBlock('af9:dtpf_injection_casing'))
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.dtpf.tooltip', 9))
        .pattern(dtpfPattern)
        // the controller: injection casing with GTNH's DTPF face (kubejs assets, overlay_front*), the force field behind it
        .model(dtpfModel())
        .hasBER(true)

        // Plasma Hatch: an input hatch for osmium plasma only, HV-UHV. The forge burns readiness
    // through these; a plasma cell from the Particle Accelerator fills them.
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
})
