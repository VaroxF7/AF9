// AF9 - The Greenhouse only grows trees. The base pack (ATM9, kubejs/server_scripts/mods/gtceu/greenhouse_recipes.js, which
// this repository does not hold) gives the Greenhouse a recipe for every Botany Pots crop, every Mystical Agriculture seed and
// the GT rubber sapling, each in four or more variants. Crops and seeds are the Agriculture Simulation Bed's now (agri_sim.js),
// so every greenhouse recipe whose planted item (the not consumed input) is not a sapling or propagule is removed here: what
// stays is the rubber sapling and the Botany Pots trees. This file loads after the base pack's, and the removal needs no ids.

ServerEvents.recipes(event => {
    const tree = /sapling|propagule/
    let removed = 0
    let kept = 0
    event.forEachRecipe({ type: 'gtceu:greenhouse' }, r => {
        const json = JSON.parse(String(r.json))
        const items = (json.inputs && json.inputs.item) || []
        // the planted item is a not consumed input (chance 0); any sapling among the item inputs counts
        const planted = items.filter(c => c.chance === 0 || c.chance === undefined).map(c => JSON.stringify(c))
        if (planted.some(s => tree.test(s))) {
            kept++
            return
        }
        event.remove({ id: String(r.getId()) })
        removed++
    })
    console.info(`greenhouse_trees_only.js: kept ${kept} tree recipes, removed ${removed}`)
})
