// AF9 - shaping GT parts in the Energizing Orb Mk2: an extruder mold in one slot (it stays: the recipe marks it
// "nc", AF9 Core's patch of Powah) and the material in the other, and the orb lasers it into shape. A second way
// beside GT's own machines (the lathe, cutter, extruder and press stay; nothing is taken away).
//
// Every rod, bolt, screw, gear, small gear and rotor of the pack works: the recipes are generated from the forge
// tags, so GT's materials and AF9's own (gtceu:<name>) are all covered, and a future material joins by itself.
// The mold is GT's extruder mold, except the screw's: GT has no screw mold, so it is AF9's (af9:screw_extruder_mold).
// GT's ratios, at a flat charge:
//
//   rod         1 ingot + rod mold           -> 2 rods          (2000 FE)
//   bolt        1 ingot + bolt mold          -> 8 bolts         (2000 FE, GT's extruder)
//   screw       1 bolt + screw mold          -> 1 screw         (1000 FE, GT's lathe)
//   gear        4 ingots + gear mold         -> 1 gear         (12000 FE, GT's extruder)
//   small gear  1 ingot + small gear mold    -> 1 small gear    (3000 FE, GT's extruder)
//   rotor       4 ingots + rotor mold        -> 1 rotor        (16000 FE, GT's extruder)

ServerEvents.recipes(event => {
    // The ids of a forge tag, as a set (id -> true). Empty when the tag cannot be read (the linter's stubs).
    const tagSet = tag => {
        const set = {}
        try {
            const ing = Ingredient.of('#' + tag)
            if (ing == null || typeof ing.kjs$getItemIds !== 'function') return set
            const it = ing.kjs$getItemIds().iterator()
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
            const ingredients = []
            if (inputCount > 1) ingredients.push({ item: input, count: inputCount })
            else ingredients.push({ item: input })
            ingredients.push({ item: mold, nc: true })
            const result = outCount > 1 ? { item: out, count: outCount } : { item: out }
            event.custom({
                type: 'powah:energizing',
                ingredients: ingredients,
                energy: energy,
                result: result
            }).id('af9:powah/' + name + '_' + mat)
        })
    }

    shape('rod', 'gtceu:rod_extruder_mold', 'ingot', 1, '', 'rod', 2, 2000)
    shape('bolt', 'gtceu:bolt_extruder_mold', 'ingot', 1, '', 'bolt', 8, 2000)
    shape('screw', 'af9:screw_extruder_mold', 'bolt', 1, '', 'screw', 1, 1000)
    shape('gear', 'gtceu:gear_extruder_mold', 'ingot', 4, '', 'gear', 1, 12000)
    shape('small_gear', 'gtceu:small_gear_extruder_mold', 'ingot', 1, 'small_', 'gear', 1, 3000)
    shape('rotor', 'gtceu:rotor_extruder_mold', 'ingot', 4, '', 'rotor', 1, 16000)
})
