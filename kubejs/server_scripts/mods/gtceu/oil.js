// AF9 - The oil chain (fluids: startup_scripts/gtceu/oil.js; the regolith: af9-core). Spec: docs/oil.md
//
//   Oil Regolith (asteroid rock)  -> centrifuge -> Impure Oil (+ sand, sulfur)
//   Impure Oil + a little sulfuric acid -> chemical reactor -> Shiny Oil (the wash: the acid takes the solids)
//   Shiny Oil -> distillation tower -> Oil and Heavy Oil
// GT's own refining goes on from Oil and Heavy Oil. GT's world oil is switched off (vein_oil.js). The rockets' fuel needs
// ethylene, which GT also makes from ethanol (biomass), so the first flight does not need oil.

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // The press: two blocks of regolith, 1,500 mB of Impure Oil, the sand it was soaked in, sulfur from the tar
    event.recipes.gtceu.centrifuge('af9:oil_regolith_to_impure_oil')
        .itemInputs('2x af9:oil_regolith')
        .itemOutputs('minecraft:sand')
        .chancedOutput('gtceu:sulfur_dust', 2500, 0)
        .outputFluids(Fluid.of('gtceu:impure_oil', 1500))
        .duration(200)
        .EUt(VA[GTValues.MV])

    // The wash: the acid takes the grit and the tar's solids, what is left shines
    event.recipes.gtceu.chemical_reactor('af9:impure_oil_wash')
        .inputFluids(Fluid.of('gtceu:impure_oil', 1000))
        .inputFluids(Fluid.of('gtceu:sulfuric_acid', 100))
        .outputFluids(Fluid.of('gtceu:shiny_oil', 900))
        .outputFluids(Fluid.of('gtceu:diluted_sulfuric_acid', 100))
        .duration(160)
        .EUt(VA[GTValues.MV])

    // The distillation: Oil and Heavy Oil, the way GT's tower splits crude (it goes on from here)
    event.recipes.gtceu.distillation_tower('af9:shiny_oil_distillation')
        .inputFluids(Fluid.of('gtceu:shiny_oil', 1000))
        .outputFluids(Fluid.of('gtceu:oil', 600))
        .outputFluids(Fluid.of('gtceu:oil_heavy', 300))
        .chancedOutput('gtceu:sulfur_dust', 1500, 0)
        .duration(240)
        .EUt(VA[GTValues.MV])

    // Heavy Water: deuterium and oxygen back (GT has no other easy deuterium)
    event.recipes.gtceu.electrolyzer('af9:heavy_water_electrolysis')
        .inputFluids(Fluid.of('gtceu:heavy_water', 1000))
        .outputFluids(Fluid.of('gtceu:deuterium', 2000))
        .outputFluids(Fluid.of('gtceu:oxygen', 1000))
        .duration(400)
        .EUt(VA[GTValues.MV])
})
