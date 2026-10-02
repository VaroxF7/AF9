// AF9 - The Microverse Projector: every ore GregTech has is farmable (after Nomifactory's Microverse Projector). Items and
// the machine here; the ores are listed by af9-core (com.af9.core.microverse.OreCatalog) and their recipes made in
// server_scripts/mods/gtceu/microverse.js. Spec: docs/microverse.md
//
// A Microverse Core of the right tier (not used up) projects a world's rock into the chamber; a Miner Drone of the tier
// (used up) works it; a dust of the ore (not used up, the seed) says which ore: out come 16 raw ores.

const $MICROVERSES = [
    // tier, world, what it holds
    [1, 'Overworld', 'the stone and deepslate ores'],
    [2, 'Nether', 'the netherrack ores'],
    [3, 'End', 'the end stone ores'],
    [4, 'Asteroid', 'the ores of the Asteroid Field and every ore no vein holds']
]

StartupEvents.registry('item', event => {
    $MICROVERSES.forEach(([tier, world, holds]) => {
        event.create(`microverse_core_mk${tier}`)
            .displayName(`Microverse Core Mk${tier}`)
            .tooltip(`A pocket ${world.toLowerCase()} for the Microverse Projector: ${holds}.`)
            .tooltip('Not used up. The ore comes from the dust of it in the input bus.')
        event.create(`miner_drone_mk${tier}`)
            .displayName(`Miner Drone Mk${tier}`)
            .tooltip(`Works a Mk${tier} Microverse: one drone, 16 raw ores. Used up.`)
    })
})

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    // a Miner Drone (used up), a Microverse Core and a seed dust (not used up) in, raw ore out
    event.create('microverse_mining')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(3, 1, 0, 0)
        .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.ARC)
})

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const $MicroverseDirection = Java.loadClass('com.gregtechceu.gtceu.api.pattern.util.RelativeDirection')

    // Microverse Projector: 5 x 5 x 5. A cube of fusion glass 3 x 3 x 3 in the middle of fusion casings, a
    // superconducting coil at its centre (the point the microverse is projected to). Aisles front (controller) -> back,
    // rows bottom -> top. Hatches on any casing.
    event.create('microverse_projector', 'multiblock')
        .langValue('Microverse Projector')
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('microverse_mining')])
        .recipeModifiers([GTRecipeModifiers.PARALLEL_HATCH, GTRecipeModifiers.OC_NON_PERFECT])
        .appearanceBlock(GTBlocks.FUSION_CASING)
        ['tooltips(net.minecraft.network.chat.Component[])']([0, 1, 2, 3].map(i =>
            Component.translatable(`af9.microverse_projector.tooltip.${i}`)))
        .pattern(definition => FactoryBlockPattern.start($MicroverseDirection.LEFT, $MicroverseDirection.UP,
            $MicroverseDirection.BACK)
            .aisle('CCCCC', 'CCCCC', 'CCSCC', 'CCCCC', 'CCCCC')   // front: the controller
            .aisle('CCCCC', 'CGGGC', 'CGGGC', 'CGGGC', 'CCCCC')
            .aisle('CCCCC', 'CGGGC', 'CGLGC', 'CGGGC', 'CCCCC')   // the middle: the coil
            .aisle('CCCCC', 'CGGGC', 'CGGGC', 'CGGGC', 'CCCCC')
            .aisle('CCCCC', 'CCCCC', 'CCCCC', 'CCCCC', 'CCCCC')   // back
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            .where('G', Predicates.blocks('gtceu:fusion_glass'))
            .where('L', Predicates.blocks('gtceu:superconducting_coil'))
            // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count))
            .where('C', Predicates.blocks('gtceu:fusion_casing')
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1, 0))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
            .build())
        .workableCasingModel('gtceu:block/casings/fusion/fusion_casing', 'gtceu:block/multiblock/data_bank')
})
