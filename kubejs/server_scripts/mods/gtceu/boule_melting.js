// AF9 - Boule Melting recipes (the EBF's second mode) and the Endion coils. Items, materials and coils:
// startup_scripts/gtceu/boule_melting.js. Spec: docs/semiconductor-factory.md
//
// A boule is ten times the material of GT's old boule. So it fits the EBF's three input slots, the material is blended
// into melt charges first (blending mode): 10 charges + 1 seed crystal (crystal growth mode) + 1 crucible.
// EU/t: 2x GT's for silicon, 4x phosphorus, 6x naquadah, 8x neutronium (as amps of the same tier); the substrates in
// between follow on.

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const EG_SI = 'gtceu:electronic_grade_silicon_dust'

    // ---- Endion: the End's noble gas ----
    // Ender Air, from the gas collector in the End: the centrifuge now also spins a little endion out of it, the
    // distillation tower gets more from liquid Ender Air.
    event.remove({ id: 'gtceu:centrifuge/ender_air_separation' })
    event.recipes.gtceu.centrifuge('af9:ender_air_separation')
        .inputFluids(Fluid.of('gtceu:ender_air', 10000))
        .outputFluids(Fluid.of('gtceu:nitrogen_dioxide', 3900), Fluid.of('gtceu:deuterium', 1000),
            Fluid.of('gtceu:endion', 250))
        .duration(1600)
        .EUt(VA[GTValues.HV])
    event.remove({ id: 'gtceu:distillation_tower/distill_liquid_ender_air' })
    event.recipes.gtceu.distillation_tower('af9:distill_liquid_ender_air')
        .inputFluids(Fluid.of('gtceu:liquid_ender_air', 200000))
        .outputFluids(Fluid.of('gtceu:nitrogen_dioxide', 122000), Fluid.of('gtceu:deuterium', 50000),
            Fluid.of('gtceu:helium', 15000), Fluid.of('gtceu:tritium', 10000), Fluid.of('gtceu:krypton', 1000),
            Fluid.of('gtceu:xenon', 1000), Fluid.of('gtceu:radon', 1000), Fluid.of('gtceu:endion', 8000))
        .chancedOutput('gtceu:ender_pearl_dust', 1000, 0)
        .duration(2000)
        .EUt(VA[GTValues.IV])

    // ---- Endionite and the Endion coils ----
    // Tungstensteel and naquadah soaked in endion; smelted by GT's EBF (5400 K) from the dust
    event.recipes.gtceu.mixer('af9:endionite_dust')
        .itemInputs('2x gtceu:tungsten_steel_dust', 'gtceu:naquadah_dust')
        .inputFluids(Fluid.of('gtceu:endion', 1000))
        .itemOutputs('3x gtceu:endionite_dust')
        .duration(400)
        .EUt(VA[GTValues.EV])

    event.recipes.gtceu.assembler('af9:endion_coil_block')
        .itemInputs('16x gtceu:fine_endionite_wire', '8x gtceu:endionite_foil', 'gtceu:tungsten_steel_frame')
        .inputFluids(Fluid.of('gtceu:endion', 2000))
        .itemOutputs('af9:endion_coil_block')
        .duration(900)
        .EUt(VA[GTValues.IV])
    event.recipes.gtceu.assembler('af9:resonant_endion_coil_block')
        .itemInputs('af9:endion_coil_block', '32x gtceu:fine_endionite_wire', '4x gtceu:neutronium_plate',
            'gtceu:uv_field_generator')
        .inputFluids(Fluid.of('gtceu:supercooled_endion', 1000))
        .itemOutputs('af9:resonant_endion_coil_block')
        .duration(1200)
        .EUt(VA[GTValues.UV])

    // ---- Crucibles ----
    // Fused quartz: quartzite melted into a crucible (1800 K); tritanium for the exotic melts
    event.recipes.gtceu.electric_blast_furnace('af9:fused_quartz_crucible')
        .itemInputs('6x gtceu:quartzite_dust')
        .itemOutputs('af9:fused_quartz_crucible')
        .blastFurnaceTemp(1800)
        .duration(400)
        .EUt(VA[GTValues.MV])
    event.recipes.gtceu.assembler('af9:tritanium_crucible')
        .itemInputs('6x gtceu:tritanium_plate')
        .inputFluids(Fluid.of('gtceu:supercooled_argon', 500))
        .itemOutputs('af9:tritanium_crucible')
        .duration(400)
        .EUt(VA[GTValues.UV])

    // ---- Boules ----
    // All boules are made in the High Temperature Precision Fabricator now (precision_fabricator.js); the EBF mode has
    // no recipes left. GT's EBF boules go too (the CZ recipes of the old fab furnaces: fab_chemistry.js).
    ;['silicon_boule', 'phosphorus_boule', 'naquadah_boule', 'neutronium_boule']
        .forEach(boule => event.remove({ id: `gtceu:electric_blast_furnace/${boule}` }))

    // ---- Cutting the new boules (GT cuts its own four) ----
    const cuts = [
        ['trinium', 64, GTValues.IV, 2000],
        ['naquadria', 80, GTValues.LuV, 2200],
        ['strange_matter', 96, GTValues.UV, 2800],
        ['chromodynium', 128, GTValues.UHV, 3200]
    ]
    cuts.forEach(([id, wafers, tier, duration]) => {
        const outputs = []
        for (let left = wafers; left > 0; left -= 64) outputs.push(`${Math.min(left, 64)}x af9:${id}_wafer`)
        event.recipes.gtceu.cutter(`af9:cut_${id}_boule`)
            .itemInputs(`af9:${id}_boule`)
            .inputFluids(Fluid.of('gtceu:lubricant', 250))
            .itemOutputs(outputs)
            .duration(duration)
            .EUt(VA[tier])
            .cleanroom(CleanroomType.CLEANROOM)
    })
})
