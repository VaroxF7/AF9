// AF9 - The lithography process around the print: calibration, wafer clean-up, the finer coatings, the Metrology
// Station. Items and materials here; behaviour: AF9 Core (LithoMachine); recipes: server_scripts/mods/gtceu/
// litho_process.js. Spec: docs/semiconductor-factory.md §18

StartupEvents.registry('item', event => {
    // The reference wafer of a calibration run: a Line, Scanner or Orbital Station that has drifted takes one from an
    // input bus (or a Metrology Station for the machines on its bus) and aligns its optics and stages on its marks.
    event.create('calibration_wafer')
        .displayName('Calibration Wafer')
        .texture('kubejs:item/wafers/calibration_wafer')
        .tooltip('A reference wafer with alignment marks. Put it in the input bus of a lithography machine that has')
        .tooltip('drifted (below 70% calibration) and it calibrates itself, or feed a Metrology Station.')
})

// ---- Chemistry the process needs beside the track fluids ----
// Formula only, no components (so GT adds no electrolyzer or centrifuge shortcut). Recipes: server_scripts/mods/gtceu/
// litho_process.js.
GTCEuStartupEvents.registry('gtceu:material', event => {
    const materials = [
        // RCA clean, the wafer cleaning every fab starts with: SC-1 (ammonia, peroxide, water, 1:1:5) lifts particles
        // and organics, SC-2 (HCl, peroxide, water, 1:1:6) takes the metal ions off. Piranha (SPM, 3:1 sulfuric acid
        // to peroxide) strips baked resist and heavy organics.
        ['sc1_solution', 'liquid', 0xcfe8f5, '(NH3)(H2O2)(H2O)5'],
        ['sc2_solution', 'liquid', 0xe6f0d2, '(HCl)(H2O2)(H2O)6'],
        ['piranha_solution', 'liquid', 0xf2e2b8, '(H2SO4)3(H2O2)'],
        ['spent_piranha', 'liquid', 0x6b5a3c, '(H2SO4)(H2O)(C)'],

        // Ethyl lactate, the green solvent of the coatings: acetaldehyde from ethanol over copper, lactonitrile with
        // hydrogen cyanide, hydrolysed to lactic acid, esterified with ethanol.
        ['acetaldehyde', 'liquid', 0xeef3e0, 'CH3CHO'],
        ['lactonitrile', 'liquid', 0xe8eadc, 'CH3CH(OH)CN'],
        ['lactic_acid', 'liquid', 0xf0ecd8, 'CH3CH(OH)COOH'],
        ['ethyl_lactate', 'liquid', 0xe9efe2, 'CH3CH(OH)COOC2H5'],

        // BARC, the bottom anti-reflective coat under the resist (KrF and ArF): an acrylic polymer with a dye that
        // soaks up the light that passed the resist, in ethyl lactate. The dye is a nitrated naphthalene.
        ['nitronaphthalene', 'dust', 0xd9b24a, 'C10H7NO2'],
        ['barc', 'liquid', 0xc9a447, '(C5H8O2)n(C10H7NO2)(C5H10O3)']
    ]
    materials.forEach(([id, form, color, formula]) => {
        const material = event.create(id)
        if (form === 'dust') material.dust()
        else material.liquid()
        material.color(color)
        if (formula) material.formula(formula)
    })
})
