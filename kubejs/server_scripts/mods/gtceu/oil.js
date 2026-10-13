// AF9 - The oil chain (fluids + asphalt: af9-core, registry/AF9Materials; the regolith: af9-core).
// Spec: docs/oil.md
//
//   Oil Regolith (asteroid rock) -> centrifuge -> Impure Oil (+ sand, sulfur)
//   Impure Oil + a little sulfuric acid -> chemical reactor -> Shiny Oil (the acid takes the tar's solids)
//   Shiny Oil + distilled water -> mixer (the desalter) -> Desalted Oil + salt (salt loops to brine)
//   Desalted Oil -> distillation tower (atmospheric) -> refinery gas + naphtha + diesel + oil + heavy oil
//   Heavy Oil -> distillation tower, circuit 1 (vacuum) -> diesel + lubricant + asphalt
//   Heavy Oil + hydrogen -> cracker (hydrocracking) -> diesel + hydrogen sulfide
//   Naphtha -> distillation tower (splitter) -> light naphtha (gasoline pool) + heavy naphtha (reformer feed)
//   Heavy Naphtha -> chemical reactor (catalytic reforming, Pt-Re) -> reformate + hydrogen (the refinery's own H2)
//   Reformate -> distillation tower (BTX extraction) -> benzene + toluene + gasoline (fossil aromatics beside the
//     wood-tar green route in aromatics.js)
//   Butane -> chemical reactor (isomerization, per-pass equilibrium) -> isobutane + butane back (recycle)
//   Isobutane + butene + sulfuric acid -> chemical reactor (alkylation) -> alkylate + spent (diluted) acid
//   Alkylate + gasoline -> mixer (blending) -> high octane gasoline
//   Hydrogen sulfide + oxygen -> chemical reactor (Claus) -> sulfur + water (sulfur loops to acid)
//   Asphalt + steam -> pyrolyse oven (coker) -> coke + refinery gas (gas loops to shiny diesel)
// GT's own refining goes on from Oil, Heavy Oil, naphtha and diesel. GT's world oil is switched off
// (vein_oil.js). The rockets' fuel needs ethylene, which GT also makes from ethanol (biomass), so the
// first flight does not need oil. Every byproduct loops somewhere: salt to brine, sulfur to acid,
// refinery gas to shiny diesel, coke and asphalt to burn.

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // The press: two blocks of regolith, 1,800 mB of Impure Oil, the sand it was soaked in, sulfur from the tar
    event.recipes.gtceu.centrifuge('af9:oil_regolith_to_impure_oil')
        .itemInputs('2x af9:oil_regolith')
        .itemOutputs('minecraft:sand')
        .chancedOutput('gtceu:sulfur_dust', 2500, 0)
        .outputFluids(Fluid.of('gtceu:impure_oil', 1800))
        .duration(200)
        .EUt(VA[GTValues.MV])

    // The wash: the acid takes the grit and the tar's solids, what is left shines
    event.recipes.gtceu.chemical_reactor('af9:impure_oil_wash')
        .inputFluids(Fluid.of('gtceu:impure_oil', 1000))
        .inputFluids(Fluid.of('gtceu:sulfuric_acid', 100))
        .outputFluids(Fluid.of('gtceu:shiny_oil', 950))
        .outputFluids(Fluid.of('gtceu:diluted_sulfuric_acid', 100))
        .duration(160)
        .EUt(VA[GTValues.MV])

    // The desalter: distilled water washes the residual salts out (electrostatic-desalter analog);
    // the salt loops to the brine line and chlor-alkali
    event.recipes.gtceu.mixer('af9:shiny_oil_desalting')
        .inputFluids(Fluid.of('gtceu:shiny_oil', 1000), Fluid.of('gtceu:distilled_water', 100))
        .itemOutputs('gtceu:salt_dust')
        .outputFluids(Fluid.of('gtceu:desalted_oil', 1000))
        .duration(160)
        .EUt(VA[GTValues.MV])

    // Atmospheric distillation: five real cuts, gas to heavy ends
    event.recipes.gtceu.distillation_tower('af9:desalted_oil_distillation')
        .inputFluids(Fluid.of('gtceu:desalted_oil', 1000))
        .outputFluids(Fluid.of('gtceu:refinery_gas', 150), Fluid.of('gtceu:naphtha', 250),
            Fluid.of('gtceu:diesel', 200), Fluid.of('gtceu:oil', 200), Fluid.of('gtceu:oil_heavy', 200))
        .duration(240)
        .EUt(VA[GTValues.HV])

    // Vacuum distillation: the heavy ends split under vacuum into diesel, lube stock and asphalt bottoms
    event.recipes.gtceu.distillation_tower('af9:heavy_oil_vacuum_distillation')
        .inputFluids(Fluid.of('gtceu:oil_heavy', 1000))
        .itemOutputs('gtceu:asphalt_dust')
        .outputFluids(Fluid.of('gtceu:diesel', 300), Fluid.of('gtceu:lubricant', 400))
        .duration(300)
        .EUt(VA[GTValues.EV])

    // Hydrocracking: hydrogen opens the heavy ends to diesel, H2S carries the sulfur off
    event.recipes.gtceu.cracker('af9:heavy_oil_hydrocracking')
        .circuit(1)
        .inputFluids(Fluid.of('gtceu:oil_heavy', 1000), Fluid.of('gtceu:hydrogen', 800))
        .outputFluids(Fluid.of('gtceu:diesel', 1000), Fluid.of('gtceu:hydrogen_sulfide', 150))
        .duration(200)
        .EUt(VA[GTValues.HV])

    // Naphtha splitter: straight-run naphtha in two cuts, light ends to gasoline blending, heavy to the reformer
    event.recipes.gtceu.distillation_tower('af9:naphtha_splitting')
        .inputFluids(Fluid.of('gtceu:naphtha', 1000))
        .outputFluids(Fluid.of('gtceu:light_naphtha', 600), Fluid.of('gtceu:heavy_naphtha', 400))
        .duration(200)
        .EUt(VA[GTValues.HV])

    // Catalytic reforming (Pt-Re, circuit 2): heavy naphtha to the aromatic pool, hydrogen back to the network.
    // The refinery's own H2: with this on, hydrocracking and hydrotreating stop drawing electrolyzed hydrogen
    event.recipes.gtceu.chemical_reactor('af9:heavy_naphtha_reforming')
        .circuit(2)
        .inputFluids(Fluid.of('gtceu:heavy_naphtha', 1000))
        .outputFluids(Fluid.of('gtceu:reformate', 800), Fluid.of('gtceu:hydrogen', 300))
        .duration(300)
        .EUt(VA[GTValues.HV])

    // BTX extraction: the reformate splits into benzene, toluene and a gasoline-range residual that blends straight.
    // Fossil aromatics beside aromatics.js (wood tar topping + toluene hydrodealkylation): two routes, same pool
    event.recipes.gtceu.distillation_tower('af9:reformate_extraction')
        .inputFluids(Fluid.of('gtceu:reformate', 1000))
        .outputFluids(Fluid.of('gtceu:benzene', 300), Fluid.of('gtceu:toluene', 300),
            Fluid.of('gtceu:gasoline', 400))
        .duration(200)
        .EUt(VA[GTValues.HV])

    // Isomerization: butane to isobutane per pass, the unconverted fifth loops back (recycle, like the real unit)
    event.recipes.gtceu.chemical_reactor('af9:butane_isomerization')
        .inputFluids(Fluid.of('gtceu:butane', 1000))
        .outputFluids(Fluid.of('gtceu:isobutane', 800), Fluid.of('gtceu:butane', 200))
        .duration(200)
        .EUt(VA[GTValues.HV])

    // Alkylation: isobutane + butene over sulfuric acid (the acid belt's doing) to alkylate; the spent acid
    // leaves diluted, like the desalter wash's. Butene comes from GT's own steam cracking, untouched
    event.recipes.gtceu.chemical_reactor('af9:alkylation')
        .inputFluids(Fluid.of('gtceu:isobutane', 1000), Fluid.of('gtceu:butene', 1000),
            Fluid.of('gtceu:sulfuric_acid', 500))
        .outputFluids(Fluid.of('gtceu:alkylate', 1500), Fluid.of('gtceu:diluted_sulfuric_acid', 500))
        .duration(300)
        .EUt(VA[GTValues.HV])

    // Blending: alkylate cuts straight-run gasoline up to high octane, no loss
    event.recipes.gtceu.mixer('af9:alkylate_blending')
        .inputFluids(Fluid.of('gtceu:alkylate', 1000), Fluid.of('gtceu:gasoline', 1000))
        .outputFluids(Fluid.of('gtceu:high_octane_gasoline', 2000))
        .duration(100)
        .EUt(VA[GTValues.HV])

    // Claus: 2 H2S + O2 -> 2 S + 2 H2O, the sulfur loops back to acid
    event.recipes.gtceu.chemical_reactor('af9:claus_sulfur_recovery')
        .inputFluids(Fluid.of('gtceu:hydrogen_sulfide', 2000), Fluid.of('gtceu:oxygen', 1000))
        .itemOutputs('2x gtceu:sulfur_dust')
        .outputFluids(Fluid.of('minecraft:water', 2000))
        .duration(200)
        .EUt(VA[GTValues.MV])

    // Coking: steam strips the asphalt bottoms to coke and fuel gas (the gas loops to shiny diesel)
    event.recipes.gtceu.pyrolyse_oven('af9:asphalt_coking')
        .itemInputs('gtceu:asphalt_dust')
        .inputFluids(Fluid.of('gtceu:steam', 1000))
        .itemOutputs('gtceu:coke_dust')
        .outputFluids(Fluid.of('gtceu:refinery_gas', 400))
        .duration(300)
        .EUt(VA[GTValues.HV])

    // Heavy Water: deuterium and oxygen back (GT has no other easy deuterium)
    event.recipes.gtceu.electrolyzer('af9:heavy_water_electrolysis')
        .inputFluids(Fluid.of('gtceu:heavy_water', 1000))
        .outputFluids(Fluid.of('gtceu:deuterium', 2000))
        .outputFluids(Fluid.of('gtceu:oxygen', 1000))
        .duration(400)
        .EUt(VA[GTValues.MV])
})
