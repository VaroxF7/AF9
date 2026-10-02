// AF9 - Crafting of the CWU Server and the ME Computation Link (startup_scripts/gtceu/cwu_server.js, af9-core
// com.af9.core.machine / com.af9.core.ae2). The glass fibre (fine borosilicate glass wire) is what the computation
// parts' optical ports are made of.

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // ME Computation Link: AE2's calculation processors on the network side, an optical port on the back
    event.recipes.gtceu.assembler('af9:me_computation_link')
        .itemInputs('2x ae2:calculation_processor', 'ae2:fluix_glass_cable', 'ae2:quartz_fiber', 'kubejs:mcu_chip',
            '4x gtceu:fine_borosilicate_glass_wire')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 144))
        .itemOutputs('af9:me_computation_link')
        .duration(200)
        .EUt(VA[GTValues.MV])

    // CWU Server: a server in a machine hull. LV runs on plain circuits; from MV on AF9's chips: APUs (CPU and GPU
    // on one die) and GT's RAM, ASICs from HV, eDRAM and MRAM at IV
    const servers = [
        ['lv', ['4x #gtceu:circuits/lv', '2x gtceu:tin_single_cable'], 'gtceu:tin', 144],
        ['mv', ['2x #gtceu:circuits/mv', '2x kubejs:apu_chip', '4x gtceu:ram_chip', '2x gtceu:fine_borosilicate_glass_wire'],
            'gtceu:soldering_alloy', 144],
        ['hv', ['2x #gtceu:circuits/hv', '4x kubejs:apu_chip', '8x gtceu:ram_chip', '2x kubejs:asic_chip',
            '2x gtceu:fine_borosilicate_glass_wire'], 'gtceu:soldering_alloy', 288],
        ['ev', ['2x #gtceu:circuits/ev', '8x kubejs:apu_chip', '16x gtceu:ram_chip', '4x kubejs:asic_chip',
            '4x gtceu:fine_borosilicate_glass_wire'], 'gtceu:soldering_alloy', 432],
        ['iv', ['2x #gtceu:circuits/iv', '8x kubejs:apu_chip', '4x kubejs:edram_chip', '4x kubejs:mram_chip',
            '8x kubejs:asic_chip', '4x gtceu:fine_borosilicate_glass_wire'], 'gtceu:soldering_alloy', 576]
    ]
    servers.forEach(([tier, parts, fluid, mb], i) => {
        event.recipes.gtceu.assembler(`af9:${tier}_cwu_server`)
            .itemInputs([`gtceu:${tier}_machine_hull`].concat(parts))
            .inputFluids(Fluid.of(fluid, mb))
            .itemOutputs(`gtceu:${tier}_cwu_server`)
            .duration(200)
            .EUt(VA[GTValues.LV + i])
    })
})
