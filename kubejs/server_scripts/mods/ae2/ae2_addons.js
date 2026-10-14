// AF9 - AE2 addons at HV: MEGA Cells components and the Infinity Booster cards.
//
// Everything here builds in the assembler at HV with high-grade solder (tin budget),
// in a cleanroom. MEGA components chain off base AE2's 256k component: 3x previous +
// engineering processor + sky dust + quartz glass + HV circuit. Each file section is
// guarded by its mod id (neither addon is in the dev run), and removals are mod-scoped
// so the af9: recipes never match. MEGA cell assembly (housing + component) and any
// ExtendedAE recipes stay vanilla and follow through the new components.
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

    // ---- MEGA Cells (mod id megacells) ----
    if (Platform.isLoaded('megacells')) {
        const mega = [
            ['megacells:cell_component_1m', 'ae2:cell_component_256k', 400, 2],
            ['megacells:cell_component_4m', 'megacells:cell_component_1m', 400, 2],
            ['megacells:cell_component_16m', 'megacells:cell_component_4m', 500, 2],
            ['megacells:cell_component_64m', 'megacells:cell_component_16m', 500, 4],
            ['megacells:cell_component_256m', 'megacells:cell_component_64m', 600, 4]
        ]
        mega.forEach(([out, prev, ticks, solder]) => {
            event.remove({ mod: 'megacells', output: out })
            const shortId = out.split(':')[1]
            hvSoldered(`mega_${shortId}`, solder, (recipe, fluid) => {
                clean(recipe.itemInputs(`3x ${prev}`, 'ae2:engineering_processor', '4x ae2:sky_dust',
                    'ae2:quartz_glass', 'gtceu:advanced_integrated_circuit')
                    .inputFluids(fluid).itemOutputs(out)
                    .duration(ticks).EUt(HV))
            })
        })
        event.remove({ mod: 'megacells', output: 'megacells:bulk_item_cell' })
        hvSoldered('mega_bulk_item_cell', 2, (recipe, fluid) => {
            clean(recipe.itemInputs('ae2:chest', 'megacells:cell_component_1m',
                'gtceu:advanced_integrated_circuit', '4x ae2:sky_dust')
                .inputFluids(fluid).itemOutputs('megacells:bulk_item_cell')
                .duration(400).EUt(HV))
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
