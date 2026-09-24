// AF9 - Photolithography Line recipes (machine, materials and items: startup_scripts/gtceu/photolithography.js)

ServerEvents.recipes(allthemods => {
    const EU_LV = GTValues.VA[GTValues.LV]
    const EU_MV = GTValues.VA[GTValues.MV]

    // Chip wafers are printed by photolithography instead of being laser engraved directly
    allthemods.remove({ id: 'gtceu:laser_engraver/engrave_ram_silicon' })

    // ---- Machine ----
    allthemods.recipes.gtceu.assembler('af9:photolithography_line')
        .itemInputs(
            'gtceu:mv_machine_hull',
            '4x #gtceu:circuits/mv',
            '2x gtceu:mv_emitter',
            '2x gtceu:mv_sensor',
            '2x gtceu:mv_robot_arm',
            '4x gtceu:mv_electric_motor',
            '2x gtceu:mv_electric_pump',
            '4x gtceu:glass_lens',
            '8x gtceu:stainless_steel_plate')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 576))
        .itemOutputs('gtceu:photolithography_line')
        .duration(1200)
        .EUt(EU_MV)

    // ---- Photomasks ----
    // Mask blanks ship pre-coated with resist; the pattern is then written by a laser mask writer
    allthemods.recipes.gtceu.assembler('af9:photomask_blank')
        .itemInputs('gtceu:quartzite_plate', 'gtceu:chromium_plate')
        .inputFluids(Fluid.of('gtceu:photoresist', 100))
        .itemOutputs('kubejs:photomask_blank')
        .duration(400)
        .EUt(EU_MV)

    allthemods.recipes.gtceu.laser_engraver('af9:ram_reticle')
        .itemInputs('kubejs:photomask_blank')
        .notConsumable('#forge:lenses/green')
        .itemOutputs('kubejs:ram_reticle')
        .duration(1800)
        .EUt(EU_MV)

    // ---- Process gases ----
    allthemods.recipes.gtceu.chemical_reactor('af9:extreme_clean_dry_air')
        .notConsumable('gtceu:zeolite_dust')
        .inputFluids(Fluid.of('gtceu:air', 1000))
        .outputFluids(Fluid.of('gtceu:extreme_clean_dry_air', 1000))
        .duration(200)
        .EUt(EU_LV)

    allthemods.recipes.gtceu.chemical_reactor('af9:trimethylchlorosilane')
        .itemInputs('gtceu:magnesium_dust')
        .inputFluids(Fluid.of('gtceu:dimethyldichlorosilane', 1000), Fluid.of('gtceu:chloromethane', 1000))
        .itemOutputs('3x gtceu:magnesium_chloride_dust')
        .outputFluids(Fluid.of('gtceu:trimethylchlorosilane', 1000))
        .duration(300)
        .EUt(EU_MV)

    allthemods.recipes.gtceu.chemical_reactor('af9:hexamethyldisilazane')
        .inputFluids(Fluid.of('gtceu:trimethylchlorosilane', 2000), Fluid.of('gtceu:ammonia', 3000))
        .itemOutputs('4x gtceu:ammonium_chloride_dust')
        .outputFluids(Fluid.of('gtceu:hexamethyldisilazane', 1000))
        .duration(400)
        .EUt(EU_MV)

    allthemods.recipes.gtceu.mixer('af9:hmds_vapor')
        .inputFluids(Fluid.of('gtceu:hexamethyldisilazane', 100), Fluid.of('gtceu:nitrogen', 900))
        .outputFluids(Fluid.of('gtceu:hmds_vapor', 1000))
        .duration(100)
        .EUt(EU_LV)

    // ---- Photoresist ----
    allthemods.recipes.gtceu.chemical_reactor('af9:novolac_resin')
        .inputFluids(Fluid.of('gtceu:phenol', 1000), Fluid.of('gtceu:formaldehyde', 1000))
        .notConsumableFluid(Fluid.of('gtceu:hydrochloric_acid', 100))
        .outputFluids(Fluid.of('gtceu:novolac_resin', 1000), Fluid.of('minecraft:water', 1000))
        .duration(400)
        .EUt(EU_MV)

    allthemods.recipes.gtceu.chemical_reactor('af9:diazonaphthoquinone')
        .inputFluids(Fluid.of('gtceu:naphthalene', 1000), Fluid.of('gtceu:nitric_acid', 1000), Fluid.of('gtceu:ammonia', 1000))
        .itemOutputs('gtceu:diazonaphthoquinone_dust')
        .outputFluids(Fluid.of('minecraft:water', 2000), Fluid.of('gtceu:hydrogen', 1000))
        .duration(600)
        .EUt(EU_MV)

    allthemods.recipes.gtceu.mixer('af9:photoresist')
        .itemInputs('gtceu:diazonaphthoquinone_dust')
        .inputFluids(Fluid.of('gtceu:novolac_resin', 1000), Fluid.of('gtceu:dimethylbenzene', 3000))
        .outputFluids(Fluid.of('gtceu:photoresist', 4000))
        .duration(400)
        .EUt(EU_MV)

    // ---- Developer ----
    allthemods.recipes.gtceu.chemical_reactor('af9:tetramethylammonium_chloride')
        .inputFluids(Fluid.of('gtceu:dimethylamine', 1000), Fluid.of('gtceu:chloromethane', 2000))
        .itemOutputs('gtceu:tetramethylammonium_chloride_dust')
        .outputFluids(Fluid.of('gtceu:hydrochloric_acid', 1000))
        .duration(300)
        .EUt(EU_MV)

    allthemods.recipes.gtceu.chemical_reactor('af9:tmah_developer')
        .itemInputs('gtceu:tetramethylammonium_chloride_dust', '3x gtceu:potassium_hydroxide_dust')
        .inputFluids(Fluid.of('gtceu:distilled_water', 5000))
        .itemOutputs('2x gtceu:rock_salt_dust')
        .outputFluids(Fluid.of('gtceu:tmah_developer', 5000))
        .duration(300)
        .EUt(EU_MV)

    // ---- Wafers ----
    // HMDS prime -> resist coat -> soft bake -> i-line exposure -> PEB -> TMAH develop -> DI rinse -> hard bake
    allthemods.recipes.gtceu.photolithography('af9:ram_wafer_silicon')
        .itemInputs('gtceu:silicon_wafer')
        .notConsumable('kubejs:ram_reticle')
        .inputFluids(
            Fluid.of('gtceu:hmds_vapor', 50),
            Fluid.of('gtceu:photoresist', 100),
            Fluid.of('gtceu:tmah_developer', 250),
            Fluid.of('gtceu:distilled_water', 1000),
            Fluid.of('gtceu:extreme_clean_dry_air', 1000))
        .itemOutputs('gtceu:ram_wafer')
        .duration(900)
        .EUt(EU_MV, 4)
})
