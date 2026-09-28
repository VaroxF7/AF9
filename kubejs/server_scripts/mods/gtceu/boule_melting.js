// AF9 - Boule Melting recipes (the EBF's second mode) and the Endion coils. Items, materials and coils:
// startup_scripts/gtceu/boule_melting.js. Spec: docs/semiconductor-factory.md
//
// A boule is ten times the material of GT's old boule. So it fits the EBF's three input slots, the material is blended
// into melt charges first (SMC blending): 10 charges + 1 seed crystal (SMC crystal growth) + 1 crucible.
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
        .itemOutputs('kubejs:endion_coil_block')
        .duration(900)
        .EUt(VA[GTValues.IV])
    event.recipes.gtceu.assembler('af9:resonant_endion_coil_block')
        .itemInputs('kubejs:endion_coil_block', '32x gtceu:fine_endionite_wire', '4x gtceu:neutronium_plate',
            'gtceu:uv_field_generator')
        .inputFluids(Fluid.of('gtceu:supercooled_endion', 1000))
        .itemOutputs('kubejs:resonant_endion_coil_block')
        .duration(1200)
        .EUt(VA[GTValues.UV])

    // ---- Crucibles ----
    // Fused quartz: quartzite melted into a crucible (1800 K); tritanium for the exotic melts
    event.recipes.gtceu.electric_blast_furnace('af9:fused_quartz_crucible')
        .itemInputs('6x gtceu:quartzite_dust')
        .itemOutputs('kubejs:fused_quartz_crucible')
        .blastFurnaceTemp(1800)
        .duration(400)
        .EUt(VA[GTValues.MV])
    event.recipes.gtceu.assembler('af9:tritanium_crucible')
        .itemInputs('6x gtceu:tritanium_plate')
        .inputFluids(Fluid.of('gtceu:supercooled_argon', 500))
        .itemOutputs('kubejs:tritanium_crucible')
        .duration(400)
        .EUt(VA[GTValues.UV])

    // ---- Melt charges (SMC blending), seed crystals (SMC crystal growth) and boules (EBF Boule Melting) ----
    // charge: one of GT's old boule inputs; seed: grown at the boule's temperature; boule: 10 charges + seed + crucible
    // eu: [voltage tier, energy multiplier] = the boule costs multiplier x (1A of that tier for `duration`). It draws at
    // most 4A, what two normal energy hatches give the EBF; a bigger multiplier makes the run longer instead.
    const BOULE_MAX_AMPS = 4
    const boules = [
        { id: 'silicon', charge: [`32x ${EG_SI}`, 'gtceu:tiny_boron_dust'], chargeTier: GTValues.MV,
            seed: [`4x ${EG_SI}`, 'gtceu:tiny_boron_dust'], seedGas: ['gtceu:argon', 100],
            gas: ['gtceu:argon', 2500], temp: 1784, duration: 9000, eu: [GTValues.MV, 2], crucible: 'fused_quartz',
            boule: 'gtceu:silicon_boule' },
        { id: 'phosphorus', charge: [`64x ${EG_SI}`, '8x gtceu:phosphorus_dust'], chargeTier: GTValues.HV,
            seed: [`4x ${EG_SI}`, 'gtceu:small_phosphorus_dust'], seedGas: ['gtceu:argon', 200],
            gas: ['gtceu:argon', 10000], temp: 2484, duration: 12000, eu: [GTValues.HV, 4], crucible: 'fused_quartz',
            boule: 'gtceu:phosphorus_boule' },
        { id: 'naquadah', charge: [`144x ${EG_SI}`, 'gtceu:naquadah_dust', 'gtceu:gallium_arsenide_dust'],
            chargeTier: GTValues.EV, seed: [`4x ${EG_SI}`, 'gtceu:small_naquadah_dust'], seedGas: ['gtceu:argon', 400],
            gas: ['gtceu:argon', 80000], temp: 5400, duration: 15000, eu: [GTValues.EV, 6], crucible: 'fused_quartz',
            boule: 'gtceu:naquadah_boule' },
        { id: 'trinium', charge: [`192x ${EG_SI}`, '2x gtceu:trinium_dust', 'gtceu:gallium_arsenide_dust'],
            chargeTier: GTValues.IV, seed: [`4x ${EG_SI}`, 'gtceu:small_trinium_dust'], seedGas: ['gtceu:argon', 400],
            gas: ['gtceu:argon', 80000], temp: 6000, duration: 16000, eu: [GTValues.IV, 6], crucible: 'fused_quartz',
            boule: 'kubejs:trinium_boule' },
        { id: 'naquadria', charge: [`240x ${EG_SI}`, '2x gtceu:naquadria_dust', '2x gtceu:gallium_arsenide_dust'],
            chargeTier: GTValues.LuV, seed: [`4x ${EG_SI}`, 'gtceu:small_naquadria_dust'], seedGas: ['gtceu:xenon', 200],
            gas: ['gtceu:xenon', 80000], temp: 6800, duration: 17000, eu: [GTValues.IV, 7], crucible: 'fused_quartz',
            boule: 'kubejs:naquadria_boule' },
        { id: 'neutronium', charge: [`288x ${EG_SI}`, '4x gtceu:neutronium_dust', '2x gtceu:gallium_arsenide_dust'],
            chargeTier: GTValues.ZPM, seed: [`4x ${EG_SI}`, 'gtceu:small_neutronium_dust'], seedGas: ['gtceu:xenon', 400],
            gas: ['gtceu:xenon', 80000], temp: 7200, duration: 18000, eu: [GTValues.IV, 8], crucible: 'fused_quartz',
            boule: 'gtceu:neutronium_boule' },
        { id: 'strange_matter', charge: [`288x ${EG_SI}`, 'gtceu:strange_matter_dust', '4x gtceu:neutronium_dust'],
            chargeTier: GTValues.UV, seed: [`4x ${EG_SI}`, 'gtceu:small_strange_matter_dust'],
            seedGas: ['gtceu:xenon', 800], gas: ['gtceu:xenon', 160000], temp: 9000, duration: 20000,
            eu: [GTValues.UV, 8], crucible: 'tritanium', boule: 'kubejs:strange_matter_boule' },
        { id: 'chromodynium', charge: ['4x gtceu:chromodynium_dust', 'gtceu:strange_matter_dust'],
            chargeTier: GTValues.UHV, seed: ['4x gtceu:small_chromodynium_dust', 'gtceu:small_strange_matter_dust'],
            seedGas: ['gtceu:endion', 400], gas: ['gtceu:endion', 80000], temp: 12000, duration: 24000,
            eu: [GTValues.UHV, 8], crucible: 'tritanium', boule: 'kubejs:chromodynium_boule' }
    ]
    // GT's EBF boules (the CZ recipes of the SMC furnaces are gone too, fab_chemistry.js)
    ;['silicon_boule', 'phosphorus_boule', 'naquadah_boule', 'neutronium_boule']
        .forEach(boule => event.remove({ id: `gtceu:electric_blast_furnace/${boule}` }))
    boules.forEach(b => {
        event.recipes.gtceu.fab_blending(`af9:${b.id}_melt_charge`)
            .itemInputs(b.charge)
            .itemOutputs(`kubejs:${b.id}_melt_charge`)
            .duration(200)
            .EUt(VA[b.chargeTier])
        event.recipes.gtceu.fab_crystal_growth(`af9:${b.id}_seed_crystal`)
            .itemInputs(b.seed)
            .inputFluids(Fluid.of(b.seedGas[0], b.seedGas[1]))
            .itemOutputs(`kubejs:${b.id}_seed_crystal`)
            .blastFurnaceTemp(b.temp)
            .duration(1200)
            .EUt(VA[b.chargeTier])
        event.recipes.gtceu.boule_melting(`af9:${b.id}_boule`)
            .itemInputs(`10x kubejs:${b.id}_melt_charge`, `kubejs:${b.id}_seed_crystal`,
                `kubejs:${b.crucible}_crucible`)
            .inputFluids(Fluid.of(b.gas[0], b.gas[1]))
            .itemOutputs(b.boule)
            .blastFurnaceTemp(b.temp)
            .duration(b.duration * b.eu[1] / Math.min(b.eu[1], BOULE_MAX_AMPS))
            .EUt(VA[b.eu[0]], Math.min(b.eu[1], BOULE_MAX_AMPS))
    })

    // ---- Cutting the new boules (GT cuts its own four) ----
    const cuts = [
        ['trinium', 64, GTValues.IV, 2000],
        ['naquadria', 80, GTValues.LuV, 2200],
        ['strange_matter', 96, GTValues.UV, 2800],
        ['chromodynium', 128, GTValues.UHV, 3200]
    ]
    cuts.forEach(([id, wafers, tier, duration]) => {
        const outputs = []
        for (let left = wafers; left > 0; left -= 64) outputs.push(`${Math.min(left, 64)}x kubejs:${id}_wafer`)
        event.recipes.gtceu.cutter(`af9:cut_${id}_boule`)
            .itemInputs(`kubejs:${id}_boule`)
            .inputFluids(Fluid.of('gtceu:lubricant', 250))
            .itemOutputs(outputs)
            .duration(duration)
            .EUt(VA[tier])
            .cleanroom(CleanroomType.CLEANROOM)
    })
})
