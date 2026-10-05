// AF9 - The ore veins of the two asteroid belts (spec: docs/asteroid-fission.md).
//
// Brannerite, pentlandite, magnetite and cooperite generate as GT dike veins
// in both belts (af9:asteroid_field and af9:ceres), in the belts' own layer
// (af9_asteroid, registered by af9-core, space/AF9Space): vertical dikes
// wherever the rock is, piercing every rock of their column at every height.
// Dikes, not blobs: a standard blob at one random height mostly misses the
// floating rocks or cuts one in a thin slab. Same form as the quantanium vein
// (vein_quantanium.js).
//
// Named to load after the pack's mining_dim_ores.js, which moves every GT vein
// to the Mining Dimension: these veins keep the belts (like vein_asteroid.js
// and vein_quantanium.js). No biomes filter: the belts have no GT biomes.
//
// Numbers (tune in game with the prospector): clusterSize 16 fits the medium
// rocks and up; density 1.0, so rock the dike hits is solid ore;
// discardChanceOnAirExposure 0.0, or the void air eats it (every asteroid is
// exposed). Weights keep the old rock shares (brannerite ~9 %, pentlandite and
// magnetite ~4.5 % each, cooperite ~2.5 %) and set the Mk-IV elevator's shares
// (af9_asteroid is tier 4: only the Mk-IV drone draws these veins).

GTCEuServerEvents.oreVeins(event => {
    const dike = (material, weight) => new GTDikeBlockDefinition['(com.gregtechceu.gtceu.api.data.chemical.material.Material,int,int,int)'](
        GTMaterials.get(material), weight, 5, 270)
    const beltVein = (id, material, weight) => {
        event.add(id, builder => {
            builder.clusterSize(16)
                .weight(weight)
                .density(1.0)
                .discardChanceOnAirExposure(0.0)
                .layer('af9_asteroid')
                .dimensions('af9:asteroid_field', 'af9:ceres')
                .heightRangeUniform(5, 270)
                .dikeVeinGenerator(generator => generator
                    .withBlock(dike(material, 1)))
        })
    }
    beltVein('af9:brannerite_vein', 'brannerite', 80)
    beltVein('af9:pentlandite_vein', 'pentlandite', 40)
    beltVein('af9:magnetite_vein', 'magnetite', 40)
    beltVein('af9:cooperite_vein', 'cooperite', 22)
})
