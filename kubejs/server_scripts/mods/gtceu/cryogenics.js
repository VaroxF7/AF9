// AF9 - Supercooling Cryostat and Coolant Hatch recipes (startup_scripts/gtceu/cryogenics.js).
// Spec: docs/semiconductor-factory.md
//
// Two passes through the same cryostat: dense cooling (gas -> dense liquid, 4:1) and supercooling (dense liquid ->
// supercooled fluid). Every recipe draws 4A of HV; the cryostat runs them with perfect overclocks above that.

ServerEvents.recipes(allthemods => {
    const VA = GTValues.VA

    // ---- The cryostat and the coolant hatches ----
    allthemods.recipes.gtceu.assembler('af9:supercooling_cryostat')
        .itemInputs('gtceu:hv_machine_hull', '4x #gtceu:circuits/hv', '2x gtceu:hv_electric_pump',
            '2x gtceu:hv_electric_motor', '2x gtceu:frostproof_machine_casing', '8x gtceu:aluminium_plate',
            '4x gtceu:polytetrafluoroethylene_plate')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 576))
        .itemOutputs('gtceu:supercooling_cryostat')
        .duration(1200)
        .EUt(VA[GTValues.HV])

    // an input hatch of the tier, pre-chilled and lined for the supercooled fluids
    const hatchTiers = [['luv', 'luv'], ['zpm', 'zpm'], ['uv', 'uv'], ['uhv', 'uv']]
    hatchTiers.forEach(([tier, parts], index) => {
        allthemods.recipes.gtceu.assembler(`af9:${tier}_coolant_hatch`)
            .itemInputs(`gtceu:${tier}_input_hatch`, `2x gtceu:${parts}_electric_pump`,
                'gtceu:frostproof_machine_casing', '4x gtceu:polytetrafluoroethylene_plate')
            .inputFluids(Fluid.of('gtceu:supercooled_hydrogen', 1000))
            .itemOutputs(`gtceu:${tier}_coolant_hatch`)
            .duration(400)
            .EUt(VA[GTValues.LuV + index])
    })

    // ---- Dense cooling and supercooling ----
    // gas, dense cooling ticks, supercooling ticks
    const gases = [['hydrogen', 200, 400], ['argon', 300, 600], ['xenon', 400, 800], ['endion', 600, 1200]]
    gases.forEach(([gas, denseTicks, superTicks]) => {
        allthemods.recipes.gtceu.dense_cooling(`af9:dense_${gas}`)
            .inputFluids(Fluid.of(`gtceu:${gas}`, 1000))
            .outputFluids(Fluid.of(`gtceu:dense_${gas}`, 250))
            .duration(denseTicks)
            .EUt(VA[GTValues.HV], 4)
        allthemods.recipes.gtceu.supercooling(`af9:supercooled_${gas}`)
            .inputFluids(Fluid.of(`gtceu:dense_${gas}`, 250))
            .outputFluids(Fluid.of(`gtceu:supercooled_${gas}`, 250))
            .duration(superTicks)
            .EUt(VA[GTValues.HV], 4)
    })
})
