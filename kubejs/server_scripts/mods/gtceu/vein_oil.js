// AF9 - The world's oil is switched off and the Asteroid Field gets fluid deposits (GT's bedrock fluid veins, found by
// the prospector and drilled with the Fluid Drilling Rig). Spec: docs/oil.md
//
// Named to load after the pack's own scripts (alphabetical order; mining_dim_ores.js rewrites every ore vein).

const $OilGTRegistries = Java.loadClass('com.gregtechceu.gtceu.api.registry.GTRegistries')
const $OilForgeRegistries = Java.loadClass('net.minecraftforge.registries.ForgeRegistries')
const $OilResourceLocation = Java.loadClass('net.minecraft.resources.ResourceLocation')

// ---- The oil fluid veins off ----
GTCEuServerEvents.fluidVeins(event => {
    const oils = ['gtceu:oil', 'gtceu:oil_heavy', 'gtceu:oil_light', 'gtceu:oil_medium', 'gtceu:raw_oil']
    let off = 0
    $OilGTRegistries.BEDROCK_FLUID_DEFINITIONS.entries().forEach(entry => {
        const vein = entry.getValue()
        const fluid = vein.getStoredFluid().get()
        const id = String($OilForgeRegistries.FLUIDS.getKey(fluid))
        if (oils.includes(id)) {
            vein.setWeight(0)
            off++
        }
    })
    console.info(`vein_oil.js: ${off} oil fluid veins switched off`)

    // ---- The asteroids' deposits: void fluids, rich in nitrogen, oxygen, heavy water and acids ----
    const fluidOf = id => () => $OilForgeRegistries.FLUIDS.getValue(new $OilResourceLocation(id))
    // [id, fluid, weight, min yield, max yield]
    const deposits = [
        ['nitrogen', 'gtceu:nitrogen', 40, 250, 600],
        ['oxygen', 'gtceu:oxygen', 40, 250, 600],
        ['heavy_water', 'gtceu:heavy_water', 20, 100, 300],
        ['sulfuric_acid', 'gtceu:sulfuric_acid', 20, 150, 350],
        ['hydrochloric_acid', 'gtceu:hydrochloric_acid', 15, 150, 350],
        ['nitric_acid', 'gtceu:nitric_acid', 15, 150, 350],
        ['hydrofluoric_acid', 'gtceu:hydrofluoric_acid', 10, 100, 250],
        ['phosphoric_acid', 'gtceu:phosphoric_acid', 10, 100, 250],
        ['acetic_acid', 'gtceu:acetic_acid', 8, 100, 250]
    ]
    deposits.forEach(([name, fluid, weight, minYield, maxYield]) => {
        event.add(`af9:void_${name}`, builder => {
            builder.fluid(fluidOf(fluid))
                .weight(weight)
                .yield(minYield, maxYield)
                .depletionAmount(1)
                .depletionChance(100)
                .depletedYield(25)
                .dimensions('af9:asteroid_field')
        })
    })
})

// ---- The oil sands off (the ore of GT that holds oil) ----
GTCEuServerEvents.oreVeins(event => {
    event.modifyAll((id, vein) => {
        if (String(id).startsWith('af9:')) return
        let sandy = false
        try {
            vein.veinGenerator().getAllMaterials().forEach(material => {
                if (String(material.getName()) === 'oilsands') sandy = true
            })
        } catch (error) {
            // a generator without materials has no oil sands in it
        }
        if (sandy) vein.weight(0)
    })
})
