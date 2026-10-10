// AF9 - The ore veins of the upper Asteroid Field (spec: docs/asteroid-fission.md,
// docs/quantanium.md).
//
// Naquadah, platinum, quantanium and iridium generate as GT dike veins in
// af9:asteroid_field only, in the belts' own layer (af9_asteroid, registered by
// af9-core, space/AF9Space): vertical dikes wherever the rock is, piercing
// every rock of their column at every height. Dikes, not blobs: a standard
// blob at one random height mostly misses the floating rocks or cuts one in
// a thin slab. Same form as the Ceres belt's fission veins (vein_ceres.js).
//
// The field is the exotic belt: naquadah for the post-EV superconductor lines,
// platinum for the EV circuits, quantanium (the UHV gate, AF9's own material)
// and a small iridium treasure. All four dikes are a single pure ore, so the
// naquadah here leaks no plutonium: the reactor chain stays the only way to
// it (asteroid_fission.js makes GT's own naquadah vein raw naquadah only,
// vein_asteroid.js).
//
// Named to load after the pack's mining_dim_ores.js, which moves every GT vein
// to the Mining Dimension: these veins keep the field. No biomes filter: the
// belts have no GT biomes to name.
//
// Numbers (tune in game with the prospector): clusterSize 16 fits the medium
// rocks and up; density 1.0, so rock the dike hits is solid ore;
// discardChanceOnAirExposure 0.0, or the void air eats it (every asteroid is
// exposed). Weight sets both the worldgen share and the Mk-IV elevator's
// share (af9_asteroid is tier 4: only the Mk-IV drone draws these veins).

GTCEuServerEvents.oreVeins(event => {
    const dike = (material, weight) => new GTDikeBlockDefinition['(com.gregtechceu.gtceu.api.data.chemical.material.Material,int,int,int)'](
        GTMaterials.get(material), weight, 5, 270)
    const fieldVein = (id, material, weight) => {
        event.add(id, builder => {
            builder.clusterSize(16)
                .weight(weight)
                .density(1.0)
                .discardChanceOnAirExposure(0.0)
                .layer('af9_asteroid')
                .dimensions('af9:asteroid_field')
                .heightRangeUniform(5, 270)
                .dikeVeinGenerator(generator => generator
                    .withBlock(dike(material, 1)))
        })
    }
    fieldVein('af9:field_naquadah_vein', 'naquadah', 65)
    fieldVein('af9:field_platinum_vein', 'platinum', 45)
    fieldVein('af9:quantanium_vein', 'quantanium', 30)
    fieldVein('af9:field_iridium_vein', 'iridium', 22)
})
