// AF9 - the LV and MV components in Powah's Energizing Orb Mk2, a second way beside GT's own recipes (the crafting
// table and the assembler stay; nothing is taken away). Same idea as hv_components.js, which holds the HV set.
//
// Why: a component is a pile of parts, and the Mk2 orb (orb_mk2.js) takes a stack per slot, so the parts go in as
// they are and the orb gives the finished component when its rods have charged it. Each recipe is cheaper than GT's
// by a part or two and takes a Powah material as the glue, a small detour through Powah, not a second factory.
// The glue steps up with the tier: LV glues with blazing crystal (the basic orb's crystal) and dielectric paste
// (coal and clay), MV with energized steel (iron and gold); HV keeps energized steel and paste (hv_components.js).
// The charge scales with the rods round the orb, more rods, a shorter wait.
//
// LV (GT assembler: 2 tin cable, 2 steel rods, 1 magnetic rod, 4 copper wires, and so on for the rest):
//   motor    2 tin cables, 1 steel rod, 1 magnetic steel rod, 3 copper wires, blazing crystal       (GT: 9 items)
//   conveyor 2 motors, 1 tin cable, 4 rubber plates, dielectric paste                               (GT: 6 plates)
//   pump     motor, tin cable, bronze pipe, tin rotor, 2 rubber rings, dielectric paste             (GT: and a screw)
//   piston   motor, 2 steel rods, 2 tin cables, 2 steel plates, small gear, blazing crystal         (GT: 3 plates)
//   arm      2 motors, piston, LV circuit, 2 tin cables, steel rod, blazing crystal                 (GT: 3 cables)
//   emitter  3 brass rods, 2 tin cables, 2 LV circuits, quartzite                                   (GT: 4 rods)
//   sensor   brass rod, 3 steel plates, LV circuit, quartzite                                       (GT: 4 plates)
//   field    ender pearl, 2 steel plates, 2 LV circuits, 3 quadruple wires                          (GT: 4 wires)
//
// MV (GT assembler: 2 copper cables, 2 aluminium rods, 1 magnetic rod, 4 cupronickel wires, and so on):
//   motor    2 copper cables, 1 aluminium rod, 1 magnetic steel rod, 3 cupronickel wires, steel     (GT: 9 items)
//   conveyor 2 motors, 1 copper cable, 4 rubber plates, dielectric paste                            (GT: 6 plates)
//   pump     motor, copper cable, steel pipe, bronze rotor, 2 rubber rings, paste                   (GT: and a screw)
//   piston   motor, 2 aluminium rods, 2 copper cables, 2 aluminium plates, small gear, steel        (GT: 3 plates)
//   arm      2 motors, piston, MV circuit, 2 copper cables, aluminium rod, steel                    (GT: 3 cables)
//   emitter  3 electrum rods, 2 copper cables, 2 MV circuits, flawless emerald                     (GT: 4 rods)
//   sensor   electrum rod, 3 aluminium plates, MV circuit, flawless emerald                         (GT: 4 plates)
//   field    ender eye, 2 aluminium plates, 2 MV circuits, 3 quadruple wires                       (GT: 4 wires)
//
// The energy is GT's assembler cost at the tier's voltage, converted 1 EU to 4 FE: LV runs 100 ticks at 32 EU/t
// (3200 EU, so the motor takes 12800 FE), MV at 128 EU/t (12800 EU, 51200 FE for the motor). The costlier parts
// scale from there the way the HV set does (conveyor and pump 1.25x, piston 1.5x, arm 3x, emitter and sensor
// 2.25x, field generator 6.25x).

ServerEvents.recipes(event => {
    const item = (id, count) => (count > 1 ? { item: id, count: count } : { item: id })
    const orb = (name, ingredients, energy, result) => {
        event.custom({
            type: 'powah:energizing',
            ingredients: ingredients,
            energy: energy,
            result: { item: result }
        }).id('af9:powah/' + name)
    }

    const lvCircuit = count => ({ tag: 'gtceu:circuits/lv', count: count })
    const mvCircuit = count => ({ tag: 'gtceu:circuits/mv', count: count })
    const BLAZING = item('powah:crystal_blazing', 1)
    const ENERGIZED_STEEL = item('powah:steel_energized', 1)
    const DIELECTRIC_PASTE = item('powah:dielectric_paste', 1)

    // ---- LV (100 ticks at 32 EU/t = 3200 EU -> 12800 FE for the motor) ----

    orb('lv_electric_motor', [
        item('gtceu:tin_single_cable', 2),
        item('gtceu:steel_rod', 1),
        item('gtceu:magnetic_steel_rod', 1),
        item('gtceu:copper_single_wire', 3),
        BLAZING
    ], 12800, 'gtceu:lv_electric_motor')

    orb('lv_conveyor_module', [
        item('gtceu:lv_electric_motor', 2),
        item('gtceu:tin_single_cable', 1),
        item('gtceu:rubber_plate', 4),
        DIELECTRIC_PASTE
    ], 16000, 'gtceu:lv_conveyor_module')

    orb('lv_electric_pump', [
        item('gtceu:lv_electric_motor', 1),
        item('gtceu:tin_single_cable', 1),
        item('gtceu:bronze_normal_fluid_pipe', 1),
        item('gtceu:tin_rotor', 1),
        item('gtceu:rubber_ring', 2),
        DIELECTRIC_PASTE
    ], 16000, 'gtceu:lv_electric_pump')

    orb('lv_electric_piston', [
        item('gtceu:lv_electric_motor', 1),
        item('gtceu:steel_rod', 2),
        item('gtceu:tin_single_cable', 2),
        item('gtceu:steel_plate', 2),
        item('gtceu:small_steel_gear', 1),
        BLAZING
    ], 19200, 'gtceu:lv_electric_piston')

    orb('lv_robot_arm', [
        item('gtceu:lv_electric_motor', 2),
        item('gtceu:lv_electric_piston', 1),
        lvCircuit(1),
        item('gtceu:tin_single_cable', 2),
        item('gtceu:steel_rod', 1),
        BLAZING
    ], 38400, 'gtceu:lv_robot_arm')

    orb('lv_emitter', [
        item('gtceu:brass_rod', 3),
        item('gtceu:tin_single_cable', 2),
        lvCircuit(2),
        item('gtceu:quartzite_gem', 1)
    ], 28800, 'gtceu:lv_emitter')

    orb('lv_sensor', [
        item('gtceu:brass_rod', 1),
        item('gtceu:steel_plate', 3),
        lvCircuit(1),
        item('gtceu:quartzite_gem', 1)
    ], 28800, 'gtceu:lv_sensor')

    orb('lv_field_generator', [
        item('minecraft:ender_pearl', 1),
        item('gtceu:steel_plate', 2),
        lvCircuit(2),
        item('gtceu:manganese_phosphide_quadruple_wire', 3)
    ], 80000, 'gtceu:lv_field_generator')

    // ---- MV (100 ticks at 128 EU/t = 12800 EU -> 51200 FE for the motor) ----

    orb('mv_electric_motor', [
        item('gtceu:copper_single_cable', 2),
        item('gtceu:aluminium_rod', 1),
        item('gtceu:magnetic_steel_rod', 1),
        item('gtceu:cupronickel_double_wire', 3),
        ENERGIZED_STEEL
    ], 51200, 'gtceu:mv_electric_motor')

    orb('mv_conveyor_module', [
        item('gtceu:mv_electric_motor', 2),
        item('gtceu:copper_single_cable', 1),
        item('gtceu:rubber_plate', 4),
        DIELECTRIC_PASTE
    ], 64000, 'gtceu:mv_conveyor_module')

    orb('mv_electric_pump', [
        item('gtceu:mv_electric_motor', 1),
        item('gtceu:copper_single_cable', 1),
        item('gtceu:steel_normal_fluid_pipe', 1),
        item('gtceu:bronze_rotor', 1),
        item('gtceu:rubber_ring', 2),
        DIELECTRIC_PASTE
    ], 64000, 'gtceu:mv_electric_pump')

    orb('mv_electric_piston', [
        item('gtceu:mv_electric_motor', 1),
        item('gtceu:aluminium_rod', 2),
        item('gtceu:copper_single_cable', 2),
        item('gtceu:aluminium_plate', 2),
        item('gtceu:small_aluminium_gear', 1),
        ENERGIZED_STEEL
    ], 76800, 'gtceu:mv_electric_piston')

    orb('mv_robot_arm', [
        item('gtceu:mv_electric_motor', 2),
        item('gtceu:mv_electric_piston', 1),
        mvCircuit(1),
        item('gtceu:copper_single_cable', 2),
        item('gtceu:aluminium_rod', 1),
        ENERGIZED_STEEL
    ], 153600, 'gtceu:mv_robot_arm')

    orb('mv_emitter', [
        item('gtceu:electrum_rod', 3),
        item('gtceu:copper_single_cable', 2),
        mvCircuit(2),
        item('gtceu:flawless_emerald_gem', 1)
    ], 115200, 'gtceu:mv_emitter')

    orb('mv_sensor', [
        item('gtceu:electrum_rod', 1),
        item('gtceu:aluminium_plate', 3),
        mvCircuit(1),
        item('gtceu:flawless_emerald_gem', 1)
    ], 115200, 'gtceu:mv_sensor')

    orb('mv_field_generator', [
        item('minecraft:ender_eye', 1),
        item('gtceu:aluminium_plate', 2),
        mvCircuit(2),
        item('gtceu:magnesium_diboride_quadruple_wire', 3)
    ], 320000, 'gtceu:mv_field_generator')
})
