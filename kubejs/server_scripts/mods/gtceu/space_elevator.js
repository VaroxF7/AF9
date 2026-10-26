// AF9 - Crafting of the Space Elevator, its blocks and its Mining Drones, and the expeditions it runs
// (startup_scripts/gtceu/space_elevator.js; the asteroids themselves are made by af9-core, SpaceElevatorMachine).
// Spec: docs/space-elevator.md
//
// An expedition names a drone (not used up), hydrogen and a supercooled coolant (the Cryostat's, cryogenics.js) and the
// energy: the ore is not in the recipe, a run gets its asteroid when it starts (the recipe viewers' page lists the ores
// the drone's asteroids hold: af9-core, SpaceMiningRecipeUI). The Mining Modules of the tower fly several of them at once,
// each with the full inputs.
// The Pumping Module pumps a planet's fluid: one gtceu:space_pumping recipe a fluid of af9-core's PlanetCatalog (GTNH's Space
// Pumping table), all at 4A of UV and 1200 to 3600 seconds, the drone of the planet's reach and the programmed circuit that
// picks the fluid (the n-th of that drone's fluids, PlanetCatalog.ofDrone) set in the module.

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const solder = mb => Fluid.of('gtceu:soldering_alloy', mb)

    // ---- The expeditions: drone tier, hydrogen (mB), coolant, coolant (mB), amps of UV, seconds ----
    // 50 to 100 buckets of each. UV recipes of 4 amps, 20 to 60 minutes, for an endgame of many towers.
    // [drone tier, hydrogen mB, coolant, coolant mB, amps of UV, seconds, drill head, crate]. The ore expeditions use up a drill
    // head and a crate (GT's; the rarer the ore the drone reaches the harder the drill head). The numbers are also in af9-core
    // SpaceElevatorMachine.REQUIREMENTS
    const expeditions = [
        [1, 64000, 'gtceu:supercooled_hydrogen', 50000, 4, 1200, 'gtceu:tungsten_carbide_drill_head', 'gtceu:stainless_steel_crate'],
        [2, 80000, 'gtceu:supercooled_argon', 64000, 4, 2000, 'gtceu:hsse_drill_head', 'gtceu:titanium_crate'],
        [3, 96000, 'gtceu:supercooled_xenon', 80000, 4, 2800, 'gtceu:naquadah_alloy_drill_head', 'gtceu:tungsten_steel_crate'],
        [4, 100000, 'gtceu:supercooled_endion', 100000, 4, 3600, 'gtceu:neutronium_drill_head', 'gtceu:tungsten_steel_crate']
    ]
    // One ore expedition recipe a programmed circuit of the drone's tier: the circuit's number picks the asteroid, the n-th of
    // the veins that tier first reaches (alphabetical, the exotic one last: af9-core OreCatalog.veinIds; the veins are not
    // registered yet while the recipes load, so the recipes only count). ASTEROIDS holds how many there are of each tier now;
    // a circuit past them flies nowhere. The run brings a random number of stacks between the tier's least and most
    // (af9-core SpaceMissionMachine.MIN_STACKS / MAX_STACKS), about half of them the vein's main ore; the recipe pages list the
    // ores (client side, with their ranges).
    const ASTEROIDS = { 1: 24, 2: 12, 3: 4, 4: 9 }
    expeditions.forEach(([tier, hydrogen, coolant, coolantMb, amps, seconds, drill, crate]) => {
        for (let circuit = 1; circuit <= ASTEROIDS[tier]; circuit++) {
            event.recipes.gtceu.space_mining(`af9:space_mining_mk${tier}_${circuit}`)
                .notConsumable(`af9:space_mining_drone_mk${tier}`)
                .circuit(circuit)
                .itemInputs(drill, crate)
                .inputFluids(Fluid.of('gtceu:hydrogen', hydrogen))
                .inputFluids(Fluid.of(coolant, coolantMb))
                .addData('af9_expedition', 1)
                .addData('af9_module', Math.min(tier, 3))
                .duration(seconds * 20)
                .EUt(VA[GTValues.UV], amps)
        }
    })

    // ---- The Pumping Module's recipes: one a fluid ----
    // The flight costs of the drone that reaches the planet (hydrogen, coolant), a UV recipe of 4 amps whatever the drone,
    // and a time that rises along the table from 1200 s (the nearest planet's fluid) to 3600 s (the last of the far dark's).
    // The recipe names its fluid in the data too (af9_cargo, PlanetCatalog.Cargo#code): the module's run reads it from there.
    const $PlanetCatalog = Java.loadClass('com.af9.core.elevator.PlanetCatalog')
    const $PumpForge = Java.loadClass('net.minecraftforge.registries.ForgeRegistries')
    const cargoes = $PlanetCatalog.all()
    const perDrone = {}
    const pump = (cargo, i) => {
        const tier = cargo.drone()
        const flight = expeditions[tier - 1]
        // the n-th fluid of its drone: the programmed circuit that picks it
        perDrone[tier] = (perDrone[tier] || 0) + 1
        const seconds = 1200 + Math.round(2400 * i / Math.max(1, cargoes.size() - 1) / 60) * 60
        event.recipes.gtceu.space_pumping(`af9:space_pumping_${cargo.planet()}_${cargo.gas()}`)
            .notConsumable(`af9:space_mining_drone_mk${tier}`)
            .circuit(perDrone[tier])
            .inputFluids(Fluid.of('gtceu:hydrogen', flight[1]))
            .inputFluids(Fluid.of(flight[2], flight[3]))
            .outputFluids(Fluid.of(String($PumpForge.FLUIDS.getKey(cargo.fluid())), cargo.millibuckets()))
            .addData('af9_cargo', cargo.code())
            .duration(seconds * 20)
            .EUt(VA[GTValues.UV], 4)
    }
    for (let i = 0; i < cargoes.size(); i++) pump(cargoes.get(i), i)

    // ---- The Mining Drones: UV parts (GT's last tier with parts: highTierContent is off) for all, the whole elevator is
    // UV crafting: more of them and a rarer plate for each tier ----
    // [tier, part tier, arms, sensors, emitters, circuits, plate, plates, solder mB, voltage]
    const drones = [
        [1, 'uv', 1, 2, 1, 4, 'naquadah_alloy', 4, 576, GTValues.UV],
        [2, 'uv', 1, 3, 1, 6, 'osmiridium', 6, 1152, GTValues.UV],
        [3, 'uv', 2, 4, 2, 8, 'tritanium', 4, 1728, GTValues.UV],
        [4, 'uv', 4, 8, 4, 16, 'neutronium', 4, 2304, GTValues.UV]
    ]
    drones.forEach(([tier, name, arms, sensors, emitters, circuits, plate, plates, mb, voltage]) => {
        event.recipes.gtceu.assembler(`af9:space_mining_drone_mk${tier}`)
            .itemInputs(`${arms}x gtceu:${name}_robot_arm`, `${sensors}x gtceu:${name}_sensor`,
                `${emitters}x gtceu:${name}_emitter`, `${circuits}x #gtceu:circuits/${name}`,
                `${plates}x gtceu:${plate}_plate`)
            .inputFluids(solder(mb))
            .itemOutputs(`af9:space_mining_drone_mk${tier}`)
            .duration(600)
            .EUt(VA[voltage])
    })

    // ---- The blocks of the tower: GTNH's tower takes 800 of the concrete, up to 785 base casings, 620 stress-proof
    // casings and 360 internal structures, so a craft makes 8 or 16. The casings share their frames and plates ----
    event.recipes.gtceu.assembler('af9:ultra_high_strength_concrete_floor')
        .itemInputs('8x gtceu:dark_concrete', '2x gtceu:tungsten_steel_rod')
        .inputFluids(Fluid.of('gtceu:polybenzimidazole', 72))
        .itemOutputs('8x af9:ultra_high_strength_concrete_floor')
        .duration(100)
        .EUt(VA[GTValues.UV])
    event.recipes.gtceu.assembler('af9:space_elevator_base_casing')
        .itemInputs('gtceu:naquadah_alloy_frame', '4x gtceu:naquadah_alloy_plate', '8x gtceu:tungsten_steel_plate')
        .circuit(1)
        .itemOutputs('16x af9:space_elevator_base_casing')
        .duration(200)
        .EUt(VA[GTValues.UV])
    event.recipes.gtceu.assembler('af9:space_elevator_internal_structure')
        .itemInputs('gtceu:naquadah_alloy_frame', '4x gtceu:osmiridium_plate', '4x gtceu:tungsten_steel_plate')
        .circuit(2)
        .itemOutputs('16x af9:space_elevator_internal_structure')
        .duration(200)
        .EUt(VA[GTValues.UV])
    // one cable block: it stands on top of the motor shaft
    event.recipes.gtceu.assembler('af9:space_elevator_cable')
        .itemInputs('32x gtceu:carbon_fiber_plate', '8x gtceu:naquadah_alloy_rod', '2x gtceu:uv_field_generator')
        .inputFluids(solder(288))
        .itemOutputs('af9:space_elevator_cable')
        .duration(300)
        .EUt(VA[GTValues.UV])

    // ---- The motors (88 of one tier round the shaft): MK-I from UV motors, every tier after it from the tier before ----
    event.recipes.gtceu.assembler('af9:space_elevator_motor_mk1')
        .itemInputs('4x gtceu:uv_electric_motor', 'gtceu:naquadah_alloy_frame', '4x gtceu:naquadah_alloy_plate')
        .inputFluids(solder(288))
        .itemOutputs('4x af9:space_elevator_motor_mk1')
        .duration(400)
        .EUt(VA[GTValues.UV])
    // [tier, what four motors of the tier before take to become it]
    const motorUpgrades = [
        [2, ['4x gtceu:uv_electric_motor', '4x gtceu:tritanium_plate']],
        [3, ['4x gtceu:uv_electric_motor', '4x gtceu:neutronium_plate', '8x gtceu:endionite_foil']],
        [4, ['4x gtceu:uv_field_generator', '4x gtceu:neutronium_gear', '2x #gtceu:circuits/uhv']],
        [5, ['2x gtceu:gravi_star', '4x gtceu:chromodynium_plate', '4x #gtceu:circuits/uhv']]
    ]
    motorUpgrades.forEach(([tier, parts]) => {
        event.recipes.gtceu.assembler(`af9:space_elevator_motor_mk${tier}`)
            .itemInputs([`4x af9:space_elevator_motor_mk${tier - 1}`].concat(parts))
            .inputFluids(solder(576))
            .itemOutputs(`4x af9:space_elevator_motor_mk${tier}`)
            .duration(400)
            .EUt(VA[GTValues.UV])
    })

    // ---- The Pumping Module (a module slot's MK-I module; the Mining Modules' parts, with pumps for the arms) ----
    event.recipes.gtceu.assembler('gtceu:space_pumping_module')
        .itemInputs('gtceu:uv_machine_hull', '2x gtceu:uv_electric_pump', '2x gtceu:uv_sensor', '4x #gtceu:circuits/uv',
            '4x af9:space_elevator_base_casing')
        .inputFluids(solder(1152))
        .itemOutputs('gtceu:space_pumping_module')
        .duration(600)
        .EUt(VA[GTValues.UV])

    // ---- The Mining Modules (in the module slots: one expedition each at once, more modules fly more): MK-I from UV parts, the next from
    // the one before ----
    event.recipes.gtceu.assembler('gtceu:space_mining_module_mk1')
        .itemInputs('gtceu:uv_machine_hull', '2x gtceu:uv_robot_arm', '2x gtceu:uv_sensor', '2x gtceu:uv_emitter',
            '4x #gtceu:circuits/uv', '4x af9:space_elevator_base_casing')
        .inputFluids(solder(1152))
        .itemOutputs('gtceu:space_mining_module_mk1')
        .duration(600)
        .EUt(VA[GTValues.UV])
    event.recipes.gtceu.assembler('gtceu:space_mining_module_mk2')
        .itemInputs('gtceu:space_mining_module_mk1', '2x gtceu:uv_robot_arm', '2x gtceu:uv_sensor',
            '2x gtceu:uv_emitter', '4x #gtceu:circuits/uv', '4x gtceu:tritanium_plate')
        .inputFluids(solder(2304))
        .itemOutputs('gtceu:space_mining_module_mk2')
        .duration(600)
        .EUt(VA[GTValues.UV])
    event.recipes.gtceu.assembler('gtceu:space_mining_module_mk3')
        .itemInputs('gtceu:space_mining_module_mk2', '4x gtceu:uv_robot_arm', '2x gtceu:uv_field_generator',
            '4x #gtceu:circuits/uhv', '4x gtceu:neutronium_plate')
        .inputFluids(solder(4608))
        .itemOutputs('gtceu:space_mining_module_mk3')
        .duration(600)
        .EUt(VA[GTValues.UV])

    // ---- The Space Elevator: UV ----
    event.recipes.gtceu.assembly_line('af9:space_elevator')
        .itemInputs('gtceu:uv_machine_hull', '8x gtceu:uv_electric_motor', '4x gtceu:uv_field_generator',
            '2x gtceu:uv_sensor', '2x gtceu:uv_emitter', '4x #gtceu:circuits/uv',
            '16x af9:space_elevator_base_casing', '8x gtceu:stress_proof_casing',
            '8x af9:space_elevator_internal_structure', 'af9:space_elevator_cable',
            '4x gtceu:double_naquadah_alloy_plate')
        .inputFluids(solder(4608))
        .itemOutputs('gtceu:space_elevator')
        .duration(1200)
        .EUt(VA[GTValues.UV])
})
