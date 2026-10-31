// AF9 - the High Temperature Precision Fabricator and TerraFirmaGreg's boule recipes, as they are in TFG
// (Modpack-Modern, recipes.boules.js, LGPL-3.0): the silane line, polysilicon as a melt, the seed crystal, the
// quartz crucible and the four boules. Machine: startup_scripts/gtceu/precision_fabricator.js (MV).
// The Precision Fabricator Input Port has three item slots, a slot for one non-consumable and the ghost circuit, so a
// recipe takes the crucible (chanced) and two more items at most: the holder rod is kept (non-consumable), the
// dopants a boule's own material replaces are dropped, strange matter's neutronium comes as a melt. No coil heat: the
// hall is steel, the voltage tier of the hatches is the gate.
// The EBF's boule recipes are gone (boule_melting.js): every boule comes from here. The first four are TFG's;
// trinium, naquadria, strange matter and chromodynium continue the same pattern (more polysilicon and seeds, a hotter
// coil, a tougher crucible).

ServerEvents.tags('item', event => {
    // the gems a seed crystal can hang on
    ;['amethyst', 'opal', 'diamond', 'sapphire', 'green_sapphire', 'ruby', 'red_garnet', 'yellow_garnet', 'lapis']
        .forEach(gem => event.add('af9:precision_fabricator_holder_rods', `#forge:rods/${gem}`))
    // the 2026-10-22 log shows the tag empty (the gem rods are not all GT items): metal rods hold a seed as well
    event.add('af9:precision_fabricator_holder_rods', ['gtceu:steel_rod', 'gtceu:titanium_rod', 'gtceu:tungsten_steel_rod'])
})

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const VHA = GTValues.VHA
    const gt = event.recipes.gtceu

    event.shaped('gtceu:high_temperature_precision_fabricator', [
        'BEB',
        'CAC',
        'DFD'
    ], {
        A: 'gtceu:mv_machine_hull',
        B: 'gtceu:mv_robot_arm',
        C: '#gtceu:circuits/mv',
        D: '#forge:double_wires/cupronickel',
        E: '#forge:single_cables/copper',
        F: 'gtceu:solid_machine_casing'
    }).id('af9:shaped/high_temperature_precision_fabricator')

    // ---- Silane line: silicon dust to polysilicon ----
    gt.chemical_reactor('af9:chlorosilanes')
        .itemInputs('4x #forge:dusts/silicon')
        .inputFluids(Fluid.of('gtceu:hydrochloric_acid', 6000), Fluid.of('gtceu:hydrogen', 4000))
        .outputFluids(Fluid.of('gtceu:chlorosilane', 3000), Fluid.of('gtceu:trichlorosilane', 1000))
        .circuit(1)
        .duration(60)
        .EUt(VHA[GTValues.MV])
    gt.chemical_reactor('af9:silane')
        .inputFluids(Fluid.of('gtceu:chlorosilane', 3000), Fluid.of('gtceu:trichlorosilane', 1000))
        .outputFluids(Fluid.of('gtceu:dichlorosilane', 3000), Fluid.of('gtceu:silane', 1000))
        .duration(60)
        .EUt(VHA[GTValues.MV])
    gt.large_chemical_reactor('af9:silane_shortcut')
        .itemInputs('4x #forge:dusts/silicon')
        .inputFluids(Fluid.of('gtceu:hydrochloric_acid', 6000), Fluid.of('gtceu:hydrogen', 4000))
        .outputFluids(Fluid.of('gtceu:dichlorosilane', 3000), Fluid.of('gtceu:silane', 1000))
        .circuit(2)
        .duration(100)
        .EUt(VHA[GTValues.HV])
    gt.electrolyzer('af9:silane_decomp')
        .inputFluids(Fluid.of('gtceu:silane', 1000))
        .outputFluids(Fluid.of('gtceu:hydrogen', 4000))
        .itemOutputs('gtceu:polysilicon_dust')
        .duration(60)
        .EUt(VA[GTValues.MV])
    // the boules' feed: electronic-grade silicon (etched polysilicon, EBF with Kanthal coils) melted at 1800 K
    gt.extractor('af9:polysilicon_melt')
        .itemInputs('gtceu:electronic_grade_silicon_dust')
        .outputFluids(Fluid.of('gtceu:polysilicon', 144))
        .duration(40)
        .EUt(VA[GTValues.MV])

    // ---- Silicon seed crystal ----
    gt.autoclave('af9:silicon_seed_crystal_autoclave')
        .itemInputs('gtceu:electronic_grade_silicon_dust')
        .inputFluids(Fluid.of('minecraft:water', 250))
        .chancedOutput('af9:silicon_seed_crystal', 7500, 0)
        .duration(600)
        .EUt(24)
    gt.autoclave('af9:silicon_seed_crystal_distilled')
        .itemInputs('gtceu:electronic_grade_silicon_dust')
        .inputFluids(Fluid.of('gtceu:distilled_water', 50))
        .itemOutputs('af9:silicon_seed_crystal')
        .duration(300)
        .EUt(24)

    // ---- Quartz crucible (TFG's needs a TFC crucible as the mould; here the arc furnace takes the dust alone) ----
    gt.arc_furnace('af9:quartz_crucible')
        .itemInputs('5x #forge:dusts/quartzite')
        .itemOutputs('af9:fused_quartz_crucible')
        .duration(400)
        .EUt(VA[GTValues.LV])
    gt.arc_furnace('af9:quartz_crucible_nether')
        .itemInputs('5x #forge:dusts/nether_quartz')
        .itemOutputs('af9:fused_quartz_crucible')
        .duration(400)
        .EUt(VA[GTValues.LV])

    // ---- Boules ----
    const ROD = '#af9:precision_fabricator_holder_rods'
    const SMALL_GAAS = '#forge:small_dusts/gallium_arsenide'
    const GAAS = '#forge:dusts/gallium_arsenide'
    const SEED = 'af9:silicon_seed_crystal'
    const CRUCIBLE = 'af9:fused_quartz_crucible'

    gt.high_temperature_precision_fabricator('af9:silicon_boule')
        .chancedInput(CRUCIBLE, 500, 0)
        .itemInputs(`1x ${SEED}`, SMALL_GAAS)
        .notConsumable(ROD)
        .inputFluids(Fluid.of('gtceu:polysilicon', 32 * 144))
        .itemOutputs('gtceu:silicon_boule')
        .circuit(1)
        .duration(450 * 20)
        .EUt(VA[GTValues.MV])
    gt.high_temperature_precision_fabricator('af9:silicon_boule_cleanroom')
        .chancedInput(CRUCIBLE, 500, 0)
        .itemInputs(`1x ${SEED}`, SMALL_GAAS)
        .notConsumable(ROD)
        .inputFluids(Fluid.of('gtceu:polysilicon', 32 * 144))
        .itemOutputs('gtceu:silicon_boule')
        .circuit(2)
        .cleanroom(CleanroomType.CLEANROOM)
        .duration(300 * 20)
        .EUt(VA[GTValues.MV])
    gt.high_temperature_precision_fabricator('af9:phosphorus_boule')
        .chancedInput(CRUCIBLE, 2000, 0)
        .itemInputs(`4x ${SEED}`, '8x #forge:dusts/phosphorus')
        .notConsumable(ROD)
        .inputFluids(Fluid.of('gtceu:polysilicon', 64 * 144), Fluid.of('gtceu:nitrogen', 8000))
        .itemOutputs('gtceu:phosphorus_boule')
        .circuit(3)
        .cleanroom(CleanroomType.CLEANROOM)
        .duration(600 * 20)
        .EUt(VA[GTValues.HV])
    gt.high_temperature_precision_fabricator('af9:naquadah_boule')
        .chancedInput(CRUCIBLE, 5000, 0)
        .itemInputs(`8x ${SEED}`, GAAS)
        .notConsumable(ROD)
        .inputFluids(Fluid.of('gtceu:polysilicon', 128 * 144), Fluid.of('gtceu:naquadah', 144),
            Fluid.of('gtceu:argon', 8000))
        .itemOutputs('gtceu:naquadah_boule')
        .circuit(4)
        .cleanroom(CleanroomType.CLEANROOM)
        .duration(750 * 20)
        .EUt(VA[GTValues.EV])
    gt.high_temperature_precision_fabricator('af9:neutronium_boule')
        .chancedInput(CRUCIBLE, 9900, 0)
        .itemInputs(`1x ${SEED}`, `2x ${GAAS}`)
        .notConsumable(ROD)
        .inputFluids(Fluid.of('gtceu:polysilicon', 256 * 144), Fluid.of('gtceu:neutronium', 144 * 4),
            Fluid.of('gtceu:xenon', 8000))
        .itemOutputs('gtceu:neutronium_boule')
        .circuit(5)
        .cleanroom(CleanroomType.CLEANROOM)
        .duration(900 * 20)
        .EUt(VA[GTValues.IV])
    gt.high_temperature_precision_fabricator('af9:trinium_boule')
        .chancedInput(CRUCIBLE, 6500, 0)
        .itemInputs(`4x ${SEED}`, '2x gtceu:trinium_dust')
        .notConsumable(ROD)
        .inputFluids(Fluid.of('gtceu:polysilicon', 320 * 144), Fluid.of('gtceu:argon', 8000))
        .itemOutputs('af9:trinium_boule')
        .circuit(6)
        .cleanroom(CleanroomType.CLEANROOM)
        .duration(1000 * 20)
        .EUt(VA[GTValues.IV])
    gt.high_temperature_precision_fabricator('af9:naquadria_boule')
        .chancedInput(CRUCIBLE, 8000, 0)
        .itemInputs(`4x ${SEED}`, '2x gtceu:naquadria_dust')
        .notConsumable(ROD)
        .inputFluids(Fluid.of('gtceu:polysilicon', 384 * 144), Fluid.of('gtceu:xenon', 8000))
        .itemOutputs('af9:naquadria_boule')
        .circuit(7)
        .cleanroom(CleanroomType.CLEANROOM)
        .duration(1100 * 20)
        .EUt(VA[GTValues.LuV])
    gt.high_temperature_precision_fabricator('af9:strange_matter_boule')
        .chancedInput('af9:tritanium_crucible', 5000, 0)
        .itemInputs(`4x ${SEED}`, 'gtceu:strange_matter_dust')
        .notConsumable(ROD)
        .inputFluids(Fluid.of('gtceu:polysilicon', 448 * 144), Fluid.of('gtceu:neutronium', 144 * 4),
            Fluid.of('gtceu:xenon', 16000))
        .itemOutputs('af9:strange_matter_boule')
        .circuit(8)
        .cleanroom(CleanroomType.CLEANROOM)
        .duration(1200 * 20)
        .EUt(VA[GTValues.UV])
    gt.high_temperature_precision_fabricator('af9:chromodynium_boule')
        .chancedInput('af9:tritanium_crucible', 7500, 0)
        .itemInputs(`4x ${SEED}`, '5x gtceu:chromodynium_dust')
        .notConsumable(ROD)
        .inputFluids(Fluid.of('gtceu:polysilicon', 512 * 144), Fluid.of('gtceu:endion', 16000))
        .itemOutputs('af9:chromodynium_boule')
        .circuit(9)
        .cleanroom(CleanroomType.CLEANROOM)
        .duration(1400 * 20)
        .EUt(VA[GTValues.UHV])

    // ---- Precision Fabricator Input and Output Ports (MV, the only tier): hull + bus + hatch (output: hull + bus) ----
    gt.assembler('af9:precision_fabricator_input_port_mv')
        .itemInputs('gtceu:mv_machine_hull', 'gtceu:mv_input_bus', 'gtceu:mv_input_hatch', '2x #gtceu:circuits/mv')
        .itemOutputs('gtceu:mv_precision_fabricator_input_port')
        .duration(200)
        .EUt(VA[GTValues.MV])
    gt.assembler('af9:precision_fabricator_output_port_mv')
        .itemInputs('gtceu:mv_machine_hull', 'gtceu:mv_output_bus', '#gtceu:circuits/mv')
        .itemOutputs('gtceu:mv_precision_fabricator_output_port')
        .duration(200)
        .EUt(VA[GTValues.MV])

})
