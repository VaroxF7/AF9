// AF9 - Wireless energy hatches (startup_scripts/gtceu/wireless_energy.js). Assembler, at the hull's tier: GT's energy
// hatch of that size (the laser hatch from 256A), two sensors (receiver) or emitters (transmitter), a field generator
// for the link, two circuits of the tier and soldering alloy, plus two RF Transceiver chips (the radio link).

ServerEvents.recipes(allthemods => {
    const VA = GTValues.VA
    // tier, receiver base, transmitter base (the hatch the wireless one is built on)
    const variants = [
        ['ev', 'gtceu:ev_energy_input_hatch', 'gtceu:ev_energy_output_hatch'],
        ['iv', 'gtceu:iv_energy_input_hatch_4a', 'gtceu:iv_energy_output_hatch_4a'],
        ['luv', 'gtceu:luv_energy_input_hatch_16a', 'gtceu:luv_energy_output_hatch_16a'],
        ['zpm', '4x gtceu:zpm_energy_input_hatch_16a', '4x gtceu:zpm_energy_output_hatch_16a'],
        ['uv', 'gtceu:uv_256a_laser_target_hatch', 'gtceu:uv_256a_laser_source_hatch'],
        ['uhv', 'gtceu:uhv_1024a_laser_target_hatch', 'gtceu:uhv_1024a_laser_source_hatch']]
    variants.forEach(([tier, receiverBase, transmitterBase], index) => {
        const voltage = VA[GTValues.EV + index]
        allthemods.recipes.gtceu.assembler(`af9:${tier}_wireless_energy_receiver`)
            .itemInputs(receiverBase, `2x gtceu:${tier}_sensor`, `gtceu:${tier}_field_generator`,
                `2x #gtceu:circuits/${tier}`, '2x kubejs:rf_transceiver_chip')
            .inputFluids(Fluid.of('gtceu:soldering_alloy', 576))
            .itemOutputs(`gtceu:${tier}_wireless_energy_receiver`)
            .duration(600)
            .EUt(voltage)
        allthemods.recipes.gtceu.assembler(`af9:${tier}_wireless_energy_transmitter`)
            .itemInputs(transmitterBase, `2x gtceu:${tier}_emitter`, `gtceu:${tier}_field_generator`,
                `2x #gtceu:circuits/${tier}`, '2x kubejs:rf_transceiver_chip')
            .inputFluids(Fluid.of('gtceu:soldering_alloy', 576))
            .itemOutputs(`gtceu:${tier}_wireless_energy_transmitter`)
            .duration(600)
            .EUt(voltage)
    })
})
