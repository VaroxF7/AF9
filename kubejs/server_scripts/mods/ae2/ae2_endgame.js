// AF9 - AE2 endgame wearables, gregified at HV/EV.
//
// The wireless booster extends any wireless terminal at HV; the quantum armor
// set builds at EV on entangled singularities, processors and sturdy plates.
// Vanilla crafting-table recipes for these outputs are removed mod-scoped.
// See docs/ae2.md.

ServerEvents.recipes(event => {
    if (!Platform.isLoaded('ae2')) return
    const VA = GTValues.VA
    const HV = VA[GTValues.HV]
    const EV = VA[GTValues.EV]
    const gtr = event.recipes.gtceu

    // Three solders at a fixed voltage: certus (canonical), soldering alloy (normal) and
    // high-grade, which runs at half the time.
    const soldered = (id, eut, inputs, outputs, ticks) => {
        const run = (suffix, fluid, time) => {
            gtr.assembler('af9:' + id + suffix)
                .itemInputs(inputs)
                .inputFluids(fluid)
                .itemOutputs(outputs)
                .duration(time)
                .EUt(eut)
        }
        run('', Fluid.of('gtceu:liquid_certus_quartz', 144), ticks)
        run('_solder', Fluid.of('gtceu:soldering_alloy', 72), ticks)
        run('_high_grade', Fluid.of('gtceu:high_grade_solder', 72), Math.ceil(ticks / 2))
    }

    // ---- Wireless booster: range for any wireless terminal ----
    event.remove({ mod: 'ae2', output: 'ae2:wireless_booster' })
    soldered('ae2_wireless_booster', HV,
        ['ae2:wireless_receiver', '2x ae2:fluix_crystal', 'ae2:engineering_processor',
            '2x gtceu:aluminium_plate'],
        'ae2:wireless_booster', 400)

    // ---- Quantum armor: the entangled suit, at EV ----
    // [piece, plates, signature input (each piece differs, so no two match)]
    const armor = [
        ['helmet', '5x gtceu:titanium_plate', 'ae2:logic_processor'],
        ['chestplate', '8x gtceu:titanium_plate', 'ae2:calculation_processor'],
        ['leggings', '7x gtceu:titanium_plate', 'ae2:engineering_processor'],
        ['boots', '4x gtceu:titanium_plate', 'gtceu:micro_processor']
    ]
    armor.forEach(entry => {
        const piece = entry[0]
        const plates = entry[1]
        const signature = entry[2]
        const out = 'ae2:quantum_' + piece
        // the quantum armor is not part of AE2 15 for 1.20.1: skip when absent
        if (!Item.exists(out)) return
        event.remove({ mod: 'ae2', output: out })
        soldered('ae2_quantum_' + piece, EV,
            [plates, 'ae2:quantum_entangled_singularity', 'ae2:engineering_processor',
                '2x ae2:fluix_pearl', signature],
            out, 600)
    })
})
