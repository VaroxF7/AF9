// AF9 - Wire spools (the spool item is the TagPrefix `wireSpool` in AF9 Core's AF9TagPrefixes). One spool is 64 fine
// wires, eight ingots of the material.
//
// Winding is the Wiremill's Spooling mode (recipe type `spooling`, added to every Wiremill by AF9 Core's SpoolingMode;
// switch with GT's mode tab); there is no separate Spooler machine.
// The recipes are generated for every material that has fine wire, so a material added later gets its spool without
// touching this file.

const $GTCEuAPI = Java.loadClass('com.gregtechceu.gtceu.api.GTCEuAPI')
const $MaterialFlags = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags')

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // ---- Spools: 64x fine wire -> 1 spool ----
    $GTCEuAPI.materialManager.getRegisteredMaterials().forEach(material => {
        if (!material.hasFlag($MaterialFlags.GENERATE_FINE_WIRE)) return
        if (material.hasFlag($MaterialFlags.DISABLE_MATERIAL_RECIPES)) return

        const name = material.getName()
        event.recipes.gtceu.spooling(`af9:spool_${name}`)
            .itemInputs(`64x #forge:fine_wires/${name}`)
            .itemOutputs(`gtceu:${name}_wire_spool`)
            .duration(Math.max(20, material.getMass() * 4))
            .EUt(VA[GTValues.LV])
    })
})
