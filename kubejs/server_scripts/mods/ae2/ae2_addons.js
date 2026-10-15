// AF9 - AE2 addons: MEGA Cells climb IV -> UHV (256m only in the Advanced Circuit
// Manufacturer, true endgame), the Infinity Booster cards build at HV.
//
// MEGA components chain off base AE2's 256k component: 3x previous + engineering
// processor + sky dust + quartz glass + its tier circuit, in the assembler with
// high-grade solder (tin budget) in a cleanroom — one voltage tier per cell, 1m at
// IV up to 64m at UV. The 256m goal builds only in the Advanced Circuit Manufacturer
// at UHV on 300 A with computation (organized Pico circuit + sanguinite wire on top
// of the 64m stack). Each file section is guarded by its mod id (neither addon is in
// the dev run), and removals are mod-scoped so the af9: recipes never match. MEGA cell
// assembly (housing + component) and any ExtendedAE recipes stay vanilla and follow
// through the new components.
// Spec: docs/ae2.md

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const HV = VA[GTValues.HV]
    const clean = recipe => recipe.cleanroom(CleanroomType.CLEANROOM)

    // Tin budget + high-grade solder canonical pair.
    const hvSoldered = (id, solder, build) => {
        build(event.recipes.gtceu.assembler(`af9:${id}`), Fluid.of('gtceu:tin', 144 * solder))
        build(event.recipes.gtceu.assembler(`af9:${id}_high_grade`),
            Fluid.of('gtceu:high_grade_solder', 72 * solder))
    }

    // ---- MEGA Cells (mod id megacells): the endgame ladder, IV -> UHV ----
    // 1m at IV, then one voltage tier per cell, up to the 256m goal at UHV in the
    // Assembly Line. Each takes 3x previous + engineering processor + sky dust +
    // quartz glass + its tier circuit. Bulk cells ride along at IV.
    if (Platform.isLoaded('megacells')) {
        // RAM climbs with the tier: eDRAM at 1m, MRAM from 4m (stacks of 64 chips).
        const megaRam = {
            'megacells:cell_component_1m': ['64x af9:edram_chip'],
            'megacells:cell_component_4m': ['64x af9:mram_chip'],
            'megacells:cell_component_16m': ['64x af9:mram_chip', '64x af9:mram_chip'],
            'megacells:cell_component_64m': ['64x af9:edram_chip', '64x af9:mram_chip', '64x af9:mram_chip']
        }
        const mega = [
            ['megacells:cell_component_1m', 'ae2:cell_component_256k',
                'gtceu:nano_processor_computer', GTValues.IV, 400, 2],
            ['megacells:cell_component_4m', 'megacells:cell_component_1m',
                'gtceu:quantum_processor_computer', GTValues.LuV, 500, 2],
            ['megacells:cell_component_16m', 'megacells:cell_component_4m',
                'gtceu:crystal_processor_computer', GTValues.ZPM, 600, 4],
            ['megacells:cell_component_64m', 'megacells:cell_component_16m',
                'gtceu:wetware_processor_computer', GTValues.UV, 800, 4]
        ]
        mega.forEach(([out, prev, circuit, tier, ticks, solder]) => {
            event.remove({ mod: 'megacells', output: out })
            const shortId = out.split(':')[1]
            hvSoldered(`mega_${shortId}`, solder, (recipe, fluid) => {
                clean(recipe.itemInputs(`3x ${prev}`, 'ae2:engineering_processor', '4x ae2:sky_dust',
                    'ae2:quartz_glass', circuit)
                    .itemInputs(megaRam[out])
                    .inputFluids(fluid).itemOutputs(out)
                    .duration(ticks).EUt(VA[tier]))
            })
        })
        // 256m: the goal, only in the Advanced Circuit Manufacturer at UHV with computation.
        // True endgame: the 64m stack plus an organized Pico circuit and sanguinite wire.
        event.remove({ mod: 'megacells', output: 'megacells:cell_component_256m' })
        event.recipes.gtceu.advanced_circuit_manufacturer('af9:mega_cell_component_256m')
            .itemInputs(
                '2x gtceu:tritanium_frame',
                '2x megacells:cell_component_64m',
                '4x ae2:engineering_processor',
                'af9:organized_pico_circuit',
                '8x ae2:sky_dust',
                '16x gtceu:fine_tritanium_wire',
                '8x gtceu:europium_plate',
                '8x gtceu:fine_sanguinite_wire',
                '64x af9:mram_chip', '64x af9:mram_chip', '64x af9:edram_chip')
            .inputFluids(
                Fluid.of('gtceu:high_grade_solder', 1152),
                Fluid.of('gtceu:polybenzimidazole', 1152))
            .itemOutputs('megacells:cell_component_256m')
            .CWUt(128)
            .duration(1600)
            .EUt(VA[GTValues.UHV], 300)
        event.remove({ mod: 'megacells', output: 'megacells:bulk_item_cell' })
        hvSoldered('mega_bulk_item_cell', 2, (recipe, fluid) => {
            clean(recipe.itemInputs('ae2:chest', 'megacells:cell_component_1m',
                'gtceu:nano_processor_computer', '4x ae2:sky_dust')
                .inputFluids(fluid).itemOutputs('megacells:bulk_item_cell')
                .duration(400).EUt(VA[GTValues.IV]))
        })
    }

    // ---- Infinity Booster (mod id aeinfinitybooster) ----
    if (Platform.isLoaded('aeinfinitybooster')) {
        event.remove({ mod: 'aeinfinitybooster', output: 'aeinfinitybooster:infinity_card' })
        hvSoldered('ae2_infinity_card', 2, (recipe, fluid) => {
            clean(recipe.itemInputs('ae2:engineering_processor', 'ae2:calculation_processor',
                '4x ae2:fluix_crystal', 'gtceu:advanced_integrated_circuit')
                .inputFluids(fluid).itemOutputs('aeinfinitybooster:infinity_card')
                .duration(300).EUt(HV))
        })
        event.remove({ mod: 'aeinfinitybooster', output: 'aeinfinitybooster:dimension_card' })
        hvSoldered('ae2_dimension_card', 2, (recipe, fluid) => {
            clean(recipe.itemInputs('aeinfinitybooster:infinity_card', 'ae2:engineering_processor',
                '2x minecraft:diamond', 'gtceu:advanced_integrated_circuit')
                .inputFluids(fluid).itemOutputs('aeinfinitybooster:dimension_card')
                .duration(300).EUt(HV))
        })
    }
})
