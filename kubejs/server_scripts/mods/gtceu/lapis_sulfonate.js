// AF9 - Lapis Butene-II-Sulfonate: the pack's stand-in for iron(III) chloride, made from lapis (materials: af9-core,
// registry/AF9Materials (lapisSulfonate())). Spec: docs/lapis-sulfonate.md
//
//   lapis + H2SO4              -reactor->  leach liquor (Na2SO4, Al2(SO4)3) + H2S + SiO2 residue
//   2 H2S + 3 O2               -LCR->      2 SO2 + 2 H2O                    (the sulfide of the lazurite, burnt)
//   4 V + 5 O2                 -reactor->  2 V2O5                           (the contact catalyst)
//   2 SO2 + O2 (V2O5)          -reactor->  2 SO3                            (contact process)
//   butene + SO3               -reactor->  butene sulfonic acid             (alkene sulfonation, as for olefin sulfonates)
//   sulfonic acid + liquor     -reactor->  Lapis Butene-II-Sulfonate + H2SO4 back (ion exchange, the acid loops)

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const gt = event.recipes.gtceu

    // ---- lazurite breaks down in hot acid: the sulfide leaves as H2S, silica stays behind ----
    gt.chemical_reactor('af9:lapis_sulfonate/leach')
        .itemInputs('4x gtceu:lapis_dust')
        .inputFluids(Fluid.of('gtceu:sulfuric_acid', 2000))
        .circuit(1)
        .itemOutputs('2x gtceu:silicon_dioxide_dust')
        .outputFluids(Fluid.of('gtceu:lapis_leach_liquor', 2000), Fluid.of('gtceu:hydrogen_sulfide', 1000))
        .duration(300)
        .EUt(VA[GTValues.HV])

    // ---- the sulfide burns to sulfur dioxide (the Claus recipe stops at sulfur, this one goes on) ----
    gt.large_chemical_reactor('af9:lapis_sulfonate/hydrogen_sulfide_combustion')
        .circuit(2)
        .inputFluids(Fluid.of('gtceu:hydrogen_sulfide', 2000), Fluid.of('gtceu:oxygen', 3000))
        .outputFluids(Fluid.of('gtceu:sulfur_dioxide', 2000), Fluid.of('minecraft:water', 2000))
        .duration(200)
        .EUt(VA[GTValues.MV])

    // ---- contact process: vanadium pentoxide is made from vanadium and oxygen and is not used up ----
    gt.chemical_reactor('af9:lapis_sulfonate/vanadium_pentoxide')
        .itemInputs('4x gtceu:vanadium_dust')
        .inputFluids(Fluid.of('gtceu:oxygen', 5000))
        .itemOutputs('2x gtceu:vanadium_pentoxide_dust')
        .duration(240)
        .EUt(VA[GTValues.HV])

    gt.chemical_reactor('af9:lapis_sulfonate/contact_sulfur_trioxide')
        .notConsumable('gtceu:vanadium_pentoxide_dust')
        .inputFluids(Fluid.of('gtceu:sulfur_dioxide', 2000), Fluid.of('gtceu:oxygen', 1000))
        .outputFluids(Fluid.of('gtceu:sulfur_trioxide', 2000))
        .duration(160)
        .EUt(VA[GTValues.HV])

    // ---- butene takes SO3 across its double bond: the sulfonic acid ----
    gt.chemical_reactor('af9:lapis_sulfonate/butene_sulfonation')
        .inputFluids(Fluid.of('gtceu:butene', 1000), Fluid.of('gtceu:sulfur_trioxide', 1000))
        .outputFluids(Fluid.of('gtceu:butene_sulfonic_acid', 1000))
        .duration(200)
        .EUt(VA[GTValues.HV])

    // ---- the sulfonic acid trades its proton for the lapis metals; the sulfuric acid goes back to the leach ----
    gt.chemical_reactor('af9:lapis_sulfonate/ion_exchange')
        .inputFluids(Fluid.of('gtceu:butene_sulfonic_acid', 2000), Fluid.of('gtceu:lapis_leach_liquor', 1000))
        .outputFluids(Fluid.of('gtceu:lapis_butene_ii_sulfonate', 2000), Fluid.of('gtceu:sulfuric_acid', 1000))
        .duration(240)
        .EUt(VA[GTValues.HV])
})
