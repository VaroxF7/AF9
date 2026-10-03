// AF9 - Nether chemistry: sulfur out of the Nether's plants (startup: none needed, all GT materials).
// Spec: docs/green-chemistry.md
//
//   nether wart / wart blocks -macerator-> sulfur dust -H2-> hydrogen sulfide -O2-> sulfuric acid (WSA)
//   crimson/warped flora      -centrifuge-> radon gas (a trace seeps out of every load)
//
// The acid's last step is the wet sulfuric acid process (H2S + 2 O2 -> H2SO4); the large chemical reactor
// also runs GTNH's all-in-one (sulfur + water + oxygen straight to acid) as the EV upscale.

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // ---- Nether wart to sulfur: the wart's sulfur compounds stay behind as brimstone ----
    event.recipes.gtceu.macerator('af9:nether_wart_sulfur')
        .itemInputs('minecraft:nether_wart')
        .itemOutputs('2x gtceu:sulfur_dust')
        .chancedOutput('gtceu:sulfur_dust', 2500, 0)
        .duration(200)
        .EUt(VA[GTValues.MV])

    // Wart blocks press the same way, lossy on purpose (a block unwarts to nine wart)
    event.recipes.gtceu.macerator('af9:nether_wart_block_sulfur')
        .itemInputs('minecraft:nether_wart_block')
        .itemOutputs('16x gtceu:sulfur_dust')
        .duration(400)
        .EUt(VA[GTValues.MV])

    // ---- Sulfur + hydrogen -> hydrogen sulfide (hot, like the Claus feed) ----
    event.recipes.gtceu.chemical_reactor('af9:sulfur_hydrogenation')
        .itemInputs('gtceu:sulfur_dust')
        .inputFluids(Fluid.of('gtceu:hydrogen', 1000))
        .outputFluids(Fluid.of('gtceu:hydrogen_sulfide', 1000))
        .duration(160)
        .EUt(VA[GTValues.MV])

    // ---- Wet sulfuric acid process: H2S + 2 O2 -> H2SO4, atom for atom ----
    event.recipes.gtceu.chemical_reactor('af9:wet_sulfuric_acid')
        .inputFluids(Fluid.of('gtceu:hydrogen_sulfide', 1000), Fluid.of('gtceu:oxygen', 2000))
        .outputFluids(Fluid.of('gtceu:sulfuric_acid', 1000))
        .duration(200)
        .EUt(VA[GTValues.HV])

    // GTNH's all-in-one as the EV upscale: sulfur + water + oxygen straight to acid
    event.recipes.gtceu.large_chemical_reactor('af9:contact_sulfuric_acid')
        .itemInputs('gtceu:sulfur_dust')
        .inputFluids(Fluid.of('minecraft:water', 1000), Fluid.of('gtceu:oxygen', 3000))
        .outputFluids(Fluid.of('gtceu:sulfuric_acid', 1000))
        .duration(100)
        .EUt(VA[GTValues.EV])

    // ---- Nether flora to radon: sixteen plants, half a bucket of gas, ash left over ----
    // forEach, not a for loop: Rhino keeps a loop's const at its first value (lint S3)
    const flora = [
        'crimson_roots', 'warped_roots', 'nether_sprouts', 'crimson_fungus',
        'warped_fungus', 'weeping_vines', 'twisting_vines'
    ]
    flora.forEach(plant => {
        event.recipes.gtceu.centrifuge(`af9:radon_from_${plant}`)
            .itemInputs(`16x minecraft:${plant}`)
            .itemOutputs('2x gtceu:ash_dust')
            .chancedOutput('minecraft:glowstone_dust', 1500, 0)
            .outputFluids(Fluid.of('gtceu:radon', 500))
            .duration(300)
            .EUt(VA[GTValues.HV])
    })
})
