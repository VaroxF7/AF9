// AF9 - Biofuels: bioethanol, the farm-grown spirit behind fuel ethanol and oil-free ethylene.
// GT already has biodiesel (gtceu:bio_diesel: seed oil + methanol, and its own fuel value), ethanol,
// methanol, seed oil, glycerol, biomass and fermented biomass, so only the missing spirit is registered
// here. Recipes: server_scripts/mods/gtceu/biofuels.js. Spec: docs/green-chemistry.md
//
// Only the formula is given (no components), so GT adds no electrolyzer or centrifuge shortcut.

GTCEuStartupEvents.registry('gtceu:material', event => {
    // Bioethanol: the distillation azeotrope (~95 %), dried to fuel ethanol over AF9's molecular sieves
    event.create('bioethanol')
        .liquid()
        .color(0xe4efe4)
        .formula('C2H5OH(H2O)')
})
