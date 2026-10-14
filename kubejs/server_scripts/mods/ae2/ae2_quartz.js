// AF9 - Quartz crystal growth and polishing (MV).
//
// Growth: seeds + water in the autoclave beat throwing them in a pool. Water gives one
// crystal slowly, distilled water gives two faster. Polishing: the cutter laps crystals
// (and sky stone, ender pearls) with distilled water into dust at 2x the inscriber's
// 1:1 crushing. All additive: vanilla seeds, pool growth, budding and crushing stay as
// the pre-MV path. Fluix pearls are redefined (assembler, MV) since the network eats them.
// Spec: docs/ae2.md

ServerEvents.recipes(event => {
    if (!Platform.isLoaded('ae2')) return
    const VA = GTValues.VA
    const MV = VA[GTValues.MV]

    // ---- Growth (autoclave, MV) ----
    const growth = [
        ['certus', 'ae2:certus_quartz_seed', 'ae2:certus_quartz_crystal'],
        ['fluix', 'ae2:fluix_seed', 'ae2:fluix_crystal']
    ]
    growth.forEach(([id, seed, crystal]) => {
        event.recipes.gtceu.autoclave(`af9:grown_${id}_quartz`)
            .itemInputs(seed)
            .inputFluids(Fluid.of('minecraft:water', 1000))
            .itemOutputs(crystal)
            .duration(600)
            .EUt(MV)
        event.recipes.gtceu.autoclave(`af9:grown_${id}_quartz_distilled`)
            .itemInputs(seed)
            .inputFluids(Fluid.of('gtceu:distilled_water', 1000))
            .itemOutputs(`2x ${crystal}`)
            .duration(400)
            .EUt(MV)
    })

    // ---- Polishing (cutter + distilled water, MV): 1 crystal -> 2 dust ----
    const polish = [
        ['certus_dust', 'ae2:certus_quartz_crystal', 'ae2:certus_quartz_dust', 2],
        ['fluix_dust', 'ae2:fluix_crystal', 'ae2:fluix_dust', 2],
        ['sky_dust', 'ae2:sky_stone_block', 'ae2:sky_dust', 4],
        ['ender_dust', 'minecraft:ender_pearl', 'ae2:ender_dust', 2]
    ]
    polish.forEach(([id, input, output, count]) => {
        event.recipes.gtceu.cutter(`af9:polished_${id}`)
            .itemInputs(input)
            .inputFluids(Fluid.of('gtceu:distilled_water', 100))
            .itemOutputs(`${count}x ${output}`)
            .duration(100)
            .EUt(MV)
    })

    // ---- Fluix pearls (assembler, MV): the network's wireless ingredient ----
    event.remove({ mod: 'ae2', output: 'ae2:fluix_pearl' })
    event.recipes.gtceu.assembler('af9:ae2_fluix_pearl')
        .itemInputs('ae2:fluix_crystal', '4x ae2:ender_dust')
        .inputFluids(Fluid.of('gtceu:tin', 144))
        .itemOutputs('ae2:fluix_pearl')
        .duration(200)
        .EUt(MV)
    event.recipes.gtceu.assembler('af9:ae2_fluix_pearl_soldering_alloy')
        .itemInputs('ae2:fluix_crystal', '4x ae2:ender_dust')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 72))
        .itemOutputs('ae2:fluix_pearl')
        .duration(200)
        .EUt(MV)
})
