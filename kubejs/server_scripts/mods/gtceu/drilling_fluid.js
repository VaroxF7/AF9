// AF9 - Drilling Fluid, a real water-based mud instead of GT's "stone dust + lubricant + water" (materials: af9-core,
// registry/AF9Materials (drillingMud()). Spec: docs/drilling-fluid.md). Nothing in it is above HV and no step needs oil,
// asteroids or the void miner: the first thing that drinks drilling fluid is the HV Void Miner.
//
//   water + soda ash                         -mixer->    softened water + calcite   (Ca2+ + Na2CO3 -> CaCO3, Na+ stays)
//   bentonite + soda ash + softened water    -mixer->    sodium bentonite slurry + calcite   (sodium activation)
//   acetic acid + Cl2                        -reactor->  chloroacetic acid + HCl
//   wood dust + NaOH + water                 -mixer->    alkali cellulose                     (mercerisation)
//   alkali cellulose + chloroacetic acid     -reactor->  CMC solution + salt                  (Williamson etherification)
//   slurry + CMC + salt + calcite + NaOH     -mixer->    Drilling Fluid                       (viscosifier, shale inhibitor, weight, pH)
ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const gt = event.recipes.gtceu

    // GT's mud is gone; the void miner (miner.js) takes the new one
    for (const id of ['gtceu:mixer/drilling_fluid', 'gtceu:mixer/drilling_fluid_distilled']) event.remove({ id: id })

    // ---- hard water would flocculate the clay: soda ash takes calcium and magnesium out as carbonate ----
    gt.mixer('af9:drilling_fluid/water_softening')
        .itemInputs('gtceu:soda_ash_dust')
        .inputFluids(Fluid.of('minecraft:water', 4000))
        .itemOutputs('gtceu:calcite_dust')
        .outputFluids(Fluid.of('gtceu:softened_water', 4000))
        .duration(100)
        .EUt(VA[GTValues.LV])

    // ---- calcium bentonite is swapped for sodium bentonite, which swells to many times its volume ----
    gt.mixer('af9:drilling_fluid/bentonite_activation')
        .itemInputs('4x gtceu:bentonite_dust', 'gtceu:soda_ash_dust')
        .inputFluids(Fluid.of('gtceu:softened_water', 3500))
        .itemOutputs('gtceu:calcite_dust')
        .outputFluids(Fluid.of('gtceu:sodium_bentonite_slurry', 3500))
        .duration(200)
        .EUt(VA[GTValues.MV])

    // ---- the polymer (carboxymethyl cellulose) holds the cuttings up; it starts as sawdust and chloroacetic acid ----
    gt.chemical_reactor('af9:drilling_fluid/chloroacetic_acid')
        .circuit(1)
        .inputFluids(Fluid.of('gtceu:acetic_acid', 1000), Fluid.of('gtceu:chlorine', 1000))
        .outputFluids(Fluid.of('gtceu:chloroacetic_acid', 1000), Fluid.of('gtceu:hydrochloric_acid', 1000))
        .duration(240)
        .EUt(VA[GTValues.HV])

    gt.mixer('af9:drilling_fluid/mercerisation')
        .itemInputs('6x gtceu:wood_dust', 'gtceu:sodium_hydroxide_dust')
        .inputFluids(Fluid.of('minecraft:water', 1500))
        .outputFluids(Fluid.of('gtceu:alkali_cellulose', 1500))
        .duration(160)
        .EUt(VA[GTValues.MV])

    gt.chemical_reactor('af9:drilling_fluid/cmc_etherification')
        .itemInputs('gtceu:sodium_hydroxide_dust')
        .inputFluids(Fluid.of('gtceu:alkali_cellulose', 1500), Fluid.of('gtceu:chloroacetic_acid', 750))
        .itemOutputs('gtceu:salt_dust')
        .outputFluids(Fluid.of('gtceu:cmc_solution', 1500))
        .duration(300)
        .EUt(VA[GTValues.HV])

    // ---- the mud itself: clay for body, CMC for carrying power, salt against swelling shale, calcite for weight, lye for pH ----
    gt.mixer('af9:drilling_fluid/blend')
        .itemInputs('2x gtceu:salt_dust', 'gtceu:calcite_dust', 'gtceu:sodium_hydroxide_dust')
        .inputFluids(Fluid.of('gtceu:sodium_bentonite_slurry', 3500), Fluid.of('gtceu:cmc_solution', 1500))
        .outputFluids(Fluid.of('gtceu:drilling_fluid', 5000))
        .duration(200)
        .EUt(VA[GTValues.HV])
})
