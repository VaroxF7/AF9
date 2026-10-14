// AF9 - AE2 processors from silicon (MV entry). No AE2 presses, no inscriber prints.
//
//   1. Litho 350nm (existing prints) + cutter: coated silicon wafer -> GT chip wafer ->
//      ilc / cpu / lpic / ram dies. Every chip is printed on silicon.
//   2. Die packaging (assembler, MV): silicon die + electrum foil + copper foil +
//      gold fine wire + RAM die -> AE2 printed processor.
//   3. Carrier (cutter): a silicon wafer dices straight into printed silicon.
//   4. Final assembly (assembler, MV): printed + printed silicon + redstone + MV circuit
//      (good integrated for logic/engineering, micro processor for calculation) -> processor.
//
// The AE2 presses are out of the chain (the Mysterious Cube still drops one random
// press as a souvenir; meteorites still give sky stone). The inscriber
// keeps crushing and the name press. Spec: docs/ae2.md

ServerEvents.recipes(event => {
    if (!Platform.isLoaded('ae2')) return
    const VA = GTValues.VA
    const MV = VA[GTValues.MV]

    // ---- Removed vanilla paths: inscriber prints and processors ----
    const vanilla = ['ae2:printed_silicon', 'ae2:printed_logic_processor',
        'ae2:printed_calculation_processor', 'ae2:printed_engineering_processor',
        'ae2:logic_processor', 'ae2:calculation_processor', 'ae2:engineering_processor']
    vanilla.forEach(out => event.remove({ mod: 'ae2', output: out }))

    // ---- Printed silicon: dice the wafer itself (cutter, MV) ----
    event.recipes.gtceu.cutter('af9:ae2_printed_silicon')
        .itemInputs('gtceu:silicon_wafer')
        .inputFluids(Fluid.of('minecraft:water', 100))
        .itemOutputs('8x ae2:printed_silicon')
        .duration(200)
        .EUt(MV)

    // ---- Die packaging: silicon die + foils + wire + RAM -> AE2 printed (assembler, MV) ----
    const packaging = [
        ['logic_processor', 'gtceu:ilc_chip'],
        ['calculation_processor', 'gtceu:cpu_chip'],
        ['engineering_processor', 'gtceu:lpic_chip']
    ]
    packaging.forEach(([out, die]) => {
        event.recipes.gtceu.assembler(`af9:ae2_printed_${out}`)
            .itemInputs(die, '2x gtceu:electrum_foil', '2x gtceu:copper_foil',
                '4x gtceu:fine_gold_wire', 'gtceu:ram_chip')
            .inputFluids(Fluid.of('gtceu:tin', 144))
            .itemOutputs(`ae2:printed_${out}`)
            .duration(300)
            .EUt(MV)
        event.recipes.gtceu.assembler(`af9:ae2_printed_${out}_soldering_alloy`)
            .itemInputs(die, '2x gtceu:electrum_foil', '2x gtceu:copper_foil',
                '4x gtceu:fine_gold_wire', 'gtceu:ram_chip')
            .inputFluids(Fluid.of('gtceu:soldering_alloy', 72))
            .itemOutputs(`ae2:printed_${out}`)
            .duration(300)
            .EUt(MV)
    })

    // ---- Final assembly: printed + carrier + redstone + MV circuit (assembler, MV) ----
    const processors = [
        ['logic_processor', 'gtceu:good_integrated_circuit'],
        ['calculation_processor', 'gtceu:micro_processor'],
        ['engineering_processor', 'gtceu:good_integrated_circuit']
    ]
    processors.forEach(([out, circuit]) => {
        event.recipes.gtceu.assembler(`af9:ae2_${out}`)
            .itemInputs(`ae2:printed_${out}`, 'ae2:printed_silicon', 'minecraft:redstone', circuit)
            .inputFluids(Fluid.of('gtceu:tin', 144))
            .itemOutputs(`ae2:${out}`)
            .duration(200)
            .EUt(MV)
        event.recipes.gtceu.assembler(`af9:ae2_${out}_soldering_alloy`)
            .itemInputs(`ae2:printed_${out}`, 'ae2:printed_silicon', 'minecraft:redstone', circuit)
            .inputFluids(Fluid.of('gtceu:soldering_alloy', 72))
            .itemOutputs(`ae2:${out}`)
            .duration(200)
            .EUt(MV)
    })
})
