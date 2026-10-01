// AF9 - Recipes of the lithography process around the print (items, materials and machines:
// startup_scripts/gtceu/litho_process.js and air_conditioning.js; behaviour: AF9 Core LithoMachine).
// Spec: docs/semiconductor-factory.md §18

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // ---- Air Conditioning Hatches (MV-IV) ----
    // A compressor (pump), a fan (motor), the condenser (tier plates) and the control circuits in a machine hull. Every
    // part is made with the machines of the tier before, like the line's own parts.
    const hatches = [
        ['mv', 'gtceu:aluminium_plate', GTValues.MV],
        ['hv', 'gtceu:stainless_steel_plate', GTValues.HV],
        ['ev', 'gtceu:titanium_plate', GTValues.EV],
        ['iv', 'gtceu:tungsten_steel_plate', GTValues.IV]
    ]
    hatches.forEach(([tier, plate, voltage]) => {
        event.recipes.gtceu.assembler(`af9:${tier}_air_conditioning_hatch`)
            .itemInputs(`gtceu:${tier}_machine_hull`, `gtceu:${tier}_electric_pump`, `gtceu:${tier}_electric_motor`,
                `2x #gtceu:circuits/${tier}`, `4x ${plate}`)
            .inputFluids(Fluid.of('gtceu:soldering_alloy', 144))
            .itemOutputs(`gtceu:${tier}_air_conditioning_hatch`)
            .duration(200)
            .EUt(VA[voltage])
    })

    // ---- Calibration Wafer ----
    // A blank silicon wafer with chrome alignment marks: resist it, expose the marks, etch the chrome. Four from one.
    event.recipes.gtceu.assembler('af9:calibration_wafer')
        .itemInputs('gtceu:silicon_wafer', 'gtceu:chromium_plate')
        .inputFluids(Fluid.of('gtceu:photoresist', 100))
        .itemOutputs('4x kubejs:calibration_wafer')
        .duration(300)
        .EUt(VA[GTValues.MV])
})
