// Self-test fixture (tools/lint/selftest.sh copies this into a scratch tree): every line is a mistake the linters must find.
// The items the recipes of server.js name (af9:selftest_orphan ...) are put into the scratch tree's registry list by selftest.sh.
StartupEvents.registry('item', event => {
    event.create('selftest_kubejs_item').displayName('KubeJS item')   // S4: AF9's items are registered by AF9 Core
    for (let i = 0; i < 2; i++) {
        const selftestKept = i                                   // S3: a const in a loop's body (Rhino keeps the first)
    }
    Math.max(...[1, 2])                                          // S6: spread syntax (Rhino does not parse it)
    Array.from({ length: 2 }).forEach(() => {})                  // S7: Rhino leaves holes, the body never runs
})

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    event.create('selftest_nomachine').category('multiblock').setEUIO('in').setMaxIOSize(1, 1, 1, 0)   // M4
    event.create('selftest_run').category('multiblock').setEUIO('in').setMaxIOSize(1, 1, 1, 0)         // R2, R11
    event.create('selftest_single').category('multiblock').setEUIO('in').setMaxIOSize(1, 1, 1, 1)      // R12
    event.create('selftest_single').category('multiblock').setEUIO('in').setMaxIOSize(1, 1, 1, 1)      // S2: registered twice
})

GTCEuStartupEvents.registry('gtceu:machine', event => {
    // M1: rows of different width, a character without where(), a minimum, autoAbilities, an unused where(); M3: a block nobody defines
    event.create('selftest_multi', 'multiblock')
        .recipeTypes([GTRecipeTypes.get('selftest_run')])
        .appearanceBlock(() => Block.getBlock('gtceu:solid_machine_casing'))
        .pattern(definition => FactoryBlockPattern.start()
            .aisle('CCC', 'CCC')
            .aisle('CS', 'CCC')
            .aisle('CZC', 'CCC')
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            .where('C', Predicates.blocks('af9:selftest_no_such_block')
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMinGlobalLimited(1))
                .or(Predicates.autoAbilities(true, false)))
            .where('U', Predicates.air())
            .build())
    // R12: single blocks up to MV for a recipe of 2048 EU/t
    event.create('selftest_tiered', 'custom')
        .tiers(GTValues.LV, GTValues.MV)
        .definition((tier, builder) => { builder.recipeTypes([GTRecipeTypes.get('selftest_single')]).langValue('Tiered') })
})
