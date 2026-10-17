// AF9 - XPS board: a placeholder alloy from sand + dirt, laid up in the HILE with sanguinite traces.
// Materials: af9-core, registry/AF9Materials (xps_alloy in alloys()). Spec: docs/xps-board.md
//
//   xps_alloy   3 sand + 1 dirt (Mixer, LV) -> 4 dust; GT's EBF melts it (1200 K, LOW, LV, 400 ticks)
//   xps_board   Hyper-Intensity Laser Engraver (plasma_soldering): alloy plates + sanguinite foil + wire

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // ---- The alloy: sand and dirt mixed at LV ----
    event.recipes.gtceu.mixer('af9:xps_alloy_dust')
        .itemInputs('3x gtceu:sand_dust', 'gtceu:dirt_dust')
        .circuit(2)
        .itemOutputs('4x gtceu:xps_alloy_dust')
        .duration(200)
        .EUt(VA[GTValues.LV])

    // ---- The board: laid up in the HILE (plasma_soldering) ----
    // Alloy plates as the laminate, sanguinite foil and fine wire as the traces, PBI as the binder.
    // plasma_soldering slots: 9 items, 1 out, 2 fluids in.
    event.recipes.gtceu.plasma_soldering('af9:xps_board')
        .itemInputs('4x gtceu:xps_alloy_plate', '4x gtceu:sanguinite_foil', '8x gtceu:fine_sanguinite_wire',
            '2x gtceu:polybenzimidazole_foil', 'gtceu:tritanium_frame')
        .inputFluids(Fluid.of('gtceu:plasma_solder', 144), Fluid.of('gtceu:polybenzimidazole', 288))
        .itemOutputs('af9:xps_board')
        .duration(600)
        .EUt(VA[GTValues.UHV], 100)
})
