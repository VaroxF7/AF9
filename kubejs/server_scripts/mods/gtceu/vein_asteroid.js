// AF9 - The end of GT's uranium and naquadah veins (asteroid_fission.js has the rest of the chain; spec: docs/asteroid-fission.md).
//
// The belts' own ores are GT dike veins now: brannerite, pentlandite, magnetite and cooperite in Ceres
// (vein_ceres.js), naquadah, platinum, quantanium and iridium in the upper field (vein_field.js). Vertical
// dikes piercing every rock of their column at every height: a standard GT blob at one random height
// would mostly miss the floating rocks or cut one in a thin slab, which is why the field used custom-grown ore noise before.
//
// This file is named to load AFTER mining_dim_ores.js: that script of the pack runs modifyAll over every GT vein and
// moves it to the Mining dimension, so a change made before it could be overwritten. Server scripts load in alphabetical order.

const $AsteroidArrayList = Java.loadClass('java.util.ArrayList')

// ---- Naquadah and uranium: no worldgen outside the belts ----
// The upper Asteroid Field is the only place first naquadah comes from (vein_field.js); Ceres the only place
// brannerite does (vein_ceres.js). Every other vein holding naquadah, pitchblende or uraninite is switched off
// (weight 0), wherever the pack put it — GT's naquadah vein held plutonium besides the naquadah, one more way to
// plutonium that the fission chain is meant to be the only one of. Naquadah stays renewable two ways: the void
// miner (End and Asteroids modes, miner.js) and the space elevator (the belt veins are tier 4: the Mk-IV drone).
GTCEuServerEvents.oreVeins(event => {
    // a vein of AF9's own is never a target: it holds the belts' ores on purpose
    const dead = ['pitchblende', 'uraninite', 'naquadah']
    event.modifyAll((id, vein) => {
        if (String(id).startsWith('af9:')) return
        let condemned = false
        try {
            // a copy in an ArrayList: Rhino cannot walk GT's immutable lists (java.util keeps their classes to itself)
            new $AsteroidArrayList(vein.veinGenerator().getAllMaterials()).forEach(material => {
                if (dead.includes(String(material.getName()))) condemned = true
            })
        } catch (error) {
            // a generator without materials holds neither
        }
        if (condemned) vein.weight(0)
    })
})
