// AF9 - The Artemite vein of the Moon (the ore: af9-core, registry/AF9Materials, moonSilicon()).
// Spec: docs/semiconductor-factory.md
//
// Artemite, the lunar borate that dopes the first silicon boules, generates only on Earth's Moon
// (ad_astra:moon), in the Moon's own layer (af9_moon, registered by af9-core, space/AF9Space: Ad Astra's
// moon stone and moon sand). The Moon has solid terrain, so this is a standard blob vein, not a dike:
// a dike was only ever needed for the floating rocks of the belts (vein_ceres.js, vein_field.js).
// Named to load after the pack's mining_dim_ores.js, which moves every GT vein to the Mining Dimension:
// this vein keeps the Moon. No biomes filter: the Moon has no GT biomes to name.
//
// Numbers (tune in game with the prospector): clusterSize 24 for generous surface blobs in the 40-120 band
// (the terrain); density 0.8; discardChanceOnAirExposure 0.0, or the air eats the surface blobs;
// weight 45 sets both the worldgen share and the Mk-I elevator's share (af9_moon is tier 1:
// every drone draws it, like Overworld ores).

const $MoonChemicalHelper = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper')
const $MoonTagPrefix = Java.loadClass('com.gregtechceu.gtceu.api.data.tag.TagPrefix')

GTCEuServerEvents.oreVeins(event => {
    const artemite = GTMaterials.get('artemite')
    const oreIn = prefix => () => $MoonChemicalHelper.getBlock(prefix, artemite)
    event.add('af9:artemite_vein', builder => {
        builder.clusterSize(24)
            .weight(45)
            .density(0.8)
            .discardChanceOnAirExposure(0.0)
            .layer('af9_moon')
            .dimensions('ad_astra:moon')
            .heightRangeUniform(40, 120)
            .standardVeinGenerator(generator => {
                // all three stone variants: GT syncs the vein registry to every client on login, and its codec
                // reads each of them (a missing one NPEs the sync and the join dies as "invalid player data").
                // Only the stone ore ever generates (the layer matches moon stone and moon sand alone).
                generator.withBlock(oreIn($MoonTagPrefix.ore))
                generator.withNetherBlock(oreIn($MoonTagPrefix.oreNetherrack))
                try {
                    generator.deepBlock = oreIn($MoonTagPrefix.oreDeepslate)
                } catch (error) {
                    console.warn(`vein_moon.js: the artemite vein keeps stone ore in deepslate: ${error}`)
                }
            })
    })
})
