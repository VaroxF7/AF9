// AF9 - every Energizing Orb Mk2 recipe of the pack in one script: the LV, MV and HV components and the shaping
// of GT parts (rods, bolts, screws, gears, small gears, rotors). GT's own recipes stay; the orb is a second way.
// (The orb block itself is crafted in orb_mk2.js; this file only holds what runs inside it.)
//
// To add a recipe, copy the TEMPLATE below, fill it in, reload (/reload or restart): that is all. The mold stays
// in the orb when the recipe says keep(): normal inputs go with item().
//
// TEMPLATE - a second way to an HV conveyor that also takes a mold (do not uncomment, it is an example):
// orb('hv_conveyor_module_molded', [
//     item('gtceu:hv_electric_motor', 2),
//     item('gtceu:gold_single_cable', 1),
//     item('gtceu:rubber_plate', 4),
//     keep('gtceu:plate_extruder_mold')
// ], 256000, 'gtceu:hv_conveyor_module')
//
// Helpers: item(id, count) is a consumed input (count 1 or missing takes one), keep(id) is an input the craft
// keeps (a mold: matched but not consumed), circ(tier, count) is a circuit tag input. orb() writes the recipe;
// a fifth argument is the output count (shaping: 1 ingot lasers into 2 rods, 8 bolts).
//
// Components: each is cheaper than GT's assembler recipe by a part or two and takes a Powah material as the glue,
// a small detour through Powah, not a second factory. The glue steps up with the tier: LV glues with blazing
// crystal (the basic orb's crystal) and dielectric paste (coal and clay), MV and HV with energized steel (iron
// and gold) and paste. The energy is GT's assembler cost at the tier's voltage, converted 1 EU to 4 FE (LV 100
// ticks at 32 EU/t = 12800 FE for the motor, MV 51200, HV 204800); the costlier parts scale from there
// (conveyor and pump 1.25x, piston 1.5x, arm 3x, emitter and sensor 2.25x, field generator 6.25x). The charge
// scales with the rods round the orb, more rods, a shorter wait.
//
// Shaping: an extruder mold in one slot (it stays) and the material in the other, at GT's ratios and a flat
// charge. Every material of the pack works: the recipes generate from the forge tags, so a future material joins
// by itself. The mold is GT's, except the screw's: GT has none, so it is AF9's (af9:screw_extruder_mold).

ServerEvents.recipes(event => {
    const item = (id, count) => (count > 1 ? { item: id, count: count } : { item: id })
    const keep = id => ({ item: id, nc: true })
    const circ = (tier, count) => ({ tag: 'gtceu:circuits/' + tier, count: count })
    const orb = (name, ingredients, energy, result, count) => {
        event.custom({
            type: 'powah:energizing',
            ingredients: ingredients,
            energy: energy,
            result: count > 1 ? { item: result, count: count } : { item: result }
        }).id('af9:powah/' + name)
    }

    // ---- LV components (GT assembler: 2 tin cables, 2 steel rods, 1 magnetic rod, 4 copper wires, and so on) ----
    //   motor    2 tin cables, 1 steel rod, 1 magnetic steel rod, 3 copper wires, blazing crystal       (GT: 9 items)
    //   conveyor 2 motors, 1 tin cable, 4 rubber plates, dielectric paste                               (GT: 6 plates)
    //   pump     motor, tin cable, bronze pipe, tin rotor, 2 rubber rings, dielectric paste             (GT: and a screw)
    //   piston   motor, 2 steel rods, 2 tin cables, 2 steel plates, small gear, blazing crystal         (GT: 3 plates)
    //   arm      2 motors, piston, LV circuit, 2 tin cables, steel rod, blazing crystal                 (GT: 3 cables)
    //   emitter  3 brass rods, 2 tin cables, 2 LV circuits, quartzite                                   (GT: 4 rods)
    //   sensor   brass rod, 3 steel plates, LV circuit, quartzite                                       (GT: 4 plates)
    //   field    ender pearl, 2 steel plates, 2 LV circuits, 3 quadruple wires                          (GT: 4 wires)

    orb('lv_electric_motor', [
        item('gtceu:tin_single_cable', 2),
        item('gtceu:steel_rod', 1),
        item('gtceu:magnetic_steel_rod', 1),
        item('gtceu:copper_single_wire', 3),
        item('powah:crystal_blazing', 1)
    ], 12800, 'gtceu:lv_electric_motor')

    orb('lv_conveyor_module', [
        item('gtceu:lv_electric_motor', 2),
        item('gtceu:tin_single_cable', 1),
        item('gtceu:rubber_plate', 4),
        item('powah:dielectric_paste', 1)
    ], 16000, 'gtceu:lv_conveyor_module')

    orb('lv_electric_pump', [
        item('gtceu:lv_electric_motor', 1),
        item('gtceu:tin_single_cable', 1),
        item('gtceu:bronze_normal_fluid_pipe', 1),
        item('gtceu:tin_rotor', 1),
        item('gtceu:rubber_ring', 2),
        item('powah:dielectric_paste', 1)
    ], 16000, 'gtceu:lv_electric_pump')

    orb('lv_electric_piston', [
        item('gtceu:lv_electric_motor', 1),
        item('gtceu:steel_rod', 2),
        item('gtceu:tin_single_cable', 2),
        item('gtceu:steel_plate', 2),
        item('gtceu:small_steel_gear', 1),
        item('powah:crystal_blazing', 1)
    ], 19200, 'gtceu:lv_electric_piston')

    orb('lv_robot_arm', [
        item('gtceu:lv_electric_motor', 2),
        item('gtceu:lv_electric_piston', 1),
        circ('lv', 1),
        item('gtceu:tin_single_cable', 2),
        item('gtceu:steel_rod', 1),
        item('powah:crystal_blazing', 1)
    ], 38400, 'gtceu:lv_robot_arm')

    orb('lv_emitter', [
        item('gtceu:brass_rod', 3),
        item('gtceu:tin_single_cable', 2),
        circ('lv', 2),
        item('gtceu:quartzite_gem', 1)
    ], 28800, 'gtceu:lv_emitter')

    orb('lv_sensor', [
        item('gtceu:brass_rod', 1),
        item('gtceu:steel_plate', 3),
        circ('lv', 1),
        item('gtceu:quartzite_gem', 1)
    ], 28800, 'gtceu:lv_sensor')

    orb('lv_field_generator', [
        item('minecraft:ender_pearl', 1),
        item('gtceu:steel_plate', 2),
        circ('lv', 2),
        item('gtceu:manganese_phosphide_quadruple_wire', 3)
    ], 80000, 'gtceu:lv_field_generator')

    // ---- MV components (GT assembler: 2 copper cables, 2 aluminium rods, 1 magnetic rod, 4 cupronickel wires) ----
    //   motor    2 copper cables, 1 aluminium rod, 1 magnetic steel rod, 3 cupronickel wires, steel     (GT: 9 items)
    //   conveyor 2 motors, 1 copper cable, 4 rubber plates, dielectric paste                            (GT: 6 plates)
    //   pump     motor, copper cable, steel pipe, bronze rotor, 2 rubber rings, paste                   (GT: and a screw)
    //   piston   motor, 2 aluminium rods, 2 copper cables, 2 aluminium plates, small gear, steel        (GT: 3 plates)
    //   arm      2 motors, piston, MV circuit, 2 copper cables, aluminium rod, steel                    (GT: 3 cables)
    //   emitter  3 electrum rods, 2 copper cables, 2 MV circuits, flawless emerald                     (GT: 4 rods)
    //   sensor   electrum rod, 3 aluminium plates, MV circuit, flawless emerald                         (GT: 4 plates)
    //   field    ender eye, 2 aluminium plates, 2 MV circuits, 3 quadruple wires                       (GT: 4 wires)

    orb('mv_electric_motor', [
        item('gtceu:copper_single_cable', 2),
        item('gtceu:aluminium_rod', 1),
        item('gtceu:magnetic_steel_rod', 1),
        item('gtceu:cupronickel_double_wire', 3),
        item('powah:steel_energized', 1)
    ], 51200, 'gtceu:mv_electric_motor')

    orb('mv_conveyor_module', [
        item('gtceu:mv_electric_motor', 2),
        item('gtceu:copper_single_cable', 1),
        item('gtceu:rubber_plate', 4),
        item('powah:dielectric_paste', 1)
    ], 64000, 'gtceu:mv_conveyor_module')

    orb('mv_electric_pump', [
        item('gtceu:mv_electric_motor', 1),
        item('gtceu:copper_single_cable', 1),
        item('gtceu:steel_normal_fluid_pipe', 1),
        item('gtceu:bronze_rotor', 1),
        item('gtceu:rubber_ring', 2),
        item('powah:dielectric_paste', 1)
    ], 64000, 'gtceu:mv_electric_pump')

    orb('mv_electric_piston', [
        item('gtceu:mv_electric_motor', 1),
        item('gtceu:aluminium_rod', 2),
        item('gtceu:copper_single_cable', 2),
        item('gtceu:aluminium_plate', 2),
        item('gtceu:small_aluminium_gear', 1),
        item('powah:steel_energized', 1)
    ], 76800, 'gtceu:mv_electric_piston')

    orb('mv_robot_arm', [
        item('gtceu:mv_electric_motor', 2),
        item('gtceu:mv_electric_piston', 1),
        circ('mv', 1),
        item('gtceu:copper_single_cable', 2),
        item('gtceu:aluminium_rod', 1),
        item('powah:steel_energized', 1)
    ], 153600, 'gtceu:mv_robot_arm')

    orb('mv_emitter', [
        item('gtceu:electrum_rod', 3),
        item('gtceu:copper_single_cable', 2),
        circ('mv', 2),
        item('gtceu:flawless_emerald_gem', 1)
    ], 115200, 'gtceu:mv_emitter')

    orb('mv_sensor', [
        item('gtceu:electrum_rod', 1),
        item('gtceu:aluminium_plate', 3),
        circ('mv', 1),
        item('gtceu:flawless_emerald_gem', 1)
    ], 115200, 'gtceu:mv_sensor')

    orb('mv_field_generator', [
        item('minecraft:ender_eye', 1),
        item('gtceu:aluminium_plate', 2),
        circ('mv', 2),
        item('gtceu:magnesium_diboride_quadruple_wire', 3)
    ], 320000, 'gtceu:mv_field_generator')

    // ---- HV components (GT assembler: 2 silver cables, 2 stainless rods, 1 magnetic rod, 4 electrum wires) ----
    //   motor    2 silver cable, 1 stainless rod, 1 energized steel, 1 magnetic rod, 3 electrum wire        (GT: 9 items)
    //   conveyor 2 motors, 1 gold cable, 4 rubber plates, dielectric paste                                  (GT: 6 ingots of rubber)
    //   pump     motor, gold cable, stainless pipe, steel rotor, 2 rubber rings, dielectric paste           (GT: and a screw)
    //   piston   motor, 2 stainless rods, 2 gold cables, 2 stainless plates, small gear, energized steel    (GT: 3 plates)
    //   arm      2 motors, piston, HV circuit, 2 gold cables, stainless rod, energized steel                (GT: 3 cables)
    //   emitter  3 chromium rods, 2 gold cables, 2 HV circuits, ender eye                                   (GT: 4 rods)
    //   sensor   chromium rod, 3 stainless plates, HV circuit, ender eye                                    (GT: 4 plates)
    //   field    quantum eye, 2 stainless plates, 2 HV circuits, 3 quadruple wires                          (GT: 4 wires)

    orb('hv_electric_motor', [
        item('gtceu:silver_double_cable', 2),
        item('gtceu:stainless_steel_rod', 1),
        item('powah:steel_energized', 1),
        item('gtceu:magnetic_steel_rod', 1),
        item('gtceu:electrum_double_wire', 3)
    ], 204800, 'gtceu:hv_electric_motor')

    orb('hv_conveyor_module', [
        item('gtceu:hv_electric_motor', 2),
        item('gtceu:gold_single_cable', 1),
        item('gtceu:rubber_plate', 4),
        item('powah:dielectric_paste', 1)
    ], 256000, 'gtceu:hv_conveyor_module')

    orb('hv_electric_pump', [
        item('gtceu:hv_electric_motor', 1),
        item('gtceu:gold_single_cable', 1),
        item('gtceu:stainless_steel_normal_fluid_pipe', 1),
        item('gtceu:steel_rotor', 1),
        item('gtceu:rubber_ring', 2),
        item('powah:dielectric_paste', 1)
    ], 256000, 'gtceu:hv_electric_pump')

    orb('hv_electric_piston', [
        item('gtceu:hv_electric_motor', 1),
        item('gtceu:stainless_steel_rod', 2),
        item('gtceu:gold_single_cable', 2),
        item('gtceu:stainless_steel_plate', 2),
        item('gtceu:small_stainless_steel_gear', 1),
        item('powah:steel_energized', 1)
    ], 307200, 'gtceu:hv_electric_piston')

    orb('hv_robot_arm', [
        item('gtceu:hv_electric_motor', 2),
        item('gtceu:hv_electric_piston', 1),
        circ('hv', 1),
        item('gtceu:gold_single_cable', 2),
        item('gtceu:stainless_steel_rod', 1),
        item('powah:steel_energized', 1)
    ], 614400, 'gtceu:hv_robot_arm')

    orb('hv_emitter', [
        item('gtceu:chromium_rod', 3),
        item('gtceu:gold_single_cable', 2),
        circ('hv', 2),
        item('minecraft:ender_eye', 1)
    ], 460800, 'gtceu:hv_emitter')

    orb('hv_sensor', [
        item('gtceu:chromium_rod', 1),
        item('gtceu:stainless_steel_plate', 3),
        circ('hv', 1),
        item('minecraft:ender_eye', 1)
    ], 460800, 'gtceu:hv_sensor')

    orb('hv_field_generator', [
        item('gtceu:quantum_eye', 1),
        item('gtceu:stainless_steel_plate', 2),
        circ('hv', 2),
        item('gtceu:mercury_barium_calcium_cuprate_quadruple_wire', 3)
    ], 1280000, 'gtceu:hv_field_generator')

    // ---- Shaping (generated from the forge tags; a future material joins by itself) ----
    //   rod         1 ingot + rod mold           -> 2 rods          (2000 FE)
    //   bolt        1 ingot + bolt mold          -> 8 bolts         (2000 FE)
    //   screw       1 bolt + screw mold          -> 1 screw         (1000 FE)
    //   gear        4 ingots + gear mold         -> 1 gear         (12000 FE)
    //   small gear  1 ingot + small gear mold    -> 1 small gear    (3000 FE)
    //   rotor       4 ingots + rotor mold        -> 1 rotor        (16000 FE)

    // The ids of a forge tag, as a set (id -> true). Empty when the tag cannot be read (the linter's stubs).
    // (No const or let inside: Rhino keeps a const of a nested block once for the whole script, so the second call
    // died with "redeclaration of var ing" and no shaping recipe was made at all; var is function scoped.)
    const tagSet = tag => {
        var set = {}
        var ing, ids, it
        try {
            ing = Ingredient.of('#' + tag)
            if (ing == null) return set
            if (typeof ing.kjs$getItemIds === 'function') ids = ing.kjs$getItemIds()
            else if (typeof ing.getItemIds === 'function') ids = ing.getItemIds()
            else return set
            it = ids.iterator()
            while (it.hasNext()) set[String(it.next())] = true
        } catch (e) {
            console.error('[af9] shaping: cannot read tag #' + tag + ': ' + e)
        }
        return set
    }

    const ingots = tagSet('forge:ingots')
    const parts = {
        rod: tagSet('forge:rods'),
        bolt: tagSet('forge:bolts'),
        screw: tagSet('forge:screws'),
        gear: tagSet('forge:gears'),
        small_gear: tagSet('forge:small_gears'),
        rotor: tagSet('forge:rotors')
    }

    // One shaping recipe per material that has both ends: the input part and the output part.
    const shapedCount = [0]
    const shape = (name, mold, inputSuffix, inputCount, outPrefix, outSuffix, outCount, energy) => {
        const seen = {}
        Object.keys(ingots).forEach(ingotId => {
            if (!ingotId.startsWith('gtceu:') || !ingotId.endsWith('_ingot')) return
            const mat = ingotId.slice('gtceu:'.length, -'_ingot'.length)
            if (!mat || seen[mat]) return
            seen[mat] = true
            const out = 'gtceu:' + outPrefix + mat + '_' + outSuffix
            if (!parts[name][out]) return
            const input = 'gtceu:' + mat + '_' + inputSuffix
            if (inputSuffix === 'rod' && !parts.rod[input]) return
            if (inputSuffix === 'bolt' && !parts.bolt[input]) return
            const inputItem = inputCount > 1 ? { item: input, count: inputCount } : { item: input }
            orb(name + '_' + mat, [inputItem, keep(mold)], energy, out, outCount)
            shapedCount[0]++
        })
    }

    shape('rod', 'gtceu:rod_extruder_mold', 'ingot', 1, '', 'rod', 2, 2000)
    shape('bolt', 'gtceu:bolt_extruder_mold', 'ingot', 1, '', 'bolt', 8, 2000)
    shape('screw', 'af9:screw_extruder_mold', 'bolt', 1, '', 'screw', 1, 1000)
    shape('gear', 'gtceu:gear_extruder_mold', 'ingot', 4, '', 'gear', 1, 12000)
    shape('small_gear', 'gtceu:small_gear_extruder_mold', 'ingot', 1, 'small_', 'gear', 1, 3000)
    shape('rotor', 'gtceu:rotor_extruder_mold', 'ingot', 4, '', 'rotor', 1, 16000)
    console.info('[af9] orb shaping: ' + shapedCount[0] + ' recipes (rods, bolts, screws, gears, small gears, rotors)')
})
