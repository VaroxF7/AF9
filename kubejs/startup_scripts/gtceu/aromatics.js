// AF9 - Aromatics support: dichloromethane, the middle step of the GTNH-style methane chlorination
// chain (methane -> chloromethane -> dichloromethane -> chloroform). GT has chloromethane and chloroform
// but nothing between them, and the chain ends at chloroform on purpose (no carbon tetrachloride).
// Recipes: server_scripts/mods/gtceu/aromatics.js. Spec: docs/green-chemistry.md
//
// Only the formula is given (no components), so GT adds no electrolyzer or centrifuge shortcut.

GTCEuStartupEvents.registry('gtceu:material', event => {
    // Dichloromethane: colourless, sweet-smelling, bp 40 C; the paint-stripper step of the chain
    event.create('dichloromethane')
        .liquid()
        .color(0xd8e4e8)
        .formula('CH2Cl2')
})
