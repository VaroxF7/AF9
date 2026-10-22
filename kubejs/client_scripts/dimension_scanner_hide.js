// AF9 - hide the removed Dimension Scanner and its dimension data from the recipe viewer.
// Their recipes are removed in server_scripts/mods/gtceu/dimension_scanner_removed.js.

JEIEvents.hideItems(event => {
    // KEEP-IN-SYNC: duplicates the lists in dimension_scanner_removed.js
    const tiers = ['lv', 'mv', 'hv', 'ev', 'iv', 'luv', 'zpm', 'uv', 'uhv']
    tiers.forEach(t => event.hide(`gtceu:${t}_world_data_scanner`))
    ;['kubejs:overworld_data', 'kubejs:nether_data', 'kubejs:end_data'].forEach(id => event.hide(id))
})
