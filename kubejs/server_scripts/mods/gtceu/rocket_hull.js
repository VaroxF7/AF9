// AF9 - The tier 2 rocket's hull: Desh Titanium Rocket Alloy (materials: af9-core AF9Materials.rocketInsulation()).
// Spec: docs/electric-implosion.md. The rocket itself: rockets.js (the tier 2 row of its table).
//
//   mixer (HV)   4x titanium dust + 2x desh dust + 16x chromium dust -> 4x Desh Titanium Rocket Alloy dust
//   the hull     4 dust implode to ONE dense plate: with dynamite or industrial TNT in GT's Implosion Compressor,
//                or without explosives in the Electric Implosion Compressor (electric_implosion.js, with the vapor).
//                GT's bender recipes (9 plates or 9 ingots -> dense plate) are gone for this alloy: the only way
//                to a dense plate is the implosion.
//   tier 2 parts the Rocket Cone Tier 2 and the Rocket Fin Tier 2 (4 at a time): the tier 1 parts, desh metal and the
//                tier's guidance chips (HV assembler)
// The insulation (Alumina Silicate Rocket Insulation, its foil, wool, dust and liquid mixture) has no recipes yet:
// they are Janne's to come.

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const ALLOY = 'gtceu:desh_titanium_rocket_alloy'

    event.recipes.gtceu.mixer('af9:desh_titanium_rocket_alloy_dust')
        .itemInputs('4x gtceu:titanium_dust', '2x gtceu:desh_dust', '16x gtceu:chromium_dust')
        .itemOutputs(`4x ${ALLOY}_dust`)
        .duration(300)
        .EUt(VA[GTValues.HV])

    // the only way to a dense plate (GT bends them from 9 plates or ingots)
    event.remove({ output: `${ALLOY}_dense_plate`, type: 'gtceu:bender' })

    // GT's implosion compressor: the shock wave of explosives, as for every gem (4 dust in)
    event.recipes.gtceu.implosion_compressor('af9:desh_titanium_rocket_alloy_dense_plate_dynamite')
        .itemInputs(`4x ${ALLOY}_dust`, '2x gtceu:dynamite')
        .itemOutputs(`${ALLOY}_dense_plate`)
        .duration(20)
        .EUt(VA[GTValues.LV])
    event.recipes.gtceu.implosion_compressor('af9:desh_titanium_rocket_alloy_dense_plate_itnt')
        .itemInputs(`4x ${ALLOY}_dust`, 'gtceu:industrial_tnt')
        .itemOutputs(`${ALLOY}_dense_plate`)
        .duration(20)
        .EUt(VA[GTValues.LV])

    // ---- The parts of the tier 2 rocket ----
    // the cone: the tier 1 cone with desh skin, an HV sensor and two ASICs for the guidance
    event.recipes.gtceu.assembler('af9:rocket_cone_tier_2')
        .itemInputs('ad_astra:rocket_nose_cone', '2x gtceu:desh_plate', 'gtceu:hv_sensor', '2x af9:asic_chip')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 288))
        .itemOutputs('af9:rocket_cone_tier_2')
        .duration(400)
        .EUt(VA[GTValues.HV])
    // the fins: four tier 1 fins re-skinned in desh plate and rod
    event.recipes.gtceu.assembler('af9:rocket_fin_tier_2')
        .itemInputs('4x ad_astra:rocket_fin', '4x gtceu:desh_plate', '4x gtceu:desh_rod')
        .itemOutputs('4x af9:rocket_fin_tier_2')
        .duration(300)
        .EUt(VA[GTValues.HV])
})
