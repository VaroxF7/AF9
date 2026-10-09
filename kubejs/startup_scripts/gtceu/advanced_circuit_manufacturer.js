// AF9 - Advanced Circuit Manufacturer: the circuit plant of the Genesis pack (its
// startup_scripts/Startup/Multiblock Registry/advanced_circuit_manufacturer.js), structure and blocks as they were there.
// It makes the Pico circuits (server_scripts/mods/gtceu/circuits_af9.js); the controller's recipe is in
// server_scripts/mods/gtceu/advanced_circuit_manufacturer.js.
//
// 17 wide, 5 high, 5 deep: a hall of Large-Scale Assembler Casing in the middle (laminated glass front and back, the
// controller under the front window), a line of molybdenum disilicide coils with a PTFE pipe casing at its centre and
// tungsten carbide frames at both ends running through it, tungstensteel gearboxes beside the hall and two wings of
// nonconducting casing. Rows bottom -> top, aisles back -> front (the controller's last).

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    // up to 16 items and 4 fluids in, one item out; computation through the computation hatch
    event.create('advanced_circuit_manufacturer')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(16, 1, 4, 0)
        .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.ASSEMBLER)
})

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }

    event.create('advanced_circuit_manufacturer', 'multiblock')
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('advanced_circuit_manufacturer')])
        // a parallel hatch multiplies the runs; perfect overclocks
        .recipeModifiers([GTRecipeModifiers.PARALLEL_HATCH, GTRecipeModifiers.OC_PERFECT])
        .appearanceBlock(() => Block.getBlock('gtceu:large_scale_assembler_casing'))
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.advanced_circuit_manufacturer.tooltip', 4))
        .pattern(definition => FactoryBlockPattern.start()
            .aisle('#######C#C#######', '######CCCCC######', '#####CCGGGCC#####', '######CCCCC######', '#######C#C#######')
            .aisle('######CCCCC######', '###DDDCCCCCDDD###', '#DD###Y###Y###DD#', '###DDDCCCCCDDD###', '######CCCCC######')
            .aisle('#####CCCCCCC#####', '##DDDD#PBP#DDDD##', 'ZZZZPPPPFPPPPZZZZ', '##DDDD#PBP#DDDD##', '#####CCCCCCC#####')
            .aisle('######CCCCC######', '###DDDCCCCCDDD###', '#DD###Y###Y###DD#', '###DDDCCCCCDDD###', '######CCCCC######')
            .aisle('#######C#C#######', '######CCKCC######', '#####CCGGGCC#####', '######CCCCC######', '#######C#C#######')
            .where('K', Predicates.controller(Predicates.blocks(definition.get())))
            .where('G', Predicates.blocks('gtceu:laminated_glass'))
            .where('Y', Predicates.blocks('gtceu:tungstensteel_gearbox'))
            .where('P', Predicates.blocks('gtceu:molybdenum_disilicide_coil_block'))
            .where('D', Predicates.blocks('gtceu:nonconducting_casing'))
            .where('F', Predicates.blocks('gtceu:ptfe_pipe_casing'))
            .where('Z', Predicates.blocks('gtceu:tungsten_carbide_frame'))
            .where('B', Predicates.blocks('gtceu:assembly_line_grating'))
            // any casing of the hall may be a part; parts have a maximum only, never a required count
            // (setMaxGlobalLimited(max, preview count)). The Pico recipes draw 300 A of UHV: a laser target hatch
            // carries that.
            .where('C', Predicates.blocks('gtceu:large_scale_assembler_casing')
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1, 0))
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(4, 2))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(4, 2))
                .or(Predicates.abilities(PartAbility.COMPUTATION_DATA_RECEPTION).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
            .where('#', Predicates.any())
            .build())
        .workableCasingModel('gtceu:block/casings/gcym/large_scale_assembling_casing',
            'gtceu:block/multiblock/fusion_reactor')
})
