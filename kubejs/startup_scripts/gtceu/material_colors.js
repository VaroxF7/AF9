// AF9 - material colours changed in place.
// Soldering alloy: lead grey-violet like TerraFirmaGreg's, instead of GT's near-white (it is mostly lead and tin).
// Dust, ingot, nugget and the molten fluid all follow the material colour.

GTCEuStartupEvents.materialModification(event => {
    GTMaterials.SolderingAlloy.setMaterialARGB(0x8C648C)
})
