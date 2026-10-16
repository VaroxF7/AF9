// AF9 - hide GTCEu's Large Cutter from the recipe viewer: the Industrial Cutting Machine replaces it.
// Its crafting recipe is removed in server_scripts/mods/gtceu/industrial_cutter.js.

JEIEvents.hideItems(event => {
    event.hide('gtceu:large_cutter')
})
