// AF9 - Distillation Facility: the IV Distillation Tower of the pack. A 10 x 7 x 15 walk-through of real GT fluid pipes (Tungsten
// and Stainless Steel Small Fluid Pipes) round three Distillation Tower Modules, taken 1:1 from Janne's structure file (v3.nbt)
// block by block, with the file's exact pipe connections (af9-core DistillationFacilityPipes, as in the Chemical Plant): the pattern
// asks for the exact pipe and the exact connection mask at every pipe spot, and the controller connects the pipes of an
// auto-built facility itself while it is unformed.
//
// The controller stands where the file's green wool stood, in the arch at the front. The hatches go on the Turbine Casing spine
// that runs down the middle of the floor (up to six energy hatches, one maintenance hatch, fluid and item hatches). The three
// controllers where the file had Distillation Towers are Distillation Tower Modules (gtceu:distillation_module), small
// multiblocks of their own: each has its own recipe (mode tab, recipe lock), at most one Parallel Hatch in its casings and its muffler
// hatch on top, and runs on the facility's hatches. Behind each tower a rotor holder (EV or better) holds the rotor that speeds it up
// and saves energy. The facility is its own clean room.
//
// Modes (the Distillation Tower's three): Distillation Tower, Distillation, Cryogenic Rectification. Perfect overclock, no batch mode.
// Docs: docs/distillation-facility.md.
//
// Rows bottom -> top (the file's y 1 to 7), aisles back -> front (the file's z 0 to 14), columns as in the file's x (0 to 9), left to
// right as seen from the front. The file stands in the world with the controller facing SOUTH. A space is anything.
//
//   S controller (the green wool)         D a Distillation Tower Module           R a rotor holder, EV or better
//   M Turbine Casing: the hatch spot      U a muffler hatch                       K Kanthal Coil Block
//   C Clean Casing  c Clean Casing of a tower (one Parallel Hatch a tower may stand here)
//   I Inert Casing  T Stable Casing  F Stainless Firebox Casing  H HSLA Steel Frame  A air (the tower's shaft)
//   a b d e f g h   the pipes (see the where() lines)

const $DistillationFacility = Java.loadClass('com.af9.core.distillation.DistillationFacilityMachine')
const $DistillationModule = Java.loadClass('com.af9.core.distillation.DistillationModuleMachine')
const $FacilityPipes = Java.loadClass('com.af9.core.pattern.DistillationFacilityPipes')
const $DistillationDirection = Java.loadClass('com.gregtechceu.gtceu.api.pattern.util.RelativeDirection')

GTCEuStartupEvents.registry('gtceu:machine', event => {
    // the modes: the Distillation Tower's recipes and the fab chemistry's two separations (af9-core FabModes gives the tower the same)
    const modes = () => [GTRecipeTypes.get('distillation_tower'), GTRecipeTypes.get('fab_distillation'),
        GTRecipeTypes.get('fab_cryogenic_rectification')]

    // ---- the tower: a module of its own, in the Distillation Tower's shape with a kanthal coil at its foot. Its structure is the
    // casings round it; its recipe is its own, what it runs on (energy, inputs, outputs, rotor, clean room) is the facility's.
    // The tower is 5 or 6 high (the file's towers are 6, 5 and 6): the shaft is 2 or 3 layers.
    event.create('distillation_module', 'multiblock')
        .machine(holder => new $DistillationModule(holder))
        .langValue('Distillation Tower Module')
        .rotationState(RotationState.NON_Y_AXIS)
        .allowFlip(false)
        .recipeTypes(modes())
        // no batch mode: parallels from the hatch, the rotor's speed and energy, a perfect overclock
        .recipeModifiers([$DistillationModule.TOWER_PARALLEL, $DistillationModule.FACILITY, GTRecipeModifiers.OC_PERFECT])
        .appearanceBlock(() => Block.getBlock('gtceu:clean_machine_casing'))
        .pattern(definition => FactoryBlockPattern.start($DistillationDirection.RIGHT, $DistillationDirection.BACK, $DistillationDirection.UP)
            .aisle('CSC', 'CKC', 'CCC')
            .aisle('CCC', 'CKC', 'CCC')
            .aisle('CCC', 'CAC', 'CCC').setRepeatable(2, 3)
            .aisle('CCC', 'CUC', 'CCC')
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            .where('C', Predicates.blocks('gtceu:clean_machine_casing')
                .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1, 0)))
            .where('K', Predicates.blocks('gtceu:kanthal_coil_block'))
            .where('A', Predicates.air())
            .where('U', Predicates.abilities(PartAbility.MUFFLER).setMaxGlobalLimited(1, 1))
            .build())
        .workableCasingModel('gtceu:block/casings/solid/machine_casing_clean_stainless_steel',
            'gtceu:block/multiblock/distillation_tower')

    // ---- the facility
    event.create('distillation_facility', 'multiblock')
        .machine(holder => new $DistillationFacility(holder))
        .langValue('Distillation Facility')
        .rotationState(RotationState.NON_Y_AXIS)
        .allowFlip(false)
        // the towers run the recipes; these are the facility's categories in the recipe viewers
        .recipeTypes(modes())
        .recipeModifiers([GTRecipeModifiers.OC_PERFECT])
        .appearanceBlock(() => Block.getBlock('gtceu:inert_machine_casing'))
        .pattern(definition => FactoryBlockPattern.start()
            .aisle('   I I    ', '   I I    ', '   III    ', '          ', '          ', '          ', '          ')
            .aisle('   IMI    ', '   IMI    ', '   III    ', '          ', '          ', '          ', '          ')
            .aisle('FFFIMITTTT', 'cccIIITTTT', 'cccIIIa   ', 'cccbjbd   ', 'ccc       ', 'ccc       ', 'ccc       ')
            .aisle('FCCIMITTTT', 'DKcIIITTRT', 'cKcIIIe   ', 'cAcfkfg   ', 'cAc       ', 'cAc       ', 'cUc       ')
            .aisle('FFFIMITTTT', 'cccIIITTTT', 'cccIIIe   ', 'cccfkfg   ', 'ccc       ', 'ccc       ', 'ccc       ')
            .aisle('   IMI    ', '   III    ', '   III    ', '          ', '          ', '          ', '          ')
            .aisle('FFFIMITTTT', 'cccIIITTTT', 'cccIIIe   ', 'ccchkfg   ', 'cccg      ', 'ccc       ', '          ')
            .aisle('FCCIMITTTT', 'DKcIIITTRT', 'cKcIIIa   ', 'cAcbjbd   ', 'cAc       ', 'cUc       ', '          ')
            .aisle('FFFIMITTTT', 'cccIIITTTT', 'cccIIIe   ', 'cccfkfg   ', 'ccc       ', 'ccc       ', '          ')
            .aisle('   IMI    ', '   III    ', '   III    ', '          ', '          ', '          ', '          ')
            .aisle('FFFIMITTTT', 'cccIIITTTT', 'cccIIIe   ', 'cccfkfg   ', 'ccc H     ', 'ccc       ', 'ccc       ')
            .aisle('FCCIMITTTT', 'DKcIIITTRT', 'cKcIIIa   ', 'cAc H a   ', 'cAcbjbd   ', 'cAc       ', 'cUc       ')
            .aisle('FFFIMITTTT', 'cccIIITTTT', 'cccIIIa   ', 'cccbjbd   ', 'ccc H     ', 'ccc       ', 'ccc       ')
            .aisle('   IMI    ', '   ISI    ', '   III    ', '          ', '          ', '          ', '          ')
            .aisle('   I I    ', '   I I    ', '   III    ', '          ', '          ', '          ', '          ')
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            .where('D', $DistillationFacility.modules())
            .where('R', $DistillationFacility.rotorHolders())
            .where('I', Predicates.blocks('gtceu:inert_machine_casing'))
            .where('T', Predicates.blocks('gtceu:stable_machine_casing'))
            .where('F', Predicates.blocks('af9:stainless_firebox_casing'))
            .where('C', Predicates.blocks('gtceu:clean_machine_casing'))
            .where('c', Predicates.blocks('gtceu:clean_machine_casing')
                .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(3, 0)))
            .where('K', Predicates.blocks('gtceu:kanthal_coil_block'))
            .where('H', Predicates.blocks('gtceu:hsla_steel_frame'))
            .where('A', Predicates.air())
            .where('U', Predicates.abilities(PartAbility.MUFFLER).setMaxGlobalLimited(3, 3))
            // the hatch spine: the Turbine Casings of the file
            .where('M', Predicates.blocks('gtceu:steel_turbine_casing')
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(6, 2))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(3, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(8, 4))
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 0))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2, 0))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
            // the pipes, with the file's exact connection masks (bit 1 << Direction ordinal, controller facing SOUTH)
            .where('a', $FacilityPipes.pipe('gtceu:tungsten_small_fluid_pipe', 3))
            .where('b', $FacilityPipes.pipe('gtceu:tungsten_small_fluid_pipe', 48))
            .where('d', $FacilityPipes.pipe('gtceu:tungsten_small_fluid_pipe', 17))
            .where('e', $FacilityPipes.pipe('gtceu:stainless_steel_small_fluid_pipe', 3))
            .where('f', $FacilityPipes.pipe('gtceu:stainless_steel_small_fluid_pipe', 48))
            .where('g', $FacilityPipes.pipe('gtceu:stainless_steel_small_fluid_pipe', 17))
            .where('h', $FacilityPipes.pipe('gtceu:stainless_steel_small_fluid_pipe', 34))
            // the pipes that sit in a HSLA Steel Frame (a frame box on the pipe)
            .where('j', $FacilityPipes.pipe('gtceu:tungsten_small_fluid_pipe', 48, true))
            .where('k', $FacilityPipes.pipe('gtceu:stainless_steel_small_fluid_pipe', 48, true))
            .build())
        .workableCasingModel('gtceu:block/casings/solid/machine_casing_inert_ptfe',
            'gtceu:block/multiblock/distillation_tower')
})
