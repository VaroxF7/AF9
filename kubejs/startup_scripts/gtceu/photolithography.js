// AF9 - Photolithography Line
// Realistic lithography cluster: a coater/developer track feeding a stepper, with five UV exposure modes (MUV 350 nm
// down to LUV 50 nm). Wafers it prints carry the mode's node and transistor count as NBT.
// Replaces direct laser engraving of chip wafers. Recipes live in server_scripts/mods/gtceu/photolithography.js
// The controller's behaviour (power gate, UI buttons, statistics, wafer tooltips/textures) comes from AF9 Core (af9-core/).

const $PhotolithographyLineMachine = Java.loadClass('com.af9.core.machine.PhotolithographyLineMachine')

GTCEuStartupEvents.registry('gtceu:material', allthemods => {
    // Clean utility gas: compressed air dried over a zeolite molecular sieve, used to purge the exposure tool
    allthemods.create('extreme_clean_dry_air')
        .gas()
        .color(0xe6f2ff)

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
})

StartupEvents.registry('item', allthemods => {
    allthemods.create('photomask_blank').displayName('Chrome-on-Quartz Photomask Blank')

    // One reticle per chip. The wafers themselves are GT's own items; the line adds the mode's node and
    // transistor count to them as NBT (read by AF9 Core for the tooltip and texture).
    const chips = [
        { id: 'ilc', name: 'ILC' },
        { id: 'ram', name: 'RAM' },
        { id: 'cpu', name: 'CPU' },
        { id: 'ulpic', name: 'ULPIC' },
        { id: 'lpic', name: 'LPIC' },
        { id: 'simple_soc', name: 'Simple SoC' }
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
GTCEuStartupEvents.registry('gtceu:recipe_type', allthemods => {
    ['muv', 'huv', 'euv', 'xuv', 'luv'].forEach(mode => {
        allthemods.create(`lithography_${mode}`)
            .category('multiblock')
            .setEUIO('in')
            .setMaxIOSize(2, 1, 5, 0)
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
        ['tooltips(net.minecraft.network.chat.Component[])']([0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
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