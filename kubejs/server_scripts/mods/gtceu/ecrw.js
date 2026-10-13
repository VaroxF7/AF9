// AF9 - the Evaporation Condensation Retaining Well: its controller and what it boils down (machine and
// structure: startup_scripts/gtceu/ecrw.js, af9-core EvaporationWellMachine + EcrwArmRender). Spec: docs/ecrw.md
//
// The well's first production: sea salt out of salt water, the way the old evaporation plants did it. Salt water
// comes from GT's own mixer (2 salt dust + water); the well keeps the salt and most of the water back as
// distilled. The chemical and bio lines take it from there.

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // The controller: an HV hull in stainless plate and glass, with the pumps that move the brine
    event.recipes.gtceu.assembler('af9:ecrw')
        .itemInputs('gtceu:hv_machine_hull', '4x gtceu:stainless_steel_plate', '4x gtceu:tempered_glass',
            '2x gtceu:hv_electric_pump', '4x #gtceu:circuits/hv')
        .itemOutputs('gtceu:ecrw')
        .duration(600)
        .EUt(VA[GTValues.HV])

    // ---- Evaporation: dilute fluids in, the solids and the condensate out ----
    // Sea salt: 1,000 mB of salt water leaves its salt behind, 800 mB comes back distilled
    event.recipes.gtceu.evaporation('af9:ecrw/salt_evaporation')
        .inputFluids(Fluid.of('gtceu:salt_water', 1000))
        .itemOutputs('2x gtceu:salt_dust')
        .outputFluids(Fluid.of('gtceu:distilled_water', 800))
        .duration(400)
        .EUt(VA[GTValues.MV])
})
