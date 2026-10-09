// AF9 - the HV components in Powah's Energizing Orb, a second way beside GT's own recipes (the crafting table and the
// assembler stay; nothing is taken away).
//
// Why: from HV on, a component is a pile of parts (a motor: nine items). Powah's orb takes a stack per slot (AF9 Core's
// patch of Powah: an ingredient may say "count"), so the parts go in as they are and the orb gives the finished component
// when its rods have charged it. The recipes are cheaper than GT's by a part or two and take a Powah material as the
// glue (energized steel, dielectric paste: iron and gold, and the orb itself, from the first Powah hours): a small detour
// through Powah, not a second factory. The charge scales with the rods round the orb, more rods, a shorter wait.
//
//   motor    2 silver cable, 1 stainless rod, 1 energized steel, 1 magnetic rod, 3 electrum wire        (GT: 9 items)
//   conveyor 2 motors, 1 gold cable, 4 rubber plates, dielectric paste                                  (GT: 6 ingots of rubber)
//   pump     motor, gold cable, stainless pipe, steel rotor, 2 rubber rings, dielectric paste           (GT: and a screw)
//   piston   motor, 2 stainless rods, 2 gold cables, 2 stainless plates, small gear, energized steel    (GT: 3 plates)
//   arm      2 motors, piston, HV circuit, 2 gold cables, stainless rod, energized steel                (GT: 3 cables)
//   emitter  3 chromium rods, 2 gold cables, 2 HV circuits, ender eye                                   (GT: 4 rods)
//   sensor   chromium rod, 3 stainless plates, HV circuit, ender eye                                    (GT: 4 plates)
//   field    quantum eye, 2 stainless plates, 2 HV circuits, 3 quadruple wires                          (GT: 4 wires)
//
// The energy is in Powah's own unit, RF; the Powah config's energizing ratio scales it like its own recipes.

ServerEvents.recipes(event => {
    const item = (id, count) => (count > 1 ? { item: id, count: count } : { item: id })
    const circuit = count => ({ tag: 'gtceu:circuits/hv', count: count })
    const orb = (name, ingredients, energy, result) => {
        event.custom({
            type: 'powah:energizing',
            ingredients: ingredients,
            energy: energy,
            result: { item: result }
        }).id('af9:powah/' + name)
    }

    const ENERGIZED_STEEL = item('powah:steel_energized', 1)
    const DIELECTRIC_PASTE = item('powah:dielectric_paste', 1)

    orb('hv_electric_motor', [
        item('gtceu:silver_double_cable', 2),
        item('gtceu:stainless_steel_rod', 1),
        ENERGIZED_STEEL,
        item('gtceu:magnetic_steel_rod', 1),
        item('gtceu:electrum_double_wire', 3)
    ], 40000, 'gtceu:hv_electric_motor')

    orb('hv_conveyor_module', [
        item('gtceu:hv_electric_motor', 2),
        item('gtceu:gold_single_cable', 1),
        item('gtceu:rubber_plate', 4),
        DIELECTRIC_PASTE
    ], 50000, 'gtceu:hv_conveyor_module')

    orb('hv_electric_pump', [
        item('gtceu:hv_electric_motor', 1),
        item('gtceu:gold_single_cable', 1),
        item('gtceu:stainless_steel_normal_fluid_pipe', 1),
        item('gtceu:steel_rotor', 1),
        item('gtceu:rubber_ring', 2),
        DIELECTRIC_PASTE
    ], 50000, 'gtceu:hv_electric_pump')

    orb('hv_electric_piston', [
        item('gtceu:hv_electric_motor', 1),
        item('gtceu:stainless_steel_rod', 2),
        item('gtceu:gold_single_cable', 2),
        item('gtceu:stainless_steel_plate', 2),
        item('gtceu:small_stainless_steel_gear', 1),
        ENERGIZED_STEEL
    ], 60000, 'gtceu:hv_electric_piston')

    orb('hv_robot_arm', [
        item('gtceu:hv_electric_motor', 2),
        item('gtceu:hv_electric_piston', 1),
        circuit(1),
        item('gtceu:gold_single_cable', 2),
        item('gtceu:stainless_steel_rod', 1),
        ENERGIZED_STEEL
    ], 120000, 'gtceu:hv_robot_arm')

    orb('hv_emitter', [
        item('gtceu:chromium_rod', 3),
        item('gtceu:gold_single_cable', 2),
        circuit(2),
        item('minecraft:ender_eye', 1)
    ], 90000, 'gtceu:hv_emitter')

    orb('hv_sensor', [
        item('gtceu:chromium_rod', 1),
        item('gtceu:stainless_steel_plate', 3),
        circuit(1),
        item('minecraft:ender_eye', 1)
    ], 90000, 'gtceu:hv_sensor')

    orb('hv_field_generator', [
        item('gtceu:quantum_eye', 1),
        item('gtceu:stainless_steel_plate', 2),
        circuit(2),
        item('gtceu:mercury_barium_calcium_cuprate_quadruple_wire', 3)
    ], 250000, 'gtceu:hv_field_generator')
})
