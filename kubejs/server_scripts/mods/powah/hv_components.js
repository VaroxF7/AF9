// AF9 - the HV components in Powah's Energizing Orb, a second way beside GT's own recipes (the crafting table and the
// assembler stay; nothing is taken away).
//
// Why: from HV on, a component is a pile of parts (a motor: nine items). The orb takes a stack per slot (AF9 Core's patch
// of Powah: an ingredient may say "count"; the Energizing Orb Mk2, orb_mk2.js, is the orb with a screen to put them in
// through), so the parts go in as they are and the orb gives the finished component
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
// The energy is GT's assembler cost at the tier's voltage, converted 1 EU to 4 FE: at HV the assembler would run
// 100 ticks at 512 EU/t (51200 EU), so the motor takes 204800 FE. The costlier parts scale from there the way the
// old RF values did (conveyor and pump 1.25x, piston 1.5x, arm 3x, emitter and sensor 2.25x, field generator 6.25x).
// LV and MV follow the same rule at their voltages (32 and 128 EU/t) in lv_mv_components.js.

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
    ], 204800, 'gtceu:hv_electric_motor')

    orb('hv_conveyor_module', [
        item('gtceu:hv_electric_motor', 2),
        item('gtceu:gold_single_cable', 1),
        item('gtceu:rubber_plate', 4),
        DIELECTRIC_PASTE
    ], 256000, 'gtceu:hv_conveyor_module')

    orb('hv_electric_pump', [
        item('gtceu:hv_electric_motor', 1),
        item('gtceu:gold_single_cable', 1),
        item('gtceu:stainless_steel_normal_fluid_pipe', 1),
        item('gtceu:steel_rotor', 1),
        item('gtceu:rubber_ring', 2),
        DIELECTRIC_PASTE
    ], 256000, 'gtceu:hv_electric_pump')

    orb('hv_electric_piston', [
        item('gtceu:hv_electric_motor', 1),
        item('gtceu:stainless_steel_rod', 2),
        item('gtceu:gold_single_cable', 2),
        item('gtceu:stainless_steel_plate', 2),
        item('gtceu:small_stainless_steel_gear', 1),
        ENERGIZED_STEEL
    ], 307200, 'gtceu:hv_electric_piston')

    orb('hv_robot_arm', [
        item('gtceu:hv_electric_motor', 2),
        item('gtceu:hv_electric_piston', 1),
        circuit(1),
        item('gtceu:gold_single_cable', 2),
        item('gtceu:stainless_steel_rod', 1),
        ENERGIZED_STEEL
    ], 614400, 'gtceu:hv_robot_arm')

    orb('hv_emitter', [
        item('gtceu:chromium_rod', 3),
        item('gtceu:gold_single_cable', 2),
        circuit(2),
        item('minecraft:ender_eye', 1)
    ], 460800, 'gtceu:hv_emitter')

    orb('hv_sensor', [
        item('gtceu:chromium_rod', 1),
        item('gtceu:stainless_steel_plate', 3),
        circuit(1),
        item('minecraft:ender_eye', 1)
    ], 460800, 'gtceu:hv_sensor')

    orb('hv_field_generator', [
        item('gtceu:quantum_eye', 1),
        item('gtceu:stainless_steel_plate', 2),
        circuit(2),
        item('gtceu:mercury_barium_calcium_cuprate_quadruple_wire', 3)
    ], 1280000, 'gtceu:hv_field_generator')
})
