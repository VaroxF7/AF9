// AF9 - Wireless energy hatches (startup_scripts/gtceu/wireless_energy.js). Assembler, at the hull's tier: GT's energy
// hatch of that size (the laser hatch from 256A), two sensors (receiver) or emitters (transmitter), a field generator
// for the link (UHV: twice that in UV parts), two circuits of the tier and soldering alloy, plus two RF Transceiver chips (the radio link) and a SAW
// Filter with two aluminium nitride piezo films (the band filter in front of the radio).

ServerEvents.recipes(event => {
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
        // GT has no parts above UV while its high-tier content is off, as it is in the pack: UHV takes twice the UV parts
        const parts = tier === 'uhv' ? 'uv' : tier
        const n = tier === 'uhv' ? 2 : 1
        event.recipes.gtceu.assembler(`af9:${tier}_wireless_energy_receiver`)
            .itemInputs(receiverBase, `${2 * n}x gtceu:${parts}_sensor`, `${n}x gtceu:${parts}_field_generator`,
                `2x #gtceu:circuits/${tier}`, '2x af9:rf_transceiver_chip', 'af9:saw_filter_chip',
                '2x gtceu:aluminium_nitride_dust')
            .inputFluids(Fluid.of('gtceu:soldering_alloy', 576))
            .itemOutputs(`gtceu:${tier}_wireless_energy_receiver`)
            .duration(600)
            .EUt(voltage)
        event.recipes.gtceu.assembler(`af9:${tier}_wireless_energy_transmitter`)
            .itemInputs(transmitterBase, `${2 * n}x gtceu:${parts}_emitter`, `${n}x gtceu:${parts}_field_generator`,
                `2x #gtceu:circuits/${tier}`, '2x af9:rf_transceiver_chip', 'af9:saw_filter_chip',
                '2x gtceu:aluminium_nitride_dust')
            .inputFluids(Fluid.of('gtceu:soldering_alloy', 576))
            .itemOutputs(`gtceu:${tier}_wireless_energy_transmitter`)
            .duration(600)
            .EUt(voltage)
    })
})
