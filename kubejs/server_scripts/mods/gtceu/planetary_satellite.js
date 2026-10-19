// AF9 - Planetary Satellite: crafting and the neutron rays. Machine: startup_scripts/gtceu/planetary_satellite.js.
// Spec: docs/planetary-satellite.md
//
// The satellite is the only source of neutronium now: GT's fusion recipe for it (americium and naquadria, the
// Mk3 fusion reactor) is removed. It runs UV, 1 A (524,288 EU/t) and turns a Neutron Ray Reflector into 12 - 120 mB
// of liquid neutronium every 12 s; parallel hatches and batch mode work. The reflector is not consumed: it wears
// one point a completed cycle (AF9Modifiers.satelliteWear) and breaks after its 1,000 / 5,000 / 10,000 cycles.

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

    // the rays: a reflector for the dish (not consumed: it wears a point a cycle and breaks at the end of its
    // durability), 12 to 120 mB of liquid neutronium out (rolled in 12 mB steps by AF9Modifiers.SATELLITE_YIELD,
    // once for every parallel); the better the reflector the higher the lowest roll
    const reflectors = [
        { id: 'iridium', reflector: 'af9:iridium_ray_reflector' },                          // 12 - 120 mB
        { id: 'osmiridium', reflector: 'af9:osmiridium_ray_reflector' },                    // 36 - 120 mB
        { id: 'unobtainium_vibranium', reflector: 'af9:unobtainium_vibranium_ray_reflector' } // 96 - 120 mB
    ]
    reflectors.forEach(r => {
        event.recipes.gtceu.neutron_collection(`af9:neutronium_rays_${r.id}`)
            .notConsumable(r.reflector)
            .outputFluids(Fluid.of('gtceu:neutronium', 12))
            .duration(240)
            .EUt(GTValues.VA[GTValues.UV])
    })

    // the reflectors themselves: a ladder, each built on the tier below (assembler, UV). The first starts from GT's
    // own Iridium Neutron Reflector; a worn one trades up the same way, the new materials being the rest of the cost
    const ladder = [
        { id: 'iridium_ray_reflector', base: 'gtceu:neutron_reflector', cost: '4x gtceu:iridium_plate' },
        { id: 'osmiridium_ray_reflector', base: 'af9:iridium_ray_reflector', cost: '4x gtceu:osmiridium_plate' },
        { id: 'unobtainium_vibranium_ray_reflector', base: 'af9:osmiridium_ray_reflector',
            cost: '4x allthemodium:unobtainium_vibranium_alloy_ingot' }
    ]
    ladder.forEach(r => {
        event.recipes.gtceu.assembler(`af9:${r.id}`)
            .itemInputs(r.base, r.cost)
            .itemOutputs(`af9:${r.id}`)
            .duration(400)
            .EUt(GTValues.VA[GTValues.UV])
    })
})
