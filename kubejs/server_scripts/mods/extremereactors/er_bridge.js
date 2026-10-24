// AF9 - Extreme Reactors bridge: AF9's fuels, coolants and moderator on top of the mod's own progression.
// Spec: docs/extreme-reactors.md. The reactants, coolants, vapors, reactions and moderator are in
// config/extremereactors/modpack_api.json (made by tools/er/gen_modpack_api.py); Extreme Reactors reads what counts as
// what by TAG, so this file only fills the tags it names:
//   af9:er/fuel/<id>     the rod that is one unit (1,000 mB) of that fuel in a reactor's access port
//   af9:er/waste/<id>    the spent rod the port ejects (the first item of the tag; one item each, on purpose)
// Nothing of Extreme Reactors' own fuels (yellorium, blutonium, verderium) is touched; its uranium and plutonium ingot
// mappings are the mod's own, so GT's uranium and plutonium ingots burn there as well.

const ER_FUELS = [
    ['leu', 'af9:fx_fuel_rod', 'af9:fx_spent_fuel_rod'],
    ['thorium', 'af9:fx_thorium_rod', 'af9:fx_spent_thorium_rod'],
    ['mox', 'af9:fx_mox_rod', 'af9:fx_spent_mox_rod'],
    ['mox_2', 'af9:fx_mox_2_rod', 'af9:fx_spent_mox_2_rod'],
    ['heu', 'af9:fx_heu_rod', 'af9:fx_spent_heu_rod'],
    ['lep', 'af9:fx_lep_rod', 'af9:fx_spent_lep_rod'],
    ['mox_3', 'af9:fx_mox_3_rod', 'af9:fx_spent_mox_3_rod'],
    ['hep', 'af9:fx_hep_rod', 'af9:fx_spent_hep_rod'],
    ['mox_4', 'af9:fx_mox_4_rod', 'af9:fx_spent_mox_4_rod'],
    ['np', 'af9:fx_np_rod', 'af9:fx_spent_np_rod'],
    ['am', 'af9:fx_am_rod', 'af9:fx_spent_am_rod'],
    ['cm', 'af9:fx_cm_rod', 'af9:fx_spent_cm_rod'],
    ['cf', 'af9:fx_cf_rod', 'af9:fx_spent_cf_rod'],
]

ServerEvents.tags('item', event => {
    ER_FUELS.forEach(fuel => {
        event.add(`af9:er/fuel/${fuel[0]}`, fuel[1])
        event.add(`af9:er/waste/${fuel[0]}`, fuel[2])
    })
})

ServerEvents.tags('fluid', event => {
    // coolants and the vapor of the molten-salt loop (the Heat Exchanger takes the hot salt out of the reactor)
    event.add('af9:er/coolant/distilled_water', 'gtceu:distilled_water')
    event.add('af9:er/coolant/flibe', 'gtceu:flibe')
    event.add('af9:er/vapor/hot_flibe', 'gtceu:hot_flibe')
})

ServerEvents.tags('block', event => {
    event.add('af9:er/moderator/nuclear_graphite', 'af9:nuclear_graphite_block')
})

ServerEvents.recipes(event => {
    // four graphite moderators pressed into one block, and back
    event.shaped('af9:nuclear_graphite_block', ['MM', 'MM'], { M: 'af9:graphite_moderator' })
        .id('af9:nuclear_graphite_block')
    event.shapeless('4x af9:graphite_moderator', 'af9:nuclear_graphite_block')
        .id('af9:nuclear_graphite_block_unpack')
})
