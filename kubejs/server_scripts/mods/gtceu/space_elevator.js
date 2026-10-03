// AF9 - Crafting of the Space Elevator, its blocks and its Mining Drones, and the expeditions it runs
// (startup_scripts/gtceu/space_elevator.js; the asteroids themselves are made by af9-core, SpaceElevatorMachine).
// Spec: docs/space-elevator.md
//
// An expedition names a drone (not used up), hydrogen and a supercooled coolant (the Cryostat's, cryogenics.js) and the
// energy: the ore is not in the recipe, a run gets its asteroid when it starts (the recipe viewers' page lists the ores
// the drone's asteroids hold: af9-core, SpaceMiningRecipeUI). The Mining Modules of the tower fly several of them at once,
// each with the full inputs.
// A liquid mission is the same flight for a planet's fluid (gtceu:space_pumping): the fluid is not in the recipe either,
// the elevator puts in the one picked on its screen (af9-core, PlanetCatalog: GTNH's Space Pumping table).

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const solder = mb => Fluid.of('gtceu:soldering_alloy', mb)

    // ---- The expeditions: drone tier, hydrogen (mB), coolant, coolant (mB), amps of ZPM, seconds ----
    // 50 to 100 buckets of each. ZPM recipes of several amps (as the Particle Accelerator's): Mk1 runs on one 4A ZPM
    // hatch, Mk2 on two, Mk3 on four, Mk4 on lasers.
    const expeditions = [
        [1, 64000, 'gtceu:supercooled_hydrogen', 50000, 4, 180],
        [2, 80000, 'gtceu:supercooled_argon', 64000, 8, 240],
        [3, 96000, 'gtceu:supercooled_xenon', 80000, 16, 300],
        [4, 100000, 'gtceu:supercooled_endion', 100000, 32, 360]
    ]
    expeditions.forEach(([tier, hydrogen, coolant, coolantMb, amps, seconds]) => {
        event.recipes.gtceu.space_mining(`af9:space_mining_mk${tier}`)
            .notConsumable(`kubejs:space_mining_drone_mk${tier}`)
            .inputFluids(Fluid.of('gtceu:hydrogen', hydrogen))
            .inputFluids(Fluid.of(coolant, coolantMb))
            .duration(seconds * 20)
            .EUt(VA[GTValues.ZPM], amps)
        event.recipes.gtceu.space_pumping(`af9:space_pumping_mk${tier}`)
            .notConsumable(`kubejs:space_mining_drone_mk${tier}`)
            .inputFluids(Fluid.of('gtceu:hydrogen', hydrogen))
            .inputFluids(Fluid.of(coolant, coolantMb))
            .duration(seconds * 20)
            .EUt(VA[GTValues.ZPM], amps)
    })

    // ---- The Mining Drones: ZPM parts for the first, UV parts (GT's last tier with parts: highTierContent is off) for
    // the rest, more of them and a rarer plate for each tier ----
    // [tier, part tier, arms, sensors, emitters, circuits, plate, plates, solder mB, voltage]
    const drones = [
        [1, 'zpm', 1, 2, 1, 4, 'naquadah_alloy', 4, 576, GTValues.ZPM],
        [2, 'uv', 1, 2, 1, 4, 'naquadah_alloy', 4, 1152, GTValues.UV],
        [3, 'uv', 2, 4, 2, 8, 'tritanium', 4, 1728, GTValues.UV],
        [4, 'uv', 4, 8, 4, 16, 'neutronium', 4, 2304, GTValues.UV]
    ]
    drones.forEach(([tier, name, arms, sensors, emitters, circuits, plate, plates, mb, voltage]) => {
        event.recipes.gtceu.assembler(`af9:space_mining_drone_mk${tier}`)
            .itemInputs(`${arms}x gtceu:${name}_robot_arm`, `${sensors}x gtceu:${name}_sensor`,
                `${emitters}x gtceu:${name}_emitter`, `${circuits}x #gtceu:circuits/${name}`,
                `${plates}x gtceu:${plate}_plate`)
            .inputFluids(solder(mb))
            .itemOutputs(`kubejs:space_mining_drone_mk${tier}`)
            .duration(600)
            .EUt(VA[voltage])
    })

    // ---- The blocks of the tower: GTNH's tower takes 800 of the concrete, up to 785 base casings, 620 stress-proof
    // casings and 360 internal structures, so a craft makes 8 or 16. The casings share their frames and plates ----
    event.recipes.gtceu.assembler('af9:ultra_high_strength_concrete_floor')
        .itemInputs('8x gtceu:dark_concrete', '2x gtceu:tungsten_steel_rod')
        .inputFluids(Fluid.of('gtceu:polybenzimidazole', 72))
        .itemOutputs('8x kubejs:ultra_high_strength_concrete_floor')
        .duration(100)
        .EUt(VA[GTValues.IV])
    event.recipes.gtceu.assembler('af9:space_elevator_base_casing')
        .itemInputs('gtceu:naquadah_alloy_frame', '4x gtceu:naquadah_alloy_plate', '8x gtceu:tungsten_steel_plate')
        .circuit(1)
        .itemOutputs('16x kubejs:space_elevator_base_casing')
        .duration(200)
        .EUt(VA[GTValues.LuV])
    event.recipes.gtceu.assembler('af9:space_elevator_internal_structure')
        .itemInputs('gtceu:naquadah_alloy_frame', '4x gtceu:osmiridium_plate', '4x gtceu:tungsten_steel_plate')
        .circuit(2)
        .itemOutputs('16x kubejs:space_elevator_internal_structure')
        .duration(200)
        .EUt(VA[GTValues.LuV])
    // one cable block: it stands on top of the motor shaft
    event.recipes.gtceu.assembler('af9:space_elevator_cable')
        .itemInputs('32x gtceu:carbon_fiber_plate', '8x gtceu:naquadah_alloy_rod', '2x gtceu:luv_field_generator')
        .inputFluids(solder(288))
        .itemOutputs('kubejs:space_elevator_cable')
        .duration(300)
        .EUt(VA[GTValues.ZPM])

    // ---- The motors (88 of one tier round the shaft): MK-I from ZPM motors, every tier after it from the tier before ----
    event.recipes.gtceu.assembler('af9:space_elevator_motor_mk1')
        .itemInputs('4x gtceu:zpm_electric_motor', 'gtceu:naquadah_alloy_frame', '4x gtceu:naquadah_alloy_plate')
        .inputFluids(solder(288))
        .itemOutputs('4x kubejs:space_elevator_motor_mk1')
        .duration(400)
        .EUt(VA[GTValues.ZPM])
    // [tier, what four motors of the tier before take to become it]
    const motorUpgrades = [
        [2, ['4x gtceu:uv_electric_motor', '4x gtceu:tritanium_plate']],
        [3, ['4x gtceu:uv_electric_motor', '4x gtceu:neutronium_plate', '8x gtceu:endionite_foil']],
        [4, ['4x gtceu:uv_field_generator', '4x gtceu:neutronium_gear', '2x #gtceu:circuits/uhv']],
        [5, ['2x gtceu:gravi_star', '4x gtceu:chromodynium_plate', '4x #gtceu:circuits/uhv']]
    ]
    motorUpgrades.forEach(([tier, parts]) => {
        event.recipes.gtceu.assembler(`af9:space_elevator_motor_mk${tier}`)
            .itemInputs([`4x kubejs:space_elevator_motor_mk${tier - 1}`].concat(parts))
            .inputFluids(solder(576))
            .itemOutputs(`4x kubejs:space_elevator_motor_mk${tier}`)
            .duration(400)
            .EUt(VA[GTValues.UV])
    })

    // ---- The Mining Modules (in the module slots: 2, 4 and 8 expeditions at once): MK-I from ZPM parts, the next from
    // the one before ----
    event.recipes.gtceu.assembler('af9:space_mining_module_mk1')
        .itemInputs('gtceu:zpm_machine_hull', '2x gtceu:zpm_robot_arm', '2x gtceu:zpm_sensor', '2x gtceu:zpm_emitter',
            '4x #gtceu:circuits/zpm', '4x kubejs:space_elevator_base_casing')
        .inputFluids(solder(1152))
        .itemOutputs('kubejs:space_mining_module_mk1')
        .duration(600)
        .EUt(VA[GTValues.ZPM])
    event.recipes.gtceu.assembler('af9:space_mining_module_mk2')
        .itemInputs('kubejs:space_mining_module_mk1', '2x gtceu:uv_robot_arm', '2x gtceu:uv_sensor',
            '2x gtceu:uv_emitter', '4x #gtceu:circuits/uv', '4x gtceu:tritanium_plate')
        .inputFluids(solder(2304))
        .itemOutputs('kubejs:space_mining_module_mk2')
        .duration(600)
        .EUt(VA[GTValues.UV])
    event.recipes.gtceu.assembler('af9:space_mining_module_mk3')
        .itemInputs('kubejs:space_mining_module_mk2', '4x gtceu:uv_robot_arm', '2x gtceu:uv_field_generator',
            '4x #gtceu:circuits/uhv', '4x gtceu:neutronium_plate')
        .inputFluids(solder(4608))
        .itemOutputs('kubejs:space_mining_module_mk3')
        .duration(600)
        .EUt(VA[GTValues.UV])

    // ---- The Space Elevator: ZPM ----
    event.recipes.gtceu.assembly_line('af9:space_elevator')
        .itemInputs('gtceu:zpm_machine_hull', '8x gtceu:zpm_electric_motor', '4x gtceu:zpm_field_generator',
            '2x gtceu:zpm_sensor', '2x gtceu:zpm_emitter', '4x #gtceu:circuits/zpm',
            '16x kubejs:space_elevator_base_casing', '8x gtceu:stress_proof_casing',
            '8x kubejs:space_elevator_internal_structure', 'kubejs:space_elevator_cable',
            '4x gtceu:double_naquadah_alloy_plate')
        .inputFluids(solder(4608))
        .itemOutputs('gtceu:space_elevator')
        .duration(1200)
        .EUt(VA[GTValues.ZPM])
})
