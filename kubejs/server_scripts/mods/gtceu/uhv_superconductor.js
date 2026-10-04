// AF9 - Sanguinite, the bright-red UHV superconductor, smelted in the Rotary Hearth Furnace
// (gtceu:mega_blast_furnace, which runs electric_blast_furnace recipes). Materials: AF9Materials.uhvSuperconductor.
// Spec: docs/uhv-superconductor.md
//
// The EBF takes a single fluid, so the chain blends first and smelts after:
//   1. Large Chemical Reactor: 4 neutronium + 10 tritanium dusts, hydrogen, Ares gas (Martian asteroid field,
//      vein_oil.js) and LXA-1 (Space Elevator far-dark mission, PlanetCatalog) -> 14 crude sanguinite dust.
//   2. Rotary Hearth Furnace: 14 crude + circuit 10 + supercooled endion -> 14 hot sanguinite ingots,
//      13000 K (Resonant Endion Coils at UHV), 4A UV, 60 s.
//   3. Bulk Blast Chiller: GT's own vacuum-freezer cooling (from the blast property) takes the hot ingots to
//      sanguinite ingots; GT wires them into lossless UV 4A cables on its own.

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // ---- 1. The blend: dusts and all three gases in the Large Chemical Reactor (3 items, 5 fluids) ----
    event.recipes.gtceu.large_chemical_reactor('af9:crude_sanguinite_mix')
        .itemInputs('4x gtceu:neutronium_dust', '10x gtceu:tritanium_dust')
        .inputFluids(Fluid.of('gtceu:hydrogen', 10000), Fluid.of('gtceu:ares_gas', 2000),
            Fluid.of('gtceu:lxa_1', 4000))
        .itemOutputs('14x gtceu:crude_sanguinite_dust')
        .duration(600)
        .EUt(VA[GTValues.UV])

    // ---- 2. The smelt: crude blend under supercooled endion in the Rotary Hearth Furnace ----
    // 4A UV (2,097,152 EU/t), a full minute, circuit 10. The 50 buckets of supercooled endion are the
    // quench that freezes the lossless phase in; the Bulk Blast Chiller finishes the ingots.
    event.recipes.gtceu.electric_blast_furnace('af9:sanguinite_hot_ingot')
        .itemInputs('14x gtceu:crude_sanguinite_dust')
        .circuit(10)
        .inputFluids(Fluid.of('gtceu:supercooled_endion', 50000))
        .itemOutputs('14x gtceu:hot_sanguinite_ingot')
        .blastFurnaceTemp(13000)
        .duration(1200)
        .EUt(VA[GTValues.UV], 4)
})
