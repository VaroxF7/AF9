// AF9 - ME Resonance Fabricator: TerraFirmaGreg's ME Assembler (Core-Modern, MEAssemblerMachine and TFGMultiMachines,
// LGPL-3.0; credits in CREDITS-textures.md, licence in licenses/) copied into GTCEu with a structure of AF9's own: a 7 x 9 x 7
// tower of black PTFE casings, stainless steel corner pillars, AE2 casing bands and a quartz vibrant glass window band round
// a Budding Certus in the middle. The grade of the Budding Certus sets the speed (x8 to x512), every run can wear
// the block down a grade, no overclocks, batch mode always on. The ME Redstone Port outputs the grade as a redstone
// signal. Recipe type: gtceu:me_assembler (id as in TFG, so its recipes can be taken over unchanged).
// Controller logic, the port, the budding grades and the recipe page: AF9 Core (MEResonanceFabricatorMachine,
// MERedstonePortPartMachine, BuddingTiers). Blocks: af9:ptfe_black_casing, af9:ae2_machine_casing (AF9Blocks).
//
// Differences from TFG: parts have a maximum only (one energy hatch at most, one maintenance hatch at most), the machine
// has no 'moon only' rule (that sits in TFG's recipes, as a dimension condition), and the port is registered with a tier
// (gtceu:hv_me_assembler_redstone_port).

const $MEFabricator = Java.loadClass('com.af9.core.machine.MEResonanceFabricatorMachine')
const $BuddingTiers = Java.loadClass('com.af9.core.pattern.BuddingTiers')

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    // 9 items in, 1 out, 3 fluids in; the research slot takes a data orb (the Data Access Hatch in the structure)
    event.create('me_assembler')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(9, 1, 3, 0)
        .setProgressBar(GuiTextures.PROGRESS_BAR_ASSEMBLER, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.ASSEMBLER)
        .setHasResearchSlot(true)
})

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }

    // 7 wide x 9 high x 7 deep, aisles back -> front (the controller's last), rows bottom -> top. Layers: base deck (T, all
    // hatches, H the Data Access Hatch), black PTFE ring with the controller (X) in the front, AE2 casing band, three window
    // rows (G) with the Budding Certus (Q) in a glass cube in the middle, a second AE2 band, a PTFE ring and the roof; the
    // corners are stainless steel pillars (B). The walls' inside is any block (spatial storage fits). Parts have a maximum
    // only (setMaxGlobalLimited(max, preview count)).
    event.create('me_assembler', 'multiblock')
        .machine(holder => new $MEFabricator(holder))
        .langValue('ME Resonance Fabricator')
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('me_assembler')])
        .alwaysTryModifyRecipe(true)
        .appearanceBlock(() => Block.getBlock('af9:ae2_machine_casing'))
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.me_fabricator.tooltip', 5))
        .pattern(definition => FactoryBlockPattern.start()
            .aisle(' CHC ', ' C C ', ' C C ', ' C C ')
            .aisle('CCCCC', 'CCGCC', 'CCGCC', ' C C ')
            .aisle('CCPCC', 'CC CC', 'CCGCC', ' C C ')
            .aisle('CCCCC', 'CCGCC', 'CCGCC', ' C C ')
            .aisle(' CXC ', ' C C ', ' C C ', ' C C ')
            .where('X', Predicates.controller(Predicates.blocks(definition.get())))
            .where('P', Predicates.blocks('gtceu:steel_pipe_casing'))
            .where('G', Predicates.blocks('ae2:quartz_vibrant_glass'))
            .where('C', Predicates.blocks('af9:ae2_machine_casing')
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(9, 3))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(3, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(1, 1)))
            .where('H', Predicates.dataHatchPredicate(Predicates.blocks('af9:ptfe_black_casing')))
            .where(' ', Predicates.any())
            .build())
        .shapeInfos(definition => $MEFabricator.previews(definition))
        // black PTFE casings with the laser engraver's controller face, as in TFG
        .workableCasingModel('af9:block/ae2_machine_casing', 'gtceu:block/machines/laser_engraver')
})
