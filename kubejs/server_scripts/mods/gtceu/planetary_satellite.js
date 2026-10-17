// AF9 - Planetary Satellite: crafting and the neutron rays. Machine: startup_scripts/gtceu/planetary_satellite.js.
// Spec: docs/planetary-satellite.md
//
// The satellite is the only source of neutronium now: GT's fusion recipe for it (americium and naquadria, the
// Mk3 fusion reactor) is removed. It runs UV, 1 A (524,288 EU/t, a UV energy hatch) and gathers 8 small piles of
// neutronium dust (2 dust) every 30 s on 2,000 mB of supercooled xenon for the detectors; a UHV hatch halves the time.

ServerEvents.recipes(event => {
    // neutronium comes from the satellite and nowhere else (the plasma of this recipe is what GT's fluid solidifier
    // turned into ingots; the dust, the plates and everything made of them are untouched)
    event.remove({ id: 'gtceu:fusion_reactor/americium_and_naquadria_to_neutronium_plasma' })

    // the casing: Enriched Naquadah Trinium Europium Duranide round an HSS-S frame, on a stable titanium casing
    event.recipes.gtceu.assembler('af9:satellite_casing')
        .itemInputs('gtceu:stable_machine_casing', '6x gtceu:enriched_naquadah_trinium_europium_duranide_ingot',
            'gtceu:hsss_frame')
        .circuit(27)
        .itemOutputs('2x af9:satellite_casing')
        .duration(200)
        .EUt(GTValues.VA[GTValues.UV])

    // the controller
    event.shaped('gtceu:planetary_satellite', ['CFC', 'DXD', 'CFC'], {
        C: '#gtceu:circuits/uv', F: 'gtceu:uv_field_generator', D: 'gtceu:uv_sensor', X: 'af9:satellite_casing'
    }).id('af9:planetary_satellite')

    // the rays
    event.recipes.gtceu.neutron_collection('af9:small_neutronium_dust')
        .inputFluids('#af9:coolant/xenon 2000')
        .itemOutputs('8x gtceu:small_neutronium_dust')
        .duration(600)
        .EUt(GTValues.VA[GTValues.UV])
})
