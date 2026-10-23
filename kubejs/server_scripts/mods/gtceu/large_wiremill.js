// AF9 - Large Wiremill: crafting. The machine replaces GCYM's Large Wire Factory (same id, gtceu:large_wiremill), whose
// crafting recipe went with it (gtceu-fork patch 0022); this is that recipe again. Machine: startup_scripts/gtceu/
// large_wiremill.js. Quest: LuV chapter (ludicrous_voltage).

ServerEvents.recipes(event => {
    event.shaped('gtceu:large_wiremill', ['PZP', 'SWS', 'MKM'], {
        P: 'gtceu:hsla_steel_plate', Z: '#gtceu:circuits/iv', S: 'gtceu:hsla_steel_spring',
        W: 'gtceu:iv_wiremill', M: 'gtceu:iv_electric_motor', K: 'gtceu:platinum_single_cable'
    }).id('af9:large_wiremill')
})
