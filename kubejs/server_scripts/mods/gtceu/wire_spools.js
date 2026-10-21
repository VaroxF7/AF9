// AF9 - Wire spools and the Spooler (machines: startup_scripts/gtceu/spooler.js; the spool item is the TagPrefix
// `wireSpool` in AF9 Core's AF9TagPrefixes). One spool is 64 fine wires, eight ingots of the material.
//
// The winding recipes are generated for every material that has fine wire, so a material added later gets its spool
// without touching this file. The machine recipes are the usual hull + circuits + motors pattern; the Large Spooler
// also takes four tungstensteel spools, so it needs an IV Spooler before it can be built.

const $GTCEuAPI = Java.loadClass('com.gregtechceu.gtceu.api.GTCEuAPI')
const $MaterialFlags = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags')

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // ---- Spools: 64x fine wire -> 1 spool ----
    $GTCEuAPI.materialManager.getRegisteredMaterials().forEach(material => {
        if (!material.hasFlag($MaterialFlags.GENERATE_FINE_WIRE)) return
        if (material.hasFlag($MaterialFlags.NO_UNIFICATION)) return
        if (material.hasFlag($MaterialFlags.DISABLE_MATERIAL_RECIPES)) return

        const name = material.getName()
        event.recipes.gtceu.spooling(`af9:spool_${name}`)
            .itemInputs(`64x #forge:fine_wires/${name}`)
            .itemOutputs(`gtceu:${name}_wire_spool`)
            .duration(Math.max(20, material.getMass() * 4))
            .EUt(VA[GTValues.LV])
    })

    // ---- Spooler single blocks (LV-UHV) ----
    // tier, the metal whose plates make the frame of that tier's machine, GT's tier index, and the tier whose
    // motors/conveyors/robot arms go in. UHV parts are dev-only in GT, so the UHV Spooler uses UV parts with a UHV
    // hull and UHV circuits, the way AF9's other UHV machines do.
    const tiers = [
        ['lv', 'steel', GTValues.LV, 'lv'],
        ['mv', 'aluminium', GTValues.MV, 'mv'],
        ['hv', 'stainless_steel', GTValues.HV, 'hv'],
        ['ev', 'titanium', GTValues.EV, 'ev'],
        ['iv', 'tungsten_steel', GTValues.IV, 'iv'],
        ['luv', 'rhodium_plated_palladium', GTValues.LuV, 'luv'],
        ['zpm', 'naquadah_alloy', GTValues.ZPM, 'zpm'],
        ['uv', 'tritanium', GTValues.UV, 'uv'],
        ['uhv', 'neutronium', GTValues.UHV, 'uv']
    ]
    tiers.forEach(([tier, metal, voltage, parts]) => {
        event.recipes.gtceu.assembler(`af9:${tier}_spooler`)
            .itemInputs(
                `gtceu:${tier}_machine_hull`,
                `2x #gtceu:circuits/${tier}`,
                `gtceu:${parts}_electric_motor`,
                `gtceu:${parts}_conveyor_module`,
                `gtceu:${parts}_robot_arm`,
                `4x gtceu:${metal}_plate`)
            .itemOutputs(`gtceu:${tier}_spooler`)
            .duration(200)
            .EUt(VA[voltage])
    })

    // ---- Large Spooler (IV) ----
    // Four tungstensteel spools: the controller is built around the first full reels a player can wind at IV.
    event.recipes.gtceu.assembler('af9:large_spooler')
        .itemInputs(
            '4x gtceu:robust_machine_casing',
            '4x gtceu:tungsten_steel_frame',
            '4x #gtceu:circuits/iv',
            '2x gtceu:iv_electric_motor',
            '2x gtceu:iv_conveyor_module',
            '2x gtceu:iv_robot_arm',
            '4x gtceu:tungsten_steel_gear',
            '4x gtceu:tungsten_steel_wire_spool')
        .itemOutputs('gtceu:large_spooler')
        .duration(600)
        .EUt(VA[GTValues.IV])
})
