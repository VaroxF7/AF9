// AF9 - The Space Elevator, GTNH's: a tower on a cable that reaches into space, the pack's renewable ore source from ZPM on.
// Behaviour: af9-core (com.af9.core.elevator.SpaceElevatorMachine: the asteroids, the motors, the modules, the cable; the
// cable and the climber that rides it: com.af9.core.client.render.SpaceElevatorRender). Recipes:
// server_scripts/mods/gtceu/space_elevator.js. Spec: docs/space-elevator.md
//
// A Mining Drone (not used up) in the drone slot of its screen (or an input bus), 50 to 100 buckets of hydrogen and of a
// supercooled coolant in the fluid hatches and 4 to 32 amps of ZPM energy for minutes send an expedition to a random
// asteroid: the output buses hold its ore, tens of stacks of raw ore. The asteroids are made from GT's ore veins; the better
// the drone, the more of them it reaches.
// As in GTNH the work is the modules': Mining Modules in the module slots fly 2, 4 or 8 expeditions at once, and the motors'
// tier says how many slots are powered.
// Instead of an asteroid an expedition can go to a planet and bring home a fluid (a liquid mission, picked on the screen):
// GTNH's Space Pumping table (af9-core, PlanetCatalog), the same drone, fluids and energy, into fluid output hatches.
//
// The structure is GTNH's, block for block (SE_MAIN): 35 x 35 and 43 high, a floor of concrete, a central column of motors
// round an empty shaft with the cable on top of it, and a tapering frame of base casing and stress-proof casing around it.
// Its extended size (SE_EXTENSION, switched on the controller's screen) adds a ring of 47 x 47 with twelve more module slots.

const $SpaceElevator = Java.loadClass('com.af9.core.elevator.SpaceElevatorMachine')
const $ElevatorModels = Java.loadClass('com.af9.core.machine.AF9MachineModels')
const $ElevatorDirection = Java.loadClass('com.gregtechceu.gtceu.api.pattern.util.RelativeDirection')
const $ElevatorCoolantHatch = Java.loadClass('com.af9.core.machine.part.CoolantHatchPartMachine')
const $SpaceModule = Java.loadClass('com.af9.core.elevator.SpaceModuleMachine')
const $SpaceMission = Java.loadClass('com.af9.core.elevator.SpaceMissionMachine')

const SE_ROMAN = ['I', 'II', 'III', 'IV', 'V']
// module slots the motors of each tier power, expeditions a Mining Module of each tier flies at once (GTNH's numbers; the
// machine's own are SpaceElevatorMachine.MODULE_SLOTS and MODULE_EXPEDITIONS)
const SE_SLOTS = [6, 12, 15, 18, 24]
const SE_EXPEDITIONS = [2, 4, 8]
// the farthest planet type a liquid mission of each drone tier reaches (GTNH's planet types 2 to 8; the machine's own is
// PlanetCatalog.droneFor)
const SE_PLANETS = [3, 5, 7, 8]

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    // a Mining Drone (not used up), a drill head and a crate (both used up), hydrogen and the coolant in; the ore is made when
    // a run starts (SpaceElevatorMachine)
    event.create('space_mining')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(3, 1, 2, 0)
        .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.ARC)
    // the same flights for a planet's fluid, the liquid missions: the fluid is put in when a run starts, the one picked on
    // the elevator's screen (SpaceElevatorMachine, PlanetCatalog). The elevator flies the kind of mission that is picked
    event.create('space_pumping')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(1, 0, 2, 1)
        .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.ARC)
    // the modules are controllers of their own, but run no recipes: the elevator flies the missions (SpaceModuleMachine)
    event.create('space_module')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(1, 1, 1, 0)
        .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.ARC)
})

// ---- The structure -----------------------------------------------------------------------------------------------------
// GTNH's Space Elevator (gtnhintergalactic's TileEntitySpaceElevator, STRUCTURE_PIECE_MAIN; design: Sampsa, Jimbno, Adam,
// Baunti), block for block: 35 wide, 43 high, 35 deep. The table holds the slices from the front to the middle one (the
// back half mirrors the front), each slice its rows from its highest block down to the floor (the tower narrows), a row
// its blocks from left to right:
//   A  Ultra High Strength Concrete Floor   D  Base Casing            E  Stress-Proof Casing       F  Internal Structure
//   H  Neutronium Frame Box                 C  Motor (one tier)       B  the Cable             -  air (the shaft)
//   X  Base Casing or a hatch of the elevator: the bottom centre casings (and the controller, in the front one's middle)
//   M  Base Casing or a bus / hatch of a module slot                  I  a module slot: a Mining Module, or Base Casing
// Hatches: energy and laser in X; input bus, Coolant Hatch, fluid output hatch, fluid input hatch and output bus in X or M.
const SE_MAIN = [
    [   // 0
        '               FF FF               ',
        '               AAAAA               '
    ],
    [   // 1
        '               D   D               ',
        '            FFFFF FFFFF            ',
        '            AAAAAAAAAAA            '
    ],
    [   // 2
        '            DDDDE EDDDD            ',
        '          FFFFFFF FFFFFFF          ',
        '          AAAAAAAAAAAAAAA          '
    ],
    [   // 3
        '                E E                ',
        '          DD   DE ED   DD          ',
        '         FFFFFFD   DFFFFFF         ',
        '        AAAAAAAAAAAAAAAAAAA        '
    ],
    [   // 4
        '                E E                ',
        '               DE ED               ',
        '               D   D               ',
        '         FFF           FFF         ',
        '       AAAAAAAAAAAAAAAAAAAAA       '
    ],
    [   // 5
        '                E E                ',
        '               DE ED               ',
        '               DE ED               ',
        '                                   ',
        '                                   ',
        '      AAAAAAAAAAAAAAAAAAAAAAA      '
    ],
    [   // 6
        '                E E                ',
        '               DE ED               ',
        '               DE ED               ',
        '               DE ED               ',
        '                                   ',
        '                                   ',
        '                                   ',
        '     AAAAAAAAAAAAAAAAAAAAAAAAA     '
    ],
    [   // 7
        '                E E                ',
        '              HDE EDH              ',
        '               DE ED               ',
        '               DE ED               ',
        '               DE ED               ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '    AAAAAAAAAAAAAAAAAAAAAAAAAAA    '
    ],
    [   // 8
        '                E E                ',
        '               DE ED               ',
        '               DE ED               ',
        '               DE ED               ',
        '            HH DE ED HH            ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '               M M M               ',
        '               M M M               ',
        '               M M M               ',
        '               M M M               ',
        '   AAAAAAAAAAAAM M MAAAAAAAAAAAA   '
    ],
    [   // 9
        '                E E                ',
        '               DE ED               ',
        '               DE ED               ',
        '               DE ED               ',
        '               DE ED               ',
        '                                   ',
        '         E               E         ',
        '         EHH           HHE         ',
        '         E               E         ',
        '         E               E         ',
        '         E               E         ',
        '         E               E         ',
        '         E     M M M     E         ',
        '         E     I I I     E         ',
        '         E     M M M     E         ',
        '   FF    E     M M M     E    FF   ',
        '   AAAAAAAAAAAAM M MAAAAAAAAAAAA   '
    ],
    [   // 10
        '                E E                ',
        '               DE ED               ',
        '               DE ED               ',
        '               DE ED               ',
        '               DE ED               ',
        '               DE ED               ',
        '          E             E          ',
        '          E             E          ',
        '          E             E          ',
        '          E             E          ',
        '          E             E          ',
        '         HE             EH         ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '   D                           D   ',
        '  FFF          F F F          FFF  ',
        '  AAAAAAAAAAAAA     AAAAAAAAAAAAA  '
    ],
    [   // 11
        '                E E                ',
        '               DE ED               ',
        '               DE ED               ',
        '               DE ED               ',
        '               DE ED               ',
        '               DE ED               ',
        '               DE ED               ',
        '               DE ED               ',
        '               DE ED               ',
        '               DE ED               ',
        '                                   ',
        '                                   ',
        '           E           E           ',
        '           E           E           ',
        '           E           E           ',
        '           E           E           ',
        '           E           E           ',
        '                                   ',
        '                                   ',
        '         H               H         ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '   D                           D   ',
        '  FFF          F F F          FFF  ',
        '  AAAAAAAAAAAAA     AAAAAAAAAAAAA  '
    ],
    [   // 12
        '                E E                ',
        '               DE ED               ',
        '               DE ED               ',
        '               DE ED               ',
        '               DE ED               ',
        '               DE ED               ',
        '               DE ED               ',
        '               DE ED               ',
        '               DE ED               ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '             HH     HH             ',
        '            E         E            ',
        '            E         E            ',
        '            E         E            ',
        '            E         E            ',
        '            E         E            ',
        '            E         E            ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '        H                 H        ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '  D                             D  ',
        ' FFF          FFFFFFF          FFF ',
        ' AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA '
    ],
    [   // 13
        '                FFF                ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '             E       E             ',
        '             E       E             ',
        '             E       E             ',
        '             E       E             ',
        '             E       E             ',
        '             E       E             ',
        '             E       E             ',
        '            HE       EH            ',
        '             E       E             ',
        '             E       E             ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '        H                 H        ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '  D                             D  ',
        ' FFF         FFFFFFFFF         FFF ',
        ' AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA '
    ],
    [   // 14: the front of the central column (the controller is in its 4th layer)
        '                FFF                ',
        '                 E                 ',
        '                 E                 ',
        '                 E                 ',
        '                 E                 ',
        '                 E                 ',
        '               F   F               ',
        '              E     E              ',
        '              E     E              ',
        '              E     E              ',
        '              E     E              ',
        '              E     E              ',
        '              E     E              ',
        '              E     E              ',
        '              E     E              ',
        '              E     E              ',
        '                                   ',
        '                                   ',
        '                FFF                ',
        '                                   ',
        '                                   ',
        '            H         H            ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                FFF                ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '       H                   H       ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                XXX                ',
        '                XXX                ',
        '  D             XXX             D  ',
        ' FFF        FFFFFFFFFFF        FFF ',
        ' AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA '
    ],
    [   // 15
        '               F D F               ',
        '                 D                 ',
        '                 D                 ',
        '                 D                 ',
        '                 D                 ',
        '                 D                 ',
        '              F  D  F              ',
        '                 D                 ',
        '            D    D    D            ',
        '            D    D    D            ',
        '            D    D    D            ',
        '            D    D    D            ',
        '            D    D    D            ',
        '            D    D    D            ',
        '            D    D    D            ',
        '           DD    D    DD           ',
        '           D     D     D           ',
        '           D     D     D           ',
        '           D   F D F   D           ',
        '           D     C     D           ',
        '           D     C     D           ',
        '           D     C     D           ',
        '           D     C     D           ',
        '          DD     C     DD          ',
        '          D      C      D          ',
        '          D      C      D          ',
        '          D      C      D          ',
        '         DD      C      DD         ',
        '         D     FDCDF     D         ',
        '         D      DCD      D         ',
        '        DD      DCD      DD        ',
        '        D       DCD       D        ',
        '        D       DCD       D        ',
        '       DD      DDCDD      DD       ',
        '       D       D C D       D       ',
        '       D       D C D       D       ',
        '      DD       D C D       DD      ',
        '      D        D C D        D      ',
        '     DD MM     XDCDX     MM DD     ',
        '    DD  MI     XDCDX     IM  DD    ',
        ' DDDD   MM     XDCDX     MM   DDDD ',
        'FFFD    MMFFFFFDDDDDFFFFFMM    DFFF',
        'AAAAAAAAMM  AAAXXXXXAAA  MMAAAAAAAA'
    ],
    [   // 16
        '              F     F              ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '                                   ',
        '             F       F             ',
        '            E         E            ',
        '            E         E            ',
        '            E         E            ',
        '            E         E            ',
        '            E         E            ',
        '            E         E            ',
        '            E         E            ',
        '           EE         EE           ',
        '           EE         EE           ',
        '           E           E           ',
        '           E           E           ',
        '           E  F     F  E           ',
        '           E           E           ',
        '           E           E           ',
        '           E           E           ',
        '          EE           EE          ',
        '          EE           EE          ',
        '          E             E          ',
        '          E             E          ',
        '         EE             EE         ',
        '         EE             EE         ',
        '         E    FD   DF    E         ',
        '        EE     D   D     EE        ',
        '        EE     D   D     EE        ',
        '        E      D   D      E        ',
        '       EE      D   D      EE       ',
        '       EE      D   D      EE       ',
        '       E                   E       ',
        '      EE                   EE      ',
        '      EE                   EE      ',
        '     EE                     EE     ',
        '    EEE       XD   DX       EEE    ',
        '   EEE        XD   DX        EEE   ',
        '  EE          XD   DX          EE  ',
        'FFF         FFFDDDDDFFF         FFF',
        'AAAAAAAA    AAAXXXXXAAA    AAAAAAAA'
    ],
    [   // 17: the middle, with the cable on top of the motor shaft
        '              FD   DF              ',
        '              ED   DE              ',
        '              ED   DE              ',
        '              ED   DE              ',
        '              ED   DE              ',
        '              ED   DE              ',
        '             F D   D F             ',
        '               D   D               ',
        '               D   D               ',
        '               D   D               ',
        '               D   D               ',
        '               D   D               ',
        '               D   D               ',
        '               D   D               ',
        '               D   D               ',
        '               D   D               ',
        '               D   D               ',
        '               D B D               ',
        '              FD - DF              ',
        '               C - C               ',
        '               C - C               ',
        '               C - C               ',
        '               C - C               ',
        '               C - C               ',
        '               C - C               ',
        '               C - C               ',
        '               C - C               ',
        '               C - C               ',
        '              FC - CF              ',
        '               C - C               ',
        '               C - C               ',
        '               C - C               ',
        '               C - C               ',
        '               C - C               ',
        '               C - C               ',
        '               C - C               ',
        '               C - C               ',
        '               C - C               ',
        '        MM    XC - CX    MM        ',
        '        MI    XC - CX    IM        ',
        '        MM    XC - CX    MM        ',
        '        MMFFFFFDDDDDFFFFFMM        ',
        'AAAAAAAAMM  AAAXXXXXAAA  MMAAAAAAAA'
    ]
]

// GTNH's extended elevator (STRUCTURE_PIECE_EXTENDED): a ring of 47 x 47 round the tower's foot, in its bottom five layers,
// with twelve more module slots (three in the middle of every side). Written as the tower is: the slices from the front
// to the middle one, each its rows from its highest block down; the tower stands in the middle of it.
const SE_EXTENSION = [
    [   // 0
        '                    FFFFFFF                    ',
        '                    AAAAAAA                    '
    ],
    [   // 1
        '                     M M M                     ',
        '                     M M M                     ',
        '                     M M M                     ',
        '                   FFM M MFF                   ',
        '                 AAAAM M MAAAA                 '
    ],
    [   // 2
        '                     M M M                     ',
        '                     I I I                     ',
        '                     M M M                     ',
        '                   FFM M MFF                   ',
        '              AAAAAAAM M MAAAAAAA              '
    ],
    [   // 3
        '                   FFF F FFF                   ',
        '            AAAAAAAAAA   AAAAAAAAAA            '
    ],
    [   // 4
        '                  FFFF F FFFF                  ',
        '          AAAA   AAAAA   AAAAA   AAAA          '
    ],
    [   // 5
        '                  FFFF F FFFF                  ',
        '        AAAA     AAAAA   AAAAA     AAAA        '
    ],
    [   // 6
        '                  FFFFFFFFFFF                  ',
        '       AAA      AAAAAAAAAAAAAAA      AAA       '
    ],
    [   // 7
        '      AAA       AA           AA       AAA      '
    ],
    [   // 8
        '     AAA                               AAA     '
    ],
    [   // 9
        '     AA                                 AA     '
    ],
    [   // 10
        '    AA                                   AA    '
    ],
    [   // 11
        '    AA                                   AA    '
    ],
    [   // 12
        '   AA                                     AA   '
    ],
    [   // 13
        '   AA                                     AA   '
    ],
    [   // 14
        '  AA                                       AA  '
    ],
    [   // 15
        '  AA                                       AA  '
    ],
    [   // 16
        '  AA  AA                               AA  AA  '
    ],
    [   // 17
        ' AAAAAAA                               AAAAAAA '
    ],
    [   // 18
        '    FFF                                 FFF    ',
        ' AAAAAA                                 AAAAAA '
    ],
    [   // 19
        ' FFFFFF                                 FFFFFF ',
        ' AAAAAA                                 AAAAAA '
    ],
    [   // 20
        'FFFFFFF                                 FFFFFFF',
        'AAAAAAA                                 AAAAAAA'
    ],
    [   // 21
        ' MM                                         MM ',
        ' MI                                         IM ',
        ' MM                                         MM ',
        'FMMFFFF                                 FFFFMMF',
        'AMMAAAA                                 AAAAMMA'
    ],
    [   // 22
        'F     F                                 F     F',
        'A     A                                 A     A'
    ],
    [   // 23
        ' MM                                         MM ',
        ' MI                                         IM ',
        ' MM                                         MM ',
        'FMMFFFF                                 FFFFMMF',
        'AMM   A                                 A   MMA'
    ]
]

const SE_WIDTH = 35
const SE_HEIGHT = 43
const SE_DEPTH = 35
// the controller: the front centre of the central column, in the 4th layer (column, row from the top, slice)
const SE_CONTROLLER = [17, 39, 14]
// the extension: how far it reaches out past the tower on every side, and how many of the bottom layers it is in
const SE_REACH = 6
const SE_RING_HEIGHT = 5

const seBlank = width => {
    let row = ''
    for (var i = 0; i < width; i++) row += ' '
    return row
}

// A piece's slices in full, front to back, every slice `height` rows from the top down: the table holds the front half
// and the middle slice, and of a slice only the rows from its highest block down
const seUnfold = (table, depth, height, width) => {
    const blank = seBlank(width)
    const slices = []
    for (var c = 0; c < depth; c++) {
        var rows = table[c < table.length ? c : depth - 1 - c]
        var slice = []
        for (var b = 0; b < height; b++) slice.push(b < height - rows.length ? blank : rows[b - (height - rows.length)])
        slices.push(slice)
    }
    return slices
}

// The tower's slices with the controller in its place; extended: the tower in the middle of the extension's ring
const seSlices = extended => {
    const tower = seUnfold(SE_MAIN, SE_DEPTH, SE_HEIGHT, SE_WIDTH)
    const row = tower[SE_CONTROLLER[2]][SE_CONTROLLER[1]]
    tower[SE_CONTROLLER[2]][SE_CONTROLLER[1]] = row.substring(0, SE_CONTROLLER[0]) + 'S' +
        row.substring(SE_CONTROLLER[0] + 1)
    if (!extended) return tower
    const width = SE_WIDTH + 2 * SE_REACH
    const depth = SE_DEPTH + 2 * SE_REACH
    const ring = seUnfold(SE_EXTENSION, depth, SE_RING_HEIGHT, width)
    const slices = []
    for (var c = 0; c < depth; c++) {
        var slice = []
        for (var b = 0; b < SE_HEIGHT; b++) {
            var line = ''
            for (var a = 0; a < width; a++) {
                // the tower's block where it has one, else the ring's (they agree where both have one)
                var block = c >= SE_REACH && c < SE_REACH + SE_DEPTH && a >= SE_REACH && a < SE_REACH + SE_WIDTH ?
                    tower[c - SE_REACH][b].charAt(a - SE_REACH) : ' '
                if (block === ' ' && b >= SE_HEIGHT - SE_RING_HEIGHT) {
                    block = ring[c][b - (SE_HEIGHT - SE_RING_HEIGHT)].charAt(a)
                }
                line += block
            }
            slice.push(line)
        }
        slices.push(slice)
    }
    return slices
}

// The pattern of such slices: aisles front -> back (a slice each), rows top -> bottom, as the table is written
const sePattern = (definition, slices) => {
    const casing = 'af9:space_elevator_base_casing'
    let pattern = FactoryBlockPattern.start($ElevatorDirection.RIGHT, $ElevatorDirection.DOWN, $ElevatorDirection.BACK)
    for (var c = 0; c < slices.length; c++) pattern = pattern.aisle(slices[c])
    // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count)). One set of them for
    // the whole tower, so the maximums count across it. GTNH's places: the energy in the bottom centre casings, the buses
    // and the hatches of the fluids there or in the module slots. The supercooled coolant has its Coolant Hatches, as
    // in AF9's other cooled machines: up to 4. They hold an eighth of a fluid hatch (ZPM 128 B, UV 256 B, UHV 512 B); GT
    // takes a recipe's fluids from every fluid input, so the coolant of a run bigger than they hold sits in the fluid
    // input hatches, with the hydrogen. The fluid of a liquid mission goes to fluid output hatches
    const energy = Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(4, 2)
    const laser = Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(2, 0)
    // the computation the modules need (the elevator's screen says how much): data hatches for the optical cables
    const computation = Predicates.abilities(PartAbility.COMPUTATION_DATA_RECEPTION).setMaxGlobalLimited(2, 0)
    const itemsIn = Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1)
    const coolant = Predicates.abilities($ElevatorCoolantHatch.COOLANT_INPUT).setMaxGlobalLimited(4, 1)
    const fluidsOut = Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(6, 1)
    const fluidsIn = Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(8, 1)
    const itemsOut = Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(12, 2)
    // a Coolant Hatch is a fluid input hatch too: here it is a part of its own, counted once
    $SpaceElevator.plainFluidHatches(fluidsIn)
    // what the preview shows and the terminal builds with: ZPM parts, not GT's first ones (ULV, which hold nothing an
    // expedition needs)
    $SpaceElevator.zpmFirst(energy)
    $SpaceElevator.zpmFirst(laser)
    $SpaceElevator.zpmFirst(computation)
    $SpaceElevator.zpmFirst(itemsIn)
    $SpaceElevator.zpmFirst(coolant)
    $SpaceElevator.zpmFirst(fluidsOut)
    $SpaceElevator.zpmFirst(fluidsIn)
    $SpaceElevator.zpmFirst(itemsOut)
    // The order matters to the terminal: it builds a place with the first kind that is not full, and counts the place for
    // every kind that is not full. So each kind gets its maximum less that of the kind before it, and the maximums have to
    // rise along the list (a kind whose maximum is no higher than that of the kind before it is never built): 4 energy
    // hatches, and 2 input buses, 2 Coolant Hatches, 2 fluid output hatches, 2 fluid input hatches and 4 output buses
    const power = energy.or(laser).or(computation)
    const buses = itemsIn.or(coolant).or(fluidsOut).or(fluidsIn).or(itemsOut)
    return pattern
        .where('S', Predicates.controller(Predicates.blocks(definition.get())))
        .where('A', Predicates.blocks('af9:ultra_high_strength_concrete_floor'))
        .where('D', Predicates.blocks(casing))
        .where('E', Predicates.blocks('gtceu:stress_proof_casing'))
        .where('F', Predicates.blocks('af9:space_elevator_internal_structure'))
        .where('H', Predicates.blocks('gtceu:neutronium_frame'))
        .where('C', $SpaceElevator.motors())                                // any tier, all of one tier
        .where('B', $SpaceElevator.cable())                                 // with open sky above it
        .where('X', Predicates.blocks(casing).or(power).or(buses))
        .where('M', Predicates.blocks(casing).or(buses))
        .where('I', $SpaceElevator.modules().or(Predicates.blocks(casing)))  // any tier
        .where('-', Predicates.air())
        .where(' ', Predicates.any())
        .build()
}

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }

    event.create('space_elevator', 'multiblock')
        .langValue('Space Elevator')
        .machine(holder => new $SpaceElevator(holder))
        .rotationState(RotationState.NON_Y_AXIS)
        // upright only, as GTNH's
        .allowExtendedFacing(false)
        .allowFlip(false)
        // the elevator itself runs no recipe: its Mining Modules fly the missions (GTNH's design). The missions are listed
        // here all the same, so the recipe viewers show them on the tower (SpaceElevatorMachine.attachSideTabs: no mode tab)
        .recipeTypes([GTRecipeTypes.get('space_module'), GTRecipeTypes.get('space_mining'),
            GTRecipeTypes.get('space_pumping')])
        .appearanceBlock(() => Block.getBlock('af9:space_elevator_base_casing'))
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.space_elevator.tooltip', 11))
        .pattern(definition => {
            // GT asks for this once: both sizes are built, the extended one is the machine's own to switch to
            $SpaceElevator.setExtendedPattern(sePattern(definition, seSlices(true)))
            return sePattern(definition, seSlices(false))
        })
        // the structure preview's two pages: the basic and the extended tower
        .shapeInfos(definition => $SpaceElevator.previews(definition))
        .workableCasingModel('af9:block/space_elevator_base_casing', 'gtceu:block/multiblock/fusion_reactor')
        // the same model plus the cable and the climber on it; where the cable block is from the controller lives in
        // af9-core
        .model($ElevatorModels.workableCasingWithSpaceElevator('af9:block/space_elevator_base_casing',
            'gtceu:block/multiblock/fusion_reactor', $SpaceElevator.CABLE_UP, $SpaceElevator.CABLE_BACK))
        .hasBER(true)

    // ---- The Mining Modules: GTNH's elevator modules, each a small multiblock of its own that stands in a module slot ('I' above)
    // and flies the missions (af9-core SpaceModuleMachine: the drone, the fuel and the coolant in its hatches, the cargo out of
    // them, the energy from the buffer the elevator fills). Its structure is the slot's casings round it, where the slot's
    // hatches go: the same parts as the tower's; the side towards the shaft is the tower's business. Parts have a maximum only,
    // never a required count (setMaxGlobalLimited(max, preview count)); the two sides are alike, so the module forms whichever
    // way it faces.
    ;['I', 'II', 'III'].forEach((roman, index) => {
        const tier = index + 1
        event.create(`space_mining_module_mk${tier}`, 'multiblock')
            .langValue(`Space Mining Module MK-${roman}`)
            .machine(holder => new $SpaceModule(holder, tier))
            .rotationState(RotationState.NON_Y_AXIS)
            .recipeTypes([GTRecipeTypes.get('space_mining'), GTRecipeTypes.get('space_pumping')])
            // the missions only (the modifier is the engine: drone, asteroid or planet, expeditions at once)
            .recipeModifiers([$SpaceMission.MISSION])
            .appearanceBlock(() => Block.getBlock('af9:space_elevator_base_casing'))
            ['tooltips(net.minecraft.network.chat.Component[])'](tooltips(`af9.space_mining_module_mk${tier}.tooltip`, 4))
            .pattern(definition => {
                const slotPart = Predicates.blocks('af9:space_elevator_base_casing')
                    .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                    .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(4, 1))
                    .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(2, 1))
                    .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(4, 1))
                    .or(Predicates.abilities($ElevatorCoolantHatch.COOLANT_INPUT).setMaxGlobalLimited(2, 1))
                    // what else the tower has in the slot's casings (energy hatches and the like) is not the module's
                    .or(Predicates.any())
                return FactoryBlockPattern.start()
                    .aisle('MMM', 'MSM', 'MMM')
                    .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                    .where('M', slotPart)
                    .build()
            })
            .workableCasingModel('af9:block/space_elevator_base_casing',
                `gtceu:block/multiblock/space_mining_module_mk${tier}`)
    })
})
