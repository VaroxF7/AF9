// AF9 - Bio Vat recipes (startup_scripts/gtceu/bio_vat.js).
// Biochaff-fed cultures for the Water Filtration Plant's LuV/ZPM biosorption modes (circuits 9 and 10 there): the
// LuV culture grows first, the ZPM culture is subcultured out of it. Each recipe names the glass it needs
// (glass_tier: tempered 1, laminated 2, fusion 3). A run is multiplied by how full the output hatch is.

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // ---- The vat: stainless tanks and glass, MV bones round an MV hull ----
    event.shaped('gtceu:bio_vat', [
        'SGS',
        'GCG',
        'SMS'
    ], {
        S: '#forge:plates/stainless_steel',
        G: 'gtceu:tempered_glass',
        C: '#gtceu:circuits/mv',
        M: 'gtceu:mv_machine_hull'
    }).id('af9:bio_vat')

    // ---- LuV culture: biochaff and water, slow biology; laminated glass ----
    event.recipes.gtceu.bio_vat('af9:bacteria_culture')
        .circuit(1)
        .itemInputs('4x gtceu:bio_chaff')
        .inputFluids(Fluid.of('minecraft:water', 2000))
        .outputFluids(Fluid.of('gtceu:bacteria_culture', 1000))
        .addData('glass_tier', 2)
        .duration(400)
        .EUt(VA[GTValues.MV])

    // ---- ZPM culture: subcultured out of the LuV strain; fusion glass ----
    event.recipes.gtceu.bio_vat('af9:enriched_bacteria_culture')
        .circuit(2)
        .inputFluids(Fluid.of('gtceu:bacteria_culture', 1000), Fluid.of('minecraft:water', 1000))
        .outputFluids(Fluid.of('gtceu:enriched_bacteria_culture', 1000))
        .addData('glass_tier', 3)
        .duration(400)
        .EUt(VA[GTValues.HV])
})
