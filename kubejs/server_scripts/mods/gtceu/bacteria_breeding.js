// AF9 - Bacteria Breeding Station recipes (startup_scripts/gtceu/bacteria_breeding.js).
// Biochaff-fed cultures for the Water Filtration Plant's LuV/ZPM biosorption modes (circuits 9
// and 10 there): the LuV culture grows first, the ZPM culture is subcultured out of it.

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // ---- The station: stainless tanks and glass, MV bones round an MV hull ----
    event.shaped('gtceu:bacteria_breeding_station', [
        'SGS',
        'GCG',
        'SMS'
    ], {
        S: '#forge:plates/stainless_steel',
        G: 'gtceu:tempered_glass',
        C: '#gtceu:circuits/mv',
        M: 'gtceu:mv_machine_hull'
    }).id('af9:bacteria_breeding_station')

    // ---- LuV culture: biochaff and water, slow biology ----
    event.recipes.gtceu.bacteria_breeding('af9:bacteria_culture')
        .circuit(1)
        .itemInputs('4x gtceu:bio_chaff')
        .inputFluids(Fluid.of('minecraft:water', 2000))
        .outputFluids(Fluid.of('gtceu:bacteria_culture', 1000))
        .duration(400)
        .EUt(VA[GTValues.MV])

    // ---- ZPM culture: subcultured out of the LuV strain ----
    event.recipes.gtceu.bacteria_breeding('af9:enriched_bacteria_culture')
        .circuit(2)
        .inputFluids(Fluid.of('gtceu:bacteria_culture', 1000), Fluid.of('minecraft:water', 1000))
        .outputFluids(Fluid.of('gtceu:enriched_bacteria_culture', 1000))
        .duration(400)
        .EUt(VA[GTValues.HV])
})
