// AF9 - LV and MV Voltage Coils: GT's assembler recipes (voltage_coil_lv / _mv in GT's AssemblerRecipeLoader) with the
// 16x fine wire swapped for 2x wire spool (a spool is 64 fine wires, docs/wire-spools.md). Rod, circuit 1, 200 ticks
// and EU/t (VA of the tier) are GT's own numbers.

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    event.remove({ type: 'gtceu:assembler', output: 'gtceu:lv_voltage_coil' })
    event.recipes.gtceu.assembler('af9:voltage_coil_lv')
        .itemInputs('gtceu:magnetic_iron_rod', '2x gtceu:black_steel_wire_spool')
        .circuit(1)
        .itemOutputs('gtceu:lv_voltage_coil')
        .duration(200)
        .EUt(VA[GTValues.LV])

    event.remove({ type: 'gtceu:assembler', output: 'gtceu:mv_voltage_coil' })
    event.recipes.gtceu.assembler('af9:voltage_coil_mv')
        .itemInputs('gtceu:magnetic_steel_rod', '2x gtceu:aluminium_wire_spool')
        .circuit(1)
        .itemOutputs('gtceu:mv_voltage_coil')
        .duration(200)
        .EUt(VA[GTValues.MV])
})
