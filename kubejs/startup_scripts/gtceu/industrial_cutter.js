// AF9 - Industrial Cutting Machine: GTNH's Industrial Cutting Machine (GT5-Unofficial, MTEIndustrialCuttingMachine,
// LGPL-3.0; credits in CREDITS-textures.md, licence in licenses/) copied into GTCEu: the same 9 x 4 x 3 structure with
// its purple Cutting Factory Frame casings, frame boxes (tungsten carbide here for GTNH's tantalum carbide), a glass
// window in the front and the back, and in the middle the animated saw blade with the original sprites and spin-up
// (af9-core SawBladeRender). It runs the Cutting Machine's recipes (gtceu:cutter). Controller logic: af9-core
// IndustrialCutterMachine.
//
// Rows bottom -> top, aisles back -> front (the controller's last); GTNH's slices, reversed. The controller sits in the
// front, third column of the shape, one row above the floor; the blade is centred 2.5 columns along from it, 1 block
// behind it, at the floor's top (af9-core renders it: back 1, side 2.5, up 0, 3 x 3 blocks). The hatches go in the
// casings 'D' (maximums only, never a required count: setMaxGlobalLimited(max, preview count)). GTNH's black steel
// sheet metal is a steel frame here. A space in GTNH's shape is anything ('#').

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
            .aisle('#DDDDDDDB', '#DDAAAADB', '#DDAAAADB', '##DBBBBB#')
            .aisle('CDCCCCCC#', 'CDB####C#', 'CD#####C#', 'BCCDDDDD#')
            .aisle('#DDDDDDDB', '#DKAAAADB', '#DDAAAADB', '##DBBBBB#')
            .where('K', Predicates.controller(Predicates.blocks(definition.get())))
            .where('A', Predicates.blocks('gtceu:laminated_glass'))
            .where('B', Predicates.blocks('gtceu:tungsten_carbide_frame'))
            .where('C', Predicates.blocks('gtceu:steel_frame'))
            .where('D', Predicates.blocks('af9:cutter_casing')
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1, 0))
                .or(Predicates.abilities(PartAbility.MUFFLER).setMaxGlobalLimited(1, 0))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
            .where('#', Predicates.any())
            .build())
        // Cutting Factory Frame casings with GTNH's controller face, and the saw blade (af9-core SawBladeRender)
        .model($CutterModels.workableCasingWithSawBlade('af9:block/cutter_casing',
            'gtceu:block/multiblock/industrial_cutter', 1, 2.5, 0, 3))
        .hasBER(true)
})
