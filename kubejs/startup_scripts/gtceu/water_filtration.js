// AF9 - Water Filtration Plant: an open watertight basin that filters dirty water through two
// strips of Water Filter Casing to distilled water and skims the floating debris off the top.
// Controller behaviour: AF9 Core (FiltrationPlantMachine). Basin render: FiltrationBasinRender
// (animated water, floating debris, 3D skimmer arm). Recipes: server_scripts/mods/gtceu/water_filtration.js
//
// 10 x 7 x 3 basin (10 deep), aisles back -> front, rows bottom -> top. The floor carries two 1 x 6 strips
// of Water Filter Casing (F); the 8 x 5 above the floor, two layers high, is open air (the basin the
// render draws its water in); the controller sits in the middle of the front wall, middle layer.

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
    const $Watertight = Java.loadClass('com.gregtechceu.gtceu.common.data.GCYMBlocks')
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
        .appearanceBlock($Watertight.CASING_WATERTIGHT)
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.water_filtration_plant.tooltip', 4))
        .pattern(definition => FactoryBlockPattern.start()
            // back wall
            .aisle('XXXXXXX', 'XXXXXXX', 'XXXXXXX')
            // the basin: floor with the two filter strips (F), open water above
            .aisle('XXXXXXX', 'XAAAAAX', 'XAAAAAX')
            .aisle('XXFXFXX', 'XAAAAAX', 'XAAAAAX')
            .aisle('XXFXFXX', 'XAAAAAX', 'XAAAAAX')
            .aisle('XXFXFXX', 'XAAAAAX', 'XAAAAAX')
            .aisle('XXFXFXX', 'XAAAAAX', 'XAAAAAX')
            .aisle('XXFXFXX', 'XAAAAAX', 'XAAAAAX')
            .aisle('XXFXFXX', 'XAAAAAX', 'XAAAAAX')
            .aisle('XXXXXXX', 'XAAAAAX', 'XAAAAAX')
            .aisle('XXXXXXX', 'XXXSXXX', 'XXXXXXX')
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count))
            .where('X', Predicates.blocks($Watertight.CASING_WATERTIGHT.get())
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1, 0))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
            .where('F', Predicates.blocks('af9:water_filter_casing'))
            .where('A', Predicates.air())
            .build())
        // watertight casing + LCR overlay, and the basin while it runs: water, debris, skimmer arm.
        // The water is 5 wide and 8 long, centred 4.5 behind the controller, at the controller's height
        .model($FiltrationModels.workableCasingWithFiltrationBasin(
            'gtceu:block/casings/gcym/watertight_casing',
            'gtceu:block/multiblock/large_chemical_reactor', 0.0, 4.5, 0.0, 5.0, 8.0))
        .hasBER(true)
})
