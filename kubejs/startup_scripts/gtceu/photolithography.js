// AF9 - Photolithography Line
// Realistic lithography cluster: a coater/developer track feeding a stepper, with five UV exposure modes (MUV 350 nm
// down to LUV 50 nm). Wafers it prints carry the mode's node and transistor count as NBT.
// Each mode uses the light source the real node used, and the resist made for that light:
//   MUV       mercury-lamp i-line 365 nm             DNQ-novolac resist
//   HUV, EUV  KrF excimer laser 248 nm               chemically amplified PHOST resist
//   XUV       ArF excimer laser 193 nm               chemically amplified methacrylate resist
//   LUV       ArF immersion (water film under lens)  same as XUV
// Replaces direct laser engraving of chip wafers. Recipes live in server_scripts/mods/gtceu/photolithography.js
// The controller's behaviour (power gate, UI buttons, statistics, wafer tooltips/textures) comes from AF9 Core (af9-core/).

const $PhotolithographyLineMachine = Java.loadClass('com.af9.core.machine.PhotolithographyLineMachine')

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

    // (CH3)4NCl + KOH -> (CH3)4NOH + KCl, diluted to the industry standard 2.38 %
    allthemods.create('tmah_developer')
        .liquid()
        .color(0xcfe8f0)
        .formula('(CH3)4NOH(H2O)')

    // ---- Excimer laser gas (HUV to LUV) ----
    // Premixes as fabs buy them: about 1 % rare gas and 0.1 % fluorine in a neon buffer. The laser's discharge slowly
    // uses up the fluorine, so the gas is topped up while it runs.
    allthemods.create('krf_excimer_gas')
        .gas()
        .color(0xd6c8f2)
        .formula('(Ne)(Kr)(F2)')

    allthemods.create('arf_excimer_gas')
        .gas()
        .color(0xc6d8f2)
        .formula('(Ne)(Ar)(F2)')

    // ---- Chemically amplified resists (KrF and ArF) ----
    // DNQ-novolac stops working below ~300 nm (novolac turns opaque, DNQ barely bleaches), so deep-UV resists work
    // differently: light frees an acid from a photoacid generator (PAG), and in the post-exposure bake each acid
    // unblocks hundreds of polymer groups.
    // CH4 + SO3 -> CH3SO3H (Grillo process), then Simons electrochemical fluorination with HF
    allthemods.create('trifluoromethanesulfonic_acid')
        .liquid()
        .color(0xe9edf0)
        .formula('CF3SO3H')

    // The PAG: a sulfonium salt that releases triflic acid when a photon hits it
    allthemods.create('triphenylsulfonium_triflate')
        .dust()
        .color(0xf2f0ea)
        .formula('(C6H5)3S(CF3SO3)')

    // KrF polymer: poly(4-hydroxystyrene), from phenol by the Hoechst Celanese route (acylation, hydrogenation,
    // dehydration). Transparent at 248 nm, and its phenol groups carry the acid-labile protection.
    allthemods.create('polyhydroxystyrene')
        .dust()
        .color(0xefe6d2)
        .formula('(C8H8O)n')

    // ArF monomer: aromatic rings absorb 193 nm, so ArF resists are built on methacrylates instead.
    // Acetone cyanohydrin route: acetone + HCN, then sulfuric acid and methanol
    allthemods.create('methyl_methacrylate')
        .liquid()
        .color(0xe4eef2)
        .formula('C5H8O2')

    // ArF polymer: a methacrylate copolymer with acid-labile ester side groups (real ones add adamantyl and lactone
    // groups for etch resistance and adhesion)
    allthemods.create('methacrylate_resin')
        .dust()
        .color(0xdfe8ea)
        .formula('(C5H8O2)n')

    // The standard resist solvent. C3H6 + H2O2 + CH3OH -> PGME + H2O (propylene oxide by the HPPO process)
    allthemods.create('propylene_glycol_methyl_ether')
        .liquid()
        .color(0xe6f0ea)
        .formula('C4H10O2')

    // PGME + CH3COOH -> PGMEA + H2O
    allthemods.create('propylene_glycol_methyl_ether_acetate')
        .liquid()
        .color(0xdcebe6)
        .formula('C6H12O3')

    // Polymer + PAG in PGMEA
    allthemods.create('krf_photoresist')
        .liquid()
        .color(0xd9b45c)

    allthemods.create('arf_photoresist')
        .liquid()
        .color(0xe6dcaa)

    // ---- Immersion (LUV) ----
    // 18 MOhm cm, degassed water: the film between the last lens and the wafer (n = 1.44 at 193 nm) lets the
    // lens reach NA 1.35
    allthemods.create('ultrapure_water')
        .liquid()
        .color(0x8cc4ff)
        .formula('H2O')
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

    // One reticle per printed chip (derived wafers like Nano CPU need none). The wafers themselves are GT's own items;
    // the line adds the mode's node and transistor count to them as NBT (read by AF9 Core for tooltip and texture).
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
            .tooltip('Photomask for the Photolithography Line. Not consumed.')
    })
})

// One recipe type per exposure mode; GT turns them into the line's machine modes.
// Must stay in sync with com.af9.core.litho.LithoMode in af9-core.
// Fluid inputs: the five track chemicals, + excimer laser gas from HUV on, + ultrapure water (immersion) and hafnium
// tetrachloride (high-k gate) in LUV.
GTCEuStartupEvents.registry('gtceu:recipe_type', allthemods => {
    const fluidInputs = { muv: 5, huv: 6, euv: 6, xuv: 6, luv: 8 }
    Object.keys(fluidInputs).forEach(mode => {
        allthemods.create(`lithography_${mode}`)
            .category('multiblock')
            .setEUIO('in')
            .setMaxIOSize(2, 1, fluidInputs[mode], 0)
            .setSlotOverlay(false, false, true, GuiTextures.LENS_OVERLAY)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
            .setSound(GTSoundEntries.ELECTROLYZER)
    })
})

GTCEuStartupEvents.registry('gtceu:machine', allthemods => {
    allthemods.create('photolithography_line', 'multiblock')
        .machine(holder => new $PhotolithographyLineMachine(holder))
        .rotationState(RotationState.NON_Y_AXIS)
        // machine modes, in order; switch with GT's mode tab or the buttons in the controller display
        .recipeTypes(['muv', 'huv', 'euv', 'xuv', 'luv'].map(mode => GTRecipeTypes.get(`lithography_${mode}`)))
        // LITHO_GATE: only starts a recipe when the two energy hatches can supply its EU/t; perfect overclocks above that
        .recipeModifiers([$PhotolithographyLineMachine.LITHO_GATE, GTRecipeModifiers.OC_PERFECT])
        .appearanceBlock(GTBlocks.CASING_STAINLESS_CLEAN)
        ['tooltips(net.minecraft.network.chat.Component[])']([0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14]
            .map(i => Component.translatable(`af9.photolithography_line.tooltip.${i}`)))
        // 3 wide x 3 high x 20 long. Aisles run from the back (light source) to the front (controller);
        // each aisle lists its rows bottom -> middle -> top.
        .pattern(definition => FactoryBlockPattern.start()
            // --- Stepper (exposure tool) ---
            .aisle('CCC', 'CLC', 'CCC') // UV light source / illuminator
            .aisle('CCC', 'CRC', 'CCC') // reticle stage
            .aisle('CCC', 'WTW', 'CCC') // projection lens
            .aisle('CCC', 'WTW', 'CCC') // projection lens
            .aisle('CCC', 'WTW', 'CCC') // projection lens
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
            .where('I', Predicates.blocks('gtceu:clean_machine_casing')
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMinGlobalLimited(1).setMaxGlobalLimited(2))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMinGlobalLimited(1).setMaxGlobalLimited(2)))
            .where('C', Predicates.blocks('gtceu:clean_machine_casing').setMinGlobalLimited(100)
                // two 2A hatches = 4A; their voltage decides which modes have enough power
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setExactLimit(2))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMinGlobalLimited(1))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1)))
            .where('F', Predicates.blocks('gtceu:filter_casing'))                // fan filter units
            .where('R', Predicates.blocks('gtceu:stainless_steel_gearbox'))      // robots and stages
            .where('S', Predicates.blocks('gtceu:steel_gearbox'))                // spin motors
            .where('X', Predicates.blocks('gtceu:inert_machine_casing'))         // PTFE-lined process cups
            .where('P', Predicates.blocks('gtceu:ptfe_pipe_casing'))             // chemical dispense lines
            .where('H', Predicates.blocks('gtceu:cupronickel_coil_block'))       // hot plates
            .where('K', Predicates.blocks('gtceu:frostproof_machine_casing'))    // aluminium chill plates
            .where('T', Predicates.blocks('gtceu:tempered_glass'))               // lens elements
            .where('W', Predicates.blocks('gtceu:cleanroom_glass'))
            .where('L', Predicates.blocks('gtceu:purple_lamp'))
            .build())
        .workableCasingModel('gtceu:block/casings/solid/machine_casing_clean_stainless_steel',
            'gtceu:block/multiblock/gcym/large_engraving_laser')
})