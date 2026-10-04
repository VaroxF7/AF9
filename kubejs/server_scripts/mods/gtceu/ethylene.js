// AF9 - Late-game ethylene (methanol-to-olefins, Power-to-X): a full self-contained mass chain for LuV,
// bridging the gap until the Space Elevator's ethylene missions (planet type 6). Spec: docs/green-chemistry.md §7
//
//   carbon/coke (tree farm) + water -gasifier, LCR, LuV-> syngas (CO + H2)
//   syngas + extra H2 (water electrolysis) -ICI synthesis, LCR, LuV-> methanol
//   methanol -MTO over zeolite, LCR, LuV-> ethylene + water (circuit 1, ethylene mode)
//                                            ethylene + propene + water (circuit 2, mixed mode for PGMEA/fab)
//
// GT's own methanol (CO + H2, small batch) and bioethanol dehydration (MV, 1:1, farm-limited) are untouched;
// this line runs beside them at LuV batches. No new materials: carbon/coke/zeolite are GT's own.

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // ---- Steam gasification: C + H2O -> CO + H2, oxygen-blown entrained flow, ~1000 K ----
    // Two feeds, same syngas: carbon dust (charcoal/coke ground down) or coke dust straight from the
    // pyrolyse oven that already feeds the MG-Si furnace. Different item inputs, so no R7 subset clash.
    event.recipes.gtceu.large_chemical_reactor('af9:syngas_gasification')
        .itemInputs('4x gtceu:carbon_dust')
        .inputFluids(Fluid.of('minecraft:water', 4000))
        .outputFluids(Fluid.of('gtceu:carbon_monoxide', 4000), Fluid.of('gtceu:hydrogen', 4000))
        .duration(200)
        .EUt(VA[GTValues.LuV])

    event.recipes.gtceu.large_chemical_reactor('af9:syngas_gasification_coke')
        .itemInputs('4x gtceu:coke_dust')
        .inputFluids(Fluid.of('minecraft:water', 4000))
        .outputFluids(Fluid.of('gtceu:carbon_monoxide', 4000), Fluid.of('gtceu:hydrogen', 4000))
        .duration(200)
        .EUt(VA[GTValues.LuV])

    // ---- Methanol synthesis: CO + 2 H2 -> CH3OH, ICI high-pressure (Cu/ZnO, ~250 C, 100 bar) ----
    // LuV upscale of GT's small-batch monoxide route (4x the fluids, so not an R7 subset of it).
    // A methanol run needs twice the hydrogen the gasifier makes with its CO: the rest comes from
    // water electrolysis (infinite), and the MTO bed below pays back water.
    event.recipes.gtceu.large_chemical_reactor('af9:methanol_from_syngas')
        .inputFluids(Fluid.of('gtceu:carbon_monoxide', 4000), Fluid.of('gtceu:hydrogen', 8000))
        .outputFluids(Fluid.of('gtceu:methanol', 4000))
        .duration(200)
        .EUt(VA[GTValues.LuV])

    // ---- Methanol-to-olefins: UOP/Hydro MTO over a zeolite (SAPO-34 analogue, ~450 C fluidized bed) ----
    // The circuits are load-bearing: the ethylene recipe's inputs sit inside the mixed recipe's (lint R7),
    // and a circuit-less methanol-only recipe would sit inside GT's methanol-consuming recipes.
    // Ethylene mode: 2 CH3OH -> C2H4 + 2 H2O. 8,000 mB methanol -> 4,000 mB ethylene per 200 ticks.
    event.recipes.gtceu.large_chemical_reactor('af9:mto_ethylene')
        .circuit(1)
        .notConsumable('gtceu:zeolite_dust')
        .inputFluids(Fluid.of('gtceu:methanol', 8000))
        .outputFluids(Fluid.of('gtceu:ethylene', 4000), Fluid.of('minecraft:water', 8000))
        .duration(200)
        .EUt(VA[GTValues.LuV])

    // Mixed mode: 12 CH3OH -> 3 C2H4 + 2 C3H6 + 12 H2O. The propene feeds the PGMEA chain
    // (propylene oxide over titanium silicalite) and GT's other propene chemistry.
    event.recipes.gtceu.large_chemical_reactor('af9:mto_olefins_mixed')
        .circuit(2)
        .notConsumable('gtceu:zeolite_dust')
        .inputFluids(Fluid.of('gtceu:methanol', 12000))
        .outputFluids(
            Fluid.of('gtceu:ethylene', 3000),
            Fluid.of('gtceu:propene', 2000),
            Fluid.of('minecraft:water', 12000)
        )
        .duration(200)
        .EUt(VA[GTValues.LuV])
})
