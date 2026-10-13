// AF9 - Water Filtration Plant: an open stainless basin behind glass that filters dirty water
// to distilled water and skims the floating debris off the top.
// Controller behaviour: AF9 Core (FiltrationPlantMachine). Basin render: FiltrationBasinRender
// (animated water, floating debris, 3D skimmer arm). Recipes: server_scripts/mods/gtceu/water_filtration.js
//
// 5 x 3 x 5 basin, aisles back -> front, rows bottom -> top. The middle 3 x 3 of the middle layer
// is open air (the basin the render draws its water in); the controller sits front-bottom-centre.

const $FiltrationPlantMachine = Java.loadClass('com.af9.core.machine.FiltrationPlantMachine')
const $FiltrationModels = Java.loadClass('com.af9.core.machine.AF9MachineModels')

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    event.create('water_filtration')
        .category('multiblock')
        .setEUIO('in')
        // filter (not consumed) + circuit in, up to 4 debris out, dirty water + culture in, water + reclaim out
        .setMaxIOSize(2, 4, 2, 2)
        .setProgressBar(GuiTextures.PROGRESS_BAR_BATH, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.BATH)
})

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }

    event.create('water_filtration_plant', 'multiblock')
        .machine(holder => new $FiltrationPlantMachine(holder))
        .langValue('Water Filtration Plant')
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('water_filtration')])
        .recipeModifiers([GTRecipeModifiers.PARALLEL_HATCH, GTRecipeModifiers.OC_PERFECT])
        .appearanceBlock(GTBlocks.CASING_STAINLESS_CLEAN)
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.water_filtration_plant.tooltip', 4))
        .pattern(definition => FactoryBlockPattern.start()
            .aisle('XXXXX', 'XGGGX', 'XXXXX')
            .aisle('XXXXX', 'XAAAX', 'XXXXX')
            .aisle('XXXXX', 'XAAAX', 'XXXXX')
            .aisle('XXXXX', 'XAAAX', 'XXXXX')
            .aisle('XXSXX', 'XGGGX', 'XXXXX')
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count))
            .where('X', Predicates.blocks(GTBlocks.CASING_STAINLESS_CLEAN.get())
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1, 0))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
            .where('G', Predicates.blocks('gtceu:tempered_glass'))
            .where('A', Predicates.air())
            .build())
        // stainless casing + LCR overlay, and the basin while it runs: water, debris, skimmer arm
        .model($FiltrationModels.workableCasingWithFiltrationBasin(
            'gtceu:block/casings/solid/machine_casing_clean_stainless_steel',
            'gtceu:block/multiblock/large_chemical_reactor', 1.0, 2.0))
        .hasBER(true)
})
