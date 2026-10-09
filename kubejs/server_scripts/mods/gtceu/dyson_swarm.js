// AF9 - the Dyson Swarm: its controller and casings, the three sails and the cycle (machine: startup_scripts/gtceu/dyson_swarm.js;
// sails and machine logic: af9-core DysonSwarmMachine, DysonSails). Spec: docs/dyson-swarm.md
//
// 1. Controller and casings of GTNH Intergalactic's Dyson Swarm, built from the pack's UHV materials
// 2. The sails, lowest to highest: Allthemodium (100 % of the base yield), Unobtainium Alloy (200 %) and Chromodynium Star
//    Matter Tritan Alloy (350 %). Each tier is made from the sails of the tier below, so the better sails are upgrades. They
//    are researched Assembly Line recipes at UHV voltage on 100 / 300 / 1,000 A (the same amps as the UHV / XPS / NVM circuits)
// 3. The cycle: an hour of supercooled hydrogen for the receiver. The power is the sails' (DysonSwarmMachine.SWARM)

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const V = GTValues.V

    // the Allthemodium mod's plates: the lowest sail's, and the alloy's of the middle one
    const ALLTHEMODIUM_PLATE = 'allthemodium:allthemodium_plate'
    const ALLOY_PLATE = 'allthemodium:unobtainium_plate'

    // ---- 1. Controller and casings ----
    // (UV parts: GT has no parts above UV while its high-tier content is off, as it is in the pack)
    event.shaped('gtceu:dyson_swarm', ['ESE', 'FHF', 'CPC'], {
        E: 'gtceu:uv_emitter', S: 'gtceu:uv_sensor', F: 'gtceu:uv_field_generator', H: 'gtceu:uhv_machine_hull',
        C: '#gtceu:circuits/uhv', P: 'gtceu:uv_electric_pump'
    }).id('af9:dyson_swarm')

    // four casings from plates and a frame, one circuit each (so JEI keeps the recipes apart)
    const casing = (out, circuit, items, fluid) => {
        const r = event.recipes.gtceu.assembler(`af9:${out}`)
            .itemInputs(items)
            .circuit(circuit)
            .itemOutputs(`4x af9:${out}`)
            .duration(200)
            .EUt(VA[GTValues.EV])
        if (fluid) r.inputFluids(fluid)
    }
    casing('dyson_receiver_casing', 1, ['4x gtceu:tungsten_steel_plate', '2x gtceu:titanium_plate', 'gtceu:hsss_frame'])
    casing('dyson_receiver_dish', 2, ['4x gtceu:tritanium_plate', '4x gtceu:europium_plate', 'gtceu:titanium_frame'],
        Fluid.of('gtceu:polybenzimidazole', 144))
    casing('dyson_deployment_casing', 3, ['4x gtceu:stainless_steel_plate', '2x gtceu:titanium_plate', 'gtceu:titanium_frame'])
    casing('dyson_deployment_core', 4, ['4x gtceu:naquadah_alloy_plate', '2x gtceu:neutronium_plate', 'gtceu:tritanium_frame'])
    casing('dyson_deployment_magnet', 5, ['4x gtceu:neodymium_plate', '2x gtceu:naquadah_alloy_plate', 'gtceu:tritanium_frame'],
        Fluid.of('gtceu:supercooled_hydrogen', 500))
    casing('dyson_control_casing', 6, ['4x gtceu:tungsten_steel_plate', '2x gtceu:europium_plate', 'gtceu:titanium_frame'])
    casing('dyson_control_primary', 7, ['4x gtceu:naquadah_alloy_plate', '8x gtceu:fine_sanguinite_wire', 'gtceu:titanium_frame'])
    casing('dyson_control_secondary', 8, ['4x gtceu:naquadah_alloy_plate', '4x gtceu:fine_sanguinite_wire', 'gtceu:titanium_frame'])
    casing('dyson_control_toroid', 9, ['4x gtceu:tritanium_plate', '16x gtceu:fine_sanguinite_wire', 'gtceu:tritanium_frame'])

    // ---- 2. The sails ----
    // sails(id, amps, spec): an Assembly Line recipe at UHV voltage on the tier's amps, ONE sail a run. Every sail has to be
    // researched first (the Research Station scans the research item into a data stick, with computation: spec.cwu).
    // Each tier takes 128 carbon fiber mesh and 128 fine sanguinite wire (two stacks of 64 each: a bus slot holds 64)
    const sails = (id, amps, spec) => {
        event.recipes.gtceu.assembly_line(`af9:${id}`)
            .itemInputs(spec.items.concat(['64x gtceu:carbon_fiber_mesh', '64x gtceu:carbon_fiber_mesh',
                '64x gtceu:fine_sanguinite_wire', '64x gtceu:fine_sanguinite_wire']))
            .inputFluids(spec.fluids)
            .itemOutputs(`af9:${id}`)
            .duration(spec.duration)
            .EUt(VA[GTValues.UHV], amps)
            .stationResearch(b => b
                .researchStack(Item.of(spec.research))
                .CWUt(spec.cwu)
                .EUt(VA[GTValues.UHV]))
    }
    // Allthemodium: 100 % - 30 s: plates, photonic dies and neutronium under the mesh and the wire
    sails('allthemodium_sail', 100, {
        items: [`16x ${ALLTHEMODIUM_PLATE}`, '16x af9:photonic_ic_chip', '8x gtceu:neutronium_plate'],
        fluids: [Fluid.of('gtceu:plasma_solder', 576), Fluid.of('gtceu:polybenzimidazole', 576)],
        research: 'af9:photonic_package', cwu: 96, duration: 600 })
    // Unobtainium Alloy: 200 % - 45 s: the Allthemodium sail re-laid on the alloy sheet with spin logic
    sails('unobtainium_alloy_sail', 300, {
        items: ['af9:allthemodium_sail', `16x ${ALLOY_PLATE}`, '16x af9:spin_logic_chip', '16x gtceu:neutronium_plate'],
        fluids: [Fluid.of('gtceu:plasma_solder', 1152), Fluid.of('gtceu:polybenzimidazole', 1152)],
        research: 'af9:allthemodium_sail', cwu: 128, duration: 900 })
    // Chromodynium Star Matter Tritan Alloy: 350 % - 60 s: the alloy sail plated in chromodynium and tritanium under the plasma
    // of strange matter, soldered with 10,000 mB plasma solder and 1,000 mB nickel plasma a sail; the most computation to research
    sails('chromodynium_star_matter_tritan_alloy_sail', 1000, {
        items: ['af9:unobtainium_alloy_sail', '16x gtceu:chromodynium_plate', '16x gtceu:tritanium_plate',
            '16x af9:memristor_chip'],
        fluids: [Fluid.of('gtceu:strange_matter_plasma', 1152), Fluid.of('gtceu:plasma_solder', 10000),
            Fluid.of('gtceu:nickel_plasma', 1000)],
        research: 'af9:unobtainium_alloy_sail', cwu: 192, duration: 1200 })

    // ---- 3. The cycle ----
    // An hour on supercooled hydrogen (360 B) for the receiver. The EU/t is the machine's: V[UHV] here, scaled to what the
    // sails catch (DysonSwarmMachine.SWARM).
    event.recipes.gtceu.dyson_swarm('af9:dyson_swarm_cycle')
        .inputFluids(Fluid.of('gtceu:supercooled_hydrogen', 360000))
        .duration(72000)
        .EUt(-V[GTValues.UHV])
})
