// AF9 - MV circuits without discrete semiconductors
// Transistors and diodes are replaced by lithographed chips (Photolithography Line); resistors and capacitors stay as
// board passives. Any chip works regardless of the mode it was printed in. The metal parts are MV metals (aluminium,
// the MV metal of the Circuits quest page): Aluminium-Silicon bond wire and Kovar pins (electronics_metallurgy.js),
// both from an LV mixer and the EBF, so they can be made before any MV machine.
//
// The Good Electronic Circuit must stay chip-free: the Photolithography Line and its MV parts need MV circuits, so at
// least one MV circuit has to be makeable before the line exists. It uses vacuum tubes (the pre-semiconductor
// rectifier) where GT used diodes. The SoC Microprocessor recipe is GT's own and stays: it has no discrete parts.

ServerEvents.recipes(allthemods => {
    // GT circuit assembler recipes get a tin and a soldering alloy version from GT's recipe generator, which KubeJS
    // recipes skip; this adds both the same way (144 mB tin / 72 mB soldering alloy).
    const circuitAssembler = (id, build) => {
        build(allthemods.recipes.gtceu.circuit_assembler(`af9:${id}`)).inputFluids(Fluid.of('gtceu:tin', 144))
        build(allthemods.recipes.gtceu.circuit_assembler(`af9:${id}_soldering_alloy`)).inputFluids(Fluid.of('gtceu:soldering_alloy', 72))
    }

    // GT's versions (with diodes / transistors) and their generated solder variants
    const replacedGtRecipes = ['electronic_circuit_mv', 'integrated_circuit_mv', 'processor_mv']
    allthemods.remove({ id: 'gtceu:shaped/electronic_circuit_mv' })
    replacedGtRecipes.forEach(id => {
        allthemods.remove({ id: `gtceu:circuit_assembler/${id}` })
        allthemods.remove({ id: `gtceu:circuit_assembler/${id}_soldering_alloy` })
    })

    // ---- Good Electronic Circuit (MV, bootstrap) ----
    allthemods.shaped('gtceu:good_electronic_circuit', ['VPV', 'CBC', 'WCW'], {
        V: 'gtceu:vacuum_tube',
        P: 'gtceu:steel_plate',
        C: 'gtceu:basic_electronic_circuit',
        B: 'gtceu:phenolic_printed_circuit_board',
        W: 'gtceu:copper_single_wire'
    }).id('af9:shaped/good_electronic_circuit')

    circuitAssembler('good_electronic_circuit', recipe => recipe
        .itemInputs(
            'gtceu:phenolic_printed_circuit_board',
            '2x gtceu:basic_electronic_circuit',
            '2x gtceu:vacuum_tube',
            '2x gtceu:copper_single_wire')
        .itemOutputs('gtceu:good_electronic_circuit')
        .duration(300)
        .EUt(GTValues.VA[GTValues.LV]))

    // ---- Good Integrated Circuit (MV): logic chips instead of diodes ----
    circuitAssembler('good_integrated_circuit', recipe => recipe
        .itemInputs(
            'gtceu:phenolic_printed_circuit_board',
            '2x gtceu:basic_integrated_circuit',
            '2x gtceu:ilc_chip',
            '2x #gtceu:resistors',
            '4x gtceu:fine_aluminium_silicon_wire',
            '4x gtceu:kovar_bolt')
        .itemOutputs('2x gtceu:good_integrated_circuit')
        .duration(400)
        .EUt(24))

    // ---- Microprocessor (MV): CPU with a RAM cache instead of discrete transistors ----
    circuitAssembler('micro_processor', recipe => recipe
        .itemInputs(
            'gtceu:plastic_printed_circuit_board',
            'gtceu:cpu_chip',
            'gtceu:ram_chip',
            '4x #gtceu:resistors',
            '4x #gtceu:capacitors',
            '4x gtceu:fine_aluminium_silicon_wire')
        .itemOutputs('2x gtceu:micro_processor')
        .duration(200)
        .EUt(60))
})
