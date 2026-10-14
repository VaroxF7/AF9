// AF9 - every Energizing Orb Mk2 recipe of the pack in one script: the shaping of GT parts
// (rods, bolts, screws, gears, small gears, rotors). GT's own recipes stay; the orb is a second way
// for shaping only. Components (motors in motors.js, everything else in GT's assembler) are built
// the GT way, at GT's cost.
// Also here: the craft of the orb block itself (MV, from steel, MV circuits and motors round Powah's orb), so every orb
// recipe of the pack is in this one file.
//
// To add a recipe, copy the TEMPLATE below, fill it in, reload (/reload or restart): that is all. The mold stays
// in the orb when the recipe says keep(): normal inputs go with item().
//
// TEMPLATE - a shaping recipe that also takes a mold (do not uncomment, it is an example):
// orb('rod_steel_molded', [
//     tag('forge:ingots/steel', 1),
//     keep('gtceu:rod_extruder_mold')
// ], 10000, 'gtceu:steel_rod', 2)
//
// Helpers: tag(name, count) is a consumed forge tag input (every material part: rods, plates, screws, rotors, small
// gears, rings, gems, ender pearls: use the tag, not an id, so the part of any mod works), item(id, count) is a
// consumed input by id (only for items that exist once: GT's cables, wires, pipes and machines, Powah's materials),
// keep(id) is an input the craft keeps (a mold: matched but not consumed), circ(tier, count) is a circuit tag input. orb() writes the recipe;
// a fifth argument is the output count (shaping: 1 ingot lasers into 2 rods, 8 bolts).
//
// Shaping is all the orb does now: components left the orb for the assembler (same parts, same EU,
// the tier's voltage), so the orb saves no detour through Powah anymore. What it still saves is the
// extruder: no molds tied up in machines, and every material works by tag.
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

    // ---- (LV/MV/HV components used to live here, in the orb; they assemble now, same cost) ----

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

    // One mat -> parts map in a single pass over the resolved tags (each tag resolved once above);
    // the 6 shaping recipes per material emit from the map below (same recipes, same ids and inputs).
    const mats = {}
    Object.keys(ingots).forEach(ingotId => {
        if (!ingotId.startsWith('gtceu:') || !ingotId.endsWith('_ingot')) return
        const mat = ingotId.slice('gtceu:'.length, -'_ingot'.length)
        if (!mat || mats[mat]) return
        const entry = {}
        if (parts.rod['gtceu:' + mat + '_rod']) entry.rod = 'gtceu:' + mat + '_rod'
        if (parts.bolt['gtceu:' + mat + '_bolt']) entry.bolt = 'gtceu:' + mat + '_bolt'
        if (entry.bolt && parts.screw['gtceu:' + mat + '_screw']) entry.screw = 'gtceu:' + mat + '_screw'
        if (parts.gear['gtceu:' + mat + '_gear']) entry.gear = 'gtceu:' + mat + '_gear'
        if (parts.small_gear['gtceu:small_' + mat + '_gear']) entry.small_gear = 'gtceu:small_' + mat + '_gear'
        if (parts.rotor['gtceu:' + mat + '_rotor']) entry.rotor = 'gtceu:' + mat + '_rotor'
        mats[mat] = entry
    })

    // One shaping recipe per material that has both ends: the input part and the output part.
    // (name, mold, input tag suffix, input count, output count, energy: GT's ratios, flat charge)
    const shapedCount = [0]
    const shapes = [
        ['rod', 'gtceu:rod_extruder_mold', 'ingot', 1, 2, 10000],
        ['bolt', 'gtceu:bolt_extruder_mold', 'ingot', 1, 8, 10000],
        ['screw', 'af9:screw_extruder_mold', 'bolt', 1, 1, 10000],
        ['gear', 'gtceu:gear_extruder_mold', 'ingot', 4, 1, 12000],
        ['small_gear', 'gtceu:small_gear_extruder_mold', 'ingot', 1, 1, 10000],
        ['rotor', 'gtceu:rotor_extruder_mold', 'ingot', 4, 1, 16000]
    ]
    Object.keys(mats).forEach(mat => {
        shapes.forEach(shape => {
            const name = shape[0]
            const out = mats[mat][name]
            if (!out) return
            // the input is the tag of the material's part (any mod's ingot or bolt), not one id
            const inputItem = tag('forge:' + (shape[2] === 'ingot' ? 'ingots' : 'bolts') + '/' + mat, shape[3])
            orb(name + '_' + mat, [inputItem, keep(shape[1])], shape[5], out, shape[4])
            shapedCount[0]++
        })
    })
    // (only in the game: the linter's stubs read no tags, and its output has to stay what the quest linter parses)
    if (shapedCount[0] > 0) {
        console.info('[af9] orb shaping: ' + shapedCount[0] + ' recipes (rods, bolts, screws, gears, small gears, rotors)')
    }
})
