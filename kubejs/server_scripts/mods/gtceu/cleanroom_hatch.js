// AF9 - The Cleaning Maintenance Hatch is ZPM. The plain maintenance hatch stays LV (the EBF
// and every hall need it early); only this cleanroom variant waits for ZPM parts: a plain
// maintenance hatch, a ZPM circuit, two filter casings (the cleanroom's own ceiling) and a ZPM
// pump. GT's own recipe is removed. Spec: docs/semiconductor-factory.md §4.4

ServerEvents.recipes(event => {
    event.remove({ output: 'gtceu:cleaning_maintenance_hatch' })

    event.recipes.gtceu.assembler('af9:cleaning_maintenance_hatch')
        .itemInputs('gtceu:maintenance_hatch', '#gtceu:circuits/zpm', '2x gtceu:filter_casing',
            'gtceu:zpm_electric_pump')
        .itemOutputs('gtceu:cleaning_maintenance_hatch')
        .duration(400)
        .EUt(GTValues.VA[GTValues.ZPM])
})
