// AF9 - AE2 processors from silicon (MV -> HV -> EV). No AE2 presses, no inscriber prints.
//
//   1. Litho 350nm (existing prints) + cutter: coated silicon wafer -> GT chip wafer ->
//      ilc / cpu / lpic / ram dies. Every chip is printed on silicon.
//   2. Die packaging (assembler): silicon die + foil + copper foil + fine wire + RAM die
//      -> AE2 printed processor. Logic MV, calculation HV (cleanroom), engineering EV (cleanroom).
//   3. Carrier (cutter): a silicon wafer dices straight into printed silicon.
//   4. Final assembly (assembler): printed + printed silicon + redstone + tier circuit
//      (logic: good integrated MV; calculation: advanced integrated HV; engineering: micro computer EV).
//
// The AE2 presses are out of the chain (the Mysterious Cube still drops one random
// press as a souvenir; meteorites still give sky stone). The inscriber
// keeps crushing and the name press. Spec: docs/ae2.md

ServerEvents.recipes(event => {
    if (!Platform.isLoaded('ae2')) return
    const VA = GTValues.VA
    const MV = VA[GTValues.MV]
    const HV = VA[GTValues.HV]
    const EV = VA[GTValues.EV]
    const clean = recipe => recipe.cleanroom(CleanroomType.CLEANROOM)

    // ---- Removed vanilla paths: inscriber prints and processors ----
    const vanilla = ['ae2:printed_silicon', 'ae2:printed_logic_processor',
        'ae2:printed_calculation_processor', 'ae2:printed_engineering_processor',
        'ae2:logic_processor', 'ae2:calculation_processor', 'ae2:engineering_processor']
    vanilla.forEach(out => event.remove({ mod: 'ae2', output: out }))

    // ---- All AE2 inscriber recipes, advanced_ae processor chambers and GT forming-press circuit imports ----
    event.remove({ type: 'ae2:inscriber' })
    ;[
        'advanced_ae:logic_processor_chamber', 'advanced_ae:accumulation_processor_chamber',
        'advanced_ae:energy_processor_chamber', 'advanced_ae:calculation_processor_chamber',
        'advanced_ae:engineering_processor_chamber', 'advanced_ae:quantum_processor_chamber',
        'gtceu:forming_press/ae2/logic_circuit', 'gtceu:forming_press/appflux/energy_circuit',
        'gtceu:forming_press/aae/quantum_circuit', 'gtceu:forming_press/ae2/calculation_circuit',
        'gtceu:forming_press/megacells/accumulation_circuit', 'gtceu:forming_press/ae2/engineering_circuit'
    ].forEach(id => event.remove({ id: id }))

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

    // ---- Die packaging: silicon die + foils + wire + RAM -> AE2 printed (assembler) ----
    // Redstone foil and fine electrum wire carry the printed circuit. Logic packs at MV (tin / soldering alloy),
    // calculation at HV and engineering at EV, both in a cleanroom on high-grade solder (tin stays as budget).
    // [processor, die, eut, ticks, cleanroom, foil, wire]
    const packaging = [
        ['logic_processor', 'gtceu:ilc_chip', MV, 300, false, 'electrum', 'gold'],
        ['calculation_processor', 'gtceu:cpu_chip', HV, 400, true, 'electrum', 'gold'],
        ['engineering_processor', 'gtceu:lpic_chip', EV, 500, true, 'platinum', 'platinum']
    ]
    packaging.forEach(([out, die, eut, ticks, cr, foil, wire]) => {
        const inputs = [die, `2x gtceu:${foil}_foil`, '2x gtceu:copper_foil', '2x gtceu:redstone_foil',
            `4x gtceu:fine_${wire}_wire`, '2x gtceu:fine_electrum_wire', 'gtceu:ram_chip']
        const solders = cr
            ? [['', Fluid.of('gtceu:tin', 144)], ['_high_grade', Fluid.of('gtceu:high_grade_solder', 72)]]
            : [['', Fluid.of('gtceu:tin', 144)], ['_soldering_alloy', Fluid.of('gtceu:soldering_alloy', 72)]]
        solders.forEach(([suffix, fluid]) => {
            const r = event.recipes.gtceu.assembler(`af9:ae2_printed_${out}${suffix}`)
                .itemInputs(inputs).inputFluids(fluid)
                .itemOutputs(`ae2:printed_${out}`).duration(ticks).EUt(eut)
            if (cr) clean(r)
        })
    })

    // ---- Final assembly: printed + carrier + redstone + tier circuit on liquid certus quartz (assembler) ----
    // The assembler takes one fluid, so the solder went into the packaging above and this step runs on liquid
    // certus quartz alone (the same foil and wire again). Logic MV (good integrated), calculation HV (advanced
    // integrated), engineering EV (micro processor computer).
    // [processor, circuit, eut, ticks, cleanroom]
    const processors = [
        ['logic_processor', 'gtceu:good_integrated_circuit', MV, 200, false],
        ['calculation_processor', 'gtceu:advanced_integrated_circuit', HV, 300, true],
        ['engineering_processor', 'gtceu:micro_processor_computer', EV, 400, true]
    ]
    processors.forEach(([out, circuit, eut, ticks, cr]) => {
        const r = event.recipes.gtceu.assembler(`af9:ae2_${out}`)
            .itemInputs(`ae2:printed_${out}`, 'ae2:printed_silicon', 'minecraft:redstone', circuit,
                '2x gtceu:redstone_foil', '2x gtceu:fine_electrum_wire')
            .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144))
            .itemOutputs(`ae2:${out}`).duration(ticks).EUt(eut)
        if (cr) clean(r)
    })
})
