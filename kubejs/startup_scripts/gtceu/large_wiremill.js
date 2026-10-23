// AF9 - Large Wiremill: GCYM's Large Wire Factory (gtceu:large_wiremill, removed in the fork, gtceu-fork patch 0022)
// rebuilt as a walk-around wire drawing machine: a 13 x 5 x 5 box with a one-block shell, in three parts along its
// length: a casing end on the left (3 long), a glass chamber with the machine in it (5 long), a plate of steel frames
// with a hole in the middle (the die), and the casing box with the controller on the right (4 long). The chamber and
// the boxes are air inside (the hollow of every part must stay free): af9-core WireDrawRender draws the wire out of
// the right-hand box, through the die, along the axis, through a turning flyer and onto a take-up spool at the left
// end, with no gap. It runs the Wiremill's recipes and its Spooling mode (SpoolingMode.java), switched with GT's
// mode tab. Controller logic: af9-core LargeWiremillMachine. Docs: docs/large-wiremill.md.
//
// Rows bottom -> top, aisles back -> front (the controller's last), columns left -> right as seen from the front.
// The controller is in the front wall of the right-hand box, third column in from the die (one block further right
// than the first sketch put it); the casings 'D' take the hatches (maximums only, never a required count).

const $LargeWiremillMachine = Java.loadClass('com.af9.core.machine.LargeWiremillMachine')
const $WiremillModels = Java.loadClass('com.af9.core.machine.AF9MachineModels')

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }

    event.create('large_wiremill', 'multiblock')
        .machine(holder => new $LargeWiremillMachine(holder))
        .langValue('Large Wiremill')
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('wiremill'), GTRecipeTypes.get('spooling')])
        .recipeModifiers([GTRecipeModifiers.PARALLEL_HATCH, GTRecipeModifiers.OC_NON_PERFECT_SUBTICK,
            GTRecipeModifiers.BATCH_MODE])
        .appearanceBlock(() => Block.getBlock('gtceu:stress_proof_casing'))
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.large_wiremill.tooltip', 4))
        .pattern(definition => FactoryBlockPattern.start()
            .aisle('DDDFFFFFFDDDD', 'DDDGGGGGFDDDD', 'DDDGGGGGFDDDD', 'DDDGGGGGFDDDD', 'DDDFFFFFFDDDD')
            .aisle('DDDDDDDDFDDDD', 'DAAAAAAAFAAAD', 'DAAAAAAAFAAAD', 'DAAAAAAAFAAAD', 'DDDGGGGGFDDDD')
            .aisle('DDDDDDDDFDDDD', 'DAAAAAAAFAAAD', 'DAAAAAAAAAAAD', 'DAAAAAAAFAAAD', 'DDDGGGGGFDDDD')
            .aisle('DDDDDDDDFDDDD', 'DAAAAAAAFAAAD', 'DAAAAAAAFAAAD', 'DAAAAAAAFAAAD', 'DDDGGGGGFDDDD')
            .aisle('DDDFFFFFFDDDD', 'DDDGGGGGFDDDD', 'DDDGGGGGFDSDD', 'DDDGGGGGFDDDD', 'DDDFFFFFFDDDD')
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            .where('A', Predicates.air())
            .where('G', Predicates.blocks('gtceu:laminated_glass'))
            .where('F', Predicates.blocks('gtceu:steel_frame'))
            .where('D', Predicates.blocks('gtceu:stress_proof_casing')
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1, 0))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
            .build())
        // GCYM's stress-proof casing with the controller's face (animated) and the wire drawing machine
        .model($WiremillModels.workableCasingWithWireDraw('gtceu:block/casings/gcym/stress_proof_casing',
            'gtceu:block/multiblock/large_wiremill'))
        .hasBER(true)
})
