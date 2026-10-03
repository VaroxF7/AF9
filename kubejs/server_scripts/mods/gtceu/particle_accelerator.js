// AF9 - Particle Accelerator recipes (startup_scripts/gtceu/particle_accelerator.js). Spec: docs/semiconductor-factory.md
//
// Fluids come only through Coolant Hatches, so every recipe's only fluid is its supercooled coolant: hydrogen for the
// neutron source, argon for heavy-ion collisions, xenon for strange matter, endion for chromodynium. A recipe takes its
// coolant or any colder one (like the orbital station): it asks for the grade's fluid tag, af9:coolant/<grade>, which
// holds that supercooled fluid and every colder one (hydrogen < argon < xenon < endion).

ServerEvents.tags('fluid', event => {
    const grades = ['hydrogen', 'argon', 'xenon', 'endion']
    grades.forEach((grade, index) => {
        event.add(`af9:coolant/${grade}`, grades.slice(index).map(colder => `gtceu:supercooled_${colder}`))
    })
})

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // ---- The accelerator and its parts ----
    event.recipes.gtceu.assembler('af9:particle_accelerator')
        .itemInputs('gtceu:zpm_machine_hull', '4x gtceu:zpm_field_generator', '4x gtceu:zpm_emitter',
            '4x #gtceu:circuits/zpm', '2x gtceu:zpm_sensor', '8x gtceu:niobium_titanium_plate')
        .inputFluids(Fluid.of('gtceu:supercooled_hydrogen', 4000))
        .itemOutputs('gtceu:particle_accelerator')
        .duration(3000)
        .EUt(VA[GTValues.ZPM])
    // the ring itself is GT's blocks: clean stainless steel casings, superconducting coils, fusion glass and
    // naquadah alloy frames

    // consumables: the spallation target (four wafers each) and the reusable magnetic traps
    event.recipes.gtceu.assembler('af9:beryllium_spallation_target')
        .itemInputs('4x gtceu:beryllium_plate', '2x gtceu:tungsten_steel_plate')
        .itemOutputs('af9:beryllium_spallation_target')
        .duration(200)
        .EUt(VA[GTValues.LuV])
    event.recipes.gtceu.assembler('af9:magnetic_trap')
        .itemInputs('gtceu:zpm_field_generator', '4x gtceu:niobium_titanium_plate', '2x gtceu:tungsten_steel_plate')
        .itemOutputs('af9:magnetic_trap')
        .duration(400)
        .EUt(VA[GTValues.ZPM])

    // ---- Neutron irradiation: neutron transmutation doping, the 20 nm substrate ----
    event.recipes.gtceu.neutron_irradiation('af9:transmuted_neutronium_wafer')
        .itemInputs('4x gtceu:neutronium_wafer', 'af9:beryllium_spallation_target')
        .inputFluids('#af9:coolant/hydrogen 1000')
        .itemOutputs('4x af9:transmuted_neutronium_wafer')
        .duration(1200)
        .EUt(VA[GTValues.UV], 2)

    // ---- Heavy-ion collision: quark-gluon plasma, caught in a magnetic trap ----
    event.recipes.gtceu.ion_collision('af9:qgp_trap')
        .itemInputs('af9:magnetic_trap', '16x gtceu:lead_ingot')
        .inputFluids('#af9:coolant/argon 2000')
        .itemOutputs('af9:qgp_trap')
        .duration(600)
        .EUt(VA[GTValues.UV], 4)

    // ---- Quark synthesis: strange matter, then chromodynium; the traps come back empty ----
    event.recipes.gtceu.quark_synthesis('af9:strange_matter_dust')
        .itemInputs('4x af9:qgp_trap', 'gtceu:neutronium_dust')
        .inputFluids('#af9:coolant/xenon 4000')
        .itemOutputs('gtceu:strange_matter_dust', '4x af9:magnetic_trap')
        .duration(1200)
        .EUt(VA[GTValues.UHV], 2)
    event.recipes.gtceu.quark_synthesis('af9:chromodynium_dust')
        .itemInputs('8x af9:qgp_trap', 'gtceu:strange_matter_dust')
        .inputFluids('#af9:coolant/endion 4000')
        .itemOutputs('gtceu:chromodynium_dust', '8x af9:magnetic_trap')
        .duration(2400)
        .EUt(VA[GTValues.UHV], 4)
})
