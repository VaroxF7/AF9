// AF9 - Photolithography Line and Orbital Lithography Station recipes (machines, materials and items:
// startup_scripts/gtceu/photolithography.js and wafers.js). Spec and numbers: docs/semiconductor-factory.md

// Shared with the other server scripts (server scripts share one scope). Must stay in sync with
// com.af9.core.litho.LithoMode in af9-core and AF9_WAFER_TABLE in startup_scripts/gtceu/wafers.js.
const AF9_WAFERS = (() => {
    // The nine substrates, lowest first. blank = the substrate wafer, tier = voltage of its lithography mode
    const substrates = [
        { id: 'silicon', blank: 'gtceu:silicon_wafer', tier: GTValues.MV, reclaim: 'gtceu:small_silicon_dust' },
        { id: 'phosphorus', blank: 'gtceu:phosphorus_wafer', tier: GTValues.HV, reclaim: 'gtceu:small_silicon_dust' },
        { id: 'naquadah', blank: 'gtceu:naquadah_wafer', tier: GTValues.EV, reclaim: 'gtceu:small_silicon_dust' },
        { id: 'trinium', blank: 'kubejs:trinium_wafer', tier: GTValues.IV, reclaim: 'gtceu:small_silicon_dust' },
        { id: 'naquadria', blank: 'kubejs:naquadria_wafer', tier: GTValues.LuV, reclaim: 'gtceu:small_silicon_dust' },
        { id: 'neutronium', blank: 'gtceu:neutronium_wafer', tier: GTValues.ZPM, reclaim: 'gtceu:small_silicon_dust' },
        { id: 'transmuted_neutronium', blank: 'kubejs:transmuted_neutronium_wafer', tier: GTValues.UV,
            reclaim: 'gtceu:small_silicon_dust' },
        { id: 'strange_matter', blank: 'kubejs:strange_matter_wafer', tier: GTValues.UHV,
            reclaim: 'gtceu:small_silicon_dust' },
        { id: 'chromodynium', blank: 'kubejs:chromodynium_wafer', tier: GTValues.UHV,
            reclaim: 'gtceu:small_chromodynium_dust' }
    ]
    substrates.forEach((s, index) => s.index = index)

    // One mode per substrate. baseBreak: break chance at a clean vacuum, of 10000 (= LithoMode.baseBreak; shown as the
    // chanced broken wafer, the break roll itself is AF9 Core's). light: the exposure tool the real node used; resist:
    // the photoresist made for that light; laserGas: the excimer premix; immersion: water film under the lens; highK:
    // HfO2 gate dielectric from hafnium tetrachloride; euv: tin-plasma source (molten tin + hydrogen buffer gas).
    const modes = [
        { id: '350nm', substrate: 0, resist: 'gtceu:photoresist', baseBreak: 200 },
        { id: '200nm', substrate: 1, resist: 'gtceu:krf_photoresist', laserGas: 'gtceu:krf_excimer_gas', baseBreak: 300 },
        { id: '100nm', substrate: 2, resist: 'gtceu:arf_photoresist', laserGas: 'gtceu:arf_excimer_gas', baseBreak: 500 },
        { id: '80nm', substrate: 3, resist: 'gtceu:arf_photoresist', laserGas: 'gtceu:arf_excimer_gas', baseBreak: 700 },
        { id: '65nm', substrate: 4, resist: 'gtceu:arf_photoresist', laserGas: 'gtceu:arf_excimer_gas', immersion: true,
            baseBreak: 900 },
        { id: '50nm', substrate: 5, resist: 'gtceu:arf_photoresist', laserGas: 'gtceu:arf_excimer_gas', immersion: true,
            highK: true, baseBreak: 1200 },
        { id: '20nm', substrate: 6, resist: 'gtceu:euv_photoresist', euv: true, highK: true, baseBreak: 1800 },
        { id: '7nm', substrate: 7, resist: 'gtceu:euv_photoresist', euv: true, highK: true, baseBreak: 2500 }
    ]
    modes.forEach((m, index) => m.index = index)
    const orbital = { id: '1nm', substrate: 8, baseBreak: 3500 }

    // Every GT chip wafer. native = index of the chip's own substrate (GT's: silicon, phosphorus, naquadah for ASoC,
    // neutronium for HASoC). reticle: the photomask (derived wafers are made from printed ones instead); lens: the GT
    // lens colour that engraves the reticle. chip / cut / cutEUt / cleanroom mirror GT's cutter recipes.
    const chips = [
        { id: 'ilc', native: 0, reticle: 'ilc', lens: 'red', engrave: 'engrave_ilc', chip: 'gtceu:ilc_chip', cut: 8, cutEUt: 64, cleanroom: false },
        { id: 'ram', native: 0, reticle: 'ram', lens: 'green', engrave: 'engrave_ram', chip: 'gtceu:ram_chip', cut: 32, cutEUt: 96, cleanroom: false },
        { id: 'cpu', native: 0, reticle: 'cpu', lens: 'light_blue', engrave: 'engrave_cpu', chip: 'gtceu:cpu_chip', cut: 8, cutEUt: 120, cleanroom: false },
        { id: 'ulpic', native: 0, reticle: 'ulpic', lens: 'blue', engrave: 'engrave_ulpic', chip: 'gtceu:ulpic_chip', cut: 6, cutEUt: 120, cleanroom: false },
        { id: 'lpic', native: 0, reticle: 'lpic', lens: 'orange', engrave: 'engrave_lpic', chip: 'gtceu:lpic_chip', cut: 4, cutEUt: 480, cleanroom: true },
        { id: 'simple_soc', native: 0, reticle: 'simple_soc', lens: 'cyan', engrave: 'engrave_ssoc', chip: 'gtceu:simple_soc', cut: 6, cutEUt: 64, cleanroom: false },
        { id: 'nand_memory', native: 1, reticle: 'nand', lens: 'gray', engrave: 'engrave_nand', chip: 'gtceu:nand_memory_chip', cut: 32, cutEUt: 192, cleanroom: true },
        { id: 'nor_memory', native: 1, reticle: 'nor', lens: 'pink', engrave: 'engrave_nor', chip: 'gtceu:nor_memory_chip', cut: 16, cutEUt: 192, cleanroom: true },
        { id: 'mpic', native: 1, reticle: 'mpic', lens: 'brown', engrave: 'engrave_pic', chip: 'gtceu:mpic_chip', cut: 4, cutEUt: 1920, cleanroom: true },
        { id: 'soc', native: 1, reticle: 'soc', lens: 'yellow', engrave: 'engrave_soc', chip: 'gtceu:soc', cut: 6, cutEUt: 480, cleanroom: true },
        { id: 'advanced_soc', native: 2, reticle: 'advanced_soc', lens: 'purple', engrave: 'engrave_asoc', chip: 'gtceu:advanced_soc', cut: 6, cutEUt: 1920, cleanroom: true },
        { id: 'highly_advanced_soc', native: 5, reticle: 'highly_advanced_soc', lens: 'black', engrave: 'engrave_hasoc', chip: 'gtceu:highly_advanced_soc', cut: 6, cutEUt: 7680, cleanroom: true },
        { id: 'nano_cpu', native: 0, from: 'cpu', chip: 'gtceu:nano_cpu_chip', cut: 8, cutEUt: 480, cleanroom: true },
        { id: 'qbit_cpu', native: 0, from: 'nano_cpu', chip: 'gtceu:qbit_cpu_chip', cut: 4, cutEUt: 1920, cleanroom: true },
        { id: 'hpic', native: 1, from: 'mpic', chip: 'gtceu:hpic_chip', cut: 2, cutEUt: 7680, cleanroom: true },
        { id: 'uhpic', native: 1, from: 'hpic', chip: 'gtceu:uhpic_chip', cut: 2, cutEUt: 30720, cleanroom: true }
    ]
    const chip = id => {
        const found = chips.filter(c => c.id === id)[0]
        if (!found) throw new Error(`AF9_WAFERS: unknown chip '${id}'`)
        return found
    }
    // The chip wafer printed on a substrate (index): GT's own wafer on the chip's own substrate, kubejs:<substrate>_
    // <chip>_wafer on a higher one, null on a lower one
    const printed = (substrateIndex, c) => {
        if (substrateIndex < c.native) return null
        if (substrateIndex === c.native) return `gtceu:${c.id}_wafer`
        return `kubejs:${substrates[substrateIndex].id}_${c.id}_wafer`
    }
    // Chips a wafer cuts into: GT's count, +1x per substrate above the chip's own (smaller dies), at most 128 (the
    // single-block cutter's two output slots)
    const dies = (substrateIndex, c) => Math.min(128, c.cut * (1 + substrateIndex - c.native))
    // Every wafer item of a substrate (blank + printed), for the contamination tags
    const wafersOf = substrateIndex => [substrates[substrateIndex].blank]
        .concat(chips.map(c => printed(substrateIndex, c)).filter(id => id))
    // Plain chip stack by chip or old reticle id (the circuit scripts use 'nand', 'nor' ...)
    const chipStack = (id, count) => {
        const found = chips.filter(c => c.id === id || c.reticle === id)[0]
        if (!found) throw new Error(`AF9_WAFERS: unknown chip '${id}'`)
        return `${count || 1}x ${found.chip}`
    }
    return { substrates: substrates, modes: modes, orbital: orbital, chips: chips, chip: chip, printed: printed,
        dies: dies, wafersOf: wafersOf, chipStack: chipStack,
        reticles: chips.filter(c => c.reticle).map(c => ({ id: c.reticle, lens: c.lens })) }
})()

// Contamination (af9-core WaferContamination): every wafer of a substrate, the gloves that protect
ServerEvents.tags('item', allthemods => {
    AF9_WAFERS.substrates.forEach(s => {
        const wafers = AF9_WAFERS.wafersOf(s.index)
        allthemods.add(`af9:wafers/${s.id}`, wafers)
        allthemods.add('af9:wafers', wafers)
    })
    allthemods.add('af9:wafer_gloves', ['gtceu:rubber_gloves', 'gtceu:hazmat_chestpiece'])
})

ServerEvents.recipes(allthemods => {
    const EU_LV = GTValues.VA[GTValues.LV]
    const EU_MV = GTValues.VA[GTValues.MV]
    const VA = GTValues.VA
    const substrates = AF9_WAFERS.substrates
    const modes = AF9_WAFERS.modes
    const chips = AF9_WAFERS.chips
    const printed = AF9_WAFERS.printed

    // ---- Removed GT paths: the chip wafers come from the lithography machines ----
    // Except one bootstrap: MV Energy Hatches need a ULPIC chip and the line needs MV Energy Hatches, so GT's plain
    // ULPIC engraving on silicon stays (like the chip-free Good Electronic Circuit). GT's derived-wafer chemistry
    // (Nano CPU, Qubit CPU, HPIC, UHPIC) and GT's cutter recipes of its own wafers stay as they are.
    const gtSubstrates = ['silicon', 'phosphorus', 'naquadah', 'neutronium']
    chips.filter(c => c.engrave).forEach(c => gtSubstrates.forEach(s => {
        if (c.id === 'ulpic' && s === 'silicon') return
        allthemods.remove({ id: `gtceu:laser_engraver/${c.engrave}_${s}` })
    }))

    // ---- Machines ----
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

    // Light sources of the line's versions (version 1 uses GT's purple lamp as its mercury lamp). An excimer laser: a
    // discharge chamber filled with the gas premix, a pulsed power supply, UV optics and a gas circulation pump. KrF
    // allows line version 2, ArF up to 6, the EUV source up to 8 (plus the lens slices).
    allthemods.recipes.gtceu.assembler('af9:krf_excimer_laser')
        .itemInputs('gtceu:hv_machine_hull', '2x gtceu:hv_emitter', '4x #gtceu:circuits/hv', '2x gtceu:glass_lens',
            '4x gtceu:stainless_steel_plate', 'gtceu:hv_electric_pump')
        .inputFluids(Fluid.of('gtceu:krf_excimer_gas', 4000))
        .itemOutputs('kubejs:krf_excimer_laser')
        .duration(1200)
        .EUt(VA[GTValues.HV])
    allthemods.recipes.gtceu.assembler('af9:arf_excimer_laser')
        .itemInputs('gtceu:ev_machine_hull', '2x gtceu:ev_emitter', '4x #gtceu:circuits/ev', '4x gtceu:glass_lens',
            '4x gtceu:titanium_plate', 'gtceu:ev_electric_pump')
        .inputFluids(Fluid.of('gtceu:arf_excimer_gas', 4000))
        .itemOutputs('kubejs:arf_excimer_laser')
        .duration(1200)
        .EUt(VA[GTValues.EV])
    // Laser-produced plasma: a CO2 drive laser hits tin droplets 50,000 times a second; a multilayer collector mirror
    // gathers the 13.5 nm light
    allthemods.recipes.gtceu.assembler('af9:euv_light_source')
        .itemInputs('gtceu:uv_machine_hull', '4x gtceu:uv_emitter', '4x #gtceu:circuits/uv', '8x gtceu:glass_lens',
            '2x gtceu:uv_electric_pump', '8x gtceu:neutronium_plate')
        .inputFluids(Fluid.of('gtceu:tin', 2304))
        .itemOutputs('kubejs:euv_light_source')
        .duration(2400)
        .EUt(VA[GTValues.UV])

    // Orbital Lithography Station and its casings (built on the ground, runs only in orbit)
    allthemods.recipes.gtceu.assembler('af9:orbital_lithography_station')
        .itemInputs('gtceu:uhv_machine_hull', '4x gtceu:uv_emitter', '4x gtceu:uv_field_generator',
            '4x #gtceu:circuits/uhv', '4x gtceu:uv_sensor', '4x gtceu:uv_robot_arm', '16x gtceu:chromodynium_plate')
        .inputFluids(Fluid.of('gtceu:supercooled_endion', 4000))
        .itemOutputs('gtceu:orbital_lithography_station')
        .duration(4000)
        .EUt(VA[GTValues.UHV])
    allthemods.recipes.gtceu.assembler('af9:orbital_frame_casing')
        .itemInputs('4x gtceu:neutronium_plate', 'gtceu:neutronium_frame')
        .itemOutputs('2x kubejs:orbital_frame_casing')
        .duration(100)
        .EUt(VA[GTValues.UV])
    allthemods.recipes.gtceu.assembler('af9:xfel_undulator')
        .itemInputs('gtceu:uv_emitter', '2x gtceu:uv_field_generator', '4x gtceu:neutronium_plate')
        .inputFluids(Fluid.of('gtceu:supercooled_hydrogen', 1000))
        .itemOutputs('kubejs:xfel_undulator')
        .duration(600)
        .EUt(VA[GTValues.UV])
    allthemods.recipes.gtceu.assembler('af9:maglev_wafer_stage')
        .itemInputs('gtceu:uv_robot_arm', '2x gtceu:uv_electric_motor', '4x gtceu:neutronium_plate', 'gtceu:uv_sensor')
        .itemOutputs('kubejs:maglev_wafer_stage')
        .duration(600)
        .EUt(VA[GTValues.UV])

    // ---- Photomasks ----
    // Mask blanks ship pre-coated with resist; the pattern is then written by a laser mask writer
    allthemods.recipes.gtceu.assembler('af9:photomask_blank')
        .itemInputs('gtceu:quartzite_plate', 'gtceu:chromium_plate')
        .inputFluids(Fluid.of('gtceu:photoresist', 100))
        .itemOutputs('kubejs:photomask_blank')
        .duration(400)
        .EUt(EU_MV)

    AF9_WAFERS.reticles.forEach(c => {
        allthemods.recipes.gtceu.laser_engraver(`af9:${c.id}_reticle`)
            .itemInputs('kubejs:photomask_blank')
            .notConsumable(`#forge:lenses/${c.lens}`)
            .itemOutputs(`kubejs:${c.id}_reticle`)
            .duration(1800)
            .EUt(EU_MV)
    })

    // ---- Extreme clean dry air (XCDA) ----
    // A fab's clean-dry-air plant, the purification mode of the SMC fab machines: oxidize -> scrub CO2 -> dry ->
    // cryo-cool -> filter.
    // Everything runs at MV, slowly (the expansion cooler recycles 3/4 of its air). HV adds a platinum oxidizer, a
    // caustic scrubber and liquid-air cooling, which together are much faster.
    const EU_HV = GTValues.VA[GTValues.HV]

    // Cu + 2 MnO2 + O -> CuMn2O4
    allthemods.recipes.gtceu.fab_synthesis('af9:hopcalite')
        .itemInputs('gtceu:copper_dust', '6x gtceu:pyrolusite_dust')
        .inputFluids(Fluid.of('gtceu:oxygen', 1000))
        .itemOutputs('7x gtceu:hopcalite_dust')
        .duration(400)
        .EUt(EU_MV)

    // zeolite crystallised with a bentonite binder into sieve beads (clay + distilled water is GT's clay recipe)
    allthemods.recipes.gtceu.fab_wet_processing('af9:molecular_sieve')
        .itemInputs('4x gtceu:zeolite_dust', 'gtceu:bentonite_dust')
        .inputFluids(Fluid.of('gtceu:distilled_water', 500))
        .itemOutputs('4x kubejs:molecular_sieve')
        .duration(600)
        .EUt(EU_MV)

    // temperature-swing regeneration
    allthemods.smelting('kubejs:molecular_sieve', 'kubejs:saturated_molecular_sieve').id('af9:regenerate_molecular_sieve')

    // 1. catalytic oxidation: CO, H2 and hydrocarbons -> CO2 + H2O
    allthemods.recipes.gtceu.fab_purification('af9:xcda_oxidize_hopcalite')
        .notConsumable('gtceu:hopcalite_dust')
        .inputFluids(Fluid.of('gtceu:air', 4000))
        .outputFluids(Fluid.of('gtceu:oxidized_air', 4000))
        .duration(600)
        .EUt(EU_MV)

    allthemods.recipes.gtceu.fab_purification('af9:xcda_oxidize_platinum')
        .notConsumable('gtceu:platinum_dust')
        .inputFluids(Fluid.of('gtceu:air', 4000))
        .outputFluids(Fluid.of('gtceu:oxidized_air', 4000))
        .duration(150)
        .EUt(EU_HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // 2. CO2 scrubbing: Ca(OH)2 + CO2 -> CaCO3 + H2O, or 2 NaOH + CO2 -> Na2CO3 + H2O
    allthemods.recipes.gtceu.fab_purification('af9:xcda_scrub_lime')
        .itemInputs('gtceu:small_calcium_hydroxide_dust')
        .inputFluids(Fluid.of('gtceu:oxidized_air', 4000))
        .itemOutputs('gtceu:small_calcite_dust')
        .outputFluids(Fluid.of('gtceu:decarbonated_air', 4000))
        .duration(400)
        .EUt(EU_MV)

    allthemods.recipes.gtceu.fab_purification('af9:xcda_scrub_caustic')
        .itemInputs('gtceu:small_sodium_hydroxide_dust')
        .inputFluids(Fluid.of('gtceu:oxidized_air', 4000))
        .itemOutputs('gtceu:small_soda_ash_dust')
        .outputFluids(Fluid.of('gtceu:decarbonated_air', 4000))
        .duration(100)
        .EUt(EU_HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // 3. drying: the sieve adsorbs the water
    allthemods.recipes.gtceu.fab_purification('af9:xcda_dry')
        .itemInputs('kubejs:molecular_sieve')
        .inputFluids(Fluid.of('gtceu:decarbonated_air', 4000))
        .itemOutputs('kubejs:saturated_molecular_sieve')
        .outputFluids(Fluid.of('gtceu:dry_air', 4000))
        .duration(400)
        .EUt(EU_MV)

    // 4. cryogenic cooling. MV: Joule-Thomson expansion, only a quarter gets cold enough, the rest goes round again.
    // HV: pre-cooled with liquid air, which boils back into ordinary air.
    allthemods.recipes.gtceu.fab_purification('af9:xcda_cool_expansion')
        .circuit(1)
        .inputFluids(Fluid.of('gtceu:dry_air', 4000))
        .outputFluids(Fluid.of('gtceu:cryogenic_supercooled_air', 1000), Fluid.of('gtceu:dry_air', 3000))
        .duration(800)
        .EUt(EU_MV)

    allthemods.recipes.gtceu.fab_purification('af9:xcda_cool_liquid_air')
        .circuit(2)
        .inputFluids(Fluid.of('gtceu:dry_air', 4000), Fluid.of('gtceu:liquid_air', 1000))
        .outputFluids(Fluid.of('gtceu:cryogenic_supercooled_air', 4000), Fluid.of('gtceu:air', 1000))
        .duration(200)
        .EUt(EU_HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // 5. re-warmed through a membrane filter; the last traces stayed frozen in the cold box
    allthemods.recipes.gtceu.fab_purification('af9:xcda_filter')
        .notConsumable('gtceu:fluid_filter')
        .inputFluids(Fluid.of('gtceu:cryogenic_supercooled_air', 4000))
        .outputFluids(Fluid.of('gtceu:extreme_clean_dry_air', 4000))
        .duration(200)
        .EUt(EU_MV)

    allthemods.recipes.gtceu.fab_synthesis('af9:trimethylchlorosilane')
        .itemInputs('gtceu:magnesium_dust')
        .inputFluids(Fluid.of('gtceu:dimethyldichlorosilane', 1000), Fluid.of('gtceu:chloromethane', 1000))
        .itemOutputs('3x gtceu:magnesium_chloride_dust')
        .outputFluids(Fluid.of('gtceu:trimethylchlorosilane', 1000))
        .duration(300)
        .EUt(EU_MV)

    allthemods.recipes.gtceu.fab_synthesis('af9:hexamethyldisilazane')
        .inputFluids(Fluid.of('gtceu:trimethylchlorosilane', 2000), Fluid.of('gtceu:ammonia', 3000))
        .itemOutputs('4x gtceu:ammonium_chloride_dust')
        .outputFluids(Fluid.of('gtceu:hexamethyldisilazane', 1000))
        .duration(400)
        .EUt(EU_MV)

    allthemods.recipes.gtceu.fab_blending('af9:hmds_vapor')
        .inputFluids(Fluid.of('gtceu:hexamethyldisilazane', 100), Fluid.of('gtceu:nitrogen', 900))
        .outputFluids(Fluid.of('gtceu:hmds_vapor', 1000))
        .duration(100)
        .EUt(EU_LV)

    // ---- Photoresist (i-line, MUV) ----
    // Formox process: CH3OH + 1/2 O2 -> CH2O + H2O with air over iron molybdate. GT's own formaldehyde recipe (silver
    // catalyst) is HV, which MUV cannot wait for. The catalyst: molybdenite roasted to MoO3, calcined with hematite.
    allthemods.recipes.gtceu.fab_calcination('af9:molybdenum_trioxide')
        .itemInputs('gtceu:molybdenite_dust')
        .inputFluids(Fluid.of('gtceu:oxygen', 7000))
        .itemOutputs('gtceu:molybdenum_trioxide_dust')
        .outputFluids(Fluid.of('gtceu:sulfur_dioxide', 2000))
        .blastFurnaceTemp(900)
        .duration(300)
        .EUt(EU_MV)

    // Fe2O3 + 3 MoO3 -> Fe2(MoO4)3
    allthemods.recipes.gtceu.fab_calcination('af9:iron_molybdate')
        .itemInputs('gtceu:hematite_dust', '3x gtceu:molybdenum_trioxide_dust')
        .itemOutputs('gtceu:iron_molybdate_dust')
        .blastFurnaceTemp(800)
        .duration(300)
        .EUt(EU_MV)

    allthemods.recipes.gtceu.fab_synthesis('af9:formaldehyde_formox')
        .notConsumable('gtceu:iron_molybdate_dust')
        .inputFluids(Fluid.of('gtceu:methanol', 1000), Fluid.of('gtceu:air', 3000))
        .outputFluids(Fluid.of('gtceu:formaldehyde', 1000), Fluid.of('minecraft:water', 1000))
        .duration(200)
        .EUt(EU_MV)

    allthemods.recipes.gtceu.fab_synthesis('af9:novolac_resin')
        .inputFluids(Fluid.of('gtceu:phenol', 1000), Fluid.of('gtceu:formaldehyde', 1000))
        .notConsumableFluid(Fluid.of('gtceu:hydrochloric_acid', 100))
        .outputFluids(Fluid.of('gtceu:novolac_resin', 1000), Fluid.of('minecraft:water', 1000))
        .duration(400)
        .EUt(EU_MV)

    allthemods.recipes.gtceu.fab_synthesis('af9:diazonaphthoquinone')
        .inputFluids(Fluid.of('gtceu:naphthalene', 1000), Fluid.of('gtceu:nitric_acid', 1000), Fluid.of('gtceu:ammonia', 1000))
        .itemOutputs('gtceu:diazonaphthoquinone_dust')
        .outputFluids(Fluid.of('minecraft:water', 2000), Fluid.of('gtceu:hydrogen', 1000))
        .duration(600)
        .EUt(EU_MV)

    allthemods.recipes.gtceu.fab_blending('af9:photoresist')
        .itemInputs('gtceu:diazonaphthoquinone_dust')
        .inputFluids(Fluid.of('gtceu:novolac_resin', 1000), Fluid.of('gtceu:dimethylbenzene', 3000))
        .outputFluids(Fluid.of('gtceu:photoresist', 4000))
        .duration(400)
        .EUt(EU_MV)

    // ---- Developer ----
    allthemods.recipes.gtceu.fab_synthesis('af9:tetramethylammonium_chloride')
        .inputFluids(Fluid.of('gtceu:dimethylamine', 1000), Fluid.of('gtceu:chloromethane', 2000))
        .itemOutputs('gtceu:tetramethylammonium_chloride_dust')
        .outputFluids(Fluid.of('gtceu:hydrochloric_acid', 1000))
        .duration(300)
        .EUt(EU_MV)

    // Membrane electrolysis, as electronic-grade TMAH is made: no potassium or sodium may reach the developer (metal
    // ions shift transistor thresholds). (CH3)4NCl + H2O -> (CH3)4NOH + 1/2 H2 + 1/2 Cl2
    allthemods.recipes.gtceu.fab_blending('af9:tetramethylammonium_chloride_solution')
        .itemInputs('gtceu:tetramethylammonium_chloride_dust')
        .inputFluids(Fluid.of('gtceu:distilled_water', 5000))
        .outputFluids(Fluid.of('gtceu:tetramethylammonium_chloride_solution', 5000))
        .duration(100)
        .EUt(EU_LV)

    allthemods.recipes.gtceu.fab_electrolysis('af9:tmah_developer')
        .inputFluids(Fluid.of('gtceu:tetramethylammonium_chloride_solution', 5000))
        .outputFluids(Fluid.of('gtceu:tmah_developer', 5000), Fluid.of('gtceu:chlorine', 1000), Fluid.of('gtceu:hydrogen', 1000))
        .duration(300)
        .EUt(EU_MV)


    // ---- Metal-oxide EUV resist (20 nm, 7 nm) and the orbital station's dry resist ----
    // Sn + 2 Cl2 -> SnCl4
    allthemods.recipes.gtceu.fab_synthesis('af9:tin_tetrachloride')
        .itemInputs('gtceu:tin_dust')
        .inputFluids(Fluid.of('gtceu:chlorine', 4000))
        .outputFluids(Fluid.of('gtceu:tin_tetrachloride', 1000))
        .duration(300)
        .EUt(VA[GTValues.EV])

    // Hydrolysed with methacrylic acid into tin-oxo methacrylate clusters, dissolved in PGMEA
    allthemods.recipes.gtceu.fab_synthesis('af9:euv_photoresist')
        .inputFluids(Fluid.of('gtceu:tin_tetrachloride', 1000), Fluid.of('gtceu:methacrylic_acid', 2000),
            Fluid.of('gtceu:propylene_glycol_methyl_ether_acetate', 4000), Fluid.of('gtceu:ultrapure_water', 1000))
        .outputFluids(Fluid.of('gtceu:euv_photoresist', 4000), Fluid.of('gtceu:hydrochloric_acid', 2000))
        .duration(600)
        .EUt(VA[GTValues.IV])
        .cleanroom(CleanroomType.CLEANROOM)

    // The same clusters as a vapour-deposition source (no spin coating without gravity)
    allthemods.recipes.gtceu.fab_cvd('af9:dry_resist_cartridge')
        .itemInputs('gtceu:tungsten_steel_plate')
        .inputFluids(Fluid.of('gtceu:euv_photoresist', 1000))
        .itemOutputs('kubejs:dry_resist_cartridge')
        .blastFurnaceTemp(600)
        .duration(400)
        .EUt(VA[GTValues.UV])
        .cleanroom(CleanroomType.CLEANROOM)

    // ---- Printed wafers (Photolithography Line) ----
    // HMDS prime -> resist coat -> soft bake -> exposure -> PEB -> TMAH develop -> DI rinse -> hard bake, 4A of the
    // mode's tier, each step 1.5x the chemicals. The resist is the one made for the mode's light; the excimer lasers
    // burn their premix; immersion modes expose through ultrapure water; from 50 nm a high-k HfO2 gate is grown from
    // HfCl4 + water (ALD); the EUV modes burn tin droplets in a hydrogen buffer.
    // Output: the printed wafer, and the broken wafer at the mode's base break chance for the recipe viewers. The
    // machine takes the chanced broken wafer out and rolls the real break chance (node, vacuum cleanliness, line
    // version) when the wafer is done (af9-core LithoMachine).
    modes.forEach(m => {
        const s = substrates[m.substrate]
        const chemicals = Math.pow(1.5, m.index)
        const fluids = [
            Fluid.of('gtceu:hmds_vapor', Math.round(40 * chemicals)),
            Fluid.of(m.resist, Math.round(100 * chemicals)),
            Fluid.of('gtceu:tmah_developer', Math.round(200 * chemicals)),
            Fluid.of('gtceu:distilled_water', Math.round(1000 * chemicals)),
            Fluid.of('gtceu:extreme_clean_dry_air', Math.round(1000 * chemicals))]
        if (m.laserGas) fluids.push(Fluid.of(m.laserGas, Math.round(10 * chemicals)))
        if (m.immersion) fluids.push(Fluid.of('gtceu:ultrapure_water', 1000))
        if (m.euv) {
            fluids.push(Fluid.of('gtceu:tin', 144))
            fluids.push(Fluid.of('gtceu:hydrogen', 1000))
        }
        if (m.highK) fluids.push(Fluid.of('gtceu:hafnium_tetrachloride', 100))
        chips.filter(c => c.reticle && c.native <= m.substrate).forEach(c => {
            allthemods.recipes.gtceu[`lithography_${m.id}`](`af9:print_${c.id}_${m.id}`)
                .itemInputs(s.blank)
                .notConsumable(`kubejs:${c.reticle}_reticle`)
                .inputFluids(fluids)
                .itemOutputs(printed(m.substrate, c))
                .chancedOutput(`kubejs:broken_${s.id}_wafer`, m.baseBreak, 0)
                .duration(900)
                .EUt(VA[s.tier], 4)
        })
    })

    // ---- Printed wafers (Orbital Lithography Station, 1 nm) ----
    // X-ray FEL, dry resist, supercooled endion for the undulator and the stage; 50A of UHV for eight times the line's
    // run time: a hundred times the energy of a 7 nm print
    const chromodynium = substrates[AF9_WAFERS.orbital.substrate]
    chips.filter(c => c.reticle).forEach(c => {
        allthemods.recipes.gtceu.orbital_lithography(`af9:print_${c.id}_1nm`)
            .itemInputs(chromodynium.blank, 'kubejs:dry_resist_cartridge')
            .notConsumable(`kubejs:${c.reticle}_reticle`)
            .inputFluids(Fluid.of('gtceu:supercooled_endion', 500))
            .itemOutputs(printed(chromodynium.index, c))
            .chancedOutput(`kubejs:broken_${chromodynium.id}_wafer`, AF9_WAFERS.orbital.baseBreak, 0)
            .duration(7200)
            .EUt(VA[chromodynium.tier], 50)
    })

    // ---- Derived wafers ----
    // GT's chemical upgrades of printed wafers (GT's own recipes stay for its own wafers); on every higher substrate
    // the result keeps the substrate of the wafer it was made from. Chemical Reactor and Large Chemical Reactor.
    const derived = [
        { id: 'nano_cpu', from: 'cpu', suffix: '', items: ['16x gtceu:carbon_fibers'], fluid: ['gtceu:glowstone', 576], duration: 1200, tier: GTValues.EV },
        { id: 'qbit_cpu', from: 'nano_cpu', suffix: '_quantum_eye', items: ['2x gtceu:quantum_eye'], fluid: ['gtceu:gallium_arsenide', 288], duration: 900, tier: GTValues.EV },
        { id: 'qbit_cpu', from: 'nano_cpu', suffix: '_radon', items: ['gtceu:indium_gallium_phosphide_dust'], fluid: ['gtceu:radon', 50], duration: 1200, tier: GTValues.EV },
        { id: 'hpic', from: 'mpic', suffix: '', items: ['2x gtceu:indium_gallium_phosphide_dust'], fluid: ['gtceu:vanadium_gallium', 288], duration: 1200, tier: GTValues.IV },
        { id: 'uhpic', from: 'hpic', suffix: '', items: ['8x gtceu:indium_gallium_phosphide_dust'], fluid: ['gtceu:naquadah', 576], duration: 1200, tier: GTValues.LuV }
    ]
    derived.forEach(d => {
        const target = AF9_WAFERS.chip(d.id)
        const source = AF9_WAFERS.chip(d.from)
        substrates.forEach(s => {
            if (s.index <= target.native) return // GT's own recipe
            const input = printed(s.index, source)
            const output = printed(s.index, target)
            if (!input || !output) return
            ;['chemical_reactor', 'large_chemical_reactor'].forEach(type => {
                allthemods.recipes.gtceu[type](`af9:${type}/${d.id}_wafer${d.suffix}_${s.id}`)
                    .itemInputs([input].concat(d.items))
                    .inputFluids(Fluid.of(d.fluid[0], d.fluid[1]))
                    .itemOutputs(output)
                    .duration(d.duration)
                    .EUt(VA[d.tier])
                    .cleanroom(CleanroomType.CLEANROOM)
            })
        })
    })

    // ---- Cutting the printed wafers ----
    // GT's cutter recipes cut GT's own wafers. A wafer on a higher substrate gives more dies (see AF9_WAFERS.dies), in
    // GT's three fluid variants with GT's formulas.
    const clamp = (value, min, max) => Math.min(Math.max(value, min), max)
    const chipStacks = (c, count) => {
        const stacks = []
        for (let left = count; left > 0; left -= 64) stacks.push(`${Math.min(left, 64)}x ${c.chip}`)
        return stacks
    }
    chips.forEach(c => {
        const totalEU = 900 * c.cutEUt
        const fluids = [
            { id: '', fluid: Fluid.of('gtceu:lubricant', clamp(Math.floor(totalEU / 1280), 1, 250)), duration: 900 },
            { id: '_distilled_water', fluid: Fluid.of('gtceu:distilled_water', clamp(Math.floor(totalEU / 426), 3, 750)), duration: 1350 },
            { id: '_water', fluid: Fluid.of('minecraft:water', clamp(Math.floor(totalEU / 320), 4, 1000)), duration: 1800 }
        ]
        substrates.forEach(s => {
            const wafer = printed(s.index, c)
            if (!wafer || !wafer.startsWith('kubejs:')) return
            fluids.forEach(variant => {
                const recipe = allthemods.recipes.gtceu.cutter(`af9:cut_${c.id}_${s.id}${variant.id}`)
                    .itemInputs(wafer)
                    .inputFluids(variant.fluid)
                    .itemOutputs(chipStacks(c, AF9_WAFERS.dies(s.index, c)))
                    .duration(variant.duration)
                    .EUt(c.cutEUt)
                if (c.cleanroom) recipe.cleanroom(CleanroomType.CLEANROOM)
            })
        })
    })

    // ---- Broken and contaminated wafers ----
    // Broken wafers are ground for their material; contaminated ones are stripped and RCA-cleaned back into a blank
    // wafer of their substrate (the print is lost).
    substrates.forEach(s => {
        allthemods.recipes.gtceu.macerator(`af9:reclaim_broken_${s.id}_wafer`)
            .itemInputs(`kubejs:broken_${s.id}_wafer`)
            .itemOutputs(`2x ${s.reclaim}`)
            .duration(100)
            .EUt(EU_LV)
        allthemods.recipes.gtceu.fab_wet_processing(`af9:clean_contaminated_${s.id}_wafer`)
            .itemInputs(`kubejs:contaminated_${s.id}_wafer`)
            .inputFluids(Fluid.of('gtceu:hydrofluoric_acid', 100), Fluid.of('gtceu:distilled_water', 1000))
            .itemOutputs(s.blank)
            .duration(200)
            .EUt(VA[Math.min(s.tier, GTValues.LuV)])
            .cleanroom(CleanroomType.CLEANROOM)
    })
})
