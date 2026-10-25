// AF9 - material colours changed in place.
// Soldering alloy: lead grey-violet like TerraFirmaGreg's, instead of GT's near-white (it is mostly lead and tin).
// Dust, ingot, nugget and the molten fluid all follow the material colour.

GTCEuStartupEvents.materialModification(event => {
    GTMaterials.SolderingAlloy.setMaterialARGB(0x8C648C)
})

// AF9 - parts of the Dimensionally Transcendent casings' Assembly Line recipes (server_scripts/mods/gtceu/dtpf.js):
// GT gives these materials only some of the forms, so the missing ones are added here (before items register).
GTCEuStartupEvents.materialModification(event => {
    const $MaterialFlags = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags')
    const add = (material, flags) => flags.forEach(f => {
        if (!material.hasFlag(f)) material.addFlags(f)
    })
    add(GTMaterials.get('zeron_100'), [$MaterialFlags.GENERATE_PLATE, $MaterialFlags.GENERATE_ROD,
        $MaterialFlags.GENERATE_BOLT_SCREW, $MaterialFlags.GENERATE_RING])
    add(GTMaterials.get('ruthenium_trinium_americium_neutronate'), [$MaterialFlags.GENERATE_PLATE,
        $MaterialFlags.GENERATE_DENSE])
    add(GTMaterials.get('yttrium_barium_cuprate'), [$MaterialFlags.GENERATE_FINE_WIRE])
})
