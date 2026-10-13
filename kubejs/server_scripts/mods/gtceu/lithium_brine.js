// AF9 - Lithium brine, after the salars of the Andes (and Mekanism's and old GT's brine lines).
// Materials: af9-core, registry/AF9Materials (lithiumBrine()). Spec: docs/lithium-brine.md
//
//   salt water -> brine (MV filtration) -> concentrate + borax (HV distillery, the evaporation ponds)
//   -> lithium chloride + limestone (HV liming: slaked lime + CO2, Mg/Ca filtered into the cake)
//   -> lithium carbonate + salt (EV soda-ash carbonation)
//   -> lithium hydroxide + limestone (EV causticization)
//   -> lithium oxide + CO2 (IV calcination under nitrogen)
//   -> lithium fluoride (IV, hydrofluoric acid) -> lithium hexafluorophosphate (LuV, with P2O5/HF)
// GT's own electrolyzer splits the chloride to lithium metal (the SAW filters' lithium niobate).

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // ---- MV: brine pumping. Salar water in, halite out, lithium brine on ----
    event.recipes.gtceu.water_filtration('af9:lithium_brine_pumping')
        .notConsumable('gtceu:fluid_filter')
        .circuit(5)
        .inputFluids(Fluid.of('gtceu:salt_water', 3000))
        .outputFluids(Fluid.of('gtceu:lithium_brine', 1000))
        .itemOutputs('2x gtceu:rock_salt_dust', 'gtceu:salt_dust')
        .duration(200)
        .EUt(VA[GTValues.MV])

    // ---- HV: the evaporation ponds. Borax drops out, the brine concentrates ----
    event.recipes.gtceu.distillery('af9:brine_evaporation')
        .circuit(1)
        .inputFluids(Fluid.of('gtceu:lithium_brine', 2000))
        .outputFluids(Fluid.of('gtceu:concentrated_lithium_brine', 1000))
        .itemOutputs('gtceu:borax_dust')
        .duration(300)
        .EUt(VA[GTValues.HV])

    // ---- HV: liming. Slaked lime plus CO2 polish Mg/Ca into the filter cake, LiCl crystallizes ----
    event.recipes.gtceu.chemical_reactor('af9:brine_liming')
        .itemInputs('2x gtceu:calcium_hydroxide_dust')
        .inputFluids(Fluid.of('gtceu:concentrated_lithium_brine', 1000), Fluid.of('gtceu:carbon_dioxide', 1000))
        .itemOutputs('gtceu:lithium_chloride_dust', 'gtceu:calcium_carbonate_dust')
        .outputFluids(Fluid.of('gtceu:distilled_water', 1000))
        .duration(200)
        .EUt(VA[GTValues.HV])

    // ---- EV: soda-ash carbonation. 2 LiCl + Na2CO3 -> Li2CO3 + 2 NaCl ----
    event.recipes.gtceu.large_chemical_reactor('af9:lithium_carbonate_precipitation')
        .itemInputs('2x gtceu:lithium_chloride_dust', 'gtceu:soda_ash_dust')
        .inputFluids(Fluid.of('gtceu:distilled_water', 2000))
        .itemOutputs('gtceu:lithium_carbonate_dust', 'gtceu:salt_dust')
        .outputFluids(Fluid.of('gtceu:distilled_water', 1000))
        .duration(200)
        .EUt(VA[GTValues.EV])

    // ---- EV: causticization. Li2CO3 + Ca(OH)2 -> 2 LiOH + CaCO3 ----
    event.recipes.gtceu.chemical_reactor('af9:lithium_hydroxide_causticization')
        .itemInputs('gtceu:lithium_carbonate_dust', 'gtceu:calcium_hydroxide_dust')
        .inputFluids(Fluid.of('gtceu:distilled_water', 1000))
        .itemOutputs('2x gtceu:lithium_hydroxide_dust', 'gtceu:calcium_carbonate_dust')
        .outputFluids(Fluid.of('minecraft:water', 1000))
        .duration(200)
        .EUt(VA[GTValues.EV])

    // ---- IV: calcination under nitrogen. Li2CO3 -> Li2O + CO2 ----
    event.recipes.gtceu.pyrolyse_oven('af9:lithium_oxide_calcination')
        .circuit(1)
        .itemInputs('gtceu:lithium_carbonate_dust')
        .inputFluids(Fluid.of('gtceu:nitrogen', 1000))
        .itemOutputs('gtceu:lithium_oxide_dust')
        .outputFluids(Fluid.of('gtceu:carbon_dioxide', 1000))
        .duration(300)
        .EUt(VA[GTValues.IV])

    // ---- IV: fluorination. LiOH + HF -> LiF + H2O ----
    event.recipes.gtceu.large_chemical_reactor('af9:lithium_fluoride_fluorination')
        .itemInputs('gtceu:lithium_hydroxide_dust')
        .inputFluids(Fluid.of('gtceu:hydrofluoric_acid', 1000))
        .itemOutputs('gtceu:lithium_fluoride_dust')
        .outputFluids(Fluid.of('minecraft:water', 1000))
        .duration(200)
        .EUt(VA[GTValues.IV])

    // ---- LuV: the electrolyte salt. LiF fluorinates the phosphate glass (the PF5 route, one pot) ----
    event.recipes.gtceu.large_chemical_reactor('af9:lithium_hexafluorophosphate_synthesis')
        .itemInputs('2x gtceu:lithium_fluoride_dust', 'gtceu:phosphorus_pentoxide_dust')
        .inputFluids(Fluid.of('gtceu:hydrofluoric_acid', 4000))
        .itemOutputs('2x gtceu:lithium_hexafluorophosphate_dust')
        .outputFluids(Fluid.of('minecraft:water', 2000))
        .duration(300)
        .EUt(VA[GTValues.LuV])
})
