// priority: -100
// AF9 - The Greenhouse only grows trees. The base pack (ATM9, kubejs/server_scripts/mods/gtceu/greenhouse_recipes.js, which
// this repository does not hold) gives the Greenhouse a recipe for every Botany Pots crop, every Mystical Agriculture seed and
// the GT rubber sapling, each in four or more variants. Crops and seeds are the Agriculture Simulation Bed's now (agri_sim.js),
// so every greenhouse recipe whose id does not name a sapling or propagule is removed here: what stays is the rubber sapling
// ('kubejs:rubber_sapling...') and the Botany Pots trees ('kubejs:gtceu/greenhouse/<mod>/<tree>_sapling', named after the
// planted sapling). The priority makes this file run after the base pack's, so its recipes exist when the filter is applied;
// the filter works on ids and type only, not on the recipe's json (not reliable for recipes added in the same event).

ServerEvents.recipes(event => {
    const trees = /sapling|propagule/
    let kept = 0
    let dropped = 0
    event.forEachRecipe({ type: 'gtceu:greenhouse' }, r => {
        if (trees.test(String(r.getId()))) kept++
        else dropped++
    })
    event.remove({ type: 'gtceu:greenhouse', not: { id: trees } })
    console.info(`greenhouse_trees_only.js: ${kept} greenhouse tree recipes kept, ${dropped} others removed`)
})
