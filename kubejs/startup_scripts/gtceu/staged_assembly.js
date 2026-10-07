// AF9 - Staged Assembly: a multiblock that builds its recipes in stages. Spec: this file's tooltips.
// One recipe carries every stage's inputs; the machine (AF9 Core: StagedAssemblyMachine, StagedRecipeLogic)
// runs the stages one after the other and only accepts exactly the running stage's inputs, so each stage is
// fed separately. Recipes: server_scripts/mods/gtceu/staged_assembly.js

const $StagedAssemblyMachine = Java.loadClass('com.af9.core.machine.StagedAssemblyMachine')

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    event.create('staged_assembly')
        .category('multiblock')
        .setEUIO('in')
        // the merged recipe (all stages) has to fit; one stage never needs more than a bus or two
        .setMaxIOSize(9, 2, 4, 0)
        .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.ASSEMBLER)
})

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }

    // Assembly hall: solid steel shell with glass bands, the controller front-bottom-centre.
    // 3 x 3 x 4, aisles back -> front (controller), rows bottom -> top.
    event.create('staged_assembly', 'multiblock')
        .machine(holder => new $StagedAssemblyMachine(holder))
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('staged_assembly')])
        .recipeModifiers([GTRecipeModifiers.OC_PERFECT])
        .appearanceBlock(GTBlocks.CASING_STEEL_SOLID)
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.staged_assembly.tooltip', 4))
        .pattern(definition => FactoryBlockPattern.start()
            .aisle('CCC', 'CCC', 'CCC')
            .aisle('CCC', 'C#C', 'CCC')
            .aisle('CGC', 'G#G', 'CGC')
            .aisle('CSC', 'CCC', 'CCC')
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count))
            .where('C', Predicates.blocks(GTBlocks.CASING_STEEL_SOLID.get())
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 2))
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 2))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
            .where('G', Predicates.blocks('gtceu:tempered_glass'))
            .where('#', Predicates.air())
            .build())
        .workableCasingModel('gtceu:block/casings/solid/machine_casing_solid_steel',
            'gtceu:block/multiblock/assembly_line')
})
