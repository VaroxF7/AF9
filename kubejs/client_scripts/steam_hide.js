// AF9 - hide the removed steam-age machines from the recipe viewer.
// Their crafting recipes are removed in server_scripts/mods/gtceu/create_steam_age.js,
// so they cannot be made; hiding keeps them out of JEI/EMI lookups too.

JEIEvents.hideItems(event => {
    const steamMachines = [
        'gtceu:lp_steam_solid_boiler', 'gtceu:hp_steam_solid_boiler',
        'gtceu:lp_steam_liquid_boiler', 'gtceu:hp_steam_liquid_boiler',
        'gtceu:lp_steam_solar_boiler', 'gtceu:hp_steam_solar_boiler',
        'gtceu:lp_steam_extractor', 'gtceu:hp_steam_extractor',
        'gtceu:lp_steam_macerator', 'gtceu:hp_steam_macerator',
        'gtceu:lp_steam_compressor', 'gtceu:hp_steam_compressor',
        'gtceu:lp_steam_forge_hammer', 'gtceu:hp_steam_forge_hammer',
        'gtceu:lp_steam_furnace', 'gtceu:hp_steam_furnace',
        'gtceu:lp_steam_alloy_smelter', 'gtceu:hp_steam_alloy_smelter',
        'gtceu:lp_steam_rock_crusher', 'gtceu:hp_steam_rock_crusher',
        'gtceu:lp_steam_miner', 'gtceu:hp_steam_miner',
        'gtceu:primitive_pump',
        'gtceu:charcoal_pile_igniter'
    ]
    steamMachines.forEach(id => {
        event.hide(id)
    })
})
