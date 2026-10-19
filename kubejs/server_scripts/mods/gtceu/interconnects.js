// AF9 - Interconnects: gold/electrum Leiterbahnen, one strip or a pallet of them. Two routes:
// the MV one lays them from electrum and sterling silver foils in the assembler, the Advanced
// Circuit Manufacturer (computation through its hatch, UHV like the Pico circuits) lays the
// fine-wire strips in PBI; the traces of everything from Pico to NVM (circuits coded later).

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // ---- The MV route: two foils in, strips out ----
    event.recipes.gtceu.assembler('af9:interconnect_mv')
        .itemInputs('2x gtceu:electrum_foil', '2x gtceu:sterling_silver_foil')
        .itemOutputs('4x af9:interconnect')
        .duration(200)
        .EUt(VA[GTValues.MV])

    // ---- One strip: fine gold and electrum wires laid in polymer ----
    event.recipes.gtceu.advanced_circuit_manufacturer('af9:interconnect')
        .itemInputs('4x gtceu:fine_gold_wire', '2x gtceu:fine_electrum_wire')
        .inputFluids(Fluid.of('gtceu:polybenzimidazole', 144))
        .itemOutputs('4x af9:interconnect')
        .CWUt(8)
        .duration(200)
        .EUt(VA[GTValues.UHV])

    // ---- A pallet of them: eight strips ready to lay ----
    event.recipes.gtceu.advanced_circuit_manufacturer('af9:interconnect_pallet')
        .itemInputs('8x af9:interconnect', '4x gtceu:polybenzimidazole_foil')
        .itemOutputs('af9:interconnect_pallet')
        .CWUt(16)
        .duration(300)
        .EUt(VA[GTValues.UHV])
})
