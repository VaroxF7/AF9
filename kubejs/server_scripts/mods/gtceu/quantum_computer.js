// AF9 - Quantum Computer: crafting. Machines: startup_scripts/gtceu/quantum_computer.js. Spec: docs/quantum-computer.md
//
// Tiers: the computer and its Rack Hatches are UV (their recipes run at ZPM-UV; the computer itself draws 131,072 EU/t (ZPM)
// a rack-amp, see the spec). The casings are ZPM, the heat vents EV to ZPM, one vent after the other. The processors
// the racks take are the circuits of the ladder, crystal (IV) up to NVM, and need nothing but being made.

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // ---- the casings of the corridor ----
    event.recipes.gtceu.assembler('af9:quantum_computer_casing')
        .itemInputs('gtceu:robust_machine_casing', '2x #gtceu:circuits/luv', '4x gtceu:stainless_steel_plate')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 288))
        .circuit(1)
        .itemOutputs('2x af9:quantum_computer_casing')
        .duration(100)
        .EUt(VA[GTValues.ZPM])

    event.recipes.gtceu.assembler('af9:quantum_computer_vent_casing')
        .itemInputs('af9:quantum_computer_casing', '2x gtceu:stainless_steel_rotor', 'gtceu:iv_electric_motor',
            '4x gtceu:aluminium_plate')
        .circuit(2)
        .itemOutputs('2x af9:quantum_computer_vent_casing')
        .duration(100)
        .EUt(VA[GTValues.ZPM])

    event.recipes.gtceu.assembler('af9:quantum_computer_advanced_casing')
        .itemInputs('af9:quantum_computer_casing', '2x #gtceu:circuits/zpm', '2x gtceu:zpm_sensor',
            '4x gtceu:tungsten_steel_plate')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 576))
        .circuit(3)
        .itemOutputs('2x af9:quantum_computer_advanced_casing')
        .duration(150)
        .EUt(VA[GTValues.ZPM])

    // ---- the controller ----
    event.shaped('gtceu:quantum_computer', ['CEC', 'FXF', 'CEC'], {
        C: '#gtceu:circuits/uv', E: 'gtceu:uv_emitter', F: 'gtceu:uv_field_generator',
        X: 'af9:quantum_computer_advanced_casing'
    }).id('af9:quantum_computer')

    // ---- the Computer Rack Hatch ----
    event.recipes.gtceu.assembler('af9:uv_computer_rack_hatch')
        .itemInputs('gtceu:uv_machine_hull', '2x af9:quantum_computer_advanced_casing', '2x #gtceu:circuits/uv',
            '2x gtceu:uv_conveyor_module', 'gtceu:uv_robot_arm')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 1152))
        .itemOutputs('gtceu:uv_computer_rack_hatch')
        .duration(300)
        .EUt(VA[GTValues.UV])

    // ---- the Rack Heat Vents: a vent after the vent, each tier of motor and rotor one better ----
    event.recipes.gtceu.assembler('af9:rack_heat_vent_mk1')
        .itemInputs('4x gtceu:aluminium_plate', 'gtceu:stainless_steel_rotor', 'gtceu:ev_electric_motor')
        .itemOutputs('af9:rack_heat_vent_mk1')
        .duration(200)
        .EUt(VA[GTValues.EV])

    event.recipes.gtceu.assembler('af9:rack_heat_vent_mk2')
        .itemInputs('af9:rack_heat_vent_mk1', '4x gtceu:titanium_plate', 'gtceu:tungsten_steel_rotor',
            'gtceu:iv_electric_motor')
        .itemOutputs('af9:rack_heat_vent_mk2')
        .duration(200)
        .EUt(VA[GTValues.IV])

    event.recipes.gtceu.assembler('af9:rack_heat_vent_mk3')
        .itemInputs('af9:rack_heat_vent_mk2', '4x gtceu:tungsten_steel_plate', 'gtceu:rhodium_plated_palladium_rotor',
            'gtceu:luv_electric_motor')
        .itemOutputs('af9:rack_heat_vent_mk3')
        .duration(200)
        .EUt(VA[GTValues.LuV])

    event.recipes.gtceu.assembler('af9:rack_heat_vent_mk4')
        .itemInputs('af9:rack_heat_vent_mk3', '4x gtceu:osmiridium_plate', 'gtceu:naquadah_alloy_rotor',
            'gtceu:zpm_electric_motor')
        .itemOutputs('af9:rack_heat_vent_mk4')
        .duration(200)
        .EUt(VA[GTValues.ZPM])
})
