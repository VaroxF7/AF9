// AF9 - High Temperature Precision Fabricator (TerraFirmaGreg's boule machine, available at MV). The seed crystal on
// its holder rod is dipped into liquid polysilicon in a quartz crucible and pulled: silicon, phosphorus, naquadah and
// neutronium boules. A coil multiblock like the EBF: the recipes need the coil temperature (blastFurnaceTemp).
// Recipes: server_scripts/mods/gtceu/precision_fabricator.js

const $PrecisionFab = Java.loadClass('com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine')

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    // seed, dopant, holder rod, crucible and the circuit in; the polysilicon, a dopant and a gas in
    event.create('high_temperature_precision_fabricator')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(6, 1, 3, 0)
        .setProgressBar(GuiTextures.PROGRESS_BAR_CRYSTALLIZATION, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.FURNACE)
})

GTCEuStartupEvents.registry('gtceu:machine', event => {
    // The EBF's hall (3 x 4 x 3): a heatproof shell round two layers of heating coils with the open shaft the holder
    // rod hangs in. Hatches in the shell, a maximum only, never a required count.
    event.create('high_temperature_precision_fabricator', 'multiblock')
        .langValue('High Temperature Precision Fabricator')
        .machine(holder => new $PrecisionFab(holder))
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('high_temperature_precision_fabricator')])
        .recipeModifiers([GTRecipeModifiers.OC_NON_PERFECT])
        .appearanceBlock(GTBlocks.CASING_INVAR_HEATPROOF)
        ['tooltips(net.minecraft.network.chat.Component[])']([0, 1].map(i =>
            Component.translatable(`af9.high_temperature_precision_fabricator.tooltip.${i}`)))
        .pattern(definition => FactoryBlockPattern.start()
            .aisle('XXX', 'CCC', 'CCC', 'XXX')
            .aisle('XXX', 'C#C', 'C#C', 'XXX')
            .aisle('XSX', 'CCC', 'CCC', 'XXX')
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            .where('X', Predicates.blocks(GTBlocks.CASING_INVAR_HEATPROOF.get())
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(3, 1))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
            .where('C', Predicates.heatingCoils())
            .where('#', Predicates.air())
            .build())
        .workableCasingModel('gtceu:block/casings/solid/machine_casing_heatproof',
            'gtceu:block/multiblock/electric_blast_furnace')
})
