// AF9 - Chemical Plant: a 6 x 4 x 6 plant of real GT fluid pipes (Bronze Huge, Stainless Steel Quadruple, Niobium-Titanium
// Huge), taken 1:1 from Janne's structure file (testing.nbt), block by block with the file's exact pipe connections: the
// pattern asks for the exact pipe and the exact connection mask at every pipe spot (af9-core ChemicalPlantPipes), and the
// controller connects the pipes of an auto-built plant itself while it is unformed. The file's casings (Module Casing, a
// stand-in until the plant gets its own) take the hatches; the controller replaces the casing in the front wall (the file
// had none). Test version: it runs the Chemical Reactor's and the Large Chemical Reactor's recipes.
//
// Rows bottom -> top, aisles back -> front (the controller's last), columns as in the file's x, left to right as seen
// from the front. The file stands in the world with the controller facing SOUTH. A space is anything.

const $ChemicalPlantMachine = Java.loadClass('com.af9.core.machine.ChemicalPlantMachine')
const $ChemPipes = Java.loadClass('com.af9.core.pattern.ChemicalPlantPipes')

GTCEuStartupEvents.registry('gtceu:machine', event => {
    event.create('chemical_plant', 'multiblock')
        .machine(holder => new $ChemicalPlantMachine(holder))
        .langValue('Chemical Plant')
        .rotationState(RotationState.NON_Y_AXIS)
        .allowFlip(false)
        .recipeTypes([GTRecipeTypes.get('chemical_reactor'), GTRecipeTypes.get('large_chemical_reactor')])
        .recipeModifiers([GTRecipeModifiers.PARALLEL_HATCH, GTRecipeModifiers.OC_PERFECT])
        .appearanceBlock(() => Block.getBlock('af9:dtpf_module_casing'))
        .pattern(definition => FactoryBlockPattern.start()
            .aisle(' CC   ', ' CC   ', ' a    ', ' a    ', ' bcde ', '  f   ')
            .aisle(' CCC  ', ' CCC  ', '  g   ', '      ', '    h ', '  i   ')
            .aisle('  CCC ', '  CCC ', '  j a ', '  k a ', '  k l ', '  m   ')
            .aisle('   CC ', '   SC ', '      ', '      ', '      ', '      ')
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            .where('C', Predicates.blocks('af9:dtpf_module_casing')
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(4, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(4, 1))
                .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1, 0))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
            .where('a', $ChemPipes.pipe('gtceu:bronze_huge_fluid_pipe', 3))
            .where('b', $ChemPipes.pipe('gtceu:bronze_huge_fluid_pipe', 33))
            .where('c', $ChemPipes.pipe('gtceu:bronze_huge_fluid_pipe', 50))
            .where('d', $ChemPipes.pipe('gtceu:bronze_huge_fluid_pipe', 48))
            .where('e', $ChemPipes.pipe('gtceu:bronze_huge_fluid_pipe', 24))
            .where('f', $ChemPipes.pipe('gtceu:stainless_steel_quadruple_fluid_pipe', 9))
            .where('g', $ChemPipes.pipe('gtceu:niobium_titanium_huge_fluid_pipe', 9))
            .where('h', $ChemPipes.pipe('gtceu:bronze_huge_fluid_pipe', 12))
            .where('i', $ChemPipes.pipe('gtceu:stainless_steel_quadruple_fluid_pipe', 12))
            .where('j', $ChemPipes.pipe('gtceu:stainless_steel_quadruple_fluid_pipe', 7))
            .where('k', $ChemPipes.pipe('gtceu:stainless_steel_quadruple_fluid_pipe', 3))
            .where('l', $ChemPipes.pipe('gtceu:bronze_huge_fluid_pipe', 5))
            .where('m', $ChemPipes.pipe('gtceu:stainless_steel_quadruple_fluid_pipe', 5))
            .build())
        .workableCasingModel('af9:block/dtpf_module_casing', 'gtceu:block/multiblock/large_chemical_reactor')
})
