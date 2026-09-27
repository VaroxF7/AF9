// AF9 - Where AF9's own chips (startup_scripts/gtceu/chips.js) go into other mods' recipes: chips added to GT's and
// AE2's recipes, and alternative recipes that take a chip. The uses inside AF9's own recipes sit with those recipes
// (wireless energy hatches, MV circuits, tiered circuits, fab machines, void miner, Orbital Lithography Station).
// Spec: docs/semiconductor-factory.md §8.

ServerEvents.recipes(allthemods => {
    // ---- RF Transceiver: the radio of every wireless link ----
    // AE2's Wireless Receiver, the part of the Wireless Access Point and of every wireless terminal
    allthemods.remove({ id: 'ae2:network/wireless_part' })
    allthemods.shaped('ae2:wireless_receiver', ['F', 'IQI', 'IRI'], {
        F: 'ae2:fluix_pearl', I: '#forge:ingots/iron', Q: 'ae2:quartz_fiber', R: 'kubejs:rf_transceiver_chip'
    }).id('af9:ae2/wireless_receiver')
})
