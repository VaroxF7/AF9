// AF9 - the Dimension Scanner (ATM9's world_data_scanner machines) and the dimension data it makes are gone.
// The base pack's startup scripts register the machines and the kubejs:*_data items, which this overlay cannot
// unregister; so nothing makes them, nothing takes them, and the recipe viewer hides them
// (client_scripts/dimension_scanner_hide.js). The scanner's own recipes go in miner.js.
// KEEP-IN-SYNC: the id lists are repeated in client_scripts/dimension_scanner_hide.js.

ServerEvents.recipes(event => {
    const tiers = ['lv', 'mv', 'hv', 'ev', 'iv', 'luv', 'zpm', 'uv', 'uhv']
    const scanners = tiers.map(t => `gtceu:${t}_world_data_scanner`)
    const data = ['kubejs:overworld_data', 'kubejs:nether_data', 'kubejs:end_data']
    scanners.concat(data).forEach(id => {
        event.remove({ output: id })
        event.remove({ input: id })
    })
})
