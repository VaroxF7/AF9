// AF9 - AE2 world mechanics, gregified at LV/MV.
//
// Fluix mixing moves to the mixer (charged certus + nether quartz + redstone),
// crystal crushing to the macerator (pre-MV yields, worse than the MV cutter in
// ae2_quartz.js), sky stone to sky dust the same way. Vanilla shapeless/shapeless
// mixing and crushing for these outputs are removed mod-scoped. Charging stays on
// the charger block (ae2_devices.js); budding/pool growth stay as the pre-MV path.
// See docs/ae2.md.

ServerEvents.recipes(event => {
    if (!Platform.isLoaded('ae2')) return
    const VA = GTValues.VA
    const LV = VA[GTValues.LV]
    const MV = VA[GTValues.MV]
    const gtr = event.recipes.gtceu

    // ---- Fluix dust: the mixer instead of the crafting grid ----
    event.remove({ mod: 'ae2', output: 'ae2:fluix_dust' })
    gtr.mixer('af9:fluix_dust')
        .itemInputs('ae2:charged_certus_quartz_crystal', 'minecraft:quartz', 'minecraft:redstone')
        .itemOutputs('2x ae2:fluix_dust')
        .duration(200)
        .EUt(LV)

    // ---- Pre-MV crushing: macerator yields, worse than the MV cutter ----
    // [id, input, output] (cutter in ae2_quartz.js pays 2x for crystals)
    const crush = [
        ['sky_dust', 'ae2:sky_stone_block', '2x ae2:sky_dust'],
        ['certus_dust', 'ae2:certus_quartz_crystal', 'ae2:certus_quartz_dust'],
        ['fluix_dust', 'ae2:fluix_crystal', 'ae2:fluix_dust'],
        ['nether_dust', 'minecraft:quartz', 'ae2:nether_quartz_dust']
    ]
    crush.forEach(entry => {
        const id = entry[0]
        const input = entry[1]
        const output = entry[2]
        event.remove({ mod: 'ae2', output: output.split(' ').pop() })
        gtr.macerator('af9:macerate_' + id)
            .itemInputs(input)
            .itemOutputs(output)
            .duration(150)
            .EUt(LV)
    })

    // ---- Fluix crystal back out of dust: the bath instead of the grid ----
    event.remove({ mod: 'ae2', output: 'ae2:fluix_crystal' })
    gtr.chemical_bath('af9:fluix_crystal')
        .itemInputs('4x ae2:fluix_dust')
        .inputFluids(Fluid.of('minecraft:water', 1000))
        .itemOutputs('4x ae2:fluix_crystal')
        .duration(400)
        .EUt(MV)
})
