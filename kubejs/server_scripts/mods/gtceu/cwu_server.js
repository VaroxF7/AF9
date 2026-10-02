// AF9 - Crafting of the CWU Server (startup_scripts/gtceu/cwu_server.js, af9-core com.af9.core.machine). The glass fibre (fine borosilicate glass wire) is what the computation
// parts' optical ports are made of.

ServerEvents.recipes(event => {
    const VA = GTValues.VA

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
