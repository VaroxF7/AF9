// AF9 - Diesels: shiny diesel (shiny oil + refinery gas), chloromethane diesel (cetane-boosted with
// chloromethane) and mana diesel (shiny diesel infused with Botania mana). GT has diesel, bio_diesel and
// cetane_boosted_diesel; these three are AF9's own ladder above them.
// Recipes: server_scripts/mods/gtceu/diesel.js. Spec: docs/green-chemistry.md §6
//
// Only formulas are given (no components), so GT adds no electrolyzer or centrifuge shortcut.

GTCEuStartupEvents.registry('gtceu:material', event => {
    // Shiny diesel: hydrotreated shiny oil, desulfurized with refinery gas (GTNH's HOG)
    event.create('shiny_diesel')
        .liquid()
        .color(0xe8b83a)
        .formula('C12H26(S)')

    // Chloromethane diesel: diesel with a chlorinated cetane booster, hotter ignition
    event.create('chloromethane_diesel')
        .liquid()
        .color(0x9ad87a)
        .formula('C12H26(CH3Cl)')

    // Mana diesel: shiny diesel holding a mana diamond's mana (the diamond survives, emptied)
    event.create('mana_diesel')
        .liquid()
        .color(0x5eead4)
        .formula('C12H26(Mana)')
})
