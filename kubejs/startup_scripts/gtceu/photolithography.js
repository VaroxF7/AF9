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
// for the plasma source.
GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    // [node, fluid inputs, item inputs]: substrate + reticle, + the ArF Excimer Laser for 80 and 65 nm (the scanner's
    // laser slot) or the EUV Light Source for 20 and 7 nm (both not consumed)
    const lineModes = [['350nm', 3, 2], ['200nm', 5, 2], ['100nm', 5, 2], ['80nm', 5, 3], ['65nm', 6, 3],
        ['50nm', 7, 2], ['20nm', 7, 3], ['7nm', 7, 3]]
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
    // plasma atomic soldering (UHV+ circuits, ion-by-ion deposition with plasma solder, no reflow): up to 10 item
    // inputs (the UHV mainframe's 9), 1 out, plasma solder + PBI in. Runs only on the Array Mk2 (extended, in orbit).
    event.create('plasma_soldering')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(10, 1, 2, 0)
        .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
        .setSound($LithoSounds.ORBITAL_STATION)
})

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
    // Array Mk2 (extended, 35 x 35, a screwdriver on the controller switches sizes like the Space Elevator's screen
    // switch): the same platform with a larger circular rim + cross spokes (same blocks) and a larger light ring
    // (radius 14.6, af9-core RING_RADIUS_MK2). Only the Mk2 runs plasma atomic soldering (gtceu:plasma_soldering):
    // UHV+ circuits deposited ion-by-ion with plasma solder, in orbit.
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
        ['                         ', '                         ', '                         ', '            A            ','            A            ', '           AAA           ','           AAA           ','           AA            ','            A            ','            A            ','           CBC           ','           CBC           ','           CBC           ',' C        CCBCC        C ','DCFFFFFFFHHHHHHHFFFFFFFCD',' C      FFFF FFFF      C ','      LLLLLLLLLLLLL      ','          OOOOO          '],
        ['            A            ', '            A            ', '            A            ', '           AAA           ','           ABA           ', '           ABA           ','           ABA           ','           ABA           ','           ABA           ','          AABAA          ','          ABBBA          ','          ABBBA          ',' CCC     CCBBBCC     CCC ',' CCCCCCCCCCBBBCCCCCCCCCC ','DCHHHHHHHHHHHHHHHHHHHHHCD',' CCC       F F       CCC ',' CC LLLLLLLLLLLLLLLLL CC ','       OOOOOKOOOOO       '],
        ['                         ', '                         ', '                         ', '            A            ','            A            ', '           AAA           ','           AAA           ','            AA           ','            A            ','            A            ','           CBC           ','           CBC           ','           CBC           ',' C        CCBCC        C ','DCFFFFFFFHHHHHHHFFFFFFFCD',' C      FFFF FFFF      C ','      LLLLLLLLLLLLL      ','          OOOOO          '],
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
    // Array Mk2: the basic 25 x 25 centred in 35 x 35 (offset 5), plus a larger circular rim + cross spokes in the
    // outer band (same blocks as the level: O on the top deck, D on the sturdy ring, C elsewhere; mast/air levels
    // stay empty so the mast stands free). Rim band radius 14.5-17.5 about the centre (17, 17); spokes on x/z = 17.
    const ORBITAL_PAD = 5
    const ORBITAL_MK2 = 35
    const orbitalLevelFill = row => {
        if (row.includes('O') || row.includes('K')) return 'O'
        if (row.includes('D')) return 'D'
        if (row.includes('C') || row.includes('L') || row.includes('H') || row.includes('F')) return 'C'
        return ' '
    }
    // forEach, not for: KubeJS's engine (Rhino) keeps a const in a loop body at its first pass's value, so loop
    // bodies use callbacks (a const in a callback is new each call; lint rule S3)
    const orbitalMk2Row = (az, ay) => {
        const inCoreZ = az >= ORBITAL_PAD && az < ORBITAL_PAD + 25
        const levelRow = inCoreZ ? ORBITAL_BASIC[az - ORBITAL_PAD][ay] : ORBITAL_BASIC[12][ay]
        const fill = orbitalLevelFill(levelRow)
        if (fill === ' ') {
            return inCoreZ ? '     ' + levelRow + '     ' : '                                   '
        }
        let row = ''
        Array.from({ length: ORBITAL_MK2 }).forEach((_, ax) => {
            const inCoreX = ax >= ORBITAL_PAD && ax < ORBITAL_PAD + 25
            if (inCoreX && inCoreZ) {
                row += levelRow.charAt(ax - ORBITAL_PAD)
                return
            }
            const dx = ax - 17, dz = az - 17
            const dist = Math.sqrt(dx * dx + dz * dz)
            const onRim = dist >= 14.5 && dist <= 17.5
            const onSpoke = ax === 17 || az === 17
            row += (onRim || onSpoke) ? fill : ' '
        })
        return row
    }
    const orbitalMk2Slices = () => {
        const slices = []
        Array.from({ length: ORBITAL_MK2 }).forEach((_, az) => {
            const aisle = []
            Array.from({ length: 18 }).forEach((_, ay) => { aisle.push(orbitalMk2Row(az, ay)) })
            slices.push(aisle)
        })
        return slices
    }
    const orbitalWheres = definition => {
        const energy = Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(3, 1)
        const laser = Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1, 1)
        const coolant = Predicates.abilities($LithoCoolantHatch.COOLANT_INPUT).setMaxGlobalLimited(2, 1)
        const computation = Predicates.abilities(PartAbility.COMPUTATION_DATA_RECEPTION).setMaxGlobalLimited(1, 1)
        const data = Predicates.abilities(PartAbility.DATA_ACCESS).setMaxGlobalLimited(1, 1)
        const optical = Predicates.abilities(PartAbility.OPTICAL_DATA_RECEPTION).setMaxGlobalLimited(1, 0)
        const fluids = Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(8, 1)
        const itemsIn = Predicates.abilities(PartAbility.IMPORT_ITEMS).setPreviewCount(1)
        const itemsOut = Predicates.abilities(PartAbility.EXPORT_ITEMS).setPreviewCount(1)
        return [
            ['K', Predicates.controller(Predicates.blocks(definition.get()))],
            ['A', Predicates.blocks('gtceu:hsse_frame')],
            ['B', Predicates.blocks('gtceu:hssg_coil_block')],
            ['D', Predicates.blocks('gtceu:sturdy_machine_casing')],
            ['F', Predicates.blocks('gtceu:hsss_frame')],
            ['H', Predicates.blocks('gtceu:shock_proof_cutting_casing')],
            ['L', Predicates.blocks('gtceu:stress_proof_casing')],
            ['C', Predicates.blocks('gtceu:nonconducting_casing')],
            // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview)): the track
            // chemicals through fluid input hatches, the supercooled coolant through coolant hatches, computation
            // (7 and 1 nm) through a computation hatch and the 1 nm research through a data hatch. Every hatch goes
            // on the PTFE casings of the top deck and nowhere else.
            ['O', Predicates.blocks('gtceu:inert_machine_casing').or(energy).or(laser).or(coolant).or(computation)
                .or(data).or(optical).or(fluids).or(itemsIn).or(itemsOut)
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1))],
            [' ', Predicates.any()]
        ]
    }
    const orbitalPattern = (definition, slices) => {
        let pattern = FactoryBlockPattern.start($LithoRelativeDirection.RIGHT, $LithoRelativeDirection.FRONT,
            $LithoRelativeDirection.UP)
        for (let c = 0; c < slices.length; c++) pattern = pattern.aisle(slices[c])
        orbitalWheres(definition).forEach(([char, predicate]) => { pattern = pattern.where(char, predicate) })
        return pattern.build()
    }

    event.create('orbital_lithography_station', 'multiblock')
        .machine(holder => new $OrbitalLithographyMachine(holder))
        .rotationState(RotationState.ALL)
        .allowExtendedFacing(true)
        .recipeTypes(['lithography_50nm', 'lithography_20nm', 'lithography_7nm', 'orbital_lithography',
            'plasma_soldering'].map(id => GTRecipeTypes.get(id)))
        // LITHO_GATE: only in orbit, with the recipe's full EU/t, a sealed vacuum and (researched prints) a data hatch;
        // STRIP_BROKEN: the break roll decides; COOLANT: adds the coolant (faster with a better one);
        // PLASMA_GATE: plasma soldering only in orbit on the Array Mk2 (extended); perfect overclocks; then batch
        // mode (GT's: once overclocked below 5 s, several prints in one run, the coolant too). No parallel hatch.
        .recipeModifiers([$LithoMachine.LITHO_GATE, $LithoMachine.STRIP_BROKEN, $OrbitalLithographyMachine.COOLANT,
            $OrbitalLithographyMachine.PLASMA_GATE, GTRecipeModifiers.OC_PERFECT, GTRecipeModifiers.BATCH_MODE])
        .appearanceBlock(() => Block.getBlock('gtceu:inert_machine_casing'))
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.orbital_lithography_station.tooltip', 15))
        .pattern(definition => {
            // GT asks for this once: both sizes are built, the extended one is the machine's own to switch to
            $OrbitalLithographyMachine.setExtendedPattern(orbitalPattern(definition, orbitalMk2Slices()))
            return orbitalPattern(definition, ORBITAL_BASIC)
        })
        .workableCasingModel('gtceu:block/casings/solid/machine_casing_inert_ptfe',
            'gtceu:block/multiblock/fusion_reactor')
        // the same model plus the light ring: centre 3 behind the controller (below, as it faces up), radius 9.6
        // (14.6 on the Array Mk2, af9-core reads the size through ringRadius), tube 0.25, lying across the
        // controller's front axis. The numbers live in af9-core (RING_*), which also burns whatever touches the lit
        // ring. Plasma soldering glows hotter (ringGlow 2, af9-core).
        .model($LithoMachineModels.workableCasingWithLightRing('gtceu:block/casings/solid/machine_casing_inert_ptfe',
            'gtceu:block/multiblock/fusion_reactor', $OrbitalLithographyMachine.RING_UP,
            $OrbitalLithographyMachine.RING_BACK, $OrbitalLithographyMachine.RING_RADIUS,
            $OrbitalLithographyMachine.RING_THICKNESS, 'front'))
        .hasBER(true)
        // GT would draw the preview for a controller facing north (the platform on its edge): show it facing up;
        // two pages, the basic station and the Array Mk2
        .shapeInfos(definition => $OrbitalLithographyMachine.previews(definition))
})
