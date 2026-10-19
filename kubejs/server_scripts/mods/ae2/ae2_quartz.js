// AF9 - Quartz crystal growth and polishing (MV), plus the printed-circuit parts of the
// processors and components: redstone plate/foil and the fine certus wire drawn from the
// crystals (both LV).
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
        // AE2 15 has no seed items (growth is budding/pool only): skip when absent
        if (!Item.exists(seed)) return
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
        .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144))
        .itemOutputs('ae2:fluix_pearl')
        .duration(200)
        .EUt(MV)

    // ---- Liquid certus quartz (extractor, MV): the fluid of every processor and component assembly ----
    event.recipes.gtceu.extractor('af9:liquid_certus_quartz')
        .itemInputs('ae2:certus_quartz_crystal')
        .outputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144))
        .duration(80)
        .EUt(MV)
    event.recipes.gtceu.extractor('af9:liquid_certus_quartz_charged')
        .itemInputs('ae2:charged_certus_quartz_crystal')
        .outputFluids(Fluid.of('gtceu:liquid_certus_quartz', 288))
        .duration(80)
        .EUt(MV)

    // ---- Redstone foil (compressor + bender, LV): for the processors' printed circuits ----
    event.recipes.gtceu.compressor('af9:redstone_plate')
        .itemInputs('4x minecraft:redstone')
        .itemOutputs('gtceu:redstone_plate')
        .duration(200)
        .EUt(VA[GTValues.LV])
    event.recipes.gtceu.bender('af9:redstone_foil')
        .itemInputs('gtceu:redstone_plate')
        .circuit(0)
        .itemOutputs('4x gtceu:redstone_foil')
        .duration(100)
        .EUt(VA[GTValues.LV])

    // ---- Fine certus wire (wiremill, LV): drawn from the crystals, the 256k component's traces ----
    // The item is GT's fine_<material>_wire of certus quartz (the flag goes on in AF9Materials.aeQuartz).
    event.recipes.gtceu.wiremill('af9:fine_certus_quartz_wire')
        .itemInputs('ae2:certus_quartz_crystal')
        .itemOutputs('4x gtceu:fine_certus_quartz_wire')
        .duration(100)
        .EUt(VA[GTValues.LV])
})
