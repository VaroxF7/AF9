// AF9 - Planetary Satellite: crafting and the neutron rays. Machine: startup_scripts/gtceu/planetary_satellite.js.
// Spec: docs/planetary-satellite.md
//
// The satellite is the only source of neutronium now: GT's fusion recipe for it (americium and naquadria, the
// Mk3 fusion reactor) is removed. It runs UV, 1 A (524,288 EU/t) and turns a reflector plate into 12 - 120 mB of
// liquid neutronium every 12 s; parallel hatches and batch mode work.

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

    // the rays: a reflector plate for the dish, 12 to 120 mB of liquid neutronium out (rolled in 12 mB steps by
    // AF9Modifiers.SATELLITE_YIELD, once for every parallel); the better the plate the higher the lowest roll
    const reflectors = [
        { id: 'iridium', plate: 'gtceu:iridium_plate' },            // 12 - 120 mB
        { id: 'osmiridium', plate: 'gtceu:osmiridium_plate' },      // 36 - 120 mB
        { id: 'tritanium', plate: 'gtceu:tritanium_plate' }         // 72 - 120 mB
    ]
    reflectors.forEach(r => {
        event.recipes.gtceu.neutron_collection(`af9:neutronium_rays_${r.id}`)
            .itemInputs(r.plate)
            .outputFluids(Fluid.of('gtceu:neutronium', 12))
            .duration(240)
            .EUt(GTValues.VA[GTValues.UV])
    })
})
