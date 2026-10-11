// AF9 - Advanced Circuit Manufacturer: its controller (machine: startup_scripts/gtceu/advanced_circuit_manufacturer.js).
// What it makes, the Pico circuits, is in circuits_af9.js.

ServerEvents.recipes(event => {
    // the Genesis pack's recipe, framed in AF9's nichrome-platinum alloy: an EV circuit assembler with EV
    // circuits, titanium, platinum cable and 4 alloy frames
    event.recipes.gtceu.assembler('af9:advanced_circuit_manufacturer')
        .itemInputs('8x #gtceu:circuits/ev', '8x gtceu:double_titanium_plate', '4x gtceu:platinum_single_cable',
            '4x gtceu:nichrome_platinum_frame', 'gtceu:ev_circuit_assembler')
        .itemOutputs('gtceu:advanced_circuit_manufacturer')
        .duration(250)
        .EUt(GTValues.VA[GTValues.EV])
})
