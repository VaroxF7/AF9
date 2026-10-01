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

    // ======================================================================================================
    // Chemistry (materials: startup_scripts/gtceu/litho_process.js). All in the SMC fab machines, like the
    // rest of the fab chemistry; non-thermal recipes from HV power on need a clean room.
    // ======================================================================================================
    const gt = event.recipes.gtceu
    const MV = VA[GTValues.MV], HV = VA[GTValues.HV], IV = VA[GTValues.IV]

    // ---- RCA clean and the resist strip ----
    // SC-1 1:1:5 and SC-2 1:1:6 (ammonia / HCl, peroxide, water by volume), piranha 3:1 (sulfuric acid, peroxide)
    gt.fab_blending('af9:sc1_solution')
        .inputFluids(Fluid.of('gtceu:ammonia', 1000), Fluid.of('gtceu:hydrogen_peroxide', 1000),
            Fluid.of('gtceu:distilled_water', 5000))
        .outputFluids(Fluid.of('gtceu:sc1_solution', 7000))
        .duration(100)
        .EUt(MV)
    gt.fab_blending('af9:sc2_solution')
        .inputFluids(Fluid.of('gtceu:hydrochloric_acid', 1000), Fluid.of('gtceu:hydrogen_peroxide', 1000),
            Fluid.of('gtceu:distilled_water', 6000))
        .outputFluids(Fluid.of('gtceu:sc2_solution', 8000))
        .duration(100)
        .EUt(MV)
    gt.fab_blending('af9:piranha_solution')
        .inputFluids(Fluid.of('gtceu:sulfuric_acid', 3000), Fluid.of('gtceu:hydrogen_peroxide', 1000))
        .outputFluids(Fluid.of('gtceu:piranha_solution', 4000))
        .duration(100)
        .EUt(MV)
    // The spent strip is sulfuric acid with the dissolved resist: lime turns the acid into gypsum
    gt.fab_synthesis('af9:spent_piranha_neutralisation')
        .itemInputs('gtceu:calcium_hydroxide_dust')
        .inputFluids(Fluid.of('gtceu:spent_piranha', 1000))
        .itemOutputs('gtceu:gypsum_dust')
        .outputFluids(Fluid.of('minecraft:water', 500))
        .duration(200)
        .EUt(MV)

    // ---- Ethyl lactate ----
    // 2 CH3CH2OH + O2 -> 2 CH3CHO + 2 H2O over copper
    gt.fab_synthesis('af9:acetaldehyde')
        .notConsumable('gtceu:copper_dust')
        .inputFluids(Fluid.of('gtceu:ethanol', 1000), Fluid.of('gtceu:oxygen', 500))
        .outputFluids(Fluid.of('gtceu:acetaldehyde', 1000), Fluid.of('minecraft:water', 1000))
        .duration(200)
        .EUt(MV)
    // CH3CHO + HCN -> CH3CH(OH)CN, base catalysed
    gt.fab_synthesis('af9:lactonitrile')
        .notConsumable('gtceu:sodium_hydroxide_dust')
        .inputFluids(Fluid.of('gtceu:acetaldehyde', 1000), Fluid.of('gtceu:hydrogen_cyanide', 1000))
        .outputFluids(Fluid.of('gtceu:lactonitrile', 1000))
        .duration(200)
        .EUt(MV)
    // CH3CH(OH)CN + 2 H2O + H2SO4 -> CH3CH(OH)COOH + NH4HSO4
    gt.fab_synthesis('af9:lactic_acid')
        .inputFluids(Fluid.of('gtceu:lactonitrile', 1000), Fluid.of('minecraft:water', 2000),
            Fluid.of('gtceu:sulfuric_acid', 1000))
        .itemOutputs('gtceu:ammonium_bisulfate_dust')
        .outputFluids(Fluid.of('gtceu:lactic_acid', 1000))
        .duration(300)
        .EUt(MV)
    // Fischer esterification: the acid catalyst stays
    gt.fab_synthesis('af9:ethyl_lactate')
        .inputFluids(Fluid.of('gtceu:lactic_acid', 1000), Fluid.of('gtceu:ethanol', 1000))
        .notConsumableFluid(Fluid.of('gtceu:sulfuric_acid', 100))
        .outputFluids(Fluid.of('gtceu:ethyl_lactate', 1000), Fluid.of('minecraft:water', 1000))
        .duration(300)
        .EUt(MV)

    // ---- BARC ----
    // Nitration of naphthalene (mixed acid: the sulfuric acid stays), then polymer and dye in ethyl lactate
    gt.fab_synthesis('af9:nitronaphthalene')
        .circuit(4)
        .inputFluids(Fluid.of('gtceu:naphthalene', 1000), Fluid.of('gtceu:nitric_acid', 1000))
        .notConsumableFluid(Fluid.of('gtceu:sulfuric_acid', 100))
        .itemOutputs('gtceu:nitronaphthalene_dust')
        .outputFluids(Fluid.of('minecraft:water', 1000))
        .duration(300)
        .EUt(MV)
    gt.fab_blending('af9:barc')
        .itemInputs('gtceu:methacrylate_resin_dust', 'gtceu:nitronaphthalene_dust')
        .inputFluids(Fluid.of('gtceu:ethyl_lactate', 4000))
        .outputFluids(Fluid.of('gtceu:barc', 4000))
        .duration(200)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // ---- Metal-oxo EUV resists ----
    // The same tin-free idea as the tin-oxo resist, with the zirconium and hafnium the zircon chain makes: hafnium
    // clusters absorb EUV the best (a quarter more resist per batch), zirconium is the plentiful one.
    gt.fab_synthesis('af9:zirconium_oxo_resist')
        .inputFluids(Fluid.of('gtceu:zirconium_tetrachloride', 1000), Fluid.of('gtceu:methacrylic_acid', 2000),
            Fluid.of('gtceu:propylene_glycol_methyl_ether_acetate', 4000), Fluid.of('gtceu:ultrapure_water', 1000))
        .outputFluids(Fluid.of('gtceu:euv_photoresist', 4000), Fluid.of('gtceu:hydrochloric_acid', 2000))
        .duration(600)
        .EUt(IV)
        .cleanroom(CleanroomType.CLEANROOM)
    gt.fab_synthesis('af9:hafnium_oxo_resist')
        .inputFluids(Fluid.of('gtceu:hafnium_tetrachloride', 1000), Fluid.of('gtceu:methacrylic_acid', 2000),
            Fluid.of('gtceu:propylene_glycol_methyl_ether_acetate', 4000), Fluid.of('gtceu:ultrapure_water', 1000))
        .outputFluids(Fluid.of('gtceu:euv_photoresist', 5000), Fluid.of('gtceu:hydrochloric_acid', 2000))
        .duration(600)
        .EUt(IV)
        .cleanroom(CleanroomType.CLEANROOM)
})
