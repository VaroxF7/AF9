// AF9 - Plasma Transformer: condenses superstate plasmas to solid plates at XPS (Star Matter
// Plasma Steel for the NVM boards; the DTPF forges the superstate). Recipes:
// server_scripts/mods/gtceu/plasma_transformer.js
//
// 3 x 4 x 3 condenser, aisles back -> front, rows bottom -> top. Controller front-bottom-centre.

const $PlasmaTransformerMachine = Java.loadClass('com.af9.core.machine.PlasmaTransformerMachine')

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    event.create('plasma_transform')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(2, 2, 2, 0)
        .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.COOLING)
})

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }

    event.create('plasma_transformer', 'multiblock')
        .machine(holder => new $PlasmaTransformerMachine(holder))
        .langValue('Plasma Transformer')
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('plasma_transform')])
        .recipeModifiers([GTRecipeModifiers.OC_PERFECT])
        .appearanceBlock(GTBlocks.CASING_STAINLESS_CLEAN)
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.plasma_transformer.tooltip', 3))
        .pattern(definition => FactoryBlockPattern.start()
            .aisle('XXX', 'XXX', 'XXX', 'XXX')
            .aisle('XXX', 'X#X', 'X#X', 'XXX')
            .aisle('XSX', 'X#X', 'X#X', 'XXX')
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count))
            .where('X', Predicates.blocks(GTBlocks.CASING_STAINLESS_CLEAN.get())
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
            .where('#', Predicates.air())
            .build())
        .workableCasingModel('gtceu:block/casings/solid/machine_casing_clean_stainless_steel',
            'gtceu:block/multiblock/assembly_line')
})
