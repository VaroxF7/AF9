// AF9 - Biofuels rework: a full farm-to-tank line for bioethanol, fuel ethanol and biodiesel
// (materials: startup_scripts/gtceu/biofuels.js). Spec: docs/green-chemistry.md
//
//   crops + water          -brewery->     fermented biomass (GT's own fluid)
//   fermented biomass      -distillery->  bioethanol (the 95 % azeotrope)
//   bioethanol             -sieves->      ethanol (fuel grade, over AF9's molecular sieves)
//   bioethanol             -dehydrate->   ethylene (the rockets' fuel needs this, no oil required)
//   seeds                  -extractor->   seed oil (GT's own fluid)
//   seed oil + methanol    -reactor->     biodiesel + glycerol (transesterification, GTNH-style)
//   biodiesel              -diesel gen->  power;  bioethanol -gas turbine-> power
//
// GT's own biomass, ethanol and oil chemistry is untouched; this line runs beside it.

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // ---- Mashing: farm crops + water ferment into GT's fermented biomass ----
    // forEach, not a for loop: Rhino keeps a loop's const at its first value (lint S3)
    const crops = [
        'wheat', 'potato', 'carrot', 'beetroot', 'melon_slice',
        'apple', 'sugar_cane', 'sweet_berries'
    ]
    crops.forEach(crop => {
        event.recipes.gtceu.brewery(`af9:mash_${crop}`)
            .itemInputs(`4x minecraft:${crop}`)
            .inputFluids(Fluid.of('minecraft:water', 1000))
            .outputFluids(Fluid.of('gtceu:fermented_biomass', 1000))
            .duration(400)
            .EUt(VA[GTValues.LV])
    })

    // ---- Distilling: the mash gives up its 95 % spirit ----
    event.recipes.gtceu.distillery('af9:bioethanol_distillation')
        .inputFluids(Fluid.of('gtceu:fermented_biomass', 1000))
        .outputFluids(Fluid.of('gtceu:bioethanol', 600))
        .duration(200)
        .EUt(VA[GTValues.MV])

    // ---- Drying: 3A molecular sieves take the last water, fuel-grade ethanol comes out ----
    event.recipes.gtceu.chemical_reactor('af9:bioethanol_drying')
        .notConsumable('af9:molecular_sieve')
        .inputFluids(Fluid.of('gtceu:bioethanol', 1000))
        .outputFluids(Fluid.of('gtceu:ethanol', 900))
        .duration(200)
        .EUt(VA[GTValues.MV])

    // ---- Dehydration: C2H5OH -> C2H4 + H2O, the oil-free road to ethylene ----
    // Circuit 1 is load-bearing: without it this recipe's inputs sit inside the drying recipe's (lint R7)
    event.recipes.gtceu.chemical_reactor('af9:bioethanol_dehydration')
        .circuit(1)
        .inputFluids(Fluid.of('gtceu:bioethanol', 1000))
        .outputFluids(Fluid.of('gtceu:ethylene', 1000), Fluid.of('minecraft:water', 500))
        .duration(200)
        .EUt(VA[GTValues.MV])

    // ---- Pressing: seeds give GT's seed oil ----
    const seeds = ['pumpkin_seeds', 'melon_seeds', 'beetroot_seeds', 'wheat_seeds']
    seeds.forEach(seed => {
        event.recipes.gtceu.extractor(`af9:seed_oil_${seed}`)
            .itemInputs(`4x minecraft:${seed}`)
            .outputFluids(Fluid.of('gtceu:seed_oil', 200))
            .duration(100)
            .EUt(VA[GTValues.LV])
    })

    // ---- Transesterification: seed oil + methanol, soda lye as the catalyst ----
    event.recipes.gtceu.chemical_reactor('af9:biodiesel_transesterification')
        .itemInputs('gtceu:sodium_hydroxide_dust')
        .inputFluids(Fluid.of('gtceu:seed_oil', 1000), Fluid.of('gtceu:methanol', 100))
        .outputFluids(Fluid.of('gtceu:biodiesel', 1000), Fluid.of('gtceu:glycerol', 100))
        .duration(200)
        .EUt(VA[GTValues.HV])

    // ---- Burning it: AF9-balanced fuel values (38,400 EU a bucket of biodiesel, 32,000 of bioethanol) ----
    event.recipes.gtceu.combustion_generator('af9:biodiesel_combustion')
        .inputFluids(Fluid.of('gtceu:biodiesel', 1000))
        .duration(1200)
        .EUt(32)

    event.recipes.gtceu.gas_turbine('af9:bioethanol_turbine')
        .inputFluids(Fluid.of('gtceu:bioethanol', 1000))
        .duration(1000)
        .EUt(32)
})
