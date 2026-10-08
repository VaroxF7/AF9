// AF9 - Void Miner modes: the miner's four areas as recipe types (the machine itself stays GT's: VoidMinerMachine
// takes it over in af9-core). Recipes: server_scripts/mods/gtceu/miner.js. Modes 1-4: Overworld, Nether, End,
// Asteroids; a programmed circuit picks the ore in the area. Drilling fluid in, tenfold raw ore out.
//
// MK2 and MK3 are AF9's own multiblocks, with the structures of GTNH's Void Miners (see the patterns below): every mode gets its own
// recipe type again, suffixed _mk2 / _mk3 with the base type's settings, and the controllers are gtceu:void_miner_mk2
// and gtceu:void_miner_mk3. af9-core's VoidMinerMachine.install() looks those up in the machine registry and attaches
// the classes plus the af9.void_miner_mkX.tooltip.0..3 lines itself, so no tooltips() here. Front textures:
// gtceu:block/multiblock/void_miner_mk2 and .../void_miner_mk3 (kubejs assets, overlay_front*: GTNH's ore drill face).

const $VoidMinerMachineMK2 = Java.loadClass('com.af9.core.machine.VoidMinerMachineMK2')
const $VoidMinerMachineMK3 = Java.loadClass('com.af9.core.machine.VoidMinerMachineMK3')

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    const types = [
        'void_mining_overworld',
        'void_mining_nether',
        'void_mining_end',
        'void_mining_asteroids'
    ]
    // MK2 and MK3 run each mode as its own copy of the type, so their recipes can differ in yield and tier
    // (forEach rather than flatMap: KubeJS runs on Rhino, which may lack the newer array helpers)
    const variants = []
    ;['mk2', 'mk3'].forEach(tier => types.forEach(id => variants.push(`${id}_${tier}`)))
    types.concat(variants).forEach(id => {
        event.create(id)
            .category('multiblock')
            .setEUIO('in')
            .setMaxIOSize(1, 4, 1, 0)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
            .setSound(GTSoundEntries.CHEMICAL)
    })
})

GTCEuStartupEvents.registry('gtceu:machine', event => {
    // MK2 and MK3 have the structures of GTNH's Void Miners (GT5-Unofficial, bwcrossmod.galacticgreg.MTEVoidMiners: the
    // ZPM miner's 9 x 13 x 8 tower and the UV miner's 9 x 16 x 9 one), in AF9's blocks: the mining casings and the
    // bolted casings of the originals (af9-core AF9Blocks), GT frames for GTNH's (naquadah alloy; tritanium for the
    // adamantium). Transcribed by the tool that checked the block counts against the originals' tooltips. Aisles back ->
    // front (the controller's last), rows bottom -> top; a space is anything. The pillar up the middle is the mining pipe,
    // the four legs carry the hatches. Parts have a maximum only, never a required count (setMaxGlobalLimited(max,
    // preview count)); every variant takes drilling fluid, so IMPORT_FLUIDS stays open.
    const MK2_SHAPE = [
        ['   F F   ', '   F F   ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         '],
        ['         ', '   B B   ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         '],
        ['  CCCCC  ', '  CABAC  ', '  CABAC  ', '  CCCCC  ', '  C   C  ', '  C   C  ', '         ', '         ', '         ', '         ', '         ', '         ', '         '],
        ['F C   C F', 'FBA   ABF', '  A   A  ', '  CDADC  ', '   DCD   ', '    C    ', '    C    ', '    C    ', '    C    ', '         ', '         ', '         ', '         '],
        ['  C E C  ', '  B E B  ', '  B E B  ', '  CAEAC  ', '   CEC   ', '   CEC   ', '   CEC   ', '   CEC   ', '   CEC   ', '    C    ', '    C    ', '    C    ', '    C    '],
        ['F C   C F', 'FBA   ABF', '  A   A  ', '  CDADC  ', '   DCD   ', '    C    ', '    C    ', '    C    ', '    C    ', '         ', '         ', '         ', '         '],
        ['  CCCCC  ', '  CABAC  ', '  CASAC  ', '  CCCCC  ', '  C   C  ', '  C   C  ', '         ', '         ', '         ', '         ', '         ', '         ', '         '],
        ['   F F   ', '   F F   ', '   D D   ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         '],
    ]
    const MK3_SHAPE = [
        ['   F F   ', '   F F   ', '   E E   ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         '],
        ['  C   C  ', '         ', '   B B   ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         '],
        [' CCCCCCC ', '  CBBBC  ', '  CBBBC  ', '  CEEEC  ', '  C   C  ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         '],
        ['F C   C F', 'F B   B F', 'EBB   BBE', '  EAAAE  ', '   DDD   ', '   DCD   ', '   DCD   ', '   DCD   ', '   DCD   ', '    C    ', '    C    ', '    C    ', '         ', '         ', '         ', '         '],
        ['  C D C  ', '  B D B  ', '  B D B  ', '  EADAE  ', '   DDD   ', '   CDC   ', '   CDC   ', '   CDC   ', '   CDC   ', '   CDC   ', '   CDC   ', '   CDC   ', '    C    ', '    C    ', '    C    ', '    C    '],
        ['F C   C F', 'F B   B F', 'EBB   BBE', '  EAAAE  ', '   DDD   ', '   DCD   ', '   DCD   ', '   DCD   ', '   DCD   ', '    C    ', '    C    ', '    C    ', '         ', '         ', '         ', '         '],
        [' CCCCCCC ', '  CBBBC  ', '  CBSBC  ', '  CEEEC  ', '  C   C  ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         '],
        ['  C   C  ', '         ', '   B B   ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         '],
        ['   F F   ', '   F F   ', '   E E   ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         ', '         '],
    ]

    const legCasing = block => Predicates.blocks(block)
        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(4, 2))
        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2, 1))
        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(2, 1))
        .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1))
    // chars: S controller, A item pipe casing, B mining casing, C frame, D / E the two bolted casings, F a hatch spot
    const minerPattern = (definition, shape, parts) => {
        let pattern = FactoryBlockPattern.start()
        shape.forEach(aisle => { pattern = pattern.aisle(aisle) })
        pattern = pattern.where('S', Predicates.controller(Predicates.blocks(definition.get())))
            .where(' ', Predicates.any())
        parts.forEach(([ch, predicate]) => { pattern = pattern.where(ch, predicate) })
        return pattern.build()
    }
    const mk2Parts = () => [
        ['A', Predicates.blocks('af9:black_plutonium_item_pipe_casing')],
        ['B', Predicates.blocks('af9:mining_black_plutonium_casing')],
        ['C', Predicates.blocks('gtceu:naquadah_alloy_frame')],
        ['D', Predicates.blocks('af9:bolted_naquadah_alloy_casing')],
        ['E', Predicates.blocks('af9:rebolted_naquadah_alloy_casing')],
        ['F', legCasing('af9:mining_black_plutonium_casing')]]
    const mk3Parts = () => [
        ['A', Predicates.blocks('af9:black_plutonium_item_pipe_casing')],
        ['B', Predicates.blocks('af9:mining_neutronium_casing')],
        ['C', Predicates.blocks('gtceu:tritanium_frame')],
        ['D', Predicates.blocks('af9:rebolted_iridium_casing')],
        ['E', Predicates.blocks('af9:bolted_iridium_casing')],
        ['F', legCasing('af9:mining_neutronium_casing')]]

    // MK2: twice the ore per run as the base miner, twice the power and the speed
    event.create('void_miner_mk2', 'multiblock')
        .machine(holder => new $VoidMinerMachineMK2(holder))
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('void_mining_overworld_mk2'), GTRecipeTypes.get('void_mining_nether_mk2'),
            GTRecipeTypes.get('void_mining_end_mk2'), GTRecipeTypes.get('void_mining_asteroids_mk2')])
        .recipeModifier(GTRecipeModifiers.OC_NON_PERFECT)
        .appearanceBlock(() => Block.getBlock('af9:mining_black_plutonium_casing'))
        .pattern(definition => minerPattern(definition, MK2_SHAPE, mk2Parts()))
        .workableCasingModel('af9:block/mining_black_plutonium_casing', 'gtceu:block/multiblock/void_miner_mk2')

    // MK3: three times the ore per run as the base miner, three times the power and the speed
    event.create('void_miner_mk3', 'multiblock')
        .machine(holder => new $VoidMinerMachineMK3(holder))
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('void_mining_overworld_mk3'), GTRecipeTypes.get('void_mining_nether_mk3'),
            GTRecipeTypes.get('void_mining_end_mk3'), GTRecipeTypes.get('void_mining_asteroids_mk3')])
        .recipeModifier(GTRecipeModifiers.OC_NON_PERFECT)
        .appearanceBlock(() => Block.getBlock('af9:mining_neutronium_casing'))
        .pattern(definition => minerPattern(definition, MK3_SHAPE, mk3Parts()))
        .workableCasingModel('af9:block/mining_neutronium_casing', 'gtceu:block/multiblock/void_miner_mk3')
})
