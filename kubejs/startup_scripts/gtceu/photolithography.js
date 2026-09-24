// AF9 - Photolithography Line
// Realistic lithography cluster: a coater/developer track feeding a stepper. Mk I is an i-line (365 nm, mercury lamp)
// tool; modules placed on top of the stepper upgrade it to Mk II (KrF laser) and Mk III (twin-stage scanner).
// Replaces direct laser engraving of chip wafers. Recipes live in server_scripts/mods/gtceu/photolithography.js
// The controller's behaviour (module detection, tier gating, UI buttons) comes from the AF9 Core mod (af9-core/).

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

    // One reticle per chip; graded wafers are what the upgraded line produces
    const chips = [
        { id: 'ilc', name: 'ILC', wafer: 'ilc_wafer' },
        { id: 'ram', name: 'RAM', wafer: 'ram_wafer' },
        { id: 'cpu', name: 'CPU', wafer: 'cpu_wafer' },
        { id: 'ulpic', name: 'ULPIC', wafer: 'ulpic_wafer' },
        { id: 'lpic', name: 'LPIC', wafer: 'lpic_wafer' },
        { id: 'simple_soc', name: 'Simple SoC', wafer: 'simple_soc_wafer' }
    ]
    chips.forEach(chip => {
        allthemods.create(`${chip.id}_reticle`)
            .displayName(`${chip.name} Reticle`)
            .maxStackSize(1)
            .tooltip('Photomask for the Photolithography Line. Not consumed.')
        allthemods.create(`${chip.id}_wafer_high_grade`)
            .displayName(`High-Grade ${chip.name} Wafer`)
            .texture(`gtceu:item/${chip.wafer}`)
            .tooltip('Printed by a Mk II Photolithography Line. Cuts into about 25% more chips.')
        allthemods.create(`${chip.id}_wafer_premium`)
            .displayName(`Premium ${chip.name} Wafer`)
            .texture(`gtceu:item/${chip.wafer}`)
            .glow(true)
            .tooltip('Printed by a Mk III Photolithography Line. Cuts into 50% more chips.')
    })
})

// Upgrade modules: placed on top of the stepper, detected when the structure forms
StartupEvents.registry('block', allthemods => {
    const $SoundType = Java.loadClass('net.minecraft.world.level.block.SoundType')
    allthemods.create('krf_excimer_laser_module')
        .displayName('KrF Excimer Laser Module')
        .soundType($SoundType.METAL)
        .hardness(5)
        .resistance(6)
        .requiresTool(true)
        .tagBlock('minecraft:mineable/pickaxe')
        .tagBlock('minecraft:needs_iron_tool')
    allthemods.create('twin_stage_scanner_module')
        .displayName('Twin-Stage Scanner Module')
        .soundType($SoundType.METAL)
        .hardness(5)
        .resistance(6)
        .requiresTool(true)
        .tagBlock('minecraft:mineable/pickaxe')
        .tagBlock('minecraft:needs_iron_tool')
})

GTCEuStartupEvents.registry('gtceu:recipe_type', allthemods => {
    allthemods.create('photolithography')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(2, 1, 5, 0)
        .setSlotOverlay(false, false, true, GuiTextures.LENS_OVERLAY)
        .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.ELECTROLYZER)
})

GTCEuStartupEvents.registry('gtceu:machine', allthemods => {
    allthemods.create('photolithography_line', 'multiblock')
        .machine(holder => new $PhotolithographyLineMachine(holder))
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes('photolithography')
        // LITHO_GATE: only recipes of the line's operating tier, and only when the hatches can supply their EU/t
        .recipeModifiers([$PhotolithographyLineMachine.LITHO_GATE, GTRecipeModifiers.OC_NON_PERFECT])
        .appearanceBlock(GTBlocks.CASING_STAINLESS_CLEAN)
        ['tooltips(net.minecraft.network.chat.Component[])']([
            Component.translatable('af9.photolithography_line.tooltip.0'),
            Component.translatable('af9.photolithography_line.tooltip.1'),
            Component.translatable('af9.photolithography_line.tooltip.2'),
            Component.translatable('af9.photolithography_line.tooltip.3'),
            Component.translatable('af9.photolithography_line.tooltip.4'),
            Component.translatable('af9.photolithography_line.tooltip.5')])
        // 3 wide x 3 high x 20 long, plus two module slots on top. Aisles run from the back (lamp) to the front
        // (controller); each aisle lists its rows bottom -> middle -> top -> module slot row.
        .pattern(definition => FactoryBlockPattern.start()
            // --- Stepper (exposure tool) ---
            .aisle('CCC', 'CLC', 'CCC', ' A ') // mercury arc lamp illuminator (i-line, 365 nm) + KrF module slot
            .aisle('CCC', 'CRC', 'CCC', '   ') // reticle stage
            .aisle('CCC', 'WTW', 'CCC', '   ') // projection lens
            .aisle('CCC', 'WTW', 'CCC', '   ') // projection lens
            .aisle('CCC', 'WTW', 'CCC', '   ') // projection lens
            .aisle('CRC', 'WRW', 'CCC', ' B ') // wafer XY stage + Twin-Stage module slot
            // --- Coater / developer track ---
            .aisle('CCC', 'CRC', 'CCC', '   ') // track <-> stepper interface
            .aisle('CCC', 'CRC', 'CFC', '   ') // transfer robot
            .aisle('CCC', 'CHC', 'CFC', '   ') // hard bake
            .aisle('CSC', 'WXW', 'CPC', '   ') // developer: rinse
            .aisle('CSC', 'WXW', 'CPC', '   ') // developer: TMAH puddle
            .aisle('CCC', 'CKC', 'CFC', '   ') // chill plate
            .aisle('CCC', 'CHC', 'CFC', '   ') // post-exposure bake
            .aisle('CCC', 'CHC', 'CFC', '   ') // soft bake
            .aisle('CSC', 'WXW', 'CPC', '   ') // spin coater
            .aisle('CSC', 'WXW', 'CPC', '   ') // spin coater: resist dispense
            .aisle('CCC', 'CKC', 'CFC', '   ') // chill plate
            .aisle('CPC', 'WHW', 'CFC', '   ') // HMDS vapour prime oven
            .aisle('CCC', 'CRC', 'CFC', '   ') // transfer robot
            .aisle('III', 'IMI', 'CCC', '   ') // cassette station (wafers in and out) + controller
            .where('M', Predicates.controller(Predicates.blocks(definition.get())))
            // module slots must be air or their module, so placing/removing one re-forms the structure
            .where('A', Predicates.air().or(Predicates.blocks('kubejs:krf_excimer_laser_module')))
            .where('B', Predicates.air().or(Predicates.blocks('kubejs:twin_stage_scanner_module')))
            .where(' ', Predicates.any())
            .where('I', Predicates.blocks('gtceu:clean_machine_casing')
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMinGlobalLimited(1).setMaxGlobalLimited(2))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMinGlobalLimited(1).setMaxGlobalLimited(2)))
            .where('C', Predicates.blocks('gtceu:clean_machine_casing').setMinGlobalLimited(100)
                // two 2A hatches = the 4A every tier draws (MV for Mk I, HV for Mk II, EV for Mk III)
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
