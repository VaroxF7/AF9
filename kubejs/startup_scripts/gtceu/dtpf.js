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
// (the model, af9-core ForceFieldRender). Mk2 is the same hall.

const $PlasmaForgeMachine = Java.loadClass('com.af9.core.machine.PlasmaForgeMachine')
const $PlasmaHeaterMachine = Java.loadClass('com.af9.core.machine.PlasmaHeaterMachine')
const $ForgePlasmaHatch = Java.loadClass('com.af9.core.machine.part.PlasmaHatchPartMachine')
const $DtpfModels = Java.loadClass('com.af9.core.machine.AF9MachineModels')

// The force field in the hall's middle (af9-core ForceFieldRender): 0 hoops, 1 plasma sheath, 2 helix, 3 lattice. A tube
// round the beam between the two end rings (1.6 to 40.4 blocks behind the controller's centre). The rings' blocks come as
// near as 2.9 blocks to the beam, so the radius is 2.6.
const DTPF_FIELD_STYLE = 1
const dtpfModel = () => $DtpfModels.workableCasingWithForceField('af9:block/dtpf_injection_casing',
    'gtceu:block/multiblock/dtpf', DTPF_FIELD_STYLE, 2.6, 1.6, 40.4)

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

// The DTPF, layer by layer: DTPF_AISLES[z] is the z-th plate, back (0) to front (42, the controller's); a plate's rows are
// y, bottom (0) to top (12); a row's characters are x, 0 to 10, the beam at x 5, y 6.
//   S controller, N transcendent casing, W injection casing (hatches), B bridge casing (glows),
//   C beam (superconducting coil); a space is nothing (anything may be there).
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
        '  NN   NN  ',  // y 0
        '  NNNNNNN  ',  // y 1
        '    NWN    ',  // y 2
        ' NN     NN ',  // y 3
        'NNN     NNN',  // y 4
        'NN       NN',  // y 5
        'NB   C   BN',  // y 6
        'NN       NN',  // y 7
        'NNN     NNN',  // y 8
        ' NN     NN ',  // y 9
        '    NWN    ',  // y 10
        '  NNNNNNN  ',  // y 11
        '  NN   NN  ',  // y 12
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
        '  NN   NN  ',  // y 0
        '  NNNNNNN  ',  // y 1
        '    NWN    ',  // y 2
        ' NN     NN ',  // y 3
        'NNN     NNN',  // y 4
        'NN       NN',  // y 5
        'NB   C   BN',  // y 6
        'NN       NN',  // y 7
        'NNN     NNN',  // y 8
        ' NN     NN ',  // y 9
        '    NWN    ',  // y 10
        '  NNNNNNN  ',  // y 11
        '  NN   NN  ',  // y 12
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
        '  NN   NN  ',  // y 0
        '  NNNNNNN  ',  // y 1
        '    NWN    ',  // y 2
        ' NN     NN ',  // y 3
        'NNN     NNN',  // y 4
        'NN       NN',  // y 5
        'NB   C   BN',  // y 6
        'NN       NN',  // y 7
        'NNN     NNN',  // y 8
        ' NN     NN ',  // y 9
        '    NWN    ',  // y 10
        '  NNNNNNN  ',  // y 11
        '  NN   NN  ',  // y 12
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

    // The hall's pattern: one for the Mk1 and the Mk2. Parts have a maximum only, never a required count
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
            .build()
    }

    event.create('dtpf', 'multiblock')
        .langValue('Dimensionally Transcendent Plasma Forge')
        .machine(holder => new $PlasmaForgeMachine(holder))
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('plasma_forge')])
        // FORGE_GATE: only a recipe the hatches can fully supply, with the field up; RAMP: the longer it runs, the cheaper
        // and faster (af9-core); perfect overclocks
        .recipeModifiers([$PlasmaForgeMachine.FORGE_GATE, $PlasmaForgeMachine.RAMP, GTRecipeModifiers.OC_PERFECT])
        .appearanceBlock(() => Block.getBlock('af9:dtpf_injection_casing'))
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.dtpf.tooltip', 7))
        .pattern(dtpfPattern)
        // the controller: injection casing with GTNH's DTPF face (kubejs assets, overlay_front*), the force field behind it
        .model(dtpfModel())
        .hasBER(true)

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
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.dtpf.tooltip', 7))
        .pattern(dtpfPattern)
        .model(dtpfModel())
        .hasBER(true)

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
