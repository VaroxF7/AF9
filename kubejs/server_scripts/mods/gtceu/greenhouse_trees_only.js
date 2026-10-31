// priority: -100
// AF9 - The Greenhouse only grows trees. The base pack (ATM9, kubejs/server_scripts/mods/gtceu/greenhouse_recipes.js, which
// this repository does not hold) ADDS a greenhouse recipe for every Botany Pots crop, every Mystical Agriculture seed and the
// GT rubber sapling, each in four or more variants. Crops and seeds are the Agriculture Simulation Bed's now (agri_sim.js).
//
// KubeJS (1.20.1, 6.x) cannot do this with event.remove or forEachRecipe: both only see the recipes that were loaded from data
// packs (event.originalRecipes); a recipe another script adds in the same event (event.addedRecipes) is never matched by a
// filter. So the added ones are taken out of event.addedRecipes directly, after the base pack's script has run (the priority
// makes this file the last). What stays: every recipe whose id (or, with no id yet, inputs and outputs) names a sapling or
// propagule: the rubber sapling and the Botany Pots trees. The file's recipes from data packs go through the normal filter.

ServerEvents.recipes(event => {
    const trees = /sapling|propagule/
    const isGreenhouse = r => String(r.getType()) === 'gtceu:greenhouse'
    const isTree = r => {
        const id = r.getId()
        const text = id ? String(id) : String(r.getFromToString())
        return trees.test(text)
    }

    // recipes loaded from data packs (none expected, kept for safety)
    event.remove({ type: 'gtceu:greenhouse', not: { id: trees } })

    // recipes added by scripts in this event
    const added = event.addedRecipes
    const toRemove = []
    let kept = 0
    let noId = 0
    added.forEach(r => {
        if (!isGreenhouse(r)) return
        if (!r.getId()) noId++
        if (isTree(r)) {
            kept++
            console.info(`greenhouse_trees_only.js: keeping ${r.getId() ? r.getId() : r.getFromToString()}`)
        } else {
            toRemove.push(r)
        }
    })
    toRemove.forEach(r => added.remove(r))
    console.info(`greenhouse_trees_only.js: ${kept} greenhouse tree recipes kept, ${toRemove.length} others removed (${noId} without an id yet)`)
})
