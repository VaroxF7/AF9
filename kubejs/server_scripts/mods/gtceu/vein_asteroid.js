// AF9 - The ore veins of the Asteroid Field and the end of GT's uranium veins (asteroid_fission.js has the rest of the
// chain; spec: docs/asteroid-fission.md).
//
// This file is named to load AFTER mining_dim_ores.js: that script of the pack runs modifyAll over every GT vein and
// moves it to the Mining dimension (a layer it does not know gets the height range 319-320), so veins added before it
// are rewritten into nothing. Server scripts load in alphabetical order.

// ---- 1. Veins ----
const $AsteroidChemicalHelper = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper')
const $AsteroidTagPrefix = Java.loadClass('com.gregtechceu.gtceu.api.data.tag.TagPrefix')
GTCEuServerEvents.oreVeins(event => {
    // The veins of the asteroid layer; each 3 x 3 chunks of the field hold one of them (weights). Ore only grows where
    // there is rock, and an asteroid is mostly empty space around it, so the clusters are big and exposed to the
    // void (no air discard): the ore shows on the surface of the rocks.
    const asteroidVein = (id, weight, size, density, material) => {
        event.add(id, builder => {
            builder.clusterSize(size)
                .weight(weight)
                .density(density)
                .discardChanceOnAirExposure(0.0)
                .layer('af9_asteroid')
                .dimensions('af9:asteroid_field')
                .heightRangeUniform(0, 280)
                // withBlock and withNetherBlock, not withMaterial: GT 7.2.0 sends the veins to every joining player with a
                // codec that reads the block, deep and nether block fields, and withMaterial leaves them empty ("Invalid
                // player data", no world loads). withBlock sets the first two.
                .standardVeinGenerator(generator => generator
                    .withBlock(() => $AsteroidChemicalHelper.getBlock($AsteroidTagPrefix.ore, GTMaterials.get(material)))
                    .withNetherBlock(() => $AsteroidChemicalHelper.getBlock($AsteroidTagPrefix.ore, GTMaterials.get(material))))
        })
    }
    asteroidVein('af9:asteroid_brannerite_vein', 60, 200, 0.55, 'brannerite')
    asteroidVein('af9:asteroid_pentlandite_vein', 20, 170, 0.5, 'pentlandite')
    asteroidVein('af9:asteroid_magnetite_vein', 15, 170, 0.5, 'magnetite')
    asteroidVein('af9:asteroid_cooperite_vein', 10, 150, 0.45, 'cooperite')

    // The old ways to uranium: the veins with pitchblende or uraninite are switched off (weight 0), wherever the pack put
    // them
    const uranium = ['pitchblende', 'uraninite']
    event.modifyAll((id, vein) => {
        if (String(id).startsWith('af9:')) return
        let isUranium = false
        try {
            vein.veinGenerator().getAllMaterials().forEach(material => {
                if (uranium.includes(String(material.getName()))) isUranium = true
            })
        } catch (error) {
            // a generator without materials has no uranium in it
        }
        if (isUranium) vein.weight(0)
    })
})
