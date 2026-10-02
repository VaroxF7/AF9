// AF9 - The end of GT's uranium veins (asteroid_fission.js has the rest of the chain; spec: docs/asteroid-fission.md).
//
// The Asteroid Field's own ores (brannerite, pentlandite, magnetite, cooperite) are not GT ore veins any more: af9-core's
// AsteroidOres grows them into the rock as the asteroids are made. A standard GT vein is a small blob at one random height,
// which in rocks hanging anywhere in 250 blocks of height lay in the void or cut the rock in a thin slab.
//
// This file is named to load AFTER mining_dim_ores.js: that script of the pack runs modifyAll over every GT vein and
// moves it to the Mining dimension, so a change made before it could be overwritten. Server scripts load in alphabetical order.

const $AsteroidArrayList = Java.loadClass('java.util.ArrayList')
GTCEuServerEvents.oreVeins(event => {
    // The old ways to uranium: the veins with pitchblende or uraninite are switched off (weight 0), wherever the pack put
    // them
    const uranium = ['pitchblende', 'uraninite']
    event.modifyAll((id, vein) => {
        if (String(id).startsWith('af9:')) return
        let isUranium = false
        try {
            // a copy in an ArrayList: Rhino cannot walk GT's immutable lists (java.util keeps their classes to itself)
            new $AsteroidArrayList(vein.veinGenerator().getAllMaterials()).forEach(material => {
                if (uranium.includes(String(material.getName()))) isUranium = true
            })
        } catch (error) {
            // a generator without materials has no uranium in it
        }
        if (isUranium) vein.weight(0)
    })
})
