// AF9 - Industrial Cutting Machine: the Cutting Machine's recipes (gtceu:cutter) in a walk-in housing, copied after the
// cutting multiblock of GTNH 2.9: purple plated casings, steel frame boxes on the roof, a glass window and, behind it, a
// big saw blade that spins while a recipe runs (af9-core SawBladeRender; the casing texture and the controller's
// front panel are original pixel art, tools/textures/cutter.py). The look follows GT5-Unofficial (LGPL-3.0, see
// CREDITS-textures.md); the geometry is written from scratch. Controller logic: af9-core IndustrialCutterMachine.
//
// 7 wide, 5 high, 5 deep. Rows bottom -> top, aisles back -> front (the controller's last). The chamber is the 5 x 2 x 3
// air block behind the window; the blade stands in its middle slice, 2 behind the controller, its axis along the
// front-back line and its centre on the floor's top (1 block above the controller's base), so only its upper half shows.
// Parts take any casing of the housing, with a maximum only, never a required count (setMaxGlobalLimited(max,
// preview count)).

const $IndustrialCutterMachine = Java.loadClass('com.af9.core.machine.IndustrialCutterMachine')
const $CutterModels = Java.loadClass('com.af9.core.machine.AF9MachineModels')

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }

    event.create('industrial_cutting_machine', 'multiblock')
        .machine(holder => new $IndustrialCutterMachine(holder))
        .langValue('Industrial Cutting Machine')
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('cutter')])
        // a parallel hatch multiplies the cuts; perfect overclocks
        .recipeModifiers([GTRecipeModifiers.PARALLEL_HATCH, GTRecipeModifiers.OC_PERFECT])
        .appearanceBlock(() => Block.getBlock('af9:cutter_casing'))
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.industrial_cutting_machine.tooltip', 3))
        .pattern(definition => FactoryBlockPattern.start()
            .aisle('CCCCCCC', 'CCCCCCC', 'CCCCCCC', 'CCCCCCC', '#FFFFF#')
            .aisle('CCCCCCC', 'CAAAAAC', 'CAAAAAC', 'CCCCCCC', '#######')
            .aisle('CCCCCCC', 'CAAAAAC', 'CAAAAAC', 'CCCCCCC', '#######')
            .aisle('CCCCCCC', 'CAAAAAC', 'CAAAAAC', 'CCCCCCC', '#######')
            .aisle('CCCKCCC', 'CGGGGGC', 'CGGGGGC', 'CCCCCCC', '#FFFFF#')
            .where('K', Predicates.controller(Predicates.blocks(definition.get())))
            .where('G', Predicates.blocks('gtceu:laminated_glass'))
            .where('F', Predicates.blocks('gtceu:steel_frame'))
            .where('A', Predicates.air())
            .where('C', Predicates.blocks('af9:cutter_casing')
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1, 0))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
            .where('#', Predicates.any())
            .build())
        // purple plates with the controller's panel on the front, and the saw blade: 2 behind the controller, its
        // centre 1 above the controller's base (the floor's top), radius 1.85 (af9-core SawBladeRender)
        .model($CutterModels.workableCasingWithSawBlade('af9:block/cutter_casing',
            'gtceu:block/multiblock/industrial_cutter', 2, 1, 1.85))
        .hasBER(true)
})
