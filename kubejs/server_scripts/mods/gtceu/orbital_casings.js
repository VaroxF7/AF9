// AF9 - The structure casings of the Orbital Lithography Array Mk2 (docs/orbital-casings.md): a new alloy and four casings that
// replace the Stress-Proof, Sturdy HSS-E (two) and Inert PTFE casings in the Mk2's pattern (startup_scripts/gtceu/photolithography.js).
// All four are assembled at UHV, four casings to a run. Material: af9-core, registry/AF9Materials (orbitalStructureAlloy()).
//
//   ceraquadalloy                 4 trinaquadalloy + 2 ceranium + 1 quantanium + 1 neutronium dust, mixed at UHV, melted by GT's EBF (10800 K)
//   orbital_combined_structure_casing   (Stress-Proof)   ceraquadalloy plates and frame round tritanium plate
//   orbital_truss_casing                (Sturdy HSS-E)   tritanium and naquadah alloy plate on tritanium frames
//   orbital_girder_casing               (Sturdy HSS-E)   osmiridium and europium plate on tritanium frames
//   orbital_deck_casing                 (Inert PTFE)     ceranium plate in PBI foil, the hatches go on it

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const gt = event.recipes.gtceu

    gt.mixer('af9:orbital_casings/ceraquadalloy_dust')
        .itemInputs('4x gtceu:trinaquadalloy_dust', '2x gtceu:ceranium_dust', 'gtceu:quantanium_dust', 'gtceu:neutronium_dust')
        .itemOutputs('8x gtceu:ceraquadalloy_dust')
        .circuit(7)
        .duration(600)
        .EUt(VA[GTValues.UHV])

    const casing = (id, inputs, fluid) => {
        const recipe = gt.assembler(`af9:orbital_casings/${id}`)
            .itemInputs(inputs)
            .itemOutputs(`4x af9:${id}`)
            .duration(400)
            .EUt(VA[GTValues.UHV])
        if (fluid) recipe.inputFluids(fluid)
    }
    casing('orbital_combined_structure_casing',
        ['4x gtceu:ceraquadalloy_plate', 'gtceu:ceraquadalloy_frame', '4x gtceu:tritanium_plate'])
    casing('orbital_truss_casing',
        ['4x gtceu:tritanium_plate', '2x gtceu:tritanium_frame', '2x gtceu:naquadah_alloy_plate'])
    casing('orbital_girder_casing',
        ['4x gtceu:osmiridium_plate', '2x gtceu:tritanium_frame', '2x gtceu:europium_plate'])
    casing('orbital_deck_casing',
        ['2x gtceu:ceranium_plate', '4x gtceu:polybenzimidazole_foil'], Fluid.of('gtceu:polybenzimidazole', 144))
})
