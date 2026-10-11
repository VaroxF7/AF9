// AF9 - The Artemite, Lesolite and Tranquillite veins of the Moon (the ores: af9-core,
// registry/AF9Materials, moonSilicon()).
// Spec: docs/semiconductor-factory.md (artemite), docs/lesolite.md (lesolite, tranquillite).
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
// artemite weight 45, lesolite weight 30 (a little rarer: it only feeds the EV SMD substrates),
// tranquillite weight 50 (the MV bond-wire alloy burns through it);
// all three set the Mk-I elevator's shares (af9_moon is tier 1: every drone draws them, like Overworld ores).

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
    // Lesolite, the amber EV substrate mineral: same band and form as artemite, its own vein and weight.
    const lesolite = GTMaterials.get('lesolite')
    const lesoIn = prefix => () => $MoonChemicalHelper.getBlock(prefix, lesolite)
    event.add('af9:lesolite_vein', builder => {
        builder.clusterSize(24)
            .weight(30)
            .density(0.8)
            .discardChanceOnAirExposure(0.0)
            .layer('af9_moon')
            .dimensions('ad_astra:moon')
            .heightRangeUniform(40, 120)
            .standardVeinGenerator(generator => {
                generator.withBlock(lesoIn($MoonTagPrefix.ore))
                generator.withNetherBlock(lesoIn($MoonTagPrefix.oreNetherrack))
                try {
                    generator.deepBlock = lesoIn($MoonTagPrefix.oreDeepslate)
                } catch (error) {
                    console.warn(`vein_moon.js: the lesolite vein keeps stone ore in deepslate: ${error}`)
                }
            })
    })
    // Tranquillite, the sea-teal bond-wire mineral: same band and form, its own vein and the heaviest weight.
    const tranquillite = GTMaterials.get('tranquillite')
    const tranqIn = prefix => () => $MoonChemicalHelper.getBlock(prefix, tranquillite)
    event.add('af9:tranquillite_vein', builder => {
        builder.clusterSize(24)
            .weight(50)
            .density(0.8)
            .discardChanceOnAirExposure(0.0)
            .layer('af9_moon')
            .dimensions('ad_astra:moon')
            .heightRangeUniform(40, 120)
            .standardVeinGenerator(generator => {
                generator.withBlock(tranqIn($MoonTagPrefix.ore))
                generator.withNetherBlock(tranqIn($MoonTagPrefix.oreNetherrack))
                try {
                    generator.deepBlock = tranqIn($MoonTagPrefix.oreDeepslate)
                } catch (error) {
                    console.warn(`vein_moon.js: the tranquillite vein keeps stone ore in deepslate: ${error}`)
                }
            })
    })
})
