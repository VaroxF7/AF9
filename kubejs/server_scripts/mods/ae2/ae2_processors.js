// AF9 - AE2 processors through the fab (MV entry).
//
// Printed circuits are exposed in the Photolithography Line (350 nm, MV): a coated silicon
// wafer under the AE2 press as its mask, the mode's own fluids, the mode's break chance.
// Final assembly (printed + printed silicon + redstone) runs in the assembler with solder,
// like GT's circuits. The inscriber keeps crushing and the name press; its printed-circuit
// and processor recipes are removed. Presses still come from meteorites, so the hunt stays.
// Spec: docs/ae2.md

ServerEvents.recipes(event => {
    if (!Platform.isLoaded('ae2')) return
    const VA = GTValues.VA

    // ---- Printed circuits (lithography_350nm: 2 item in, 2 out, 3 fluids in) ----
    // One coated wafer gives 4 printeds; the press is the mask and is not consumed.
    const printeds = [
        ['printed_silicon', 'ae2:silicon_press'],
        ['printed_logic_processor', 'ae2:logic_processor_press'],
        ['printed_calculation_processor', 'ae2:calculation_processor_press'],
        ['printed_engineering_processor', 'ae2:engineering_processor_press']
    ]
    printeds.forEach(([out, press]) => {
        event.remove({ mod: 'ae2', output: `ae2:${out}` })
        event.recipes.gtceu.lithography_350nm(`af9:print_ae2_${out}_350nm`)
            .itemInputs('af9:coated_silicon_wafer')
            .notConsumable(press)
            .inputFluids(
                Fluid.of('gtceu:tmah_developer', 200),
                Fluid.of('gtceu:distilled_water', 1000),
                Fluid.of('gtceu:extreme_clean_dry_air', 1000))
            .itemOutputs(`4x ae2:${out}`)
            .chancedOutput('af9:broken_silicon_wafer', 200, 0)
            .duration(900)
            .EUt(VA[GTValues.MV], 4)
    })

    // ---- Processors (assembler, MV, tin budget + soldering alloy canonical) ----
    const processors = [
        ['logic_processor', 'ae2:printed_logic_processor'],
        ['calculation_processor', 'ae2:printed_calculation_processor'],
        ['engineering_processor', 'ae2:printed_engineering_processor']
    ]
    processors.forEach(([out, printedItem]) => {
        event.remove({ mod: 'ae2', output: `ae2:${out}` })
        event.recipes.gtceu.assembler(`af9:ae2_${out}`)
            .itemInputs(printedItem, 'ae2:printed_silicon', 'minecraft:redstone')
            .inputFluids(Fluid.of('gtceu:tin', 144))
            .itemOutputs(`ae2:${out}`)
            .duration(200)
            .EUt(VA[GTValues.MV])
        event.recipes.gtceu.assembler(`af9:ae2_${out}_soldering_alloy`)
            .itemInputs(printedItem, 'ae2:printed_silicon', 'minecraft:redstone')
            .inputFluids(Fluid.of('gtceu:soldering_alloy', 72))
            .itemOutputs(`ae2:${out}`)
            .duration(200)
            .EUt(VA[GTValues.MV])
    })
})
