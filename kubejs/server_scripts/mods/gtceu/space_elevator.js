// AF9 - Crafting of the Space Elevator, its blocks and its Mining Drones, and the expeditions it runs
// (startup_scripts/gtceu/space_elevator.js; the asteroids themselves are made by af9-core, SpaceElevatorMachine).
// Spec: docs/space-elevator.md
//
// An expedition names a drone (not used up), hydrogen and a supercooled coolant (the Cryostat's, cryogenics.js) and the
// energy: the ore is not in the recipe, a run gets its asteroid when it starts (so the recipe viewers show none).

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const V = GTValues.V
    const solder = mb => Fluid.of('gtceu:soldering_alloy', mb)

    // ---- The expeditions: drone tier, hydrogen (mB), coolant, coolant (mB), amps of ZPM, seconds ----
    // 50 to 100 buckets of each. Mk1 runs on one 4A ZPM hatch, Mk2 on two, Mk3 on four, Mk4 on lasers.
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
            .EUt(V[GTValues.ZPM] * amps)
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

    // ---- The blocks of the tower ----
    event.recipes.gtceu.assembler('af9:space_elevator_base_casing')
        .itemInputs('gtceu:naquadah_alloy_frame', '4x gtceu:naquadah_alloy_plate', 'gtceu:fusion_casing')
        .itemOutputs('4x kubejs:space_elevator_base_casing')
        .duration(200)
        .EUt(VA[GTValues.LuV])
    event.recipes.gtceu.assembler('af9:space_elevator_support')
        .itemInputs('2x gtceu:naquadah_alloy_frame', '4x gtceu:tungsten_steel_plate')
        .itemOutputs('4x kubejs:space_elevator_support')
        .duration(200)
        .EUt(VA[GTValues.LuV])
    event.recipes.gtceu.assembler('af9:space_elevator_glass')
        .itemInputs('8x gtceu:fusion_glass', '2x gtceu:naquadah_alloy_plate')
        .itemOutputs('8x kubejs:space_elevator_glass')
        .duration(200)
        .EUt(VA[GTValues.LuV])
    event.recipes.gtceu.assembler('af9:space_elevator_cable')
        .itemInputs('8x gtceu:naquadah_alloy_rod', '2x gtceu:luv_field_generator')
        .inputFluids(solder(288))
        .itemOutputs('2x kubejs:space_elevator_cable')
        .duration(300)
        .EUt(VA[GTValues.ZPM])

    // ---- The Space Elevator: ZPM ----
    event.recipes.gtceu.assembly_line('af9:space_elevator')
        .itemInputs('gtceu:zpm_machine_hull', '8x gtceu:zpm_electric_motor', '4x gtceu:zpm_field_generator',
            '2x gtceu:zpm_sensor', '2x gtceu:zpm_emitter', '4x #gtceu:circuits/zpm',
            '16x kubejs:space_elevator_base_casing', '8x kubejs:space_elevator_support',
            '8x kubejs:space_elevator_glass', '2x kubejs:space_elevator_cable', '4x gtceu:double_naquadah_alloy_plate')
        .inputFluids(solder(4608))
        .itemOutputs('gtceu:space_elevator')
        .duration(1200)
        .EUt(VA[GTValues.ZPM])
})
