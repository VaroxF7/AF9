// AF9 - Spooler: winds fine wire into wire spools (gtceu:<material>_wire_spool, the TagPrefix in AF9 Core's
// AF9TagPrefixes). One spool is 64 fine wires, eight ingots of the material; the recipes are in
// server_scripts/mods/gtceu/wire_spools.js.
//
//   single block   LV-UHV, one input (the fine wire tag) and one output (the spool), no fluids
//   Large Spooler  a 3 x 3 x 3 cage of robust tungstensteel casing (IV), the controller in the middle of the front,
//                  tungstensteel frame and pipe casing forming the reel in the middle. Hatches go in the casing.
//                  Parallel hatch, perfect overclocks; the controller recipe is IV.
//
// Both run the recipe type `spooling`. The single block reuses the Wiremill's face (the same coils, a different
// name); the Large Spooler reuses the Large Electric Implosion Compressor's face until it gets its own art.

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    // 64 fine wires in, one spool out; no fluids, so the tank side is empty
    event.create('spooling')
        .setEUIO('in')
        .setMaxIOSize(1, 1, 0, 0)
        .setProgressBar(GuiTextures.PROGRESS_BAR_WIREMILL, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.MOTOR)
})

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }

    // ---- The single blocks ----
    event.create('spooler', 'simple')
        .tiers(GTValues.LV, GTValues.MV, GTValues.HV, GTValues.EV, GTValues.IV, GTValues.LuV, GTValues.ZPM,
            GTValues.UV, GTValues.UHV)
        .addDefaultModel(false)
        .definition((tier, builder) => {
            builder
                .langValue(`${GTValues.VLVH[tier]} Spooler ${GTValues.VLVT[tier]}`)
                .recipeTypes([GTRecipeTypes.get('spooling')])
                .recipeModifiers([GTRecipeModifiers.OC_NON_PERFECT])
                .workableTieredHullModel(GTCEu.id('block/machines/wiremill'))
                ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.spooler.tooltip', 3))
        })

    // ---- The multiblock ----
    event.create('large_spooler', 'multiblock')
        .langValue('Large Spooler')
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('spooling')])
        .recipeModifiers([GTRecipeModifiers.PARALLEL_HATCH, GTRecipeModifiers.OC_PERFECT])
        .appearanceBlock(() => Block.getBlock('gtceu:robust_machine_casing'))
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.large_spooler.tooltip', 4))
        .pattern(definition => FactoryBlockPattern.start()
            .aisle('XXX', 'XCX', 'XXX')
            .aisle('XCX', 'CPC', 'XCX')
            .aisle('XXX', 'XSX', 'XXX')
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count))
            .where('X', Predicates.blocks('gtceu:robust_machine_casing')
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1, 0))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
            .where('C', Predicates.blocks('gtceu:tungsten_steel_frame'))
            .where('P', Predicates.blocks('gtceu:tungstensteel_pipe_casing'))
            .build())
        .workableCasingModel('gtceu:block/casings/solid/machine_casing_robust_tungstensteel',
            'gtceu:block/multiblock/large_electric_implosion_compressor')
})
