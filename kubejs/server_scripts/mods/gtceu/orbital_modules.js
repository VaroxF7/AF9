// AF9 - Crafting of the Orbital Lithography Array Mk2's modules (startup_scripts/gtceu/orbital_modules.js): UV machines, a
// hull and the parts of what the module does.

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const solder = mb => Fluid.of('gtceu:soldering_alloy', mb)
    const modules = {
        // parallels: a computer, so circuits and robot arms
        parallel: ['4x gtceu:uv_robot_arm', '4x gtceu:uv_emitter', '8x #gtceu:circuits/uhv', '4x gtceu:tritanium_plate'],
        // less energy: electric pumps and field generators that lose less
        efficiency: ['2x gtceu:uv_field_generator', '2x gtceu:uv_electric_pump', '4x #gtceu:circuits/uv', '4x gtceu:tritanium_plate'],
        // a supercooler: pumps and sensors round endion coolant
        evaporation: ['4x gtceu:uv_electric_pump', '2x gtceu:uv_sensor', '4x #gtceu:circuits/uhv', '8x gtceu:neutronium_plate'],
        // stabilisation: field generators and sensors
        sequencer: ['4x gtceu:uv_field_generator', '4x gtceu:uv_sensor', '8x #gtceu:circuits/uhv', '8x gtceu:neutronium_plate']
    }
    Object.keys(modules).forEach(kind => {
        event.recipes.gtceu.assembler(`af9:orbital_module_${kind}`)
            .itemInputs(['gtceu:uv_machine_hull'].concat(modules[kind]))
            .inputFluids(solder(2304))
            .itemOutputs(`gtceu:orbital_module_${kind}`)
            .duration(600)
            .EUt(VA[GTValues.UV])
    })
})

// the Mk2 tower's two casings: magnet pillars (neutronium plates round a field generator) and light strips (tungsten
// carbide plates round an HSS-G coil)
ServerEvents.recipes(event => {
    event.shaped('4x af9:orbital_magnet_casing', ['PPP', 'PFP', 'PPP'], {
        P: 'gtceu:neutronium_plate', F: 'gtceu:uv_field_generator'
    }).id('af9:orbital_magnet_casing')
    event.shaped('4x af9:orbital_light_casing', ['PPP', 'PCP', 'PPP'], {
        P: 'gtceu:tungsten_carbide_plate', C: 'gtceu:hssg_coil_block'
    }).id('af9:orbital_light_casing')
})
