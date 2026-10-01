// Self-test fixture: recipes with mistakes
ServerEvents.recipes(event => {
    const gt = event.recipes.gtceu
    // R1: the same id twice
    gt.assembler('af9:selftest_dup').itemInputs('gtceu:steel_plate').itemOutputs('gtceu:steel_ingot').duration(10).EUt(32)
    gt.assembler('af9:selftest_dup').itemInputs('gtceu:iron_plate').itemOutputs('gtceu:iron_ingot').duration(10).EUt(32)
    // R3: an item nobody registered; R4: a recipe type that does not exist
    gt.assembler('af9:selftest_unknown').itemInputs('kubejs:selftest_not_registered').itemOutputs('gtceu:steel_ingot').duration(10).EUt(32)
    gt.selftest_not_a_type('af9:selftest_type').itemInputs('gtceu:steel_plate').duration(10).EUt(32)
    // R3: a typo in a GT name and in a GT material
    gt.assembler('af9:selftest_typo').itemInputs('gtceu:cleanroom_glas', 'gtceu:strange_matte_dust').itemOutputs('gtceu:steel_ingot').duration(10).EUt(32)
    // R5: takes an item nothing makes
    gt.assembler('af9:selftest_takes_orphan').itemInputs('kubejs:selftest_orphan').itemOutputs('gtceu:steel_ingot').duration(10).EUt(32)
    // R10: a and b only make each other
    gt.assembler('af9:selftest_a').itemInputs('kubejs:selftest_b').itemOutputs('kubejs:selftest_a').duration(10).EUt(32)
    gt.assembler('af9:selftest_b').itemInputs('kubejs:selftest_a').itemOutputs('kubejs:selftest_b').duration(10).EUt(32)
    // R2: over the slots (1 item in); R11: a fluid input but the machine has no fluid hatch; R8: no duration, a chance of 20000
    gt.selftest_run('af9:selftest_slots').itemInputs('gtceu:steel_plate', 'gtceu:iron_plate').inputFluids(Fluid.of('gtceu:water', 1000))
        .chancedOutput('gtceu:steel_ingot', 20000, 0).EUt(32)
    // R7: the second recipe takes everything the first takes
    gt.selftest_run('af9:selftest_c1').itemInputs('gtceu:steel_plate').itemOutputs('gtceu:steel_ingot').duration(10).EUt(32)
    gt.selftest_run('af9:selftest_c2').itemInputs('gtceu:steel_plate').itemOutputs('gtceu:iron_ingot').duration(10).EUt(32)
    // R8: an amount of 0 mB, a duration of 0, an EUt of 0
    gt.assembler('af9:selftest_zero').itemInputs('gtceu:steel_plate').inputFluids(Fluid.of('gtceu:water', 0)).itemOutputs('gtceu:steel_ingot').duration(0).EUt(0)
    // R9: a fab recipe from HV on without a clean room
    gt.fab_synthesis('af9:selftest_fab').inputFluids(Fluid.of('gtceu:water', 1000)).outputFluids(Fluid.of('gtceu:water', 1000)).duration(10).EUt(2048)
    // R12: 2048 EU/t, but the type is only run by single blocks up to MV
    gt.selftest_single('af9:selftest_power').itemInputs('gtceu:steel_plate').itemOutputs('gtceu:steel_ingot').duration(10).EUt(2048)
    // R13: a furnace recipe without a temperature
    gt.fab_cvd('af9:selftest_notemp').itemInputs('gtceu:steel_plate').itemOutputs('gtceu:steel_ingot').duration(10).EUt(32)
    // L1: a translation nobody wrote
    Component.translatable('af9.selftest.no_such_key')
    // S1: a typo
    selftestNoSuchFunction()
})
