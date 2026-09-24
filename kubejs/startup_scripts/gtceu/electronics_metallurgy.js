// AF9 - Electronics metallurgy
// Circuits are built from their own tier's metals (the ones the Circuits quest page lists per tier). Where the real
// part is an alloy of that metal, AF9 adds it, melted in the EBF: Aluminium-Silicon bond wire and Kovar pins (MV),
// Platinum-Iridium fine wire (EV). HV, IV and LuV use GT's own metals (gold, stainless steel, tungstensteel, tungsten,
// rhodium-plated palladium, osmiridium, niobium-titanium). Every part is makeable with the previous tier's machines.
// Plus the Zircon heavy-mineral-sand ore, refined like the real thing: plasma dissociation -> carbochlorination ->
// extractive distillation (ZrCl4 / HfCl4) -> Kroll process. HfCl4 is the high-k precursor of LUV lithography.
// GT makes the parts (bolts, fine wires) and the EBF / vacuum freezer recipes from these properties.
// Recipes: server_scripts/mods/gtceu/electronics_metallurgy.js. Spec: docs/semiconductor-factory.md

const $AF9DustProperty = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.DustProperty')
const $AF9IngotProperty = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.IngotProperty')
const $AF9BlastProperty = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.BlastProperty')
const $AF9GasTier = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.BlastProperty$GasTier')

GTCEuStartupEvents.registry('gtceu:material', allthemods => {
    // GT defines zirconium as a bare element with no items. Give it dust and ingots; the Kroll process makes the dust
    // (sponge), GT's EBF recipe melts it (2128 K = hot ingot, cooled in the vacuum freezer). It is the zircon chain's
    // main metal next to the hafnium tetrachloride; no circuit uses it (not a tier metal).
    const zirconium = GTMaterials.Zirconium
    if (!zirconium.hasProperty(PropertyKey.DUST)) {
        zirconium.setProperty(PropertyKey.DUST, new $AF9DustProperty())
        zirconium.setProperty(PropertyKey.INGOT, new $AF9IngotProperty())
        zirconium.setProperty(PropertyKey.BLAST, new $AF9BlastProperty(2128, $AF9GasTier.MID,
            GTValues.VA[GTValues.HV], 800, GTValues.VA[GTValues.HV], 200))
    }

    // ---- Circuit alloys (mixed in server_scripts, melted in the EBF) ----
    // MV: aluminium wedge-bonding wire (the silicon keeps it from work-softening). Aluminium is the MV metal.
    // EBF at MV voltage, which two LV hatches can supply.
    allthemods.create('aluminium_silicon')
        .ingot()
        .color(0xc8ccd2).iconSet(GTMaterialIconSet.METALLIC)
        .components('16x aluminium', '1x silicon')
        .flags(GTMaterialFlags.GENERATE_FINE_WIRE)
        .blastTemp(1700, 'low', GTValues.VA[GTValues.MV], 400)

    // MV: expands like glass, so it seals into IC packages; used for the pins
    allthemods.create('kovar')
        .ingot()
        .color(0x8e9ba6).iconSet(GTMaterialIconSet.METALLIC)
        .components('6x iron', '3x nickel', '2x cobalt')
        .flags(GTMaterialFlags.GENERATE_BOLT_SCREW)
        .blastTemp(1720, 'low', GTValues.VA[GTValues.MV], 600)

    // EV: hard, inert platinum wire (probe tips, electrodes). Platinum is the EV metal; iridium comes from the EV-era
    // platinum group chain, so the EV bootstrap circuit uses plain platinum wire instead.
    allthemods.create('platinum_iridium')
        .ingot()
        .color(0xe6e6dc).iconSet(GTMaterialIconSet.SHINY)
        .components('9x platinum', '1x iridium')
        .flags(GTMaterialFlags.GENERATE_FINE_WIRE)
        .blastTemp(2100, 'mid', GTValues.VA[GTValues.EV], 600)

    // ---- Zircon ----
    // Heavy mineral sand with ilmenite, rutile and monazite. Formulas only (no components), so GT adds no
    // electrolyzer shortcut past the refining chain.
    allthemods.create('zircon')
        .dust().ore()
        .color(0xb77f4f).iconSet('rough')
        .formula('ZrSiO4')
        .addOreByproducts(GTMaterials.Ilmenite, GTMaterials.Rutile, GTMaterials.Monazite)

    allthemods.create('zirconia')
        .dust()
        .color(0xebe4d4)
        .formula('ZrO2')

    // Zirconium tetrachloride still carrying the hafnium (the two are chemically almost identical)
    allthemods.create('crude_zirconium_tetrachloride')
        .gas()
        .color(0xd9d4c4)
        .formula('(Zr,Hf)Cl4')

    allthemods.create('zirconium_tetrachloride')
        .gas()
        .color(0xe6e2d6)
        .formula('ZrCl4')

    allthemods.create('hafnium_tetrachloride')
        .gas()
        .color(0xc8d0c8)
        .formula('HfCl4')
})
