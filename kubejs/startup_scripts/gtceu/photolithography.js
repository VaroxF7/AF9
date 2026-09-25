// AF9 - Photolithography Line and Orbital Lithography Station
// Realistic lithography: a coater/developer track feeding a stepper. One exposure mode per wafer substrate (see
// wafers.js): 350 nm on silicon up to 7 nm on strange matter on the line, 1 nm on chromodynium in orbit. A print is
// always GT's own chip wafer, more of them per blank on a higher substrate; no NBT, no variants.
// Each mode uses the light source its real node used and the resist made for that light:
//   350 nm            mercury-lamp i-line 365 nm             DNQ-novolac resist
//   200 nm            KrF excimer laser 248 nm               chemically amplified PHOST resist
//   100, 80 nm        ArF excimer laser 193 nm (dry)         chemically amplified methacrylate resist
//   65, 50 nm         ArF immersion (water under the lens)   same, + high-k gate from 50 nm
//   20, 7 nm          EUV 13.5 nm (tin plasma), 7 nm high-NA metal-oxide (tin-oxo) EUV resist
//   1 nm              X-ray free-electron laser, in orbit    dry resist cartridges (no spin coating without gravity)
// The line is built in eight versions (longer projection lens, better light source); a version runs the modes up to
// its own and runs lower ones faster and with fewer broken wafers.
// Both machines keep an exposure vacuum (cleanliness 0-100): the pumps raise it while there is energy and no
// maintenance problem, every finished wafer drops it by 10-15, and every print can break: the finer the node and the
// dirtier the vacuum, the likelier. See LithoMachine in af9-core.
// Recipes: server_scripts/mods/gtceu/photolithography.js. Controller behaviour: AF9 Core (af9-core/).

const $PhotolithographyLineMachine = Java.loadClass('com.af9.core.machine.PhotolithographyLineMachine')
const $OrbitalLithographyMachine = Java.loadClass('com.af9.core.machine.OrbitalLithographyMachine')
const $LithoMachine = Java.loadClass('com.af9.core.machine.LithoMachine')
const $LithoCoolantHatch = Java.loadClass('com.af9.core.machine.part.CoolantHatchPartMachine')

GTCEuStartupEvents.registry('gtceu:material', allthemods => {
    // ---- Extreme clean dry air (XCDA) chain, as in a fab's clean-dry-air plant ----
    // air -> catalytic oxidation (CO, H2, hydrocarbons -> CO2 + H2O) -> CO2 scrubbing -> molecular sieve drying
    // -> cryogenic cooling (freezes out the last traces) -> re-warmed through a membrane filter = XCDA
    allthemods.create('oxidized_air')
        .gas()
        .color(0xd8e4ec)

    allthemods.create('decarbonated_air')
        .gas()
        .color(0xdfeefa)

    allthemods.create('dry_air')
        .gas()
        .color(0xe8f4ff)

    // Held just above its liquefaction point (95 K): cryogenic, like liquid air
    allthemods.create('cryogenic_supercooled_air')
        .gas(95)
        .color(0x9fd8ff)

    // Purge gas for the exposure tool
    allthemods.create('extreme_clean_dry_air')
        .gas()
        .color(0xe6f2ff)

    // Room-temperature CO oxidation catalyst (copper manganese spinel, as in gas-mask filters)
    allthemods.create('hopcalite')
        .dust()
        .color(0x2e2a28)
        .formula('CuMn2O4')

    // HMDS adhesion promoter chain: (CH3)2SiCl2 + CH3Cl + Mg -> (CH3)3SiCl + MgCl2
    allthemods.create('trimethylchlorosilane')
        .liquid()
        .color(0xdde3e8)
        .formula('(CH3)3SiCl')

    // 2 (CH3)3SiCl + 3 NH3 -> [(CH3)3Si]2NH + 2 NH4Cl
    allthemods.create('hexamethyldisilazane')
        .liquid()
        .color(0xe3e8d8)
        .formula('((CH3)3Si)2NH')

    // HMDS vapour carried in nitrogen, as fed to a vapour prime oven
    allthemods.create('hmds_vapor')
        .gas()
        .color(0xc9d6e3)
        .formula('((CH3)3Si)2NH(N2)')

    // Positive DNQ-novolac photoresist chain
    // C6H5OH + CH2O -> novolac repeat unit + H2O
    allthemods.create('novolac_resin')
        .liquid()
        .color(0xb5651d)
        .formula('(C7H6O)n')

    // Photoactive compound (simplified, balanced): C10H8 + HNO3 + NH3 -> C10H6N2O + 2 H2O + H2
    allthemods.create('diazonaphthoquinone')
        .dust()
        .color(0xe8c547)
        .formula('C10H6N2O')

    // Novolac + DNQ dissolved in xylene
    allthemods.create('photoresist')
        .liquid()
        .color(0xb04a1f)

    // TMAH developer chain: (CH3)2NH + 2 CH3Cl -> (CH3)4NCl + HCl
    allthemods.create('tetramethylammonium_chloride')
        .dust()
        .color(0xf5f5f0)
        .formula('(CH3)4NCl')

    // Electrolysed out of the chloride (membrane cell), diluted to the industry standard 2.38 %
    allthemods.create('tmah_developer')
        .liquid()
        .color(0xcfe8f0)
        .formula('(CH3)4NOH(H2O)')

    // ---- Metal-oxide EUV resist (20 nm and 7 nm) ----
    // Sn + 2 Cl2 -> SnCl4, the tin source of the tin-oxo clusters
    allthemods.create('tin_tetrachloride')
        .liquid()
        .color(0xe8e8d0)
        .formula('SnCl4')

    // Tin-oxo carboxylate clusters (methacrylate ligands) in PGMEA: dense tin absorbs EUV far better than carbon
    allthemods.create('euv_photoresist')
        .liquid()
        .color(0xd9c27a)
})

StartupEvents.registry('item', allthemods => {
    allthemods.create('photomask_blank').displayName('Chrome-on-Quartz Photomask Blank')

    // Adsorbent for the XCDA dryer; a saturated one is regenerated by baking it in any furnace
    allthemods.create('molecular_sieve')
        .displayName('Molecular Sieve 13X')
        .tooltip('Dries air for extreme clean dry air. Comes out saturated.')
    allthemods.create('saturated_molecular_sieve')
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
        allthemods.create(`${chip.id}_reticle`)
            .displayName(`${chip.name} Reticle`)
            .maxStackSize(1)
            .tooltip('Photomask for the lithography machines. Not consumed.')
    })

    // Without gravity there is no spin coating: the orbital station deposits its tin-oxo resist from the vapour
    allthemods.create('dry_resist_cartridge')
        .displayName('Metal-Oxide Dry Resist Cartridge')
        .texture('kubejs:item/dry_resist_cartridge')
        .tooltip('Vapour-deposition resist for the Orbital Lithography Station. One per wafer.')
})

// Light sources of the line's versions (the purple lamp is the mercury lamp of version 1): the KrF excimer laser allows
// version 2, the ArF excimer laser up to 6, the EUV source up to 8. See PhotolithographyLineMachine in af9-core.
// And the Orbital Lithography Station's casings.
StartupEvents.registry('block', allthemods => {
    const machineBlock = (id, name, light) => allthemods.create(id)
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
    machineBlock('orbital_frame_casing', 'Orbital Frame Casing', 0)
    // electron gun + undulator of the X-ray free-electron laser
    machineBlock('xfel_undulator', 'XFEL Undulator Segment', 0.5)
    machineBlock('maglev_wafer_stage', 'Maglev Wafer Stage', 0.2)
})

// One recipe type per exposure mode; GT turns them into machine modes. Must stay in sync with
// com.af9.core.litho.LithoMode in af9-core.
// Fluid inputs: the five track chemicals, + excimer laser gas from 200 nm, + ultrapure water (immersion) from 65 nm,
// + hafnium tetrachloride (high-k gate) from 50 nm. EUV (20 and 7 nm) needs no laser gas but molten tin and hydrogen
// for the plasma source.
GTCEuStartupEvents.registry('gtceu:recipe_type', allthemods => {
    const lineModes = [['350nm', 5], ['200nm', 6], ['100nm', 6], ['80nm', 6], ['65nm', 7], ['50nm', 8], ['20nm', 8],
        ['7nm', 8]]
    lineModes.forEach(([node, fluids]) => {
        allthemods.create(`lithography_${node}`)
            .category('multiblock')
            .setEUIO('in')
            .setMaxIOSize(2, 2, fluids, 0) // substrate + reticle in; GT's chip wafers + the chanced broken wafer out
            .setSlotOverlay(false, false, true, GuiTextures.LENS_OVERLAY)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
            .setSound(GTSoundEntries.ELECTROLYZER)
    })
    // chromodynium wafer, reticle, dry resist cartridge; supercooled endion from the coolant hatches
    allthemods.create('orbital_lithography')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(3, 2, 1, 0)
        .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.ARC)
})

GTCEuStartupEvents.registry('gtceu:machine', allthemods => {
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }

    allthemods.create('photolithography_line', 'multiblock')
        .machine(holder => new $PhotolithographyLineMachine(holder))
        .rotationState(RotationState.NON_Y_AXIS)
        // machine modes, in order; switch with GT's mode tab or the tiles on the console
        .recipeTypes(['350nm', '200nm', '100nm', '80nm', '65nm', '50nm', '20nm', '7nm']
            .map(node => GTRecipeTypes.get(`lithography_${node}`)))
        // LITHO_GATE: only prints a mode the line's version allows, when the two energy hatches can supply its EU/t
        // STRIP_BROKEN: the chanced broken wafer is only for the recipe viewers, the break roll decides
        // LITHO_VERSION: faster per version above the mode; perfect overclocks above that
        .recipeModifiers([$LithoMachine.LITHO_GATE, $LithoMachine.STRIP_BROKEN,
            $PhotolithographyLineMachine.LITHO_VERSION, GTRecipeModifiers.OC_PERFECT])
        .appearanceBlock(GTBlocks.CASING_STAINLESS_CLEAN)
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.photolithography_line.tooltip', 16))
        // 3 wide x 3 high x 20-27 long. Aisles run from the back (light source) to the front (controller);
        // each aisle lists its rows bottom -> middle -> top.
        // Versions 1-8 like the Assembly Line's length: 3-10 projection-lens slices, and the light source must allow the
        // version (mercury lamp V1, KrF excimer laser V2, ArF excimer laser up to V6, EUV source up to V8). One preview
        // page each.
        .pattern(definition => FactoryBlockPattern.start()
            // --- Stepper (exposure tool) ---
            .aisle('CCC', 'CLC', 'CCC') // light source / illuminator
            .aisle('CCC', 'CRC', 'CCC') // reticle stage
            .aisle('CCC', 'WTW', 'CCC').setRepeatable(3, 10) // projection lens: one slice per version + 2
            .aisle('CRC', 'WRW', 'CCC') // wafer XY stage
            // --- Coater / developer track ---
            .aisle('CCC', 'CRC', 'CCC') // track <-> stepper interface
            .aisle('CCC', 'CRC', 'CFC') // transfer robot
            .aisle('CCC', 'CHC', 'CFC') // hard bake
            .aisle('CSC', 'WXW', 'CPC') // developer: rinse
            .aisle('CSC', 'WXW', 'CPC') // developer: TMAH puddle
            .aisle('CCC', 'CKC', 'CFC') // chill plate
            .aisle('CCC', 'CHC', 'CFC') // post-exposure bake
            .aisle('CCC', 'CHC', 'CFC') // soft bake
            .aisle('CSC', 'WXW', 'CPC') // spin coater
            .aisle('CSC', 'WXW', 'CPC') // spin coater: resist dispense
            .aisle('CCC', 'CKC', 'CFC') // chill plate
            .aisle('CPC', 'WHW', 'CFC') // HMDS vapour prime oven
            .aisle('CCC', 'CRC', 'CFC') // transfer robot
            .aisle('III', 'IMI', 'CCC') // cassette station (wafers in and out) + controller
            .where('M', Predicates.controller(Predicates.blocks(definition.get())))
            // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count))
            .where('I', Predicates.blocks('gtceu:clean_machine_casing')
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2, 1)))
            .where('C', Predicates.blocks('gtceu:clean_machine_casing')
                // up to two normal 2A hatches = 4A, what every print needs; their voltage decides the modes
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 2))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(8, 1))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
            .where('F', Predicates.blocks('gtceu:filter_casing'))                // fan filter units
            .where('R', Predicates.blocks('gtceu:stainless_steel_gearbox'))      // robots and stages
            .where('S', Predicates.blocks('gtceu:steel_gearbox'))                // spin motors
            .where('X', Predicates.blocks('gtceu:inert_machine_casing'))         // PTFE-lined process cups
            .where('P', Predicates.blocks('gtceu:ptfe_pipe_casing'))             // chemical dispense lines
            .where('H', Predicates.blocks('gtceu:cupronickel_coil_block'))       // hot plates
            .where('K', Predicates.blocks('gtceu:frostproof_machine_casing'))    // aluminium chill plates
            .where('T', Predicates.blocks('gtceu:tempered_glass'))               // lens elements
            .where('W', Predicates.blocks('gtceu:cleanroom_glass'))
            .where('L', Predicates.blocks('gtceu:purple_lamp')                   // mercury i-line lamp (V1)
                .or(Predicates.blocks('kubejs:krf_excimer_laser'))                 // V2
                .or(Predicates.blocks('kubejs:arf_excimer_laser'))                 // up to V6
                .or(Predicates.blocks('kubejs:euv_light_source')))                 // up to V8
            .build())
        .shapeInfos(definition => $PhotolithographyLineMachine.versionShapes(definition))
        .workableCasingModel('gtceu:block/casings/solid/machine_casing_clean_stainless_steel',
            'gtceu:block/multiblock/gcym/large_engraving_laser')

    // Orbital Lithography Station: an X-ray free-electron laser (electron gun + superconducting undulator) shining into
    // an exposure chamber open to space, with a maglev wafer stage. Prints only in orbit; laser hatches only (50A of
    // UHV), coolant hatches only (supercooled endion). 7 x 7 x 7, aisles back (XFEL) -> front (controller).
    allthemods.create('orbital_lithography_station', 'multiblock')
        .machine(holder => new $OrbitalLithographyMachine(holder))
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes(GTRecipeTypes.get('orbital_lithography'))
        .recipeModifiers([$LithoMachine.LITHO_GATE, $LithoMachine.STRIP_BROKEN, GTRecipeModifiers.OC_NON_PERFECT])
        .appearanceBlock(() => Block.getBlock('kubejs:orbital_frame_casing'))
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.orbital_lithography_station.tooltip', 10))
        .pattern(definition => FactoryBlockPattern.start()
            .aisle('       ', ' OOOOO ', ' OCCCO ', ' OCUCO ', ' OCCCO ', ' OOOOO ', '       ') // electron gun
            .aisle('       ', ' OOOOO ', ' OCCCO ', ' OCUCO ', ' OCCCO ', ' OOOOO ', '       ') // undulator
            .aisle('       ', ' OOOOO ', ' OCCCO ', ' OCUCO ', ' OCCCO ', ' OOOOO ', '       ') // undulator
            .aisle('OOOOOOO', 'OOOOOOO', 'OOOOOOO', 'OOOUOOO', 'OOOOOOO', 'OOOOOOO', 'OOOOOOO') // beam port
            .aisle('OOOOOOO', 'OGGGGGO', 'OG###GO', 'OG#W#GO', 'OG###GO', 'OGGGGGO', 'OOOOOOO') // exposure chamber
            .aisle('OOOOOOO', 'OGGGGGO', 'OG###GO', 'OG###GO', 'OG###GO', 'OGGGGGO', 'OOOOOOO')
            .aisle('OOOOOOO', 'OOOOOOO', 'OOOOOOO', 'OOOSOOO', 'OOOOOOO', 'OOOOOOO', 'OOOOOOO') // front + controller
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count))
            .where('O', Predicates.blocks('kubejs:orbital_frame_casing')
                .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(4, 1))
                .or(Predicates.abilities($LithoCoolantHatch.COOLANT_INPUT).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
            .where('C', Predicates.blocks('gtceu:superconducting_coil'))   // undulator magnets
            .where('U', Predicates.blocks('kubejs:xfel_undulator'))
            .where('G', Predicates.blocks('gtceu:fusion_glass'))
            .where('W', Predicates.blocks('kubejs:maglev_wafer_stage'))
            .where('#', Predicates.air())                                   // the vacuum of space
            .build())
        .workableCasingModel('kubejs:block/orbital_frame_casing', 'gtceu:block/multiblock/gcym/large_engraving_laser')
})
