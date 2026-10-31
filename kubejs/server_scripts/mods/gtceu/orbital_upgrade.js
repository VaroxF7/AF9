// AF9 - The Optical Quantum Gravitational Field Generator: the item that turns an Orbital Lithography Station's
// controller into the Mk2 (put it in the upgrade menu of the console and press Upgrade; the controller keeps it).
//
// Assembly Line, UHV, about 500 s. The research is an Optical CPU scanned at 144 CWU/t in a Research Station (the
// Assembly Line itself has no computation hatch: the computation is the research's).

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // Optical RAM and Optical CPU have no recipe anywhere else yet (their NVM circuit chain is still to come), so the
    // generator could never be built. These two Assembler recipes are placeholders from existing parts: replace them
    // when the optical chain lands.
    event.recipes.gtceu.assembler('af9:optical_ram_placeholder')
        .itemInputs('gtceu:fiber_reinforced_printed_circuit_board', '2x #gtceu:circuits/uhv',
            '8x gtceu:fine_osmiridium_wire')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 288))
        .itemOutputs('af9:optical_ram')
        .duration(400)
        .EUt(VA[GTValues.UV])
    event.recipes.gtceu.assembler('af9:optical_cpu_placeholder')
        .itemInputs('gtceu:fiber_reinforced_printed_circuit_board', '4x #gtceu:circuits/uhv',
            '16x gtceu:fine_sanguinite_wire', '8x gtceu:fine_osmiridium_wire')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 576))
        .itemOutputs('af9:optical_cpu')
        .duration(600)
        .EUt(VA[GTValues.UV])

    event.recipes.gtceu.assembly_line('af9:optical_quantum_gravitational_field_generator')
        .itemInputs(
            '4x af9:optical_ram',
            '4x af9:optical_cpu',
            '2x gtceu:gravitation_engine_unit',
            '16x #gtceu:circuits/uhv',
            '24x gtceu:sanguinite_wire_spool',
            '24x gtceu:chronium_alloy_wire_spool',
            '24x gtceu:enriched_naquadah_trinium_europium_duranide_wire_spool',
            '8x gtceu:energy_cluster',
            '64x gtceu:fine_osmiridium_wire',
            '64x gtceu:ceraquadalloy_bolt',
            '64x gtceu:cobalt_molybdenum_supersteel_screw')
        .itemOutputs('af9:optical_quantum_gravitational_field_generator')
        .stationResearch(b => b
            .researchStack(Item.of('af9:optical_cpu'))
            .CWUt(144)
            .EUt(VA[GTValues.UHV]))
        .duration(10000)
        .EUt(VA[GTValues.UHV])
})
