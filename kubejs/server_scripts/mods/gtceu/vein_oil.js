// AF9 - The world's oil is switched off and the Asteroid Field gets fluid deposits (GT's bedrock fluid veins, found by
// the prospector and drilled with the Fluid Drilling Rig). Spec: docs/oil.md
//
// Named to load after the pack's own scripts (alphabetical order; mining_dim_ores.js rewrites every ore vein).

const $OilGTRegistries = Java.loadClass('com.gregtechceu.gtceu.api.registry.GTRegistries')
const $OilForgeRegistries = Java.loadClass('net.minecraftforge.registries.ForgeRegistries')
const $OilResourceLocation = Java.loadClass('net.minecraft.resources.ResourceLocation')
const $OilResourceKey = Java.loadClass('net.minecraft.resources.ResourceKey')
const $OilRegistries = Java.loadClass('net.minecraft.core.registries.Registries')
const $OilVeinData = Java.loadClass('com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid.BedrockFluidVeinSavedData')
const $OilArrayList = Java.loadClass('java.util.ArrayList')

// Rhino cannot call methods on the unmodifiable views and immutable lists that GT hands out (java.util keeps those
// classes to itself: "cannot access a member of class java.util.Collections$Unmodifiable... with modifiers public"). A
// copy in an ArrayList (made in Java, from the view as an argument) can be walked.
const oilListOf = collection => new $OilArrayList(collection)

// ---- The oil fluid veins off ----
GTCEuServerEvents.fluidVeins(event => {
    // GT's oil deposits (GTBedrockFluids), by id, removed: they are gone from the world and from the recipe viewers' diagrams
    // (the natural gas deposit stays)
    const oils = ['gtceu:heavy_oil_deposit', 'gtceu:light_oil_deposit', 'gtceu:oil_deposit', 'gtceu:raw_oil_deposit']
    let off = 0
    // one vein that cannot be removed must not stop the deposits below from being registered
    oils.forEach(id => {
        try {
            event.remove(id)
            off++
        } catch (error) {
            console.error(`vein_oil.js: could not remove the fluid vein ${id}: ${error}`)
        }
    })
    console.info(`vein_oil.js: ${off} oil fluid veins removed`)

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
    let registered = 0
    deposits.forEach(([name, fluid, weight, minYield, maxYield]) => {
        // a fluid that does not exist would be a deposit of nothing that the prospector draws as nothing
        const found = $OilForgeRegistries.FLUIDS.getValue(new $OilResourceLocation(fluid))
        if (found === null || String($OilForgeRegistries.FLUIDS.getKey(found)) !== fluid) {
            console.error(`vein_oil.js: the deposit af9:void_${name} holds ${fluid}, which is not a fluid`)
        }
        registered++
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
    console.info(`vein_oil.js: ${registered} fluid deposits registered for af9:asteroid_field`)
})

// ---- Chunks that were looked at before the deposits existed ----
// GT decides a chunk's vein once and saves it with the world. A chunk of the Asteroid Field that was prospected (or
// drilled) while the dimension had no deposits is saved as "no fluid" for good, and so shows nothing even now. Forget those
// entries when the world loads: the next look at the chunk rolls a deposit.
ServerEvents.loaded(event => {
    // var, not const: Rhino keeps a const of a nested block once for the whole script (see rockets.js)
    try {
        var dimension = $OilResourceKey.create($OilRegistries.DIMENSION, new $OilResourceLocation('af9:asteroid_field'))
        var level = event.server.getLevel(dimension)
        if (level === null) return
        var data = $OilVeinData.getOrCreate(level)
        var forgotten = 0
        oilListOf(data.veinFluids.keySet()).forEach(chunk => {
            if (data.veinFluids.get(chunk).getDefinition() === null) {
                data.veinFluids.remove(chunk)
                forgotten++
            }
        })
        if (forgotten > 0) data.setDirty()
        console.info(`vein_oil.js: ${forgotten} empty fluid veins of af9:asteroid_field forgotten`)
    } catch (error) {
        console.error(`vein_oil.js: could not look at the fluid veins of af9:asteroid_field: ${error}`)
    }
})

// ---- The oil sands off (the ore of GT that holds oil) ----
GTCEuServerEvents.oreVeins(event => {
    event.modifyAll((id, vein) => {
        if (String(id).startsWith('af9:')) return
        let sandy = false
        try {
            oilListOf(vein.veinGenerator().getAllMaterials()).forEach(material => {
                if (String(material.getName()) === 'oilsands') sandy = true
            })
        } catch (error) {
            // a generator without materials has no oil sands in it
        }
        if (sandy) vein.weight(0)
    })
})
