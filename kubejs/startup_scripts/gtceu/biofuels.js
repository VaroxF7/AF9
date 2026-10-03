// AF9 - Biofuels: biodiesel and bioethanol, the farm-grown fuels behind rockets and turbines.
// GT has ethanol, methanol, seed oil, glycerol, biomass and fermented biomass, but no biodiesel and no
// bioethanol: both are registered here. Recipes: server_scripts/mods/gtceu/biofuels.js.
// Spec: docs/green-chemistry.md
//
// Only formulas are given (no components), so GT adds no electrolyzer or centrifuge shortcut.

GTCEuStartupEvents.registry('gtceu:material', event => {
    // Biodiesel: fatty-acid methyl esters (methyl oleate shown), from seed oil + methanol
    event.create('biodiesel')
        .liquid()
        .color(0xd8a028)
        .formula('C19H36O2')

    // Bioethanol: the distillation azeotrope (~95 %), dried to fuel ethanol over AF9's molecular sieves
    event.create('bioethanol')
        .liquid()
        .color(0xe4efe4)
        .formula('C2H5OH(H2O)')
})
