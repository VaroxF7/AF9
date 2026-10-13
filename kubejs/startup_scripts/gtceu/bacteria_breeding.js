// AF9 - Bacteria Breeding Station: the upgrade sub multi of the Water Filtration Plant. Biochaff
// fed here grows the bacterial cultures the plant's LuV/ZPM biosorption modes run on (circuits 9
// and 10 there): plain culture first, enriched culture out of it. Recipes:
// server_scripts/mods/gtceu/bacteria_breeding.js
//
// 5 x 4 x 5 station, aisles back -> front, rows bottom -> top. Controller front-bottom-centre.

const $BreedingStationMachine = Java.loadClass('com.af9.core.machine.BacteriaBreedingStationMachine')

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    event.create('bacteria_breeding')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(2, 0, 2, 1)
        .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.CHEMICAL)
})

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }

    event.create('bacteria_breeding_station', 'multiblock')
        .machine(holder => new $BreedingStationMachine(holder))
        .langValue('Bacteria Breeding Station')
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('bacteria_breeding')])
        .recipeModifiers([GTRecipeModifiers.OC_PERFECT])
        .appearanceBlock(GTBlocks.CASING_STAINLESS_CLEAN)
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.bacteria_breeding_station.tooltip', 3))
        .pattern(definition => FactoryBlockPattern.start()
            .aisle('XXXXX', 'XXXXX', 'XXXXX', 'XXXXX')
            .aisle('XXXXX', 'XGGGX', 'XGGGX', 'XXXXX')
            .aisle('XXXXX', 'XGAGX', 'XGAGX', 'XXXXX')
            .aisle('XXXXX', 'XGGGX', 'XGGGX', 'XXXXX')
            .aisle('XXSXX', 'XGGGX', 'XGGGX', 'XXXXX')
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count))
            .where('X', Predicates.blocks(GTBlocks.CASING_STAINLESS_CLEAN.get())
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
            .where('G', Predicates.blocks('gtceu:tempered_glass'))
            .where('A', Predicates.air())
            .build())
        .workableCasingModel('gtceu:block/casings/solid/machine_casing_clean_stainless_steel',
            'gtceu:block/multiblock/large_chemical_reactor')
})
