// AF9 - Advanced Circuit Manufacturer: its controller (machine: startup_scripts/gtceu/advanced_circuit_manufacturer.js).
// What it makes, the Pico circuits, is in circuits_af9.js.

ServerEvents.recipes(event => {
    // the Genesis pack's recipe: an EV circuit assembler with EV circuits, titanium and platinum cable
    event.recipes.gtceu.assembler('af9:advanced_circuit_manufacturer')
        .itemInputs('8x #gtceu:circuits/ev', '8x gtceu:double_titanium_plate', '4x gtceu:platinum_single_cable',
            'gtceu:ev_circuit_assembler')
        .itemOutputs('gtceu:advanced_circuit_manufacturer')
        .duration(250)
        .EUt(GTValues.VA[GTValues.EV])
})
