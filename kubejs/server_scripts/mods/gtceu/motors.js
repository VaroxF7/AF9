// AF9 - Electric motors built from housings, rotors and stators.
// Materials: af9-core, registry/AF9Materials (motorParts()). Items: af9-core, registry/AF9Items.
//
// Every tier owns three parts, each made of the tier's own material in the assembler:
// a motor housing (plates and rods), a rotor (tier rods round a magnetic rod) and a stator
// (the laminated core; the windings go in at motor assembly). The LV-IV motors are rebuilt on
// those parts plus the tier's fine wire windings and GT's own winding wire and cable:
//
//   motor = housing + rotor + stator + 16x fine tier wire + 2x winding wire + 2x cable
//
// GT's own motor recipes are gone up to IV (assembler and shaped, iron path included:
// the LV bootstrap is the iron hand-craft now). The LuV, ZPM, UV and UHV motors keep GT's recipes for now (assembly
// line + research), so their parts wait for the rewiring. The UHV parts run at UV voltage: with
// highTierContent off no single block runs hotter, and GT 7.5 refuses hotter recipes.
// The two industrial housings are the endgame shells out of Reinforced Titanium Niobium Alloy
// (1:1 niobium-titanium + HSLA steel, mixer + GT's EBF from the blast property).

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // ---- Reinforced Titanium Niobium Alloy dust, like the circuit-alloy mixers in circuits_af9.js ----
    event.recipes.gtceu.mixer('af9:reinforced_titanium_niobium_alloy_dust')
        .itemInputs('gtceu:niobium_titanium_dust', 'gtceu:hsla_steel_dust')
        .circuit(2)
        .itemOutputs('2x gtceu:reinforced_titanium_niobium_alloy_dust')
        .duration(60)
        .EUt(VA[GTValues.HV])

    // ---- The parts of every tier: [tier, tier material, magnetic rod, assembler voltage] ----
    // The LuV and up rotors take a normal samarium rod, like GT's own voltage coils: the pack has no magnetic
    // samarium long rod, only the normal one.
    // UHV builds at UV: the hottest voltage a single block runs while highTierContent is off.
    const PARTS = [
        ['lv', 'steel', 'gtceu:magnetic_steel_rod', GTValues.LV],
        ['mv', 'aluminium', 'gtceu:magnetic_steel_rod', GTValues.MV],
        ['hv', 'stainless_steel', 'gtceu:magnetic_steel_rod', GTValues.HV],
        ['ev', 'titanium', 'gtceu:magnetic_neodymium_rod', GTValues.EV],
        ['iv', 'tungsten_steel', 'gtceu:magnetic_neodymium_rod', GTValues.IV],
        ['luv', 'hsss', 'gtceu:magnetic_samarium_rod', GTValues.LuV],
        ['zpm', 'osmiridium', 'gtceu:magnetic_samarium_rod', GTValues.ZPM],
        ['uv', 'tritanium', 'gtceu:magnetic_samarium_rod', GTValues.UV],
        ['uhv', 'neutronium', 'gtceu:magnetic_samarium_rod', GTValues.UV]
    ]
    PARTS.forEach(cfg => {
        const tier = cfg[0]
        const mat = cfg[1]
        const eut = VA[cfg[3]]
        const plate = 'gtceu:' + mat + '_plate'
        const rod = 'gtceu:' + mat + '_rod'
        event.recipes.gtceu.assembler('af9:' + tier + '_motor_housing')
            .itemInputs('4x ' + plate, '2x ' + rod)
            .circuit(4)
            .itemOutputs('af9:' + tier + '_motor_housing')
            .duration(100)
            .EUt(eut)
        event.recipes.gtceu.assembler('af9:' + tier + '_rotor')
            .itemInputs('2x ' + rod, cfg[2])
            .circuit(5)
            .itemOutputs('af9:' + tier + '_rotor')
            .duration(100)
            .EUt(eut)
        event.recipes.gtceu.assembler('af9:' + tier + '_stator')
            .itemInputs('4x ' + plate, rod)
            .circuit(6)
            .itemOutputs('af9:' + tier + '_stator')
            .duration(100)
            .EUt(eut)
    })

    // ---- The industrial housings, out of reinforced plates (the motor shell chains off the component shell) ----
    event.recipes.gtceu.assembler('af9:industrial_component_housing')
        .itemInputs('4x gtceu:reinforced_titanium_niobium_alloy_plate',
            'gtceu:reinforced_titanium_niobium_alloy_frame')
        .circuit(1)
        .itemOutputs('af9:industrial_component_housing')
        .duration(200)
        .EUt(VA[GTValues.EV])
    event.recipes.gtceu.assembler('af9:industrial_electric_motor_housing')
        .itemInputs('af9:industrial_component_housing',
            '4x gtceu:reinforced_titanium_niobium_alloy_plate',
            '2x gtceu:reinforced_titanium_niobium_alloy_rod')
        .circuit(2)
        .itemOutputs('af9:industrial_electric_motor_housing')
        .duration(200)
        .EUt(VA[GTValues.EV])

    // ---- The LV-IV motors, rebuilt on the new parts (GT's winding wire and cable stay) ----
    // [tier, tier material, winding wire, cable, GT recipe names replaced]
    const MOTORS = [
        ['lv', 'steel', 'gtceu:copper_single_wire', 'gtceu:tin_single_cable', ['electric_motor_lv_steel']],
        ['mv', 'aluminium', 'gtceu:cupronickel_double_wire', 'gtceu:copper_single_cable', ['electric_motor_mv']],
        ['hv', 'stainless_steel', 'gtceu:electrum_double_wire', 'gtceu:silver_double_cable', ['electric_motor_hv']],
        ['ev', 'titanium', 'gtceu:kanthal_double_wire', 'gtceu:aluminium_double_cable', ['electric_motor_ev']],
        ['iv', 'tungsten_steel', 'gtceu:graphene_double_wire', 'gtceu:tungsten_double_cable', ['electric_motor_iv']]
    ]
    // GT's LV iron assembler path goes too (assembler only: the iron hand-craft stays as the bootstrap).
    // Every GT motor id below is recorded here and removed in one batched call after the loop (same ids).
    const removedMotorIds = ['gtceu:assembler/electric_motor_lv_iron']
    MOTORS.forEach(cfg => {
        const tier = cfg[0]
        const mat = cfg[1]
        cfg[4].forEach(name => {
            removedMotorIds.push('gtceu:assembler/' + name, 'gtceu:shaped/' + name)
        })
        event.recipes.gtceu.assembler('af9:' + tier + '_electric_motor')
            .itemInputs('af9:' + tier + '_motor_housing', 'af9:' + tier + '_rotor', 'af9:' + tier + '_stator',
                '16x gtceu:fine_' + mat + '_wire', '2x ' + cfg[2], '2x ' + cfg[3])
            .itemOutputs('gtceu:' + tier + '_electric_motor')
            .duration(100)
            .EUt(VA[GTValues.LV])
    })
    // One batched removal for every GT motor id recorded above (the same set, one scan).
    if (removedMotorIds.length) event.remove({ id: removedMotorIds })
})
