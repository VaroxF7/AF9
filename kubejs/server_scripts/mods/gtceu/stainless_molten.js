// AF9 - Stainless steel melts in the Alloy Blast Smelter again.
//
// Temperature: GT's 1700 K (cupronickel) is raised to 2700 K, so stainless steel needs Kanthal-tier coils.
// GT hands the ABS recipe of a stainless-like alloy the *liquid* fluid when the metal has no hot ingot (stainless blasts
// at 1700 K, below the 1750 K hot-ingot line), and then makes no freezer recipe for it either. Stainless steel does have a
// molten fluid (gtceu:molten_stainless_steel, GT registers it with the alloy-blast property), it was just never used.
//
//   4 dusts (6 Fe, Cr, Mn, Ni)  -ABS->   1296 mB molten stainless steel          (GT's inputs, times and gas boost kept)
//   144 mB molten + ingot mold  -Vacuum Freezer->   stainless steel ingot
//   144 mB molten + dense hydrogen  -Vacuum Freezer->   144 mB liquid stainless steel   (quench: what GT's own recipes want)
//
// Liquid stainless steel stays what it was for everything else (extractor, fluid solidifier, pipes and the like).

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // ---- 1. The ABS recipes: same inputs and numbers as GT's (9 units out, HV), now needing Kanthal coils (2700 K), molten out ----
    event.remove({ id: 'gtceu:alloy_blast_smelter/stainless_steel' })
    event.remove({ id: 'gtceu:alloy_blast_smelter/stainless_steel_gas' })

    const dusts = ['6x gtceu:iron_dust', 'gtceu:chromium_dust', 'gtceu:manganese_dust', 'gtceu:nickel_dust']

    event.recipes.gtceu.alloy_blast_smelter('af9:stainless_steel/molten')
        .circuit(4)
        .itemInputs(dusts)
        .outputFluids(Fluid.of('gtceu:molten_stainless_steel', 1296))
        .blastFurnaceTemp(2700)
        .duration(7425)
        .EUt(VA[GTValues.HV])

    event.recipes.gtceu.alloy_blast_smelter('af9:stainless_steel/molten_gas')
        .circuit(14)
        .itemInputs(dusts)
        .inputFluids(Fluid.of('gtceu:nitrogen', 9000))
        .outputFluids(Fluid.of('gtceu:molten_stainless_steel', 1296))
        .blastFurnaceTemp(2700)
        .duration(4974)
        .EUt(VA[GTValues.HV])

    // ---- 2. Casting: molten to ingot in the Vacuum Freezer ----
    event.recipes.gtceu.vacuum_freezer('af9:stainless_steel/ingot_cooling')
        .notConsumable('gtceu:ingot_casting_mold')
        .inputFluids(Fluid.of('gtceu:molten_stainless_steel', 144))
        .itemOutputs('gtceu:stainless_steel_ingot')
        .duration(1500)
        .EUt(VA[GTValues.HV])

    // ---- 3. Quench: molten + dense hydrogen to liquid stainless steel, no mold in the bus ----
    event.recipes.gtceu.vacuum_freezer('af9:stainless_steel/liquid_quench')
        .inputFluids(Fluid.of('gtceu:molten_stainless_steel', 144), Fluid.of('gtceu:dense_hydrogen', 100))
        .outputFluids(Fluid.of('gtceu:stainless_steel', 144))
        .duration(300)
        .EUt(VA[GTValues.HV])
})
