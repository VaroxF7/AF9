// AF9 - Void Miner modes: the miner's four areas as recipe types (the machine itself stays GT's: VoidMinerMachine
// takes it over in af9-core). Recipes: server_scripts/mods/gtceu/miner.js. Modes 1-4: Overworld, Nether, End,
// Asteroids; a programmed circuit picks the ore in the area. Drilling fluid in, tenfold raw ore out.
//
// MK2 and MK3 are AF9's own multiblocks, same 3 x 3 x 7 shell as the pack's void miner: every mode gets its own
// recipe type again, suffixed _mk2 / _mk3 with the base type's settings, and the controllers are gtceu:void_miner_mk2
// and gtceu:void_miner_mk3. af9-core's VoidMinerMachine.install() looks those up in the machine registry and attaches
// the classes plus the af9.void_miner_mkX.tooltip.0..3 lines itself, so no tooltips() here. Front textures:
// gtceu:block/multiblock/void_miner_mk2 and .../void_miner_mk3 (kubejs assets, overlay_front*).

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
    // Same shell as the pack's void miner: 3 x 3 x 7, aisles back -> front (controller), rows bottom -> top.
    // Every variant takes drilling fluid, so IMPORT_FLUIDS stays open; parts have a maximum only, never a required
    // count (setMaxGlobalLimited(max, preview count)).
    const pattern = definition => FactoryBlockPattern.start()
        .aisle('XXX', '#F#', '#F#', '#F#', '###', '###', '###')
        .aisle('XXX', 'FCF', 'FCF', 'FCF', '#F#', '#F#', '#F#')
        .aisle('XSX', '#F#', '#F#', '#F#', '###', '###', '###')
        .where('S', Predicates.controller(Predicates.blocks(definition.get())))
        .where('X', Predicates.blocks('gtceu:stable_machine_casing')
            .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(4, 2))
            .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
            .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2, 1))
            .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(2, 1))
            .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
        .where('C', Predicates.blocks('gtceu:stable_machine_casing'))
        .where('F', Predicates.blocks('gtceu:titanium_frame'))                    // TagPrefix.frameGt block
        .where('#', Predicates.any())
        .build()

    // MK2: twice the ore per run as the base miner, twice the power and the speed
    event.create('void_miner_mk2', 'multiblock')
        .machine(holder => new $VoidMinerMachineMK2(holder))
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('void_mining_overworld_mk2'), GTRecipeTypes.get('void_mining_nether_mk2'),
            GTRecipeTypes.get('void_mining_end_mk2'), GTRecipeTypes.get('void_mining_asteroids_mk2')])
        .recipeModifier(GTRecipeModifiers.OC_NON_PERFECT)
        .appearanceBlock(GTBlocks.CASING_TITANIUM_STABLE)
        .pattern(pattern)
        .workableCasingModel('gtceu:block/casings/solid/machine_casing_stable_titanium',
            'gtceu:block/multiblock/void_miner_mk2')

    // MK3: three times the ore per run as the base miner, three times the power and the speed
    event.create('void_miner_mk3', 'multiblock')
        .machine(holder => new $VoidMinerMachineMK3(holder))
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('void_mining_overworld_mk3'), GTRecipeTypes.get('void_mining_nether_mk3'),
            GTRecipeTypes.get('void_mining_end_mk3'), GTRecipeTypes.get('void_mining_asteroids_mk3')])
        .recipeModifier(GTRecipeModifiers.OC_NON_PERFECT)
        .appearanceBlock(GTBlocks.CASING_TITANIUM_STABLE)
        .pattern(pattern)
        .workableCasingModel('gtceu:block/casings/solid/machine_casing_stable_titanium',
            'gtceu:block/multiblock/void_miner_mk3')
})
