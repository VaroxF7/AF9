// AF9 - The Quantanium vein of the two asteroid belts (the ore: af9-core, registry/AF9Materials).
// Spec: docs/quantanium.md
//
// A GT dike vein in the belts' own layer (af9_asteroid, registered by
// af9-core, space/AF9Space): a vertical dike wherever the rock
// is, piercing every rock of its column. Dikes, not blobs: a standard blob at
// one random height mostly misses the floating rocks (vein_asteroid.js).
// Named to load after the pack's mining_dim_ores.js, which moves every GT vein
// to the Mining Dimension: this vein keeps the belts (like vein_asteroid.js).
// No biomes filter: the belts have no GT biomes to name.
//
// Numbers (tune in game with the prospector): clusterSize 16 fits the medium
// rocks and up; density 1.0, so rock the dike hits is solid ore;
// discardChanceOnAirExposure 0.0, or the void air eats it (every asteroid is
// exposed); weight 60 sets both the worldgen share and the Mk-IV elevator's
// share (the only drone that reaches tier 4).

GTCEuServerEvents.oreVeins(event => {
    const dike = (material, weight) => new GTDikeBlockDefinition['(com.gregtechceu.gtceu.api.data.chemical.material.Material,int,int,int)'](
        GTMaterials.get(material), weight, 5, 270)
    event.add('af9:quantanium_vein', builder => {
        builder.clusterSize(16)
            .weight(60)
            .density(1.0)
            .discardChanceOnAirExposure(0.0)
            .layer('af9_asteroid')
            .dimensions('af9:asteroid_field', 'af9:ceres')
            .heightRangeUniform(5, 270)
            .dikeVeinGenerator(generator => generator
                .withBlock(dike('quantanium', 1)))
    })
})
