// AF9 - the Hyper-Intensity Laser Engraver: its controller and casings (machine: startup_scripts/gtceu/hile.js; it runs plasma
// soldering, recipes in solders.js). Spec: docs/solders.md

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // the controller: a UHV hull with emitters and field generators, under the engraver's own optics
    // (UV parts and circuits: GT has no parts above UV while its high-tier content is off, as it is in the pack;
    // the engraver has to be buildable before the first UHV circuit it solders)
    event.shaped('gtceu:hyper_intensity_laser_engraver', ['EFE', 'CHC', 'PRP'], {
        E: 'gtceu:uv_emitter', F: 'gtceu:uv_field_generator', C: '#gtceu:circuits/uv', H: 'gtceu:uhv_machine_hull',
        P: 'gtceu:uv_electric_piston', R: 'gtceu:uv_robot_arm'
    }).id('af9:hyper_intensity_laser_engraver')

    // the casing: tungstensteel and tritanium round an emitter's lens; the plate: the same in neutronium
    event.recipes.gtceu.assembler('af9:laser_containment_casing')
        .itemInputs('4x gtceu:tungsten_steel_plate', '2x gtceu:tritanium_plate', 'gtceu:tungsten_steel_frame')
        .circuit(1)
        .itemOutputs('4x af9:laser_containment_casing')
        .duration(200)
        .EUt(VA[GTValues.EV])
    event.recipes.gtceu.assembler('af9:laser_resistant_plate')
        .itemInputs('4x gtceu:neutronium_plate', '2x gtceu:tritanium_plate', 'gtceu:tungsten_steel_frame')
        .circuit(2)
        .itemOutputs('2x af9:laser_resistant_plate')
        .duration(300)
        .EUt(VA[GTValues.IV])
})
