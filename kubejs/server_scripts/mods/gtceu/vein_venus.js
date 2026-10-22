// AF9 - The Ilmenite vein of Venus (the layer: af9-core, space/AF9Space, af9_venus = Ad Astra's venus stone and
// venus sand). Spec: docs/asteroid-fission.md (titanium).
//
// Titanium used to start on Mars sand alone; Venus gets a second ilmenite vein so the titanium chain has a renewable
// source in the tier 3 world too (Mars's ilmenite vein is in vein_mars.js). Dike through the Venusian terrain, same
// form as the Mars veins. Named to load after the pack's mining_dim_ores.js, which moves every GT vein to the Mining
// Dimension; vein_planets.js leaves AF9's own veins alone.

GTCEuServerEvents.oreVeins(event => {
    const dike = (material, weight) => new GTDikeBlockDefinition['(com.gregtechceu.gtceu.api.data.chemical.material.Material,int,int,int)'](
        GTMaterials.get(material), weight, 40, 120)
    event.add('af9:venus_ilmenite_vein', builder => {
        builder.clusterSize(30)
            .weight(40)
            .density(0.7)
            .discardChanceOnAirExposure(0.0)
            .layer('af9_venus')
            .dimensions('ad_astra:venus')
            .heightRangeUniform(40, 120)
            .dikeVeinGenerator(generator => generator
                .withBlock(dike('ilmenite', 5))
                .withBlock(dike('iron', 2)))
    })
})
