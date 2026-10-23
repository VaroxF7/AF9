// priority: 0
// The MV Energy Hatch and Dynamo Hatch no longer take a ULPIC chip. GT's recipes (the crafting grid ones and the
// assembler ones, GT's MetaTileEntityMachineRecipeLoader) are rewritten here without that ingredient: the hull, the
// cables (or springs) and the MV voltage coil stay, the assembler keeps GT's 200 ticks at MV. The ULPIC wafer / chip is
// no longer a gate for the Photolithography Line (docs/semiconductor-factory.md §6.8).

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const HULL = 'gtceu:mv_machine_hull'
    const COIL = 'gtceu:mv_voltage_coil'

    // Energy Hatch (input): hull + 2 copper cables + coil
    event.remove({ output: 'gtceu:mv_energy_input_hatch', type: 'minecraft:crafting_shaped' })
    event.remove({ output: 'gtceu:mv_energy_input_hatch', type: 'gtceu:assembler' })
    event.shaped('gtceu:mv_energy_input_hatch', [' V ', 'CHC'], {
        V: COIL,
        C: 'gtceu:copper_single_cable',
        H: HULL
    }).id('af9:mv_energy_hatch_no_chip_shaped')
    event.recipes.gtceu.assembler('af9:mv_energy_hatch_no_chip')
        .itemInputs(HULL, '2x gtceu:copper_single_cable', COIL)
        .itemOutputs('gtceu:mv_energy_input_hatch')
        .duration(200)
        .EUt(VA[GTValues.MV])

    // Dynamo Hatch (output): hull + 2 copper springs + coil
    event.remove({ output: 'gtceu:mv_energy_output_hatch', type: 'minecraft:crafting_shaped' })
    event.remove({ output: 'gtceu:mv_energy_output_hatch', type: 'gtceu:assembler' })
    event.shaped('gtceu:mv_energy_output_hatch', [' V ', 'SHS'], {
        V: COIL,
        S: 'gtceu:copper_spring',
        H: HULL
    }).id('af9:mv_dynamo_hatch_no_chip_shaped')
    event.recipes.gtceu.assembler('af9:mv_dynamo_hatch_no_chip')
        .itemInputs(HULL, '2x gtceu:copper_spring', COIL)
        .itemOutputs('gtceu:mv_energy_output_hatch')
        .duration(200)
        .EUt(VA[GTValues.MV])
})
