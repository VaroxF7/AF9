// AF9 - AE2 processors on boards and printed silicon. No AE2 presses, no inscriber prints.
//
//   1. Printed processor (assembler): plastic board + 2 foils + 16 fine wires + 4 redstone foils,
//      120 s at MV in a cleanroom on certus or soldering alloy (60 s on high-grade solder). Cheap
//      on purpose — the old die packaging (silicon die + copper foil + fine electrum + 4 RAM chips)
//      is gone.
//   2. Carrier (cutter): a silicon wafer dices straight into printed silicon.
//   3. Final assembly: the Certus Resonating Manufacturer's alone (me_resonance_fabricator_recipes.js):
//      printed + printed silicon + redstone + tier circuit, in a cleanroom with oxygenated air (good
//      integrated for logic and engineering, micro processor for calculation: every processor takes
//      an MV circuit). The plain assembler does not build the processors.
//
// The AE2 presses are out of the chain (the Mysterious Cube still drops one random
// press as a souvenir; meteorites still give sky stone). The inscriber
// keeps crushing and the name press. Spec: docs/ae2.md

ServerEvents.recipes(event => {
    if (!Platform.isLoaded('ae2')) return
    const VA = GTValues.VA
    const MV = VA[GTValues.MV]
    const clean = recipe => recipe.cleanroom(CleanroomType.CLEANROOM)

    // ---- Removed vanilla paths: inscriber prints and processors ----
    // Any recipe, from any mod and in any machine, that prints or builds one of these items is gone —
    // AE2's own recipes, GT forming press imports, addon chambers and datapack leftovers included.
    // (This sweep runs before AF9's own recipes below and before me_resonance_fabricator_recipes.js,
    // so the assembler and me_assembler processor recipes survive it.)
    const vanilla = ['ae2:printed_silicon', 'ae2:printed_logic_processor',
        'ae2:printed_calculation_processor', 'ae2:printed_engineering_processor',
        'ae2:logic_processor', 'ae2:calculation_processor', 'ae2:engineering_processor']
    vanilla.forEach(out => event.remove({ output: out }))

    // ---- All AE2 inscriber recipes, advanced_ae processor chambers and GT forming-press circuit imports ----
    event.remove({ type: 'ae2:inscriber' })
    event.remove({ id: /^ae2:inscriber\// }) // belt and braces: an inscriber recipe that hides from the type filter
    ;[
        'advanced_ae:logic_processor_chamber', 'advanced_ae:accumulation_processor_chamber',
        'advanced_ae:energy_processor_chamber', 'advanced_ae:calculation_processor_chamber',
        'advanced_ae:engineering_processor_chamber', 'advanced_ae:quantum_processor_chamber',
        'gtceu:forming_press/ae2/logic_circuit', 'gtceu:forming_press/appflux/energy_circuit',
        'gtceu:forming_press/aae/quantum_circuit', 'gtceu:forming_press/ae2/calculation_circuit',
        'gtceu:forming_press/megacells/accumulation_circuit', 'gtceu:forming_press/ae2/engineering_circuit'
    ].forEach(id => event.remove({ id: id }))
    // GT's forming press circuit imports of AE2 and its addons, by id pattern and by output, in case an id differs
    event.remove({ id: /^gtceu:forming_press\/(ae2|appflux|aae|megacells)\// })
    ;['ae2:logic_processor', 'ae2:calculation_processor', 'ae2:engineering_processor', 'appflux:energy_processor',
        'advanced_ae:quantum_processor', 'megacells:accumulation_processor'
    ].forEach(out => event.remove({ type: 'gtceu:forming_press', output: out }))

    // ---- Other mods' printed silicon recipes (ExtendedAE cutter, GT forming press) ----
    event.remove({ id: 'expatternprovider:cutter/silicon' })
    event.remove({ id: 'gtceu:forming_press/ae2/ultimate_universal/printed_silicon_circuit' })

    // ---- Printed silicon: dice the wafer itself (cutter, MV), 1 per wafer ----
    event.recipes.gtceu.cutter('af9:ae2_printed_silicon')
        .itemInputs('gtceu:silicon_wafer')
        .inputFluids(Fluid.of('minecraft:water', 100))
        .itemOutputs('ae2:printed_silicon')
        .duration(200)
        .EUt(MV)

    // ---- Printed processors: board + foils + wire + solder (assembler) ----
    // Cheap on purpose: 2 foils, one plastic board, 16 fine wires, 4 redstone foils per printed
    // processor, in a cleanroom at MV power, 120 s (2400t) on certus or soldering alloy — or
    // 60 s on high-grade solder. The die and RAM chips of the old packaging are gone — they
    // keep their jobs in GT circuits.
    // [processor, foil, wire]
    const packaging = [
        ['logic_processor', 'electrum', 'gold'],
        ['calculation_processor', 'gold', 'gold'],
        ['engineering_processor', 'platinum', 'platinum']
    ]
    packaging.forEach(([out, foil, wire]) => {
        const inputs = [`2x gtceu:${foil}_foil`, 'gtceu:plastic_printed_circuit_board',
            `16x gtceu:fine_${wire}_wire`, '4x gtceu:redstone_foil']
        const run = (suffix, fluid, time) => {
            clean(event.recipes.gtceu.assembler(`af9:ae2_printed_${out}${suffix}`)
                .itemInputs(inputs)
                .inputFluids(fluid)
                .itemOutputs(`ae2:printed_${out}`).duration(time).EUt(MV))
        }
        run('', Fluid.of('gtceu:liquid_certus_quartz', 144), 2400)
        run('_solder', Fluid.of('gtceu:soldering_alloy', 72), 2400)
        run('_high_grade', Fluid.of('gtceu:high_grade_solder', 72), 1200)
    })

    // ---- Final assembly: the Certus Resonating Manufacturer's alone ----
    // The processors themselves (logic, calculation, engineering) are assembled there
    // (me_resonance_fabricator_recipes.js): the Certus Resonating Manufacturer binds the printed
    // processor, the carrier and the circuit, and its recipes ask for a cleanroom and an oxygenated
    // air. The plain assembler does not build them.
})

// One tag for the three AE2 processors: recipes that take "a processor of choice" use it instead of a recipe per kind
ServerEvents.tags('item', event => {
    event.add('af9:ae2_processors', ['ae2:logic_processor', 'ae2:calculation_processor', 'ae2:engineering_processor'])
})
