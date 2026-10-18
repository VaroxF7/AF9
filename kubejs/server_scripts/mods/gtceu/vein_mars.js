// AF9 - The Ilmenite and Zircon veins of Mars (the layer: af9-core, space/AF9Space, af9_mars = Ad Astra's mars stone
// and mars sand). Spec: docs/asteroid-fission.md (titanium), docs/semiconductor-factory.md (zircon).
//
// Both used to be elsewhere: the ilmenite veins of the pack's Earth-side dimensions are switched off (vein_planets.js,
// weight 0) and the zircon sands vein sat in the Mining Dimension (stone layer, y 129-248). They generate only on Mars
// now (ad_astra:mars), which the tier 2 rocket reaches. Rutile comes from ilmenite and nowhere else.
// Dikes through the Martian terrain, same form as the old zircon vein. af9_mars is elevator tier 2.
// Named to load after the pack's mining_dim_ores.js, which moves every GT vein to the Mining Dimension.

GTCEuServerEvents.oreVeins(event => {
    const dike = (material, weight) => new GTDikeBlockDefinition['(com.gregtechceu.gtceu.api.data.chemical.material.Material,int,int,int)'](
        GTMaterials.get(material), weight, 40, 120)
    event.add('af9:mars_ilmenite_vein', builder => {
        builder.clusterSize(30)
            .weight(40)
            .density(0.7)
            .discardChanceOnAirExposure(0.0)
            .layer('af9_mars')
            .dimensions('ad_astra:mars')
            .heightRangeUniform(40, 120)
            .dikeVeinGenerator(generator => generator
                .withBlock(dike('ilmenite', 5))
                .withBlock(dike('iron', 2)))
    })
    event.add('af9:zircon_sands_vein', builder => {
        builder.clusterSize(35)
            .weight(30)
            .density(0.6)
            .discardChanceOnAirExposure(0.0)
            .layer('af9_mars')
            .dimensions('ad_astra:mars')
            .heightRangeUniform(40, 120)
            .dikeVeinGenerator(generator => generator
                .withBlock(dike('zircon', 5))
                .withBlock(dike('monazite', 2))
                .withBlock(dike('almandine', 1)))
    })
})
