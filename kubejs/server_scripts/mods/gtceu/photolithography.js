// AF9 - Photolithography Line recipes (machine, materials and items: startup_scripts/gtceu/photolithography.js)

ServerEvents.recipes(allthemods => {
    const EU_LV = GTValues.VA[GTValues.LV]
    const EU_MV = GTValues.VA[GTValues.MV]
    const EU_HV = GTValues.VA[GTValues.HV]
    const EU_EV = GTValues.VA[GTValues.EV]

    // The MV silicon chips. cut / cutEUt / cleanroom mirror GT's own cutter recipes; lens is GT's engraving lens colour
    const chips = [
        { id: 'ilc', wafer: 'gtceu:ilc_wafer', chip: 'gtceu:ilc_chip', lens: 'red', engrave: 'engrave_ilc_silicon', cut: 8, cutEUt: 64, cleanroom: false },
        { id: 'ram', wafer: 'gtceu:ram_wafer', chip: 'gtceu:ram_chip', lens: 'green', engrave: 'engrave_ram_silicon', cut: 32, cutEUt: 96, cleanroom: false },
        { id: 'cpu', wafer: 'gtceu:cpu_wafer', chip: 'gtceu:cpu_chip', lens: 'light_blue', engrave: 'engrave_cpu_silicon', cut: 8, cutEUt: 120, cleanroom: false },
        { id: 'ulpic', wafer: 'gtceu:ulpic_wafer', chip: 'gtceu:ulpic_chip', lens: 'blue', engrave: 'engrave_ulpic_silicon', cut: 6, cutEUt: 120, cleanroom: false },
        { id: 'lpic', wafer: 'gtceu:lpic_wafer', chip: 'gtceu:lpic_chip', lens: 'orange', engrave: 'engrave_lpic_silicon', cut: 4, cutEUt: 480, cleanroom: true },
        { id: 'simple_soc', wafer: 'gtceu:simple_soc_wafer', chip: 'gtceu:simple_soc', lens: 'cyan', engrave: 'engrave_ssoc_silicon', cut: 6, cutEUt: 64, cleanroom: false }
    ]

    // Silicon chip wafers are printed by photolithography instead of being laser engraved directly
    chips.forEach(chip => allthemods.remove({ id: `gtceu:laser_engraver/${chip.engrave}` }))

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

    chips.forEach(chip => {
        allthemods.recipes.gtceu.laser_engraver(`af9:${chip.id}_reticle`)
            .itemInputs('kubejs:photomask_blank')
            .notConsumable(`#forge:lenses/${chip.lens}`)
            .itemOutputs(`kubejs:${chip.id}_reticle`)
            .duration(1800)
            .EUt(EU_MV)
    })

    // ---- Upgrade modules ----
    // Mk II: KrF excimer laser source (248 nm) with fused-silica optics, filled with krypton laser gas
    allthemods.recipes.gtceu.assembler('af9:krf_excimer_laser_module')
        .itemInputs(
            'gtceu:hv_machine_hull',
            '4x gtceu:hv_emitter',
            '2x #gtceu:circuits/hv',
            '2x gtceu:hv_electric_pump',
            '4x gtceu:quartzite_plate',
            '8x gtceu:stainless_steel_plate')
        .inputFluids(Fluid.of('gtceu:krypton', 1000))
        .itemOutputs('kubejs:krf_excimer_laser_module')
        .duration(1200)
        .EUt(EU_HV)

    // Mk III: second wafer stage, so one wafer is measured while the other is exposed
    allthemods.recipes.gtceu.assembler('af9:twin_stage_scanner_module')
        .itemInputs(
            'gtceu:ev_machine_hull',
            '4x gtceu:ev_robot_arm',
            '4x gtceu:ev_sensor',
            '4x gtceu:ev_electric_motor',
            '2x #gtceu:circuits/ev',
            '8x gtceu:titanium_plate')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 1152))
        .itemOutputs('kubejs:twin_stage_scanner_module')
        .duration(1600)
        .EUt(EU_EV)

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
    // HMDS prime -> resist coat -> soft bake -> exposure -> PEB -> TMAH develop -> DI rinse -> hard bake
    // Each Mk halves time and fluids and draws 4A one voltage tier higher. The AF9 Core controller only runs
    // recipes whose af9_litho_tier matches its operating Mk.
    const tiers = [
        { tier: 1, eut: EU_MV, output: chip => chip.wafer },
        { tier: 2, eut: EU_HV, output: chip => `kubejs:${chip.id}_wafer_high_grade` },
        { tier: 3, eut: EU_EV, output: chip => `kubejs:${chip.id}_wafer_premium` }
    ]
    chips.forEach(chip => {
        tiers.forEach(t => {
            const divisor = Math.pow(2, t.tier - 1)
            allthemods.recipes.gtceu.photolithography(`af9:${chip.id}_wafer_mk${t.tier}`)
                .itemInputs('gtceu:silicon_wafer')
                .notConsumable(`kubejs:${chip.id}_reticle`)
                .inputFluids(
                    Fluid.of('gtceu:hmds_vapor', 40 / divisor),
                    Fluid.of('gtceu:photoresist', 100 / divisor),
                    Fluid.of('gtceu:tmah_developer', 200 / divisor),
                    Fluid.of('gtceu:distilled_water', 1000 / divisor),
                    Fluid.of('gtceu:extreme_clean_dry_air', 1000 / divisor))
                .itemOutputs(t.output(chip))
                // NBT tag, not a plain number: addData has int/long/float overloads Rhino cannot choose between
                .addData('af9_litho_tier', NBT.intTag(t.tier))
                .duration(900 / divisor)
                .EUt(t.eut, 4)
        })
    })

    // ---- Graded wafer cutting ----
    // Better wafers yield more chips. Mirrors the three fluid variants GT generates for its own cutter recipes
    // (KubeJS recipes skip that generator), using GT's formulas.
    const clamp = (value, min, max) => Math.min(Math.max(value, min), max)
    const grades = [
        { suffix: 'high_grade', factor: 1.25 },
        { suffix: 'premium', factor: 1.5 }
    ]
    chips.forEach(chip => {
        grades.forEach(grade => {
            const count = Math.round(chip.cut * grade.factor)
            const totalEU = 900 * chip.cutEUt
            const variants = [
                { id: '', fluid: Fluid.of('gtceu:lubricant', clamp(Math.floor(totalEU / 1280), 1, 250)), duration: 900 },
                { id: '_distilled_water', fluid: Fluid.of('gtceu:distilled_water', clamp(Math.floor(totalEU / 426), 3, 750)), duration: 1350 },
                { id: '_water', fluid: Fluid.of('minecraft:water', clamp(Math.floor(totalEU / 320), 4, 1000)), duration: 1800 }
            ]
            variants.forEach(variant => {
                const recipe = allthemods.recipes.gtceu.cutter(`af9:cut_${chip.id}_${grade.suffix}${variant.id}`)
                    .itemInputs(`kubejs:${chip.id}_wafer_${grade.suffix}`)
                    .inputFluids(variant.fluid)
                    .itemOutputs(`${count}x ${chip.chip}`)
                    .duration(variant.duration)
                    .EUt(chip.cutEUt)
                if (chip.cleanroom) recipe.cleanroom(CleanroomType.CLEANROOM)
            })
        })
    })
})
