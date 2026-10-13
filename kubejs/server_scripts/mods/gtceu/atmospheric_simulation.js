// AF9 - XPS and NVM circuits, built in Lunar atmosphere (startup_scripts/gtceu/atmospheric_simulation.js).
// These are the tiers circuits_af9.js had parked ("OUT FOR NOW"): the linear ladder past UHV,
// plasma solder canonical like the header there says (tin stays a dead budget option, unwritten).
// Every run breathes lunar air (Moon drilling or elevator missions); the room itself watches the door.

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // ---- The room: stainless pressure shell, glass bands, MV-grade bones round an MV hull ----
    event.shaped('gtceu:atmospheric_simulation_room', [
        'SGS',
        'GCG',
        'SMS'
    ], {
        S: '#forge:plates/stainless_steel',
        G: 'gtceu:tempered_glass',
        C: '#gtceu:circuits/ev',
        M: 'gtceu:mv_machine_hull'
    }).id('af9:atmospheric_simulation_room')

    // ---- XPS processor: UHV wetware mainframe with a Pico CPU and photonic dies ----
    event.recipes.gtceu.atmospheric_simulation('af9:xps_processor')
        .itemInputs('gtceu:wetware_processor_mainframe', 'af9:pico_cpu', '2x af9:photonic_ic_chip',
            '2x af9:spin_logic_chip', '8x gtceu:fine_sanguinite_wire')
        .inputFluids(Fluid.of('gtceu:plasma_solder', 72), Fluid.of('gtceu:lunar_air', 1000))
        .itemOutputs('af9:xps_processor')
        .duration(600)
        .EUt(VA[GTValues.UHV])

    // ---- XPS mainframe: two processors round the organized Pico circuit ----
    event.recipes.gtceu.atmospheric_simulation('af9:xps_processor_mainframe')
        .itemInputs('2x af9:xps_processor', 'af9:organized_pico_circuit', '4x af9:spin_logic_chip',
            '16x gtceu:fine_sanguinite_wire')
        .inputFluids(Fluid.of('gtceu:plasma_solder', 288), Fluid.of('gtceu:lunar_air', 2000))
        .itemOutputs('af9:xps_processor_mainframe')
        .duration(800)
        .EUt(VA[GTValues.UHV])

    // ---- NVM processor: XPS mainframe with a Pico rack and memristive dies ----
    event.recipes.gtceu.atmospheric_simulation('af9:nvm_processor')
        .itemInputs('af9:xps_processor_mainframe', 'af9:pico_circuit_rack', '2x af9:memristor_chip',
            '2x af9:quantum_dot_ic_chip', '8x gtceu:fine_sanguinite_wire')
        .inputFluids(Fluid.of('gtceu:plasma_solder', 72), Fluid.of('gtceu:lunar_air', 1000))
        .itemOutputs('af9:nvm_processor')
        .duration(600)
        .EUt(VA[GTValues.UHV])

    // ---- NVM mainframe: two processors, the memory wall comes down ----
    event.recipes.gtceu.atmospheric_simulation('af9:nvm_processor_mainframe')
        .itemInputs('2x af9:nvm_processor', '2x af9:pico_circuit_rack', '4x af9:memristor_chip',
            '4x af9:quantum_dot_ic_chip', '16x gtceu:fine_sanguinite_wire')
        .inputFluids(Fluid.of('gtceu:plasma_solder', 288), Fluid.of('gtceu:lunar_air', 2000))
        .itemOutputs('af9:nvm_processor_mainframe')
        .duration(800)
        .EUt(VA[GTValues.UHV])
})
