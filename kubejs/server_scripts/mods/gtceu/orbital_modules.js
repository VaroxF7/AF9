// AF9 - Crafting of the Orbital Lithography Array Mk2's modules (startup_scripts/gtceu/orbital_modules.js): UV machines, a
// hull and the parts of what the module does.

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const solder = mb => Fluid.of('gtceu:soldering_alloy', mb)
    const modules = {
        // faster runs: field generators hold the beam steady
        chrono: ['2x gtceu:uv_field_generator', '2x gtceu:uv_sensor', '4x #gtceu:circuits/uv', '4x gtceu:tritanium_plate'],
        // parallel prints: robot arms and emitters for more beams at once
        multiplex: ['4x gtceu:uv_robot_arm', '4x gtceu:uv_emitter', '4x #gtceu:circuits/uhv', '4x gtceu:tritanium_plate'],
        // fewer broken wafers: sensors that catch the fault before it prints
        yield: ['4x gtceu:uv_sensor', '2x gtceu:uv_emitter', '4x #gtceu:circuits/uv', '8x gtceu:glass_lens'],
        // less energy and coolant: pumps that recover the coolant, field generators that hold the heat in
        thrift: ['2x gtceu:uv_electric_pump', '2x gtceu:uv_field_generator', '4x #gtceu:circuits/uv', '4x gtceu:tritanium_plate']
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
