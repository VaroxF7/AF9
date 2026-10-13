// AF9 - Plasma Heater: the sub multi that ionises osmium dust with Endion to osmium plasma,
// the Plasma Forge's heater fuel (structure: startup_scripts/gtceu/dtpf.js).

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // ---- The heater: stainless shell, glass bands, EV bones round an EV hull ----
    event.shaped('gtceu:plasma_heater', [
        'SGS',
        'GCG',
        'SMS'
    ], {
        S: '#forge:plates/titanium',
        G: 'gtceu:tempered_glass',
        C: '#gtceu:circuits/ev',
        M: 'gtceu:ev_machine_hull'
    }).id('af9:plasma_heater')

    // ---- Ionisation: 4 osmium dust + Endion -> heater plasma ----
    event.recipes.gtceu.plasma_heating('af9:osmium_plasma_ionisation')
        .itemInputs('4x gtceu:osmium_dust')
        .inputFluids(Fluid.of('gtceu:endion', 2000))
        .outputFluids(Fluid.of('gtceu:osmium_plasma', 2000))
        .duration(600)
        .EUt(VA[GTValues.EV])
})
