// AF9 - Quantanium: the ore that unlocks UHV. Spec: docs/quantanium.md
//
// A metal of the Asteroid Field and nothing else: it has no GT vein anywhere
// else, no other source, and no recipe takes it yet (the UHV hulls and circuits
// that will need it come separately). Formulas only (no components), so GT adds
// no electrolyzer or centrifuge shortcut past the ore chain; furnace-smeltable,
// so the gate is finding the ore, not a coil tier.

GTCEuStartupEvents.registry('gtceu:material', event => {
    // Crushing gives two crushed ores per raw ore. Byproducts: the high-tech
    // metals the ore grew with (all obtainable elsewhere already). The formula
    // uses real element symbols in brannerite's substitution notation.
    event.create('quantanium')
        .ingot().dust().ore(2, 1)
        .color(0x9d4edd).secondaryColor(0x3c096c)
        .iconSet(GTMaterialIconSet.SHINY)
        .formula('(Ti,Nb)2O3')
        .addOreByproducts(GTMaterials.Titanium, GTMaterials.Trinium, GTMaterials.Niobium)
})
