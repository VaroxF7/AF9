// AF9 - Ceranium and the Pico board: the board of the Pico circuits is no wetware board any more. It is laid up at UV
// (the first UHV circuit part that is not a wetware one) from a plate of Ceranium and fibre interconnects.
// Materials: af9-core, registry/AF9Materials (ceranium()). Ore: vein_ceres.js. Spec: docs/ceranium.md
//
//   ceresite   ore of the Ceres belt only (Mk-IV elevator or a rocket and a prospector)
//   ceranium   4 ceresite + 2 tritanium + naquadria + trinium dust, mixed at UV, melted by GT's EBF (blast 9000 K)
//   fiber interconnects   fine ceranium wire and electrum in glass, assembled at UV
//   pico board   assembly line at UV; the Advanced Circuit Manufacturer (UHV) takes it from there (circuits_af9.js)

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // ---- The alloy: ceresite hardened with UV-tier metals ----
    event.recipes.gtceu.mixer('af9:ceranium_dust')
        .itemInputs('4x gtceu:ceresite_dust', '2x gtceu:tritanium_dust', 'gtceu:naquadria_dust', 'gtceu:trinium_dust')
        .itemOutputs('8x gtceu:ceranium_dust')
        .circuit(6)
        .duration(400)
        .EUt(VA[GTValues.UV])

    // ---- Fiber interconnects: a ceranium trace and a glass fibre in one polymer strip ----
    event.recipes.gtceu.assembler('af9:fiber_interconnects')
        .itemInputs('4x gtceu:fine_ceranium_wire', '2x gtceu:fine_electrum_wire', '2x gtceu:glass_tube',
            '2x gtceu:polybenzimidazole_foil')
        .inputFluids(Fluid.of('gtceu:polybenzimidazole', 144))
        .itemOutputs('4x af9:fiber_interconnects')
        .cleanroom(CleanroomType.CLEANROOM)
        .duration(300)
        .EUt(VA[GTValues.UV])

    // ---- The Pico board: UV assembly line ----
    event.recipes.gtceu.assembly_line('af9:pico_board')
        .itemInputs('2x gtceu:ceranium_plate', '4x af9:fiber_interconnects', '16x gtceu:fine_ceranium_wire',
            '4x gtceu:polybenzimidazole_foil', '2x gtceu:tritanium_foil')
        .inputFluids(Fluid.of('gtceu:polybenzimidazole', 576), Fluid.of('gtceu:living_solder', 288))
        .itemOutputs('af9:pico_board')
        .cleanroom(CleanroomType.CLEANROOM)
        .duration(600)
        .EUt(VA[GTValues.UV])
})
