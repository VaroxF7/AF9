// AF9 - titanium from Mars sand only, tungsten from Venus sand only: the GT veins holding their ores are switched
// off (weight 0), wherever the pack put them. Spec: docs/asteroid-fission.md
//
// Killed: ilmenite and rutile (titanium: bauxite_vein_end, anywhere else they hide), scheelite and tungstate
// (tungsten: scheelite_vein and anywhere else). Kept on purpose: chromite (stainless steel for tier 1), quartzite
// (LV sensors), bauxite (aluminium), magnesite/olivine (magnesium for Kroll), salt (chlorine, sodium).
// A vein of AF9's own is never a target (same guard as vein_asteroid.js).
//
// Named to load AFTER the pack's mining_dim_ores.js (it moves every GT vein to the Mining Dimension first): server
// scripts load in alphabetical order.

const $PlanetArrayList = Java.loadClass('java.util.ArrayList')

GTCEuServerEvents.oreVeins(event => {
    const dead = ['ilmenite', 'rutile', 'scheelite', 'tungstate']
    event.modifyAll((id, vein) => {
        if (String(id).startsWith('af9:')) return
        let condemned = false
        // belt and braces: the vein's id says what it is, or its materials do
        for (const word of dead) {
            if (String(id).toLowerCase().includes(word)) condemned = true
        }
        try {
            // a copy in an ArrayList: Rhino cannot walk GT's immutable lists (java.util keeps their classes to itself)
            new $PlanetArrayList(vein.veinGenerator().getAllMaterials()).forEach(material => {
                if (dead.includes(String(material.getName()))) condemned = true
            })
        } catch (error) {
            // a generator without materials holds neither
        }
        if (condemned) vein.weight(0)
    })
})
