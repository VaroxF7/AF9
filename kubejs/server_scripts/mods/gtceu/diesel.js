// AF9 - Diesel line: three AF9 fuels above GT's diesel (materials: af9-core, registry/AF9Materials).
// Spec: docs/green-chemistry.md §6
//
//   shiny oil + refinery gas       -reactor, HV->  shiny diesel + sulfur (HOG hydrotreating)
//   diesel + chloromethane         -reactor, HV->  chloromethane diesel (chlorinated cetane booster)
//   shiny diesel + mana diamond    -LCR, EV->       mana diesel + diamond (the stone survives, its mana does not)
//
// The ladder burns in diesel generators: bio diesel 38,400 < chloromethane diesel 48,000 <
// shiny diesel 96,000 < mana diesel 192,000 EU a bucket (AF9 balance).

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // ---- Hydrotreating: refinery gas (GTNH's HOG) strips the shiny oil's sulfur ----
    event.recipes.gtceu.chemical_reactor('af9:shiny_diesel_hydrotreating')
        .inputFluids(Fluid.of('gtceu:shiny_oil', 1000), Fluid.of('gtceu:refinery_gas', 1000))
        .itemOutputs('gtceu:sulfur_dust')
        .outputFluids(Fluid.of('gtceu:shiny_diesel', 1500))
        .duration(200)
        .EUt(VA[GTValues.HV])

    // ---- Cetane boosting: a shot of chloromethane in plain diesel ----
    event.recipes.gtceu.chemical_reactor('af9:chloromethane_diesel_blending')
        .inputFluids(Fluid.of('gtceu:diesel', 2000), Fluid.of('gtceu:chloromethane', 500))
        .outputFluids(Fluid.of('gtceu:chloromethane_diesel', 2500))
        .duration(200)
        .EUt(VA[GTValues.HV])

    // ---- Mana infusion: a mana diamond gives up its mana to shiny diesel (Botania is in the pack) ----
    event.recipes.gtceu.large_chemical_reactor('af9:mana_diesel_infusion')
        .itemInputs('botania:mana_diamond')
        .inputFluids(Fluid.of('gtceu:shiny_diesel', 1000))
        .itemOutputs('minecraft:diamond')
        .outputFluids(Fluid.of('gtceu:mana_diesel', 1000))
        .duration(300)
        .EUt(VA[GTValues.EV])

    // ---- Burning them, worst to best ----
    event.recipes.gtceu.combustion_generator('af9:chloromethane_diesel_combustion')
        .inputFluids(Fluid.of('gtceu:chloromethane_diesel', 1000))
        .duration(1500)
        .EUt(32)

    event.recipes.gtceu.combustion_generator('af9:shiny_diesel_combustion')
        .inputFluids(Fluid.of('gtceu:shiny_diesel', 1000))
        .duration(2000)
        .EUt(48)

    event.recipes.gtceu.combustion_generator('af9:mana_diesel_combustion')
        .inputFluids(Fluid.of('gtceu:mana_diesel', 1000))
        .duration(3000)
        .EUt(64)
})
