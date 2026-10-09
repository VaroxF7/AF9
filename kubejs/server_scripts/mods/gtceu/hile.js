// AF9 - the Hyper-Intensity Laser Engraver: its controller and casings (machine: startup_scripts/gtceu/hile.js; it runs plasma
// soldering, recipes in solders.js). Spec: docs/solders.md

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // the controller: a UHV hull with emitters and field generators, under the engraver's own optics
    event.shaped('gtceu:hyper_intensity_laser_engraver', ['EFE', 'CHC', 'PRP'], {
        E: 'gtceu:uhv_emitter', F: 'gtceu:uhv_field_generator', C: '#gtceu:circuits/uhv', H: 'gtceu:uhv_machine_hull',
        P: 'gtceu:uhv_electric_piston', R: 'gtceu:uhv_robot_arm'
    }).id('af9:hyper_intensity_laser_engraver')

    // the casing: tungstensteel and tritanium round an emitter's lens; the plate: the same in neutronium
    event.recipes.gtceu.assembler('af9:laser_containment_casing')
        .itemInputs('4x gtceu:tungsten_steel_plate', '2x gtceu:tritanium_plate', 'gtceu:tungstensteel_frame')
        .circuit(1)
        .itemOutputs('4x af9:laser_containment_casing')
        .duration(200)
        .EUt(VA[GTValues.EV])
    event.recipes.gtceu.assembler('af9:laser_resistant_plate')
        .itemInputs('4x gtceu:neutronium_plate', '2x gtceu:tritanium_plate', 'gtceu:tungstensteel_frame')
        .circuit(2)
        .itemOutputs('2x af9:laser_resistant_plate')
        .duration(300)
        .EUt(VA[GTValues.IV])
})
