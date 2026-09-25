// AF9 - Photolithography Line recipes (machine, materials and items: startup_scripts/gtceu/photolithography.js)
// Spec and numbers: docs/semiconductor-factory.md

// Shared with tiered_circuits.js (server scripts share one scope). Must stay in sync with
// com.af9.core.litho.LithoMode in af9-core.
const AF9_LITHO = (() => {
    // Exposure modes, finest last. Every mode draws 4A of its own voltage tier (always 2 hatches) and each step uses
    // 1.5x the chemicals. Each mode prints on its own substrate; substrateTier indexes the chips' GT wafer yields.
    // light / wavelength / na: the exposure tool the real node used (k1 = node x NA / wavelength stays >= 0.35).
    // resist: the photoresist made for that light; laserGas: the excimer premix the laser burns (none for the mercury
    // lamp); immersion: water film under the lens; highK: HfO2 gate dielectric from hafnium tetrachloride.
    const modes = [
        { id: 'muv', index: 0, node: 350, tier: GTValues.MV, substrate: 'gtceu:silicon_wafer', substrateTier: 0,
            light: 'i_line', wavelength: 365, na: 0.60, resist: 'gtceu:photoresist' },
        { id: 'huv', index: 1, node: 250, tier: GTValues.HV, substrate: 'gtceu:phosphorus_wafer', substrateTier: 1,
            light: 'krf', wavelength: 248, na: 0.60, resist: 'gtceu:krf_photoresist', laserGas: 'gtceu:krf_excimer_gas' },
        { id: 'euv', index: 2, node: 200, tier: GTValues.EV, substrate: 'gtceu:naquadah_wafer', substrateTier: 2,
            light: 'krf', wavelength: 248, na: 0.70, resist: 'gtceu:krf_photoresist', laserGas: 'gtceu:krf_excimer_gas' },
        { id: 'xuv', index: 3, node: 100, tier: GTValues.IV, substrate: 'gtceu:neutronium_wafer', substrateTier: 3,
            light: 'arf', wavelength: 193, na: 0.85, resist: 'gtceu:arf_photoresist', laserGas: 'gtceu:arf_excimer_gas' },
        { id: 'luv', index: 4, node: 50, tier: GTValues.LuV, substrate: 'gtceu:neutronium_wafer', substrateTier: 3,
            light: 'arf_immersion', wavelength: 193, na: 1.35, resist: 'gtceu:arf_photoresist', laserGas: 'gtceu:arf_excimer_gas',
            immersion: true, highK: true }
    ]

    // Printed wafers per substrate wafer (silicon, phosphorus, naquadah, neutronium), as GT's laser engraver gave them
    const SILICON_CLASS = [1, 4, 8, 16]
    const PHOSPHORUS_CLASS = [0, 1, 4, 8]

    // Every GT chip wafer. lens = GT engraving lens (reticle colour); derived wafers are made from another printed
    // wafer instead. minMode = first mode that can make it (its GT tier). gtCut / cut / cutEUt / cleanroom mirror
    // GT's cutter recipes. transistors = per die at 350 nm; must stay below 2^31 / 49 (LUV density) to fit the int tag.
    const chips = [
        { id: 'ilc', wafer: 'gtceu:ilc_wafer', chip: 'gtceu:ilc_chip', lens: 'red', engrave: 'engrave_ilc', yields: SILICON_CLASS, minMode: 0, gtCut: 'cut_ilc', cut: 8, cutEUt: 64, cleanroom: false, transistors: 50000 },
        { id: 'ram', wafer: 'gtceu:ram_wafer', chip: 'gtceu:ram_chip', lens: 'green', engrave: 'engrave_ram', yields: SILICON_CLASS, minMode: 0, gtCut: 'cut_ram', cut: 32, cutEUt: 96, cleanroom: false, transistors: 16000000 },
        { id: 'cpu', wafer: 'gtceu:cpu_wafer', chip: 'gtceu:cpu_chip', lens: 'light_blue', engrave: 'engrave_cpu', yields: SILICON_CLASS, minMode: 0, gtCut: 'cut_cpu', cut: 8, cutEUt: 120, cleanroom: false, transistors: 5500000 },
        { id: 'ulpic', wafer: 'gtceu:ulpic_wafer', chip: 'gtceu:ulpic_chip', lens: 'blue', engrave: 'engrave_ulpic', yields: SILICON_CLASS, minMode: 0, gtCut: 'cut_ulpic', cut: 6, cutEUt: 120, cleanroom: false, transistors: 2000 },
        { id: 'lpic', wafer: 'gtceu:lpic_wafer', chip: 'gtceu:lpic_chip', lens: 'orange', engrave: 'engrave_lpic', yields: SILICON_CLASS, minMode: 0, gtCut: 'cut_lpic', cut: 4, cutEUt: 480, cleanroom: true, transistors: 5000 },
        { id: 'simple_soc', wafer: 'gtceu:simple_soc_wafer', chip: 'gtceu:simple_soc', lens: 'cyan', engrave: 'engrave_ssoc', yields: SILICON_CLASS, minMode: 0, gtCut: 'cut_ssoc', cut: 6, cutEUt: 64, cleanroom: false, transistors: 1000000 },
        { id: 'nand', wafer: 'gtceu:nand_memory_wafer', chip: 'gtceu:nand_memory_chip', lens: 'gray', engrave: 'engrave_nand', yields: PHOSPHORUS_CLASS, minMode: 1, gtCut: 'cut_nand', cut: 32, cutEUt: 192, cleanroom: true, transistors: 32000000 },
        { id: 'nor', wafer: 'gtceu:nor_memory_wafer', chip: 'gtceu:nor_memory_chip', lens: 'pink', engrave: 'engrave_nor', yields: PHOSPHORUS_CLASS, minMode: 1, gtCut: 'cut_nor', cut: 16, cutEUt: 192, cleanroom: true, transistors: 16000000 },
        { id: 'mpic', wafer: 'gtceu:mpic_wafer', chip: 'gtceu:mpic_chip', lens: 'brown', engrave: 'engrave_pic', yields: PHOSPHORUS_CLASS, minMode: 1, gtCut: 'cut_pic', cut: 4, cutEUt: 1920, cleanroom: true, transistors: 20000 },
        { id: 'soc', wafer: 'gtceu:soc_wafer', chip: 'gtceu:soc', lens: 'yellow', engrave: 'engrave_soc', yields: PHOSPHORUS_CLASS, minMode: 1, gtCut: 'cut_soc', cut: 6, cutEUt: 480, cleanroom: true, transistors: 8000000 },
        { id: 'advanced_soc', wafer: 'gtceu:advanced_soc_wafer', chip: 'gtceu:advanced_soc', lens: 'purple', engrave: 'engrave_asoc', yields: [0, 0, 1, 2], minMode: 2, gtCut: 'cut_asoc', cut: 6, cutEUt: 1920, cleanroom: true, transistors: 20000000 },
        { id: 'highly_advanced_soc', wafer: 'gtceu:highly_advanced_soc_wafer', chip: 'gtceu:highly_advanced_soc', lens: 'black', engrave: 'engrave_hasoc', yields: [0, 0, 0, 1], minMode: 3, gtCut: 'cut_hasoc', cut: 6, cutEUt: 7680, cleanroom: true, transistors: 40000000 },
        { id: 'nano_cpu', wafer: 'gtceu:nano_cpu_wafer', chip: 'gtceu:nano_cpu_chip', minMode: 2, gtCut: 'cut_nano_cpu', cut: 8, cutEUt: 480, cleanroom: true, transistors: 12000000 },
        { id: 'qbit_cpu', wafer: 'gtceu:qbit_cpu_wafer', chip: 'gtceu:qbit_cpu_chip', minMode: 2, gtCut: 'cut_qbit_cpu', cut: 4, cutEUt: 1920, cleanroom: true, transistors: 25000000 },
        { id: 'hpic', wafer: 'gtceu:hpic_wafer', chip: 'gtceu:hpic_chip', minMode: 3, gtCut: 'cut_hpic', cut: 2, cutEUt: 7680, cleanroom: true, transistors: 50000 },
        { id: 'uhpic', wafer: 'gtceu:uhpic_wafer', chip: 'gtceu:uhpic_chip', minMode: 4, gtCut: 'cut_uhpic', cut: 2, cutEUt: 30720, cleanroom: true, transistors: 100000 }
    ]
    const chip = id => {
        const found = chips.filter(c => c.id === id)[0]
        if (!found) throw new Error(`AF9_LITHO: unknown chip '${id}'`)
        return found
    }
    const mode = id => {
        const found = modes.filter(m => m.id === id)[0]
        if (!found) throw new Error(`AF9_LITHO: unknown mode '${id}'`)
        return found
    }
    // NBT the line writes onto wafers (and the cutter copies onto chips): read by AF9 Core for tooltip and texture
    const nbt = (c, m) => {
        const transistors = Math.round(c.transistors * Math.pow(350 / m.node, 2))
        return `{AF9Litho:{Node:${m.node},Transistors:${transistors}}}`
    }
    // Exact-NBT ingredient of a chip printed in a given mode, e.g. tagged('ram', 'huv', 4)
    const tagged = (chipId, modeId, count) => {
        const c = chip(chipId)
        return Item.of(c.chip, count || 1, nbt(c, mode(modeId))).strongNBT()
    }
    return { modes: modes, chips: chips, chip: chip, mode: mode, nbt: nbt, tagged: tagged }
})()

ServerEvents.recipes(allthemods => {
    const EU_LV = GTValues.VA[GTValues.LV]
    const EU_MV = GTValues.VA[GTValues.MV]
    const modes = AF9_LITHO.modes
    const chips = AF9_LITHO.chips
    const chip = AF9_LITHO.chip
    const nbt = AF9_LITHO.nbt

    // ---- Removed GT paths: everything now comes from the Photolithography Line ----
    // Except one bootstrap: MV Energy Hatches need a ULPIC chip and the line needs MV Energy Hatches, so GT's plain
    // ULPIC engraving on silicon stays (like the chip-free Good Electronic Circuit). Its wafers have no mode.
    const substrates = ['silicon', 'phosphorus', 'naquadah', 'neutronium']
    chips.filter(c => c.engrave).forEach(c => substrates.forEach(s => {
        if (c.id === 'ulpic' && s === 'silicon') return
        allthemods.remove({ id: `gtceu:laser_engraver/${c.engrave}_${s}` })
    }))
    const gtDerived = ['nano_cpu_wafer', 'qbit_cpu_wafer_quantum_eye', 'qbit_cpu_wafer_radon', 'hpic_wafer', 'uhpic_wafer']
    gtDerived.forEach(id => {
        allthemods.remove({ id: `gtceu:chemical_reactor/${id}` })
        allthemods.remove({ id: `gtceu:large_chemical_reactor/${id}` })
    })
    chips.forEach(c => ['', '_water', '_distilled_water'].forEach(suffix =>
        allthemods.remove({ id: `gtceu:cutter/${c.gtCut}${suffix}` })))

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

    chips.filter(c => c.lens).forEach(c => {
        allthemods.recipes.gtceu.laser_engraver(`af9:${c.id}_reticle`)
            .itemInputs('kubejs:photomask_blank')
            .notConsumable(`#forge:lenses/${c.lens}`)
            .itemOutputs(`kubejs:${c.id}_reticle`)
            .duration(1800)
            .EUt(EU_MV)
    })

    // ---- Extreme clean dry air (XCDA) ----
    // A fab's clean-dry-air plant, all in chemical reactors: oxidize -> scrub CO2 -> dry -> cryo-cool -> filter.
    // Everything runs at MV, slowly (the expansion cooler recycles 3/4 of its air). HV adds a platinum oxidizer, a
    // caustic scrubber and liquid-air cooling, which together are much faster.
    const EU_HV = GTValues.VA[GTValues.HV]

    // Cu + 2 MnO2 + O -> CuMn2O4
    allthemods.recipes.gtceu.chemical_reactor('af9:hopcalite')
        .itemInputs('gtceu:copper_dust', '6x gtceu:pyrolusite_dust')
        .inputFluids(Fluid.of('gtceu:oxygen', 1000))
        .itemOutputs('7x gtceu:hopcalite_dust')
        .duration(400)
        .EUt(EU_MV)

    // zeolite crystallised with a bentonite binder into sieve beads (clay + distilled water is GT's clay recipe)
    allthemods.recipes.gtceu.autoclave('af9:molecular_sieve')
        .itemInputs('4x gtceu:zeolite_dust', 'gtceu:bentonite_dust')
        .inputFluids(Fluid.of('gtceu:distilled_water', 500))
        .itemOutputs('4x kubejs:molecular_sieve')
        .duration(600)
        .EUt(EU_MV)

    // temperature-swing regeneration
    allthemods.smelting('kubejs:molecular_sieve', 'kubejs:saturated_molecular_sieve').id('af9:regenerate_molecular_sieve')

    // 1. catalytic oxidation: CO, H2 and hydrocarbons -> CO2 + H2O
    allthemods.recipes.gtceu.chemical_reactor('af9:xcda_oxidize_hopcalite')
        .notConsumable('gtceu:hopcalite_dust')
        .inputFluids(Fluid.of('gtceu:air', 4000))
        .outputFluids(Fluid.of('gtceu:oxidized_air', 4000))
        .duration(600)
        .EUt(EU_MV)

    allthemods.recipes.gtceu.chemical_reactor('af9:xcda_oxidize_platinum')
        .notConsumable('gtceu:platinum_dust')
        .inputFluids(Fluid.of('gtceu:air', 4000))
        .outputFluids(Fluid.of('gtceu:oxidized_air', 4000))
        .duration(150)
        .EUt(EU_HV)

    // 2. CO2 scrubbing: Ca(OH)2 + CO2 -> CaCO3 + H2O, or 2 NaOH + CO2 -> Na2CO3 + H2O
    allthemods.recipes.gtceu.chemical_reactor('af9:xcda_scrub_lime')
        .itemInputs('gtceu:small_calcium_hydroxide_dust')
        .inputFluids(Fluid.of('gtceu:oxidized_air', 4000))
        .itemOutputs('gtceu:small_calcite_dust')
        .outputFluids(Fluid.of('gtceu:decarbonated_air', 4000))
        .duration(400)
        .EUt(EU_MV)

    allthemods.recipes.gtceu.chemical_reactor('af9:xcda_scrub_caustic')
        .itemInputs('gtceu:small_sodium_hydroxide_dust')
        .inputFluids(Fluid.of('gtceu:oxidized_air', 4000))
        .itemOutputs('gtceu:small_soda_ash_dust')
        .outputFluids(Fluid.of('gtceu:decarbonated_air', 4000))
        .duration(100)
        .EUt(EU_HV)

    // 3. drying: the sieve adsorbs the water
    allthemods.recipes.gtceu.chemical_reactor('af9:xcda_dry')
        .itemInputs('kubejs:molecular_sieve')
        .inputFluids(Fluid.of('gtceu:decarbonated_air', 4000))
        .itemOutputs('kubejs:saturated_molecular_sieve')
        .outputFluids(Fluid.of('gtceu:dry_air', 4000))
        .duration(400)
        .EUt(EU_MV)

    // 4. cryogenic cooling. MV: Joule-Thomson expansion, only a quarter gets cold enough, the rest goes round again.
    // HV: pre-cooled with liquid air, which boils back into ordinary air.
    allthemods.recipes.gtceu.chemical_reactor('af9:xcda_cool_expansion')
        .circuit(1)
        .inputFluids(Fluid.of('gtceu:dry_air', 4000))
        .outputFluids(Fluid.of('gtceu:cryogenic_supercooled_air', 1000), Fluid.of('gtceu:dry_air', 3000))
        .duration(800)
        .EUt(EU_MV)

    allthemods.recipes.gtceu.chemical_reactor('af9:xcda_cool_liquid_air')
        .circuit(2)
        .inputFluids(Fluid.of('gtceu:dry_air', 4000), Fluid.of('gtceu:liquid_air', 1000))
        .outputFluids(Fluid.of('gtceu:cryogenic_supercooled_air', 4000), Fluid.of('gtceu:air', 1000))
        .duration(200)
        .EUt(EU_HV)

    // 5. re-warmed through a membrane filter; the last traces stayed frozen in the cold box
    allthemods.recipes.gtceu.chemical_reactor('af9:xcda_filter')
        .notConsumable('gtceu:fluid_filter')
        .inputFluids(Fluid.of('gtceu:cryogenic_supercooled_air', 4000))
        .outputFluids(Fluid.of('gtceu:extreme_clean_dry_air', 4000))
        .duration(200)
        .EUt(EU_MV)

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

    // ---- Photoresist (i-line, MUV) ----
    // Formox process: CH3OH + 1/2 O2 -> CH2O + H2O with air over iron molybdate. GT's own formaldehyde recipe (silver
    // catalyst) is HV, which MUV cannot wait for. The catalyst: molybdenite roasted to MoO3, calcined with hematite.
    allthemods.recipes.gtceu.electric_blast_furnace('af9:molybdenum_trioxide')
        .itemInputs('gtceu:molybdenite_dust')
        .inputFluids(Fluid.of('gtceu:oxygen', 7000))
        .itemOutputs('gtceu:molybdenum_trioxide_dust')
        .outputFluids(Fluid.of('gtceu:sulfur_dioxide', 2000))
        .blastFurnaceTemp(900)
        .duration(300)
        .EUt(EU_MV)

    // Fe2O3 + 3 MoO3 -> Fe2(MoO4)3
    allthemods.recipes.gtceu.electric_blast_furnace('af9:iron_molybdate')
        .itemInputs('gtceu:hematite_dust', '3x gtceu:molybdenum_trioxide_dust')
        .itemOutputs('gtceu:iron_molybdate_dust')
        .blastFurnaceTemp(800)
        .duration(300)
        .EUt(EU_MV)

    allthemods.recipes.gtceu.chemical_reactor('af9:formaldehyde_formox')
        .notConsumable('gtceu:iron_molybdate_dust')
        .inputFluids(Fluid.of('gtceu:methanol', 1000), Fluid.of('gtceu:air', 3000))
        .outputFluids(Fluid.of('gtceu:formaldehyde', 1000), Fluid.of('minecraft:water', 1000))
        .duration(200)
        .EUt(EU_MV)

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

    // Membrane electrolysis, as electronic-grade TMAH is made: no potassium or sodium may reach the developer (metal
    // ions shift transistor thresholds). (CH3)4NCl + H2O -> (CH3)4NOH + 1/2 H2 + 1/2 Cl2
    allthemods.recipes.gtceu.mixer('af9:tetramethylammonium_chloride_solution')
        .itemInputs('gtceu:tetramethylammonium_chloride_dust')
        .inputFluids(Fluid.of('gtceu:distilled_water', 5000))
        .outputFluids(Fluid.of('gtceu:tetramethylammonium_chloride_solution', 5000))
        .duration(100)
        .EUt(EU_LV)

    allthemods.recipes.gtceu.electrolyzer('af9:tmah_developer')
        .inputFluids(Fluid.of('gtceu:tetramethylammonium_chloride_solution', 5000))
        .outputFluids(Fluid.of('gtceu:tmah_developer', 5000), Fluid.of('gtceu:chlorine', 1000), Fluid.of('gtceu:hydrogen', 1000))
        .duration(300)
        .EUt(EU_MV)

    // ---- Printed wafers ----
    // HMDS prime -> resist coat -> soft bake -> exposure -> PEB -> TMAH develop -> DI rinse -> hard bake
    // The resist is the one made for the mode's light, and the excimer lasers (HUV on) burn their premix. LUV exposes
    // through a film of ultrapure water and grows a high-k HfO2 gate dielectric from HfCl4 + water (ALD), as fabs did
    // from 45 nm on.
    chips.filter(c => c.lens).forEach(c => {
        modes.filter(m => m.index >= c.minMode).forEach(m => {
            const chemicals = Math.pow(1.5, m.index)
            const fluids = [
                Fluid.of('gtceu:hmds_vapor', Math.round(40 * chemicals)),
                Fluid.of(m.resist, Math.round(100 * chemicals)),
                Fluid.of('gtceu:tmah_developer', Math.round(200 * chemicals)),
                Fluid.of('gtceu:distilled_water', Math.round(1000 * chemicals)),
                Fluid.of('gtceu:extreme_clean_dry_air', Math.round(1000 * chemicals))]
            if (m.laserGas) fluids.push(Fluid.of(m.laserGas, Math.round(10 * chemicals)))
            if (m.immersion) fluids.push(Fluid.of('gtceu:ultrapure_water', 1000))
            if (m.highK) fluids.push(Fluid.of('gtceu:hafnium_tetrachloride', 100))
            allthemods.recipes.gtceu[`lithography_${m.id}`](`af9:${c.id}_wafer_${m.id}`)
                .itemInputs(m.substrate)
                .notConsumable(`kubejs:${c.id}_reticle`)
                .inputFluids(fluids)
                .itemOutputs(Item.of(c.wafer, c.yields[m.substrateTier], nbt(c, m)))
                .duration(900)
                .EUt(GTValues.VA[m.tier], 4)
        })
    })

    // ---- Derived wafers ----
    // GT's chemical upgrades of printed wafers, done in the line so the result keeps the mode it was printed in.
    // One recipe per mode; the input must come from that mode (exact NBT).
    const derived = [
        { id: 'nano_cpu', from: 'cpu', items: ['16x gtceu:carbon_fibers'], fluid: ['gtceu:glowstone', 576], duration: 1200 },
        { id: 'qbit_cpu', from: 'nano_cpu', items: ['2x gtceu:quantum_eye'], fluid: ['gtceu:gallium_arsenide', 288], duration: 900 },
        { id: 'qbit_cpu', suffix: '_radon', from: 'nano_cpu', items: ['gtceu:indium_gallium_phosphide_dust'], fluid: ['gtceu:radon', 50], duration: 1200 },
        { id: 'hpic', from: 'mpic', items: ['2x gtceu:indium_gallium_phosphide_dust'], fluid: ['gtceu:vanadium_gallium', 288], duration: 1200 },
        { id: 'uhpic', from: 'hpic', items: ['8x gtceu:indium_gallium_phosphide_dust'], fluid: ['gtceu:naquadah', 576], duration: 1200 }
    ]
    derived.forEach(d => {
        const target = chip(d.id)
        const source = chip(d.from)
        modes.filter(m => m.index >= target.minMode).forEach(m => {
            allthemods.recipes.gtceu[`lithography_${m.id}`](`af9:${d.id}_wafer_${m.id}${d.suffix || ''}`)
                .itemInputs([Item.of(source.wafer, nbt(source, m)).strongNBT()].concat(d.items))
                .inputFluids(Fluid.of(d.fluid[0], d.fluid[1]))
                .itemOutputs(Item.of(target.wafer, nbt(target, m)))
                .duration(d.duration)
                .EUt(GTValues.VA[m.tier], 4)
        })
    })

    // ---- Wafer cutting ----
    // Replaces GT's cutter recipes. Inputs match NBT exactly, so a plain wafer (quest rewards, loot) cuts exactly like
    // GT, while a printed wafer cuts into more dies (sqrt(350 / node)) that keep its node and transistor count. Each gets
    // GT's three fluid variants (KubeJS recipes skip GT's generator), with GT's formulas.
    const clamp = (value, min, max) => Math.min(Math.max(value, min), max)
    // the single-block cutter has two output slots, so split into stacks of at most 64
    const chipStacks = (c, count, tag) => {
        const stacks = []
        for (let left = count; left > 0; left -= 64) {
            const size = Math.min(left, 64)
            stacks.push(tag ? Item.of(c.chip, size, tag) : Item.of(c.chip, size))
        }
        return stacks
    }
    chips.forEach(c => {
        const wafers = [{ id: 'plain', tag: null, count: c.cut }].concat(modes.filter(m => m.index >= c.minMode).map(m => ({
            id: m.id,
            tag: nbt(c, m),
            count: Math.round(c.cut * Math.sqrt(350 / m.node))
        })))
        const totalEU = 900 * c.cutEUt
        const fluids = [
            { id: '', fluid: Fluid.of('gtceu:lubricant', clamp(Math.floor(totalEU / 1280), 1, 250)), duration: 900 },
            { id: '_distilled_water', fluid: Fluid.of('gtceu:distilled_water', clamp(Math.floor(totalEU / 426), 3, 750)), duration: 1350 },
            { id: '_water', fluid: Fluid.of('minecraft:water', clamp(Math.floor(totalEU / 320), 4, 1000)), duration: 1800 }
        ]
        wafers.forEach(wafer => {
            fluids.forEach(variant => {
                const recipe = allthemods.recipes.gtceu.cutter(`af9:cut_${c.id}_${wafer.id}${variant.id}`)
                    .itemInputs((wafer.tag ? Item.of(c.wafer, wafer.tag) : Item.of(c.wafer)).strongNBT())
                    .inputFluids(variant.fluid)
                    .itemOutputs(chipStacks(c, wafer.count, wafer.tag))
                    .duration(variant.duration)
                    .EUt(c.cutEUt)
                if (c.cleanroom) recipe.cleanroom(CleanroomType.CLEANROOM)
            })
        })
    })
})
