// AF9 - AE2 addons: MEGA Cells climb IV -> UHV (256m only in the Advanced Circuit
// Manufacturer, true endgame), the Infinity Booster cards build at HV.
//
// MEGA components chain off base AE2's 256k component: 3x previous + engineering
// processor + sky dust + quartz glass + its tier circuit, in the assembler on liquid
// certus quartz (or soldering alloy, or high-grade solder at half the time) in a cleanroom — one voltage tier per cell, 1m at
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

    // Three solders: liquid certus quartz (canonical), soldering alloy (normal) and high-grade
    // solder, which runs the same recipe at half the time.
    const hvSoldered = (id, solder, ticks, build) => {
        const run = (suffix, fluid, time) => {
            const recipe = event.recipes.gtceu.assembler(`af9:${id}${suffix}`)
            build(recipe, fluid)
            recipe.duration(time)
        }
        run('', Fluid.of('gtceu:liquid_certus_quartz', 144 * solder), ticks)
        run('_solder', Fluid.of('gtceu:soldering_alloy', 72 * solder), ticks)
        run('_high_grade', Fluid.of('gtceu:high_grade_solder', 72 * solder), Math.ceil(ticks / 2))
    }

    // ---- MEGA Cells (mod id megacells): the endgame ladder, IV -> UHV ----
    // 1m at IV, then one voltage tier per cell, up to the 256m goal at UHV in the
    // Assembly Line. Each takes 3x previous + engineering processor + sky dust +
    // quartz glass + its tier circuit. Bulk cells ride along at IV.
    if (Platform.isLoaded('megacells')) {
        // RAM climbs with the tier: eDRAM at 1m, MRAM from 4m (stacks of 64 chips).
        let megaRam = {
            'megacells:cell_component_1m': ['64x af9:edram_chip'],
            'megacells:cell_component_4m': ['64x af9:mram_chip'],
            'megacells:cell_component_16m': ['64x af9:mram_chip', '64x af9:mram_chip'],
            'megacells:cell_component_64m': ['64x af9:edram_chip', '64x af9:mram_chip', '64x af9:mram_chip']
        }
        let mega = [
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
            hvSoldered(`mega_${shortId}`, solder, ticks, (recipe, fluid) => {
                clean(recipe.itemInputs(`3x ${prev}`, 'ae2:engineering_processor', '4x ae2:sky_dust',
                    'ae2:quartz_glass', circuit)
                    .itemInputs(megaRam[out])
                    .inputFluids(fluid).itemOutputs(out)
                    .EUt(VA[tier]))
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
                Fluid.of('gtceu:liquid_certus_quartz', 1152),
                Fluid.of('gtceu:polybenzimidazole', 1152))
            .itemOutputs('megacells:cell_component_256m')
            .CWUt(128)
            .duration(1600)
            .EUt(VA[GTValues.UHV], 300)
        event.remove({ mod: 'megacells', output: 'megacells:bulk_item_cell' })
        hvSoldered('mega_bulk_item_cell', 2, 400, (recipe, fluid) => {
            clean(recipe.itemInputs('ae2:chest', 'megacells:cell_component_1m',
                'gtceu:nano_processor_computer', '4x ae2:sky_dust')
                .inputFluids(fluid).itemOutputs('megacells:bulk_item_cell')
                .EUt(VA[GTValues.IV]))
        })
    }

    // ---- Infinity Booster (mod id aeinfinitybooster) ----
    if (Platform.isLoaded('aeinfinitybooster')) {
        event.remove({ mod: 'aeinfinitybooster', output: 'aeinfinitybooster:infinity_card' })
        hvSoldered('ae2_infinity_card', 2, 300, (recipe, fluid) => {
            clean(recipe.itemInputs('ae2:engineering_processor', 'ae2:calculation_processor',
                '4x ae2:fluix_crystal', 'gtceu:advanced_integrated_circuit')
                .inputFluids(fluid).itemOutputs('aeinfinitybooster:infinity_card')
                .EUt(HV))
        })
        event.remove({ mod: 'aeinfinitybooster', output: 'aeinfinitybooster:dimension_card' })
        hvSoldered('ae2_dimension_card', 2, 300, (recipe, fluid) => {
            clean(recipe.itemInputs('aeinfinitybooster:infinity_card', 'ae2:engineering_processor',
                '2x minecraft:diamond', 'gtceu:advanced_integrated_circuit')
                .inputFluids(fluid).itemOutputs('aeinfinitybooster:dimension_card')
                .EUt(HV))
        })
    }

    // ---- Addon processors (assembler, no cleanroom, liquid certus quartz) ----
    // The inscriber, the advanced_ae chambers and the forming press imports are gone, so the three addon
    // processors build from AF9's own AE2 processors instead. [id, output, base, extra inputs, eut, ticks]
    const addonProcessors = [
        ['megacells', 'megacells:accumulation_processor', 'ae2:calculation_processor',
            ['gtceu:advanced_integrated_circuit', '2x gtceu:ram_chip', '4x gtceu:redstone_foil'], HV, 300],
        ['appflux', 'appflux:energy_processor', 'ae2:engineering_processor',
            ['gtceu:micro_processor_computer', '4x gtceu:lithium_dust', '4x gtceu:platinum_foil'], VA[GTValues.EV], 400],
        ['advanced_ae', 'advanced_ae:quantum_processor', 'ae2:engineering_processor',
            ['gtceu:quantum_processor_computer', '2x gtceu:ram_chip', '4x gtceu:fine_platinum_wire'], VA[GTValues.IV], 500]
    ]
    addonProcessors.forEach(([mod, out, base, extra, eut, ticks]) => {
        if (!Platform.isLoaded(mod)) return
        event.remove({ mod: mod, output: out })
        const id = out.split(':')[1]
        event.recipes.gtceu.assembler(`af9:addon_${id}`)
            .itemInputs([base].concat(extra))
            .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144))
            .itemOutputs(out).duration(ticks).EUt(eut)
    })
})
