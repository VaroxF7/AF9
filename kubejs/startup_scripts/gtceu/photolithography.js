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
const $LithoBusConnector = Java.loadClass('com.af9.core.bus.BusConnectorPartMachine')
const $LithoRelativeDirection = Java.loadClass('com.gregtechceu.gtceu.api.pattern.util.RelativeDirection')
const $LithoMachineModels = Java.loadClass('com.af9.core.machine.AF9MachineModels')
const $LithoSounds = Java.loadClass('com.af9.core.common.AF9Sounds')

GTCEuStartupEvents.registry('gtceu:material', event => {
    // ---- Extreme clean dry air (XCDA) chain, as in a fab's clean-dry-air plant ----
    // air -> catalytic oxidation (CO, H2, hydrocarbons -> CO2 + H2O) -> CO2 scrubbing -> molecular sieve drying
    // -> cryogenic cooling (freezes out the last traces) -> re-warmed through a membrane filter = XCDA
    event.create('oxidized_air')
        .gas()
        .color(0xd8e4ec)

    event.create('decarbonated_air')
        .gas()
        .color(0xdfeefa)

    event.create('dry_air')
        .gas()
        .color(0xe8f4ff)

    // Held just above its liquefaction point (95 K): cryogenic, like liquid air
    event.create('cryogenic_supercooled_air')
        .gas(95)
        .color(0x9fd8ff)

    // Purge gas for the exposure tool
    event.create('extreme_clean_dry_air')
        .gas()
        .color(0xe6f2ff)

    // Room-temperature CO oxidation catalyst (copper manganese spinel, as in gas-mask filters)
    event.create('hopcalite')
        .dust()
        .color(0x2e2a28)
        .formula('CuMn2O4')

    // HMDS adhesion promoter chain: (CH3)2SiCl2 + CH3Cl + Mg -> (CH3)3SiCl + MgCl2
    event.create('trimethylchlorosilane')
        .liquid()
        .color(0xdde3e8)
        .formula('(CH3)3SiCl')

    // 2 (CH3)3SiCl + 3 NH3 -> [(CH3)3Si]2NH + 2 NH4Cl
    event.create('hexamethyldisilazane')
        .liquid()
        .color(0xe3e8d8)
        .formula('((CH3)3Si)2NH')

    // HMDS vapour carried in nitrogen, as fed to a vapour prime oven
    event.create('hmds_vapor')
        .gas()
        .color(0xc9d6e3)
        .formula('((CH3)3Si)2NH(N2)')

    // Positive DNQ-novolac photoresist chain
    // C6H5OH + CH2O -> novolac repeat unit + H2O
    event.create('novolac_resin')
        .liquid()
        .color(0xb5651d)
        .formula('(C7H6O)n')

    // Photoactive compound (simplified, balanced): C10H8 + HNO3 + NH3 -> C10H6N2O + 2 H2O + H2
    event.create('diazonaphthoquinone')
        .dust()
        .color(0xe8c547)
        .formula('C10H6N2O')

    // Novolac + DNQ dissolved in xylene
    event.create('photoresist')
        .liquid()
        .color(0xb04a1f)

    // TMAH developer chain: (CH3)2NH + 2 CH3Cl -> (CH3)4NCl + HCl
    event.create('tetramethylammonium_chloride')
        .dust()
        .color(0xf5f5f0)
        .formula('(CH3)4NCl')

    // Electrolysed out of the chloride (membrane cell), diluted to the industry standard 2.38 %
    event.create('tmah_developer')
        .liquid()
        .color(0xcfe8f0)
        .formula('(CH3)4NOH(H2O)')

    // ---- Metal-oxide EUV resist (20 nm and 7 nm) ----
    // Sn + 2 Cl2 -> SnCl4, the tin source of the tin-oxo clusters
    event.create('tin_tetrachloride')
        .liquid()
        .color(0xe8e8d0)
        .formula('SnCl4')

    // Tin-oxo carboxylate clusters (methacrylate ligands) in PGMEA: dense tin absorbs EUV far better than carbon
    event.create('euv_photoresist')
        .liquid()
        .color(0xd9c27a)
})

StartupEvents.registry('item', event => {
    event.create('photomask_blank').displayName('Chrome-on-Quartz Photomask Blank')

    // Adsorbent for the XCDA dryer; a saturated one is regenerated by baking it in any furnace
    event.create('molecular_sieve')
        .displayName('Molecular Sieve 13X')
        .tooltip('Dries air for extreme clean dry air. Comes out saturated.')
    event.create('saturated_molecular_sieve')
        .displayName('Saturated Molecular Sieve')
        .tooltip('Smelt it to drive the water out and reuse it.')

    // One reticle per printed chip (the derived wafers, Nano CPU ... UHPIC, are made from printed ones and need none)
    const chips = [
        { id: 'ilc', name: 'ILC' },
        { id: 'ram', name: 'RAM' },
        { id: 'cpu', name: 'CPU' },
        { id: 'ulpic', name: 'ULPIC' },
        { id: 'lpic', name: 'LPIC' },
        { id: 'simple_soc', name: 'Simple SoC' },
        { id: 'nand', name: 'NAND' },
        { id: 'nor', name: 'NOR' },
        { id: 'mpic', name: 'PIC' },
        { id: 'soc', name: 'SoC' },
        { id: 'advanced_soc', name: 'ASoC' },
        { id: 'highly_advanced_soc', name: 'HASoC' }
    ]
    chips.forEach(chip => {
        event.create(`${chip.id}_reticle`)
            .displayName(`${chip.name} Reticle`)
            .maxStackSize(1)
            .tooltip('Photomask for the lithography machines. Not consumed.')
    })

    // Without gravity there is no spin coating: the orbital station deposits its tin-oxo resist from the vapour
    event.create('dry_resist_cartridge')
        .displayName('Metal-Oxide Dry Resist Cartridge')
        .texture('kubejs:item/dry_resist_cartridge')
        .tooltip('Vapour-deposition resist for the Orbital Lithography Station. One per wafer.')
})

// Light sources of the line's versions (the purple lamp is the mercury lamp of version 1): the KrF excimer laser allows
// version 2, the ArF excimer laser up to 6, the EUV source up to 8. See PhotolithographyLineMachine in af9-core.
// And the Plascrete Pipe Casing of the MV machines, and the Plascrete Filter Casing.
StartupEvents.registry('block', event => {
    const machineBlock = (id, name, light) => event.create(id)
        .displayName(name)
        .soundType('metal')
        .hardness(5)
        .resistance(6)
        .lightLevel(light)
        .requiresTool(true)
        .tagBlock('minecraft:mineable/pickaxe')
    machineBlock('krf_excimer_laser', 'KrF Excimer Laser', 0)
    machineBlock('arf_excimer_laser', 'ArF Excimer Laser', 0)
    // laser-produced plasma: CO2 laser pulses hit tin droplets, a multilayer collector mirror gathers the 13.5 nm light
    machineBlock('euv_light_source', 'EUV Light Source', 0.6)
    // chemical lines of the MV machines: GT's PTFE Pipe Casing needs PTFE, which only comes at HV
    machineBlock('plascrete_pipe_casing', 'Plascrete Pipe Casing', 0)
    // fan filter unit in a plascrete frame, from MV parts: the lithography machines' ceiling. AF9 Core registers it as a
    // GT cleanroom filter (ISO 5, like GT's Filter Casing), so it also works in a Cleanroom and the SMC machines' roofs
    machineBlock('plascrete_filter_casing', 'Plascrete Filter Casing', 0)
})

// One recipe type per exposure mode; GT turns them into machine modes. Must stay in sync with
// com.af9.core.litho.LithoMode in af9-core.
// Fluid inputs: the five track chemicals, + excimer laser gas from 200 nm, + ultrapure water (immersion) from 65 nm,
// + hafnium tetrachloride (high-k gate) from 50 nm. EUV (20 and 7 nm) needs no laser gas but molten tin and hydrogen
// for the plasma source.
GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    // [node, fluid inputs, item inputs]: substrate + reticle, + the ArF Excimer Laser for 80 and 65 nm (the scanner's
    // laser slot) or the EUV Light Source for 20 and 7 nm (both not consumed)
    // (one fluid slot more than the track chemicals and gases for the BARC of the DUV nodes: 200 to 65 nm)
    const lineModes = [['350nm', 5, 2], ['200nm', 7, 2], ['100nm', 7, 2], ['80nm', 7, 3], ['65nm', 8, 3],
        ['50nm', 8, 2], ['20nm', 8, 3], ['7nm', 8, 3]]
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
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.photolithography_line.tooltip', 16))
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
                .or(Predicates.abilities($LithoBusConnector.BUS_CONNECTOR).setMaxGlobalLimited(1, 0))
                // air cooling: the hatches carry the print's heat (cooling units, see air_conditioning.js)
                .or(Predicates.abilities($LithoAirConditioning.AIR_CONDITIONING).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(8, 1))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
            .where('F', Predicates.blocks('kubejs:plascrete_filter_casing'))     // fan filter units
            .where('R', Predicates.blocks('gtceu:stainless_steel_gearbox'))      // robots and stages
            .where('S', Predicates.blocks('gtceu:steel_gearbox'))                // spin motors
            .where('X', Predicates.blocks('gtceu:solid_machine_casing'))         // process cups (MV: no PTFE yet)
            .where('P', Predicates.blocks('kubejs:plascrete_pipe_casing'))       // chemical dispense lines
            .where('H', Predicates.blocks('gtceu:cupronickel_coil_block'))       // hot plates
            .where('K', Predicates.blocks('gtceu:frostproof_machine_casing'))    // aluminium chill plates
            .where('T', Predicates.blocks('gtceu:tempered_glass'))               // lens elements
            .where('W', Predicates.blocks('gtceu:cleanroom_glass'))
            .where('L', Predicates.blocks('gtceu:purple_lamp')                   // mercury i-line lamp (V1)
                .or(Predicates.blocks('kubejs:krf_excimer_laser'))                 // V2
                .or(Predicates.blocks('kubejs:arf_excimer_laser')))                // V3
            .build())
        .shapeInfos(definition => $PhotolithographyLineMachine.versionShapes(definition))
        .workableCasingModel('gtceu:block/casings/cleanroom/plascrete',
            'gtceu:block/multiblock/gcym/large_engraving_laser')

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
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.photolithography_scanner.tooltip', 9))
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
                .or(Predicates.abilities($LithoBusConnector.BUS_CONNECTOR).setMaxGlobalLimited(1, 0))
                // air cooling: the hatches carry the print's heat (cooling units, see air_conditioning.js)
                .or(Predicates.abilities($LithoAirConditioning.AIR_CONDITIONING).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(8, 1))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
            .where('F', Predicates.blocks('kubejs:plascrete_filter_casing'))     // fan filter units
            .where('G', Predicates.blocks('gtceu:stainless_steel_gearbox'))      // wafer stages
            .where('P', Predicates.blocks('kubejs:plascrete_pipe_casing'))       // track lines
            .where('W', Predicates.blocks('gtceu:cleanroom_glass'))              // windows: the lens slices
            .where('#', Predicates.air())                                        // the tube
            .build())
        .shapeInfos(definition => $PhotolithographyLineMachine.versionShapes(definition,
            $PhotolithographyLineMachine.MK2))
        .workableCasingModel('gtceu:block/casings/cleanroom/plascrete',
            'gtceu:block/multiblock/gcym/large_engraving_laser')

    // Orbital Lithography Station (Mk2 lithography): a 25 x 25 platform, 18 high. The top deck of inert PTFE casing
    // carries the controller and the hatches; under it lie the stress-proof deck plate, the shock-proof exposure deck
    // (cross beams) in a ring of sturdy casing, non-conducting spokes and rims, HSS-S trusses, and the X-ray undulator
    // mast: an HSS-G coil column in HSS-E frames reaching 12 blocks down. Prints 50, 20, 7 and 1 nm, only in orbit
    // (af9-core OrbitalLithographyMachine). Pattern from the sol_array design, unchanged; rows bottom -> top.
    // While it prints, a light ring like GT's fusion ring glows just inside the rim, at the level of the exposure deck
    // (3 below the controller, radius 9.6: clear of the rim, it only crosses the four beams), in the node's colour.
    // The controller faces up out of the top deck (look down when you place it); the station turns with it in any
    // direction. The pattern is laid out for that: rows along the controller's front (up), aisles along its up; with
    // the controller facing up the blocks sit exactly where the old horizontal controller had them. Coolant: every
    // print draws a supercooled fluid through the coolant hatches (af9-core OrbitalLithographyMachine.COOLANT);
    // 7 and 1 nm draw computation (computation hatch) and need their research (data hatch). While switched on and
    // powered its magnetic field gives the space around it normal gravity (Ad Astra; af9-core OrbitalField).
    event.create('orbital_lithography_station', 'multiblock')
        .machine(holder => new $OrbitalLithographyMachine(holder))
        .rotationState(RotationState.ALL)
        .allowExtendedFacing(true)
        .recipeTypes(['lithography_50nm', 'lithography_20nm', 'lithography_7nm', 'orbital_lithography']
            .map(id => GTRecipeTypes.get(id)))
        // LITHO_GATE: only in orbit, with the recipe's full EU/t, a sealed vacuum and (researched prints) a data hatch;
        // STRIP_BROKEN: the break roll decides; COOLANT: adds the coolant (faster with a better one); perfect
        // overclocks; then batch mode (GT's: once overclocked below 5 s, several prints in one run, the coolant too).
        // No parallel hatch.
        .recipeModifiers([$LithoMachine.LITHO_GATE, $LithoMachine.STRIP_BROKEN, $OrbitalLithographyMachine.COOLANT,
            GTRecipeModifiers.OC_PERFECT, GTRecipeModifiers.BATCH_MODE])
        .appearanceBlock(() => Block.getBlock('gtceu:inert_machine_casing'))
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.orbital_lithography_station.tooltip', 13))
        .pattern(definition => FactoryBlockPattern.start($LithoRelativeDirection.RIGHT, $LithoRelativeDirection.FRONT,
            $LithoRelativeDirection.UP)
            .aisle('                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','                         ','                         ','        DDDDDDDDD        ','                         ','                         ','                         ')
            .aisle('                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','            C            ','         CCCCCCC         ','      DDDCCCCCCCDDD      ','         CCCCCCC         ','            C            ','                         ')
            .aisle('                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','            C            ','       CC   C   CC       ','    DDDCC  FHF  CCDDD    ','       CC   C   CC       ','            C            ','                         ')
            .aisle('                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','            C            ','     CC     C     CC     ','   DDCC    FHF    CCDD   ','     CC     C     CC     ','                         ','                         ')
            .aisle('                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','                         ','    C       C       C    ','  DDC      FHF      CDD  ','    C               C    ','            L            ','                         ')
            .aisle('                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','                         ','   C        C        C   ','  DC       FHF       CD  ','   C                 C   ','            L            ','                         ')
            .aisle('                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','                         ','   C        C        C   ',' DDC       FHF       CDD ','   C                 C   ','           LLL           ','                         ')
            .aisle('                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','                         ','  C         C         C  ',' DC        FHF        CD ','  C                   C  ','           LLL           ','            O            ')
            .aisle('                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','                         ','  C         C         C  ','DDC        FHF        CDD','  C        F F        C  ','          LLLLL          ','            O            ')
            .aisle('                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','            C            ',' C          C          C ','DC        HHHHH        CD',' C         F F         C ','         LLLLLLL         ','            O            ')
            .aisle('                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','            A            ','            A            ','            A            ','            C            ',' C         CCC         C ','DC       HHHHHHH       CD',' C         FFF         C ','        LLLLLLLLL        ','           OOO           ')
            .aisle('                         ', '                         ', '                         ', '            A            ','            A            ', '           AAA           ','           AAA           ','           AA            ','            A            ','            A            ','           CBC           ','           CBC           ','           CBC           ',' C        CCBCC        C ','DCFFFFFFFHHHHHHHFFFFFFFCD',' C      FFFF FFFF      C ','      LLLLLLLLLLLLL      ','          OOOOO          ')
            .aisle('            A            ', '            A            ', '            A            ', '           AAA           ','           ABA           ', '           ABA           ','           ABA           ','           ABA           ','           ABA           ','          AABAA          ','          ABBBA          ','          ABBBA          ',' CCC     CCBBBCC     CCC ',' CCCCCCCCCCBBBCCCCCCCCCC ','DCHHHHHHHHHHHHHHHHHHHHHCD',' CCC       F F       CCC ',' CC LLLLLLLLLLLLLLLLL CC ','       OOOOOKOOOOO       ')
            .aisle('                         ', '                         ', '                         ', '            A            ','            A            ', '           AAA           ','           AAA           ','            AA           ','            A            ','            A            ','           CBC           ','           CBC           ','           CBC           ',' C        CCBCC        C ','DCFFFFFFFHHHHHHHFFFFFFFCD',' C      FFFF FFFF      C ','      LLLLLLLLLLLLL      ','          OOOOO          ')
            .aisle('                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','            A            ','            A            ','            A            ','            C            ',' C         CCC         C ','DC       HHHHHHH       CD',' C         FFF         C ','        LLLLLLLLL        ','           OOO           ')
            .aisle('                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','            C            ',' C          C          C ','DC        HHHHH        CD',' C         F F         C ','         LLLLLLL         ','            O            ')
            .aisle('                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','                         ','  C         C         C  ','DDC        FHF        CDD','  C        F F        C  ','          LLLLL          ','            O            ')
            .aisle('                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','                         ','  C         C         C  ',' DC        FHF        CD ','  C                   C  ','           LLL           ','            O            ')
            .aisle('                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','                         ','   C        C        C   ',' DDC       FHF       CDD ','   C                 C   ','           LLL           ','                         ')
            .aisle('                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','                         ','   C        C        C   ','  DC       FHF       CD  ','   C                 C   ','            L            ','                         ')
            .aisle('                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','                         ','    C       C       C    ','  DDC      FHF      CDD  ','    C               C    ','            L            ','                         ')
            .aisle('                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','            C            ','     CC     C     CC     ','   DDCC    FHF    CCDD   ','     CC     C     CC     ','                         ','                         ')
            .aisle('                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','            C            ','       CC   C   CC       ','    DDDCC  FHF  CCDDD    ','       CC   C   CC       ','            C            ','                         ')
            .aisle('                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','            C            ','         CCCCCCC         ','      DDDCCCCCCCDDD      ','         CCCCCCC         ','            C            ','                         ')
            .aisle('                         ', '                         ', '                         ', '                         ','                         ', '                         ','                         ','                         ','                         ','                         ','                         ','                         ','                         ','                         ','        DDDDDDDDD        ','                         ','                         ','                         ')
            .where('K', Predicates.controller(Predicates.blocks(definition.get())))
            .where('A', Predicates.blocks('gtceu:hsse_frame'))
            .where('B', Predicates.blocks('gtceu:hssg_coil_block'))
            .where('D', Predicates.blocks('gtceu:sturdy_machine_casing'))
            .where('F', Predicates.blocks('gtceu:hsss_frame'))
            .where('H', Predicates.blocks('gtceu:shock_proof_cutting_casing'))
            .where('L', Predicates.blocks('gtceu:stress_proof_casing'))
            .where('C', Predicates.blocks('gtceu:nonconducting_casing'))
            // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count)): the track
            // chemicals through fluid input hatches, the supercooled coolant through coolant hatches, computation (7 and
            // 1 nm) through a computation hatch and the 1 nm research through a data hatch, or both over the machine
            // bus through the Bus Connector. Every hatch goes on the PTFE casings of the top deck and nowhere else.
            // The connector counts as a computation and an optical data reception hatch too (GT counts a part against
            // every limit it matches), hence 2 of each: the connector and a hatch of the station's own
            .where('O', Predicates.blocks('gtceu:inert_machine_casing')
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(3, 1))
                .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities($LithoCoolantHatch.COOLANT_INPUT).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.COMPUTATION_DATA_RECEPTION).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.DATA_ACCESS).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.OPTICAL_DATA_RECEPTION).setMaxGlobalLimited(2, 0))
                .or(Predicates.abilities($LithoBusConnector.BUS_CONNECTOR).setMaxGlobalLimited(1, 0))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(8, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setPreviewCount(1))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setPreviewCount(1))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
            .where(' ', Predicates.any())
            .build())
        .workableCasingModel('gtceu:block/casings/solid/machine_casing_inert_ptfe',
            'gtceu:block/multiblock/fusion_reactor')
        // the same model plus the light ring: centre 3 behind the controller (below, as it faces up), radius 9.6,
        // tube 0.25, lying across the controller's front axis. The numbers live in af9-core (RING_*), which also burns
        // whatever touches the lit ring
        .model($LithoMachineModels.workableCasingWithLightRing('gtceu:block/casings/solid/machine_casing_inert_ptfe',
            'gtceu:block/multiblock/fusion_reactor', $OrbitalLithographyMachine.RING_UP,
            $OrbitalLithographyMachine.RING_BACK, $OrbitalLithographyMachine.RING_RADIUS,
            $OrbitalLithographyMachine.RING_THICKNESS, 'front'))
        .hasBER(true)
        // GT would draw the preview for a controller facing north (the platform on its edge): show it facing up
        .shapeInfos(definition => $OrbitalLithographyMachine.previewShapes(definition))
})
