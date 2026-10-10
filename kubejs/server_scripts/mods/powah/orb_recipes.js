// AF9 - every Energizing Orb Mk2 recipe of the pack in one script: the LV, MV and HV components and the shaping
// of GT parts (rods, bolts, screws, gears, small gears, rotors). GT's own recipes stay; the orb is a second way.
// Also here: the craft of the orb block itself (MV, from steel, MV circuits and motors round Powah's orb), so every orb
// recipe of the pack is in this one file.
//
// To add a recipe, copy the TEMPLATE below, fill it in, reload (/reload or restart): that is all. The mold stays
// in the orb when the recipe says keep(): normal inputs go with item().
//
// TEMPLATE - a second way to an HV conveyor that also takes a mold (do not uncomment, it is an example):
// orb('hv_conveyor_module_molded', [
//     item('gtceu:hv_electric_motor', 2),
//     item('gtceu:gold_single_cable', 1),
//     tag('forge:plates/rubber', 4),
//     keep('gtceu:plate_extruder_mold')
// ], 256000, 'gtceu:hv_conveyor_module')
//
// Helpers: tag(name, count) is a consumed forge tag input (every material part: rods, plates, screws, rotors, small
// gears, rings, gems, ender pearls: use the tag, not an id, so the part of any mod works), item(id, count) is a
// consumed input by id (only for items that exist once: GT's cables, wires, pipes and machines, Powah's materials),
// keep(id) is an input the craft keeps (a mold: matched but not consumed), circ(tier, count) is a circuit tag input. orb() writes the recipe;
// a fifth argument is the output count (shaping: 1 ingot lasers into 2 rods, 8 bolts).
//
// Components: each takes everything GT's assembler recipe takes, in the same amounts (the orb is not a way to make a
// component cheaper in parts), and a Powah material as the glue where a slot is left (the orb has six): a small detour
// through Powah, not a second factory. The glue steps up with the tier: LV glues with blazing crystal (the basic
// orb's crystal) and dielectric paste (coal and clay), MV and HV with energized steel (iron and gold) and paste. What
// the orb saves is power: the energy is GT's assembler cost at the tier's voltage, converted 1 EU to 4 FE (LV 100
// ticks at 32 EU/t = 12800 FE for the motor, MV 51200, HV 204800); the costlier parts scale from there
// (conveyor and pump 1.25x, piston 1.5x, arm 3x, emitter and sensor 2.25x, field generator 6.25x). The charge
// scales with the rods round the orb, more rods, a shorter wait.
//
// Shaping: an extruder mold in one slot (it stays) and the material in the other, at GT's ratios and a flat
// charge, never under 10000 FE a craft (the big parts cost more). Every material of the pack works: the recipes generate from the forge tags, so a future material joins
// by itself. The mold is GT's, except the screw's: GT has none, so it is AF9's (af9:screw_extruder_mold).

ServerEvents.recipes(event => {
    // ---- the Energizing Orb Mk2 block: Powah's orb with a screen and eight slots (AF9 Core, compat/powah) ----
    event.shaped('af9:energizing_orb_mk2', [
        'PCP',
        'MOM',
        'PCP'
    ], {
        P: '#forge:plates/steel',
        C: 'gtceu:good_electronic_circuit',
        M: 'gtceu:mv_electric_motor',
        O: 'powah:energizing_orb'
    }).id('af9:powah/energizing_orb_mk2')

    const item = (id, count) => (count > 1 ? { item: id, count: count } : { item: id })
    const keep = id => ({ item: id, nc: true })
    // a forge tag input (a material part: rods, plates, screws ... of any mod): never a bare id, so every mod's copy
    // of the part works. Items that exist once (GT's cables, wires, pipes, machines, Powah's crystals) stay ids.
    const tag = (name, count) => (count > 1 ? { tag: name, count: count } : { tag: name })
    const circ = (tier, count) => ({ tag: 'gtceu:circuits/' + tier, count: count })
    const orb = (name, ingredients, energy, result, count) => {
        event.custom({
            type: 'powah:energizing',
            ingredients: ingredients,
            energy: energy,
            result: count > 1 ? { item: result, count: count } : { item: result }
        }).id('af9:powah/' + name)
    }

    // ---- LV components: GT's assembler ingredients in full, and a glue where a slot is left ----
    //   motor    2 tin cables, 2 steel rods, 1 magnetic steel rod, 4 copper wires, blazing crystal
    //   conveyor 2 motors, 1 tin cable, 6 rubber plates, dielectric paste
    //   pump     motor, tin cable, bronze pipe, tin screw, tin rotor, 2 rubber rings (six slots: no glue)
    //   piston   motor, 2 steel rods, 2 tin cables, 3 steel plates, small gear, blazing crystal
    //   arm      2 motors, piston, LV circuit, 3 tin cables, 2 steel rods, blazing crystal
    //   emitter  4 brass rods, 2 tin cables, 2 LV circuits, quartzite
    //   sensor   brass rod, 4 steel plates, LV circuit, quartzite
    //   field    ender pearl, 2 steel plates, 2 LV circuits, 4 quadruple wires

    orb('lv_electric_motor', [
        item('gtceu:tin_single_cable', 2),
        tag('forge:rods/steel', 2),
        tag('forge:rods/magnetic_steel', 1),
        item('gtceu:copper_single_wire', 4),
        item('powah:crystal_blazing', 1)
    ], 12800, 'gtceu:lv_electric_motor')

    orb('lv_conveyor_module', [
        item('gtceu:lv_electric_motor', 2),
        item('gtceu:tin_single_cable', 1),
        tag('forge:plates/rubber', 6),
        item('powah:dielectric_paste', 1)
    ], 16000, 'gtceu:lv_conveyor_module')

    orb('lv_electric_pump', [
        item('gtceu:lv_electric_motor', 1),
        item('gtceu:tin_single_cable', 1),
        item('gtceu:bronze_normal_fluid_pipe', 1),
        tag('forge:screws/tin', 1),
        tag('forge:rotors/tin', 1),
        tag('forge:rings/rubber', 2)
    ], 16000, 'gtceu:lv_electric_pump')

    orb('lv_electric_piston', [
        item('gtceu:lv_electric_motor', 1),
        tag('forge:rods/steel', 2),
        item('gtceu:tin_single_cable', 2),
        tag('forge:plates/steel', 3),
        tag('forge:small_gears/steel', 1),
        item('powah:crystal_blazing', 1)
    ], 19200, 'gtceu:lv_electric_piston')

    orb('lv_robot_arm', [
        item('gtceu:lv_electric_motor', 2),
        item('gtceu:lv_electric_piston', 1),
        circ('lv', 1),
        item('gtceu:tin_single_cable', 3),
        tag('forge:rods/steel', 2),
        item('powah:crystal_blazing', 1)
    ], 38400, 'gtceu:lv_robot_arm')

    orb('lv_emitter', [
        tag('forge:rods/brass', 4),
        item('gtceu:tin_single_cable', 2),
        circ('lv', 2),
        tag('forge:gems/quartzite', 1)
    ], 28800, 'gtceu:lv_emitter')

    orb('lv_sensor', [
        tag('forge:rods/brass', 1),
        tag('forge:plates/steel', 4),
        circ('lv', 1),
        tag('forge:gems/quartzite', 1)
    ], 28800, 'gtceu:lv_sensor')

    orb('lv_field_generator', [
        tag('forge:ender_pearls', 1),
        tag('forge:plates/steel', 2),
        circ('lv', 2),
        item('gtceu:manganese_phosphide_quadruple_wire', 4)
    ], 80000, 'gtceu:lv_field_generator')

    // ---- MV components: GT's assembler ingredients in full, and a glue where a slot is left ----
    //   motor    2 copper cables, 2 aluminium rods, 1 magnetic steel rod, 4 cupronickel wires, energized steel
    //   conveyor 2 motors, 1 copper cable, 6 rubber plates, dielectric paste
    //   pump     motor, copper cable, steel pipe, bronze screw, bronze rotor, 2 rubber rings (six slots: no glue)
    //   piston   motor, 2 aluminium rods, 2 copper cables, 3 aluminium plates, small gear, energized steel
    //   arm      2 motors, piston, MV circuit, 3 copper cables, 2 aluminium rods, energized steel
    //   emitter  4 electrum rods, 2 copper cables, 2 MV circuits, flawless emerald
    //   sensor   electrum rod, 4 aluminium plates, MV circuit, flawless emerald
    //   field    ender eye, 2 aluminium plates, 2 MV circuits, 4 quadruple wires

    orb('mv_electric_motor', [
        item('gtceu:copper_single_cable', 2),
        tag('forge:rods/aluminium', 2),
        tag('forge:rods/magnetic_steel', 1),
        item('gtceu:cupronickel_double_wire', 4),
        item('powah:steel_energized', 1)
    ], 51200, 'gtceu:mv_electric_motor')

    orb('mv_conveyor_module', [
        item('gtceu:mv_electric_motor', 2),
        item('gtceu:copper_single_cable', 1),
        tag('forge:plates/rubber', 6),
        item('powah:dielectric_paste', 1)
    ], 64000, 'gtceu:mv_conveyor_module')

    orb('mv_electric_pump', [
        item('gtceu:mv_electric_motor', 1),
        item('gtceu:copper_single_cable', 1),
        item('gtceu:steel_normal_fluid_pipe', 1),
        tag('forge:screws/bronze', 1),
        tag('forge:rotors/bronze', 1),
        tag('forge:rings/rubber', 2)
    ], 64000, 'gtceu:mv_electric_pump')

    orb('mv_electric_piston', [
        item('gtceu:mv_electric_motor', 1),
        tag('forge:rods/aluminium', 2),
        item('gtceu:copper_single_cable', 2),
        tag('forge:plates/aluminium', 3),
        tag('forge:small_gears/aluminium', 1),
        item('powah:steel_energized', 1)
    ], 76800, 'gtceu:mv_electric_piston')

    orb('mv_robot_arm', [
        item('gtceu:mv_electric_motor', 2),
        item('gtceu:mv_electric_piston', 1),
        circ('mv', 1),
        item('gtceu:copper_single_cable', 3),
        tag('forge:rods/aluminium', 2),
        item('powah:steel_energized', 1)
    ], 153600, 'gtceu:mv_robot_arm')

    orb('mv_emitter', [
        tag('forge:rods/electrum', 4),
        item('gtceu:copper_single_cable', 2),
        circ('mv', 2),
        tag('forge:flawless_gems/emerald', 1)
    ], 115200, 'gtceu:mv_emitter')

    orb('mv_sensor', [
        tag('forge:rods/electrum', 1),
        tag('forge:plates/aluminium', 4),
        circ('mv', 1),
        tag('forge:flawless_gems/emerald', 1)
    ], 115200, 'gtceu:mv_sensor')

    orb('mv_field_generator', [
        item('minecraft:ender_eye', 1),
        tag('forge:plates/aluminium', 2),
        circ('mv', 2),
        item('gtceu:magnesium_diboride_quadruple_wire', 4)
    ], 320000, 'gtceu:mv_field_generator')

    // ---- HV components: GT's assembler ingredients in full, and a glue where a slot is left ----
    //   motor    2 silver double cables, 2 stainless rods, 1 magnetic steel rod, 4 electrum double wires, energized steel
    //   conveyor 2 motors, 1 gold cable, 6 rubber plates, dielectric paste
    //   pump     motor, gold cable, stainless pipe, steel screw, steel rotor, 2 rubber rings (six slots: no glue)
    //   piston   motor, 2 stainless rods, 2 gold cables, 3 stainless plates, small gear, energized steel
    //   arm      2 motors, piston, HV circuit, 3 gold cables, 2 stainless rods, energized steel
    //   emitter  4 chromium rods, 2 gold cables, 2 HV circuits, ender eye
    //   sensor   chromium rod, 4 stainless plates, HV circuit, ender eye
    //   field    quantum eye, 2 stainless plates, 2 HV circuits, 4 quadruple wires

    orb('hv_electric_motor', [
        item('gtceu:silver_double_cable', 2),
        tag('forge:rods/stainless_steel', 2),
        item('powah:steel_energized', 1),
        tag('forge:rods/magnetic_steel', 1),
        item('gtceu:electrum_double_wire', 4)
    ], 204800, 'gtceu:hv_electric_motor')

    orb('hv_conveyor_module', [
        item('gtceu:hv_electric_motor', 2),
        item('gtceu:gold_single_cable', 1),
        tag('forge:plates/rubber', 6),
        item('powah:dielectric_paste', 1)
    ], 256000, 'gtceu:hv_conveyor_module')

    orb('hv_electric_pump', [
        item('gtceu:hv_electric_motor', 1),
        item('gtceu:gold_single_cable', 1),
        item('gtceu:stainless_steel_normal_fluid_pipe', 1),
        tag('forge:screws/steel', 1),
        tag('forge:rotors/steel', 1),
        tag('forge:rings/rubber', 2)
    ], 256000, 'gtceu:hv_electric_pump')

    orb('hv_electric_piston', [
        item('gtceu:hv_electric_motor', 1),
        tag('forge:rods/stainless_steel', 2),
        item('gtceu:gold_single_cable', 2),
        tag('forge:plates/stainless_steel', 3),
        tag('forge:small_gears/stainless_steel', 1),
        item('powah:steel_energized', 1)
    ], 307200, 'gtceu:hv_electric_piston')

    orb('hv_robot_arm', [
        item('gtceu:hv_electric_motor', 2),
        item('gtceu:hv_electric_piston', 1),
        circ('hv', 1),
        item('gtceu:gold_single_cable', 3),
        tag('forge:rods/stainless_steel', 2),
        item('powah:steel_energized', 1)
    ], 614400, 'gtceu:hv_robot_arm')

    orb('hv_emitter', [
        tag('forge:rods/chromium', 4),
        item('gtceu:gold_single_cable', 2),
        circ('hv', 2),
        item('minecraft:ender_eye', 1)
    ], 460800, 'gtceu:hv_emitter')

    orb('hv_sensor', [
        tag('forge:rods/chromium', 1),
        tag('forge:plates/stainless_steel', 4),
        circ('hv', 1),
        item('minecraft:ender_eye', 1)
    ], 460800, 'gtceu:hv_sensor')

    orb('hv_field_generator', [
        item('gtceu:quantum_eye', 1),
        tag('forge:plates/stainless_steel', 2),
        circ('hv', 2),
        item('gtceu:mercury_barium_calcium_cuprate_quadruple_wire', 4)
    ], 1280000, 'gtceu:hv_field_generator')

    // ---- Shaping (generated from the forge tags; a future material joins by itself) ----
    //   rod         1 ingot + rod mold           -> 2 rods         (10000 FE)
    //   bolt        1 ingot + bolt mold          -> 8 bolts        (10000 FE)
    //   screw       1 bolt + screw mold          -> 1 screw        (10000 FE)
    //   gear        4 ingots + gear mold         -> 1 gear         (12000 FE)
    //   small gear  1 ingot + small gear mold    -> 1 small gear   (10000 FE)
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
            // the input is the tag of the material's part (any mod's ingot or bolt), not one id
            const input = 'gtceu:' + mat + '_' + inputSuffix
            if (inputSuffix === 'bolt' && !parts.bolt[input]) return
            const inputItem = tag('forge:' + (inputSuffix === 'ingot' ? 'ingots' : 'bolts') + '/' + mat, inputCount)
            orb(name + '_' + mat, [inputItem, keep(mold)], energy, out, outCount)
            shapedCount[0]++
        })
    }

    shape('rod', 'gtceu:rod_extruder_mold', 'ingot', 1, '', 'rod', 2, 10000)
    shape('bolt', 'gtceu:bolt_extruder_mold', 'ingot', 1, '', 'bolt', 8, 10000)
    shape('screw', 'af9:screw_extruder_mold', 'bolt', 1, '', 'screw', 1, 10000)
    shape('gear', 'gtceu:gear_extruder_mold', 'ingot', 4, '', 'gear', 1, 12000)
    shape('small_gear', 'gtceu:small_gear_extruder_mold', 'ingot', 1, 'small_', 'gear', 1, 10000)
    shape('rotor', 'gtceu:rotor_extruder_mold', 'ingot', 4, '', 'rotor', 1, 16000)
    // (only in the game: the linter's stubs read no tags, and its output has to stay what the quest linter parses)
    if (shapedCount[0] > 0) {
        console.info('[af9] orb shaping: ' + shapedCount[0] + ' recipes (rods, bolts, screws, gears, small gears, rotors)')
    }
})
