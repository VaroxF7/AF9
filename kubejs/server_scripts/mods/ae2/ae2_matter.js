// AF9 - Matter condenser mechanics and spatial storage, gregified at HV.
//
// The condenser block itself builds in ae2_devices.js; what changes here is what
// goes through it: singularities assemble at HV (matter balls + fluix + an
// engineering processor, solder pair), entangled pairs at EV, and the three
// spatial cells (2/16/128) assemble at HV from processors, quartz glass and
// sky dust. Vanilla crafting-table recipes for these outputs are removed
// mod-scoped. See docs/ae2.md.

ServerEvents.recipes(event => {
    if (!Platform.isLoaded('ae2')) return
    const VA = GTValues.VA
    const HV = VA[GTValues.HV]
    const EV = VA[GTValues.EV]
    const gtr = event.recipes.gtceu

    // Solder pair at a fixed voltage.
    const soldered = (id, eut, inputs, outputs, ticks) => {
        gtr.assembler('af9:' + id)
            .itemInputs(inputs)
            .inputFluids(Fluid.of('gtceu:tin', 144))
            .itemOutputs(outputs)
            .duration(ticks)
            .EUt(eut)
        gtr.assembler('af9:' + id + '_soldering_alloy')
            .itemInputs(inputs)
            .inputFluids(Fluid.of('gtceu:soldering_alloy', 72))
            .itemOutputs(outputs)
            .duration(ticks)
            .EUt(eut)
    }

    // ---- Singularity: condensed matter, pressed at HV ----
    event.remove({ mod: 'ae2', output: 'ae2:singularity' })
    soldered('ae2_singularity', HV,
        ['32x ae2:matter_ball', '4x ae2:fluix_crystal', 'ae2:engineering_processor'],
        'ae2:singularity', 600)

    // ---- Entangled pair: the two ends of one link, at EV ----
    event.remove({ mod: 'ae2', output: 'ae2:quantum_entangled_singularity' })
    soldered('ae2_entangled_singularity', EV,
        ['ae2:singularity', '2x ae2:calculation_processor', '4x ae2:fluix_pearl'],
        '2x ae2:quantum_entangled_singularity', 600)

    // ---- Spatial cells: the pocket dimension in a shell ----
    // [suffix, previous or base, secondary, circuit]
    const spatial = [
        ['2', '4x ae2:quartz_glass', '4x ae2:fluix_crystal', 'ae2:engineering_processor'],
        ['16', 'ae2:spatial_storage_cell_2_cubed', '4x minecraft:diamond', 'ae2:engineering_processor'],
        ['128', 'ae2:spatial_storage_cell_16_cubed', '4x ae2:sky_dust', 'ae2:calculation_processor']
    ]
    spatial.forEach(entry => {
        const suffix = entry[0]
        const base = entry[1]
        const secondary = entry[2]
        const circuit = entry[3]
        const out = 'ae2:spatial_storage_cell_' + suffix + '_cubed'
        event.remove({ mod: 'ae2', output: out })
        soldered('ae2_spatial_' + suffix, HV,
            [base, secondary, circuit, 'ae2:item_cell_housing'],
            out, 400)
    })
})
