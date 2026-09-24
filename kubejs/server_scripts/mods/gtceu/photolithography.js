// AF9 - Photolithography Line recipes (machine, materials and items: startup_scripts/gtceu/photolithography.js)

ServerEvents.recipes(allthemods => {
    const EU_LV = GTValues.VA[GTValues.LV]
    const EU_MV = GTValues.VA[GTValues.MV]
    const EU_HV = GTValues.VA[GTValues.HV]
    const EU_EV = GTValues.VA[GTValues.EV]

    // The MV silicon chips. gtCut / cut / cutEUt / cleanroom mirror GT's own cutter recipes; lens is GT's engraving lens
    // colour. transistors = transistors per die at MUV (350 nm), roughly what such a chip had on a real 350 nm process.
    const chips = [
        { id: 'ilc', wafer: 'gtceu:ilc_wafer', chip: 'gtceu:ilc_chip', lens: 'red', engrave: 'engrave_ilc_silicon', gtCut: 'cut_ilc', cut: 8, cutEUt: 64, cleanroom: false, transistors: 50000 },
        { id: 'ram', wafer: 'gtceu:ram_wafer', chip: 'gtceu:ram_chip', lens: 'green', engrave: 'engrave_ram_silicon', gtCut: 'cut_ram', cut: 32, cutEUt: 96, cleanroom: false, transistors: 16000000 },
        { id: 'cpu', wafer: 'gtceu:cpu_wafer', chip: 'gtceu:cpu_chip', lens: 'light_blue', engrave: 'engrave_cpu_silicon', gtCut: 'cut_cpu', cut: 8, cutEUt: 120, cleanroom: false, transistors: 5500000 },
        { id: 'ulpic', wafer: 'gtceu:ulpic_wafer', chip: 'gtceu:ulpic_chip', lens: 'blue', engrave: 'engrave_ulpic_silicon', gtCut: 'cut_ulpic', cut: 6, cutEUt: 120, cleanroom: false, transistors: 2000 },
        { id: 'lpic', wafer: 'gtceu:lpic_wafer', chip: 'gtceu:lpic_chip', lens: 'orange', engrave: 'engrave_lpic_silicon', gtCut: 'cut_lpic', cut: 4, cutEUt: 480, cleanroom: true, transistors: 5000 },
        { id: 'simple_soc', wafer: 'gtceu:simple_soc_wafer', chip: 'gtceu:simple_soc', lens: 'cyan', engrave: 'engrave_ssoc_silicon', gtCut: 'cut_ssoc', cut: 6, cutEUt: 64, cleanroom: false, transistors: 1000000 }
    ]

    // Exposure modes, finest last. Must stay in sync with com.af9.core.litho.LithoMode in af9-core:
    // each step doubles EU/t (always 2 hatches) and uses 1.5x the chemicals; transistor density is (350 / node)^2 and
    // dies per wafer grow with sqrt(350 / node).
    const modes = [
        { id: 'muv', node: 350, voltage: EU_MV, amps: 4 },
        { id: 'huv', node: 250, voltage: EU_HV, amps: 2 },
        { id: 'euv', node: 200, voltage: EU_HV, amps: 4 },
        { id: 'xuv', node: 100, voltage: EU_EV, amps: 2 },
        { id: 'luv', node: 50, voltage: EU_EV, amps: 4 }
    ]
    // NBT the line writes onto wafers (and the cutter copies onto chips): read by AF9 Core for tooltip and texture
    const lithoNbt = (chip, mode) => {
        const transistors = Math.round(chip.transistors * Math.pow(350 / mode.node, 2))
        return `{AF9Litho:{Node:${mode.node},Transistors:${transistors}}}`
    }
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
    // HMDS prime -> resist coat -> soft bake -> UV exposure -> PEB -> TMAH develop -> DI rinse -> hard bake
    chips.forEach(chip => {
        modes.forEach((mode, index) => {
            const chemicals = Math.pow(1.5, index)
            allthemods.recipes.gtceu[`lithography_${mode.id}`](`af9:${chip.id}_wafer_${mode.id}`)
                .itemInputs('gtceu:silicon_wafer')
                .notConsumable(`kubejs:${chip.id}_reticle`)
                .inputFluids(
                    Fluid.of('gtceu:hmds_vapor', Math.round(40 * chemicals)),
                    Fluid.of('gtceu:photoresist', Math.round(100 * chemicals)),
                    Fluid.of('gtceu:tmah_developer', Math.round(200 * chemicals)),
                    Fluid.of('gtceu:distilled_water', Math.round(1000 * chemicals)),
                    Fluid.of('gtceu:extreme_clean_dry_air', Math.round(1000 * chemicals)))
                .itemOutputs(Item.of(chip.wafer, lithoNbt(chip, mode)))
                .duration(900)
                .EUt(mode.voltage, mode.amps)
        })
    })

    // ---- Wafer cutting ----
    // Replaces GT's cutter recipes for these wafers. Inputs match NBT exactly, so plain wafers (quest rewards, the other
    // GT engraving recipes) still cut exactly like GT, while printed wafers cut into more dies that keep the wafer's
    // node and transistor count. Each gets GT's three fluid variants (KubeJS recipes skip GT's generator), with GT's
    // formulas.
    const clamp = (value, min, max) => Math.min(Math.max(value, min), max)
    // the single-block cutter has two output slots, so split into stacks of at most 64
    const chipStacks = (chip, count, nbt) => {
        const stacks = []
        for (let left = count; left > 0; left -= 64) {
            const size = Math.min(left, 64)
            stacks.push(nbt ? Item.of(chip.chip, size, nbt) : Item.of(chip.chip, size))
        }
        return stacks
    }
    chips.forEach(chip => {
        ['', '_water', '_distilled_water'].forEach(suffix => allthemods.remove({ id: `gtceu:cutter/${chip.gtCut}${suffix}` }))

        const wafers = [{ id: 'plain', nbt: null, count: chip.cut }].concat(modes.map(mode => ({
            id: mode.id,
            nbt: lithoNbt(chip, mode),
            count: Math.round(chip.cut * Math.sqrt(350 / mode.node))
        })))
        const totalEU = 900 * chip.cutEUt
        const fluids = [
            { id: '', fluid: Fluid.of('gtceu:lubricant', clamp(Math.floor(totalEU / 1280), 1, 250)), duration: 900 },
            { id: '_distilled_water', fluid: Fluid.of('gtceu:distilled_water', clamp(Math.floor(totalEU / 426), 3, 750)), duration: 1350 },
            { id: '_water', fluid: Fluid.of('minecraft:water', clamp(Math.floor(totalEU / 320), 4, 1000)), duration: 1800 }
        ]
        wafers.forEach(wafer => {
            fluids.forEach(variant => {
                const recipe = allthemods.recipes.gtceu.cutter(`af9:cut_${chip.id}_${wafer.id}${variant.id}`)
                    .itemInputs((wafer.nbt ? Item.of(chip.wafer, wafer.nbt) : Item.of(chip.wafer)).strongNBT())
                    .inputFluids(variant.fluid)
                    .itemOutputs(chipStacks(chip, wafer.count, wafer.nbt))
                    .duration(variant.duration)
                    .EUt(chip.cutEUt)
                if (chip.cleanroom) recipe.cleanroom(CleanroomType.CLEANROOM)
            })
        })
    })
})