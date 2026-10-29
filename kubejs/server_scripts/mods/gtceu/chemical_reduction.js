// AF9 - Chemical reduction: the roasted oxides of GT's metallurgy reduced by hydrogen in the Chemical Reactor, and
// tungsten's hydrogen route (materials: af9-core, registry/AF9Materials (chemicalReduction())). Spec: docs/chemical-reduction.md
//
// Hydrogen reduction after TerraFirmaGreg-Modern's chromium and tungsten scripts (recipes.chromium.js,
// recipes.tungsten.js; LGPL-3.0, see docs/chemical-reduction.md): the oxide meets hydrogen, the metal dust and water
// come out, and no carbon is burnt. GT roasts its sulfides to these oxides (cupric oxide, cobalt oxide, massicot,
// garnierite, antimony trioxide) and its tin ore is an oxide already; this is the second way down, beside the
// smelting furnace.
//
//   MO + H2        -reactor->  M + H2O                  (Cu, Co, Pb, Ni)
//   SnO2 + 2 H2    -reactor->  Sn + 2 H2O
//   Sb2O3 + 3 H2   -reactor->  2 Sb + 3 H2O
//   H2WO4          -EBF->      WO3 + H2O                (calcination, 1000 K)
//   WO3 + 3 H2     -reactor->  W + 3 H2O                (replaces GT's one-step electrolysis of the acid)
//   MoO3 + 3 H2    -reactor->  Mo + 3 H2O               (the trioxide of the fab line's molybdenite roast)

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const gt = event.recipes.gtceu

    // GT electrolyses tungstic acid straight to the metal; the calcination and the hydrogen step replace it
    event.remove({ id: 'gtceu:electrolyzer/tungstic_acid_electrolysis' })

    // one oxide, its hydrogen, the metal and the water: dust counts are GT's (atoms per formula unit)
    const reduce = (id, oxide, atoms, hydrogen, metal, metalCount, tier, duration) => {
        gt.chemical_reactor('af9:chemical_reduction/' + id)
            .itemInputs(atoms + 'x gtceu:' + oxide + '_dust')
            .inputFluids(Fluid.of('gtceu:hydrogen', hydrogen * 1000))
            .itemOutputs(metalCount + 'x gtceu:' + metal + '_dust')
            .outputFluids(Fluid.of('minecraft:water', hydrogen * 1000))
            .duration(duration)
            .EUt(VA[tier])
    }

    reduce('copper', 'cupric_oxide', 2, 1, 'copper', 1, GTValues.MV, 160)
    reduce('cobalt', 'cobalt_oxide', 2, 1, 'cobalt', 1, GTValues.MV, 200)
    reduce('lead', 'massicot', 2, 1, 'lead', 1, GTValues.LV, 140)
    reduce('nickel', 'garnierite', 2, 1, 'nickel', 1, GTValues.MV, 200)
    reduce('tin', 'cassiterite', 3, 2, 'tin', 1, GTValues.MV, 180)
    reduce('antimony', 'antimony_trioxide', 5, 3, 'antimony', 2, GTValues.MV, 240)

    // ---- tungsten: tungstic acid -> trioxide -> metal ----
    gt.electric_blast_furnace('af9:chemical_reduction/tungstic_acid_calcination')
        .itemInputs('7x gtceu:tungstic_acid_dust')
        .itemOutputs('4x gtceu:tungsten_trioxide_dust')
        .outputFluids(Fluid.of('gtceu:steam', 1000))
        .blastFurnaceTemp(1000)
        .duration(260)
        .EUt(VA[GTValues.HV])

    gt.chemical_reactor('af9:chemical_reduction/tungsten')
        .itemInputs('4x gtceu:tungsten_trioxide_dust')
        .inputFluids(Fluid.of('gtceu:hydrogen', 3000))
        .itemOutputs('gtceu:tungsten_dust')
        .outputFluids(Fluid.of('minecraft:water', 3000))
        .duration(280)
        .EUt(VA[GTValues.HV])

    // ---- molybdenum from the fab line's trioxide ----
    gt.chemical_reactor('af9:chemical_reduction/molybdenum')
        .itemInputs('4x gtceu:molybdenum_trioxide_dust')
        .inputFluids(Fluid.of('gtceu:hydrogen', 3000))
        .itemOutputs('gtceu:molybdenum_dust')
        .outputFluids(Fluid.of('minecraft:water', 3000))
        .duration(260)
        .EUt(VA[GTValues.HV])
})
