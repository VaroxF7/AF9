// AF9 - the planet metals: titanium from Mars sand only, tungsten from Venus sand only, and Desh (the Moon's
// metal, an AF9 material: registry/AF9Materials). Spec: docs/asteroid-fission.md
//
// Why gates: GT's titanium chain (ilmenite -> rutile -> TiCl4 -> Kroll with magnesium, HV) and its tungsten chain
// (scheelite/tungstate -> tungstic acid -> electrolysis) are kept as they are; what changes is where the ore comes
// from. Every Earth-side way into those chains is closed below, so the chains start on Mars sand (ilmenite, magnetic:
// the electromagnetic separator pulls it out, the acid leach digests the rest) and Venus sand (scheelite) instead.
// Magnesium turns over in GT's loop (MgCl2 + sodium), sodium and chlorine come from salt water, carbon from coke:
// all Earth-side, so only the sand is gated.
//
// Desh: Moon Desh Ore gives raw desh (Ad Astra smelts it); the macerator below doubles it to GT dust for the plates
// and screws (GT makes the parts once the material exists), and the furnace takes dust to ingot. The two shapeless
// recipes swap Ad Astra's and GT's ingots either way (same metal, two item ids).
//
// Propylene oxide without titania: the HPPO route (propylene oxide over titanium silicalite) needs Mars rutile, but
// the 200 nm prints (and with them ASIC, and with it the tier 2 rocket) come before Mars. The chlorohydrin route
// (propene + chlorine + water, the old industrial way) makes the same oxide with Earth chemistry; HPPO stays as the
// clean upgrade once rutile flows.

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // ---- Gates: the Earth-side ways into titanium and tungsten, closed ----
    // Bauxite electrolysis gave rutile next to the aluminium: now aluminium and oxygen only.
    event.remove({ id: 'gtceu:electrolyzer/bauxite_electrolysis' })
    event.recipes.gtceu.electrolyzer('af9:bauxite_electrolysis')
        .itemInputs('15x gtceu:bauxite_dust')
        .itemOutputs('6x gtceu:aluminium_dust')
        .outputFluids(Fluid.of('gtceu:oxygen', 9000))
        .duration(270)
        .EUt(VA[GTValues.LV] * 2)
    // Bauxite sludge centrifuging gave rutile next to the gallium: now gallium, silica and iron only.
    event.remove({ id: 'gtceu:centrifuge/bauxite_sludge_centrifuge' })
    event.recipes.gtceu.centrifuge('af9:bauxite_sludge_centrifuge')
        .inputFluids(Fluid.of('gtceu:decalcified_bauxite_sludge', 250))
        .chancedOutput('gtceu:gallium_dust', 5000, 0)
        .chancedOutput('gtceu:gallium_dust', 3000, 0)
        .chancedOutput('gtceu:gallium_dust', 1000, 0)
        .chancedOutput('gtceu:silicon_dioxide_dust', 9000, 0)
        .chancedOutput('gtceu:iron_dust', 8000, 0)
        .outputFluids(Fluid.of('minecraft:water', 250))
        .duration(100)
        .EUt(VA[GTValues.MV])
    // End stone macerating and centrifuging gave tungstate: now dust, sand, platinum and helium only.
    event.remove({ id: 'gtceu:macerator/macerate_end_stone' })
    event.recipes.gtceu.macerator('af9:macerate_end_stone')
        .itemInputs('minecraft:end_stone')
        .itemOutputs('gtceu:endstone_dust')
        .duration(150)
        .EUt(2)
    event.remove({ id: 'gtceu:centrifuge/endstone_separation' })
    event.recipes.gtceu.centrifuge('af9:endstone_separation')
        .itemInputs('gtceu:endstone_dust')
        .chancedOutput('minecraft:sand', 9000, 0)
        .chancedOutput('gtceu:platinum_dust', 80, 0)
        .outputFluids(Fluid.of('gtceu:helium', 120))
        .duration(320)
        .EUt(20)

    // ---- Mars: ilmenite out of Mars sand ----
    // Physical route (ilmenite is strongly magnetic): the separator pulls it out of the sand, silica stays behind.
    event.recipes.gtceu.electromagnetic_separator('af9:mars_sand_beneficiation')
        .itemInputs('4x ad_astra:mars_sand')
        .itemOutputs('gtceu:ilmenite_dust')
        .itemOutputs('3x gtceu:silicon_dioxide_dust')
        .duration(200)
        .EUt(VA[GTValues.MV])
    // Chemical route (the acid digests the silicates): three ilmenite for four sand, at HV.
    event.recipes.gtceu.chemical_reactor('af9:mars_sand_acid_leach')
        .itemInputs('4x ad_astra:mars_sand')
        .inputFluids(Fluid.of('gtceu:sulfuric_acid', 2000))
        .itemOutputs('3x gtceu:ilmenite_dust', '2x gtceu:silicon_dioxide_dust')
        .outputFluids(Fluid.of('minecraft:water', 2000))
        .duration(300)
        .EUt(VA[GTValues.HV])
    // From ilmenite GT's chain runs unchanged: blast furnace with carbon to rutile (1700 K, kanthal), chemical
    // reactor with carbon and chlorine to titanium tetrachloride, blast furnace with magnesium to titanium (Kroll,
    // 2141 K, kanthal), and the magnesium loop (MgCl2 + sodium) turns with salt-water sodium and chlorine.

    // ---- Venus: scheelite out of Venus sand ----
    // Poor route: washing the sand, a little scheelite with the silica.
    event.recipes.gtceu.macerator('af9:venus_sand_washing')
        .itemInputs('ad_astra:venus_sand')
        .itemOutputs('gtceu:silicon_dioxide_dust')
        .chancedOutput('gtceu:scheelite_dust', 2500, 0)
        .duration(100)
        .EUt(VA[GTValues.MV])
    // Rich route: the acid digests the sand the way it digests Mars sand.
    event.recipes.gtceu.chemical_reactor('af9:venus_sand_acid_leach')
        .itemInputs('4x ad_astra:venus_sand')
        .inputFluids(Fluid.of('gtceu:sulfuric_acid', 2000))
        .itemOutputs('2x gtceu:scheelite_dust', '3x gtceu:silicon_dioxide_dust')
        .outputFluids(Fluid.of('minecraft:water', 2000))
        .duration(300)
        .EUt(VA[GTValues.HV])
    // From scheelite GT's chain runs unchanged: chemical bath with hydrochloric acid to tungstic acid, electrolyzer
    // to tungsten.

    // ---- Desh: raw desh to GT dust and ingot, and the ingot swap ----
    event.recipes.gtceu.macerator('af9:desh_crushing')
        .itemInputs('ad_astra:raw_desh')
        .itemOutputs('2x gtceu:desh_dust')
        .duration(200)
        .EUt(VA[GTValues.LV])
    event.smelting('gtceu:desh_ingot', 'gtceu:desh_dust')
        .id('af9:desh_dust_smelting')
    event.shapeless('gtceu:desh_ingot', ['ad_astra:desh_ingot']).id('af9:desh_unify_ingot')
    event.shapeless('ad_astra:desh_ingot', ['gtceu:desh_ingot']).id('af9:desh_unify_ingot_reverse')

    // ---- Propylene oxide the old way (chlorohydrin): propene + chlorine + water ----
    event.recipes.gtceu.chemical_reactor('af9:propylene_oxide_chlorohydrin')
        .inputFluids(Fluid.of('gtceu:propene', 1000), Fluid.of('gtceu:chlorine', 1000),
            Fluid.of('minecraft:water', 1000))
        .outputFluids(Fluid.of('gtceu:propylene_oxide', 1000), Fluid.of('gtceu:hydrochloric_acid', 2000))
        .duration(200)
        .EUt(VA[GTValues.MV])
})
