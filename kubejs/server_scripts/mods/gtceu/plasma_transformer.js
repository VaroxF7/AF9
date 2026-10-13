// AF9 - Plasma Transformer recipes (startup_scripts/gtceu/plasma_transformer.js).
// Superstate plasmas condense to plates here at XPS. First (and only) superstate:
// Star Matter Plasma Steel, forged in the DTPF (dtpf.js) for the NVM boards.

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // ---- The transformer: stainless shell, glass-free, MV bones round an MV hull ----
    event.shaped('gtceu:plasma_transformer', [
        'SPS',
        'PCP',
        'SMS'
    ], {
        S: '#forge:plates/stainless_steel',
        P: 'gtceu:mv_electric_pump',
        C: '#gtceu:circuits/ev',
        M: 'gtceu:mv_machine_hull'
    }).id('af9:plasma_transformer')

    // ---- Star Matter Plasma Steel: 2,000 mB of superstate -> 6 plates ----
    event.recipes.gtceu.plasma_transform('af9:star_matter_plasma_steel_plates')
        .inputFluids(Fluid.of('gtceu:star_matter_plasma_steel_plasma', 2000))
        .itemOutputs('6x gtceu:star_matter_plasma_steel_plate')
        .duration(400)
        .EUt(VA[GTValues.UHV])
})
