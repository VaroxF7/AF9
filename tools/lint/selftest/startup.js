// Self-test fixture (tools/lint/selftest.sh copies this into a scratch tree): every line is a mistake the linters must find.
StartupEvents.registry('item', event => {
    event.create('selftest_orphan').displayName('Orphan')        // R5: taken, never made
    event.create('selftest_dead').displayName('Dead')            // R6: nobody makes or takes it
    event.create('selftest_a').displayName('A')                  // R10: a and b only make each other
    event.create('selftest_b').displayName('B')
    event.create('selftest_nameless')                            // A5: no name, A1: no texture
    event.create('selftest_dead').displayName('Dead again')      // S2: registered twice
})

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    event.create('selftest_nomachine').category('multiblock').setEUIO('in').setMaxIOSize(1, 1, 1, 0)   // M4
    event.create('selftest_run').category('multiblock').setEUIO('in').setMaxIOSize(1, 1, 1, 0)         // R2, R11
    event.create('selftest_single').category('multiblock').setEUIO('in').setMaxIOSize(1, 1, 1, 1)      // R12
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
            .where('C', Predicates.blocks('kubejs:selftest_no_such_block')
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMinGlobalLimited(1))
                .or(Predicates.autoAbilities(true, false)))
            .where('U', Predicates.air())
            .build())
    // R12: single blocks up to MV for a recipe of 2048 EU/t
    event.create('selftest_tiered', 'custom')
        .tiers(GTValues.LV, GTValues.MV)
        .definition((tier, builder) => { builder.recipeTypes([GTRecipeTypes.get('selftest_single')]).langValue('Tiered') })
})
