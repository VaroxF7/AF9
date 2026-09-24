// AF9 - HV to LuV circuits are built from their own tier: chips printed in the matching lithography mode and the metals
// of that tier (the ones the Circuits quest page lists per tier).
//
// Chips: HV = HUV, EV = EUV, IV = XUV, LuV = LUV. A chip GT only makes at a later tier (e.g. Nano CPU, ASoC in HV
// circuits) uses its own first mode instead. MV and lower circuits accept chips from any mode (see mv_circuits.js).
// Bootstrap: HUV/EUV printing needs HV/EV energy hatches, and those need HV/EV circuits. So, like the chip-free Good
// Electronic Circuit at MV, one entry circuit per tier takes the previous mode's chips: the Advanced Integrated Circuit
// (HV) uses MUV chips and the Workstation (EV) uses HUV chips. IV and LuV need no bootstrap: two 16A hatches of the
// tier below power XUV and LUV.
//
// Metals (all makeable with the previous tier's machines):
//   HV  gold bond wire, stainless steel bolts
//   EV  Platinum-Iridium fine wire (bootstrap: plain platinum, iridium is EV-era), titanium bolts
//   IV  tungstensteel fine wire and frames, tungsten busbars (single wire)
//   LuV osmiridium fine wire, niobium-titanium superconductor wire (the LuV cable), rhodium-plated palladium bolts
// The LuV Nano Mainframe is ATM9's Assembly Line recipe (circuits_for_atm.js), which AF9 gives LUV chips.
//
// Every recipe below is otherwise GT's own (CircuitRecipes.java, GTCEu 7.2.0, harderCircuitRecipes off).

ServerEvents.recipes(allthemods => {
    const VA = GTValues.VA
    const TIER_MODE = { hv: 'huv', ev: 'euv', iv: 'xuv', luv: 'luv' }

    // chip printed in the given mode, or in the chip's first mode when GT only makes it later
    const chipIn = (modeId, chipId, count) => {
        const modeIndex = Math.max(AF9_LITHO.mode(modeId).index, AF9_LITHO.chip(chipId).minMode)
        return AF9_LITHO.tagged(chipId, AF9_LITHO.modes[modeIndex].id, count)
    }
    // chip of the circuit tier's own mode
    const chip = (tier, chipId, count) => chipIn(TIER_MODE[tier], chipId, count)

    // Replaces GT's recipe and its generated soldering alloy copy with a tin (144 mB x multiplier) and a soldering
    // alloy (72 mB x multiplier) version, the same pair GT's generator makes (KubeJS recipes skip it)
    const circuit = (id, solder, build) => {
        allthemods.remove({ id: `gtceu:circuit_assembler/${id}` })
        allthemods.remove({ id: `gtceu:circuit_assembler/${id}_soldering_alloy` })
        build(allthemods.recipes.gtceu.circuit_assembler(`af9:${id}`)).inputFluids(Fluid.of('gtceu:tin', 144 * solder))
        build(allthemods.recipes.gtceu.circuit_assembler(`af9:${id}_soldering_alloy`)).inputFluids(Fluid.of('gtceu:soldering_alloy', 72 * solder))
    }
    const clean = recipe => recipe.cleanroom(CleanroomType.CLEANROOM)

    // ================================= HV (HUV chips, gold + stainless steel) =================================
    // bootstrap: MUV chips
    circuit('integrated_circuit_hv', 1, r => r
        .itemInputs('2x gtceu:good_integrated_circuit', chipIn('muv', 'ilc', 2), chipIn('muv', 'ram', 2), '4x #gtceu:transistors',
            '8x gtceu:fine_gold_wire', '8x gtceu:stainless_steel_bolt')
        .itemOutputs('gtceu:advanced_integrated_circuit')
        .duration(800).EUt(VA[GTValues.LV]))

    circuit('processor_assembly_hv', 2, r => r
        .itemInputs('gtceu:plastic_printed_circuit_board', '2x gtceu:micro_processor', '4x #gtceu:inductors', '8x #gtceu:capacitors',
            chip('hv', 'ram', 4), '8x gtceu:fine_gold_wire')
        .itemOutputs('2x gtceu:micro_processor_assembly')
        .duration(400).EUt(VA[GTValues.MV]))

    circuit('nano_processor_hv', 1, r => clean(r
        .itemInputs('gtceu:epoxy_printed_circuit_board', chip('hv', 'nano_cpu', 1), '8x gtceu:smd_resistor', '8x gtceu:smd_capacitor',
            '8x gtceu:smd_transistor', '8x gtceu:fine_gold_wire')
        .itemOutputs('2x gtceu:nano_processor')
        .duration(200).EUt(600)))

    circuit('nano_processor_hv_asmd', 1, r => clean(r
        .itemInputs('gtceu:epoxy_printed_circuit_board', chip('hv', 'nano_cpu', 1), '2x gtceu:advanced_smd_resistor',
            '2x gtceu:advanced_smd_capacitor', '2x gtceu:advanced_smd_transistor', '8x gtceu:fine_gold_wire')
        .itemOutputs('2x gtceu:nano_processor')
        .duration(100).EUt(600)))

    circuit('nano_processor_hv_soc', 1, r => clean(r
        .itemInputs('gtceu:epoxy_printed_circuit_board', chip('hv', 'advanced_soc', 1), '4x gtceu:fine_gold_wire', '4x gtceu:stainless_steel_bolt')
        .itemOutputs('4x gtceu:nano_processor')
        .duration(50).EUt(9600)))

    // ================================= EV (EUV chips, platinum + titanium) =================================
    // bootstrap: HUV chips and plain platinum wire
    circuit('workstation_ev', 2, r => clean(r
        .itemInputs('gtceu:plastic_printed_circuit_board', '2x gtceu:micro_processor_assembly', '4x #gtceu:diodes',
            chipIn('huv', 'ram', 4), '16x gtceu:fine_platinum_wire', '16x gtceu:titanium_bolt')
        .itemOutputs('gtceu:micro_processor_computer')
        .duration(400).EUt(VA[GTValues.MV])))

    circuit('nano_processor_assembly_ev', 2, r => clean(r
        .itemInputs('gtceu:epoxy_printed_circuit_board', '2x gtceu:nano_processor', '4x gtceu:smd_inductor', '8x gtceu:smd_capacitor',
            chip('ev', 'ram', 8), '16x gtceu:fine_platinum_iridium_wire')
        .itemOutputs('2x gtceu:nano_processor_assembly')
        .duration(400).EUt(600)))

    circuit('nano_processor_assembly_ev_asmd', 2, r => clean(r
        .itemInputs('gtceu:epoxy_printed_circuit_board', '2x gtceu:nano_processor', 'gtceu:advanced_smd_inductor',
            '2x gtceu:advanced_smd_capacitor', chip('ev', 'ram', 8), '16x gtceu:fine_platinum_iridium_wire')
        .itemOutputs('2x gtceu:nano_processor_assembly')
        .duration(200).EUt(600)))

    circuit('quantum_processor_ev', 1, r => clean(r
        .itemInputs('gtceu:fiber_reinforced_printed_circuit_board', chip('ev', 'qbit_cpu', 1), chip('ev', 'nano_cpu', 1),
            '12x gtceu:smd_capacitor', '12x gtceu:smd_transistor', '12x gtceu:fine_platinum_iridium_wire')
        .itemOutputs('2x gtceu:quantum_processor')
        .duration(200).EUt(2400)))

    circuit('quantum_processor_ev_asmd', 1, r => clean(r
        .itemInputs('gtceu:fiber_reinforced_printed_circuit_board', chip('ev', 'qbit_cpu', 1), chip('ev', 'nano_cpu', 1),
            '3x gtceu:advanced_smd_capacitor', '3x gtceu:advanced_smd_transistor', '12x gtceu:fine_platinum_iridium_wire')
        .itemOutputs('2x gtceu:quantum_processor')
        .duration(100).EUt(2400)))

    circuit('quantum_processor_ev_soc', 1, r => clean(r
        .itemInputs('gtceu:fiber_reinforced_printed_circuit_board', chip('ev', 'advanced_soc', 1), '12x gtceu:fine_platinum_iridium_wire',
            '8x gtceu:titanium_bolt')
        .itemOutputs('4x gtceu:quantum_processor')
        .duration(50).EUt(38400)))

    // ================================= IV (XUV chips, tungstensteel + tungsten) =================================
    circuit('mainframe_iv', 4, r => clean(r
        .itemInputs('2x gtceu:tungsten_steel_frame', '2x gtceu:micro_processor_computer', '8x #gtceu:inductors', '16x #gtceu:capacitors',
            chip('iv', 'ram', 16), '16x gtceu:tungsten_single_wire')
        .itemOutputs('gtceu:micro_processor_mainframe')
        .duration(800).EUt(VA[GTValues.HV])))

    circuit('mainframe_iv_asmd', 4, r => clean(r
        .itemInputs('2x gtceu:tungsten_steel_frame', '2x gtceu:micro_processor_computer', '2x gtceu:advanced_smd_inductor',
            '4x gtceu:advanced_smd_capacitor', chip('iv', 'ram', 16), '16x gtceu:tungsten_single_wire')
        .itemOutputs('gtceu:micro_processor_mainframe')
        .duration(400).EUt(VA[GTValues.HV])))

    circuit('nano_computer_iv', 2, r => clean(r
        .itemInputs('gtceu:epoxy_printed_circuit_board', '2x gtceu:nano_processor_assembly', '8x gtceu:smd_diode',
            chip('iv', 'nor', 4), chip('iv', 'ram', 16), '16x gtceu:fine_tungsten_steel_wire')
        .itemOutputs('gtceu:nano_processor_computer')
        .duration(400).EUt(600)))

    circuit('nano_computer_iv_asmd', 2, r => clean(r
        .itemInputs('gtceu:epoxy_printed_circuit_board', '2x gtceu:nano_processor_assembly', '2x gtceu:advanced_smd_diode',
            chip('iv', 'nor', 4), chip('iv', 'ram', 16), '16x gtceu:fine_tungsten_steel_wire')
        .itemOutputs('gtceu:nano_processor_computer')
        .duration(200).EUt(600)))

    circuit('quantum_assembly_iv', 2, r => clean(r
        .itemInputs('gtceu:fiber_reinforced_printed_circuit_board', '2x gtceu:quantum_processor', '8x gtceu:smd_inductor',
            '16x gtceu:smd_capacitor', chip('iv', 'ram', 4), '16x gtceu:fine_tungsten_steel_wire')
        .itemOutputs('2x gtceu:quantum_processor_assembly')
        .duration(400).EUt(2400)))

    circuit('quantum_assembly_iv_asmd', 2, r => clean(r
        .itemInputs('gtceu:fiber_reinforced_printed_circuit_board', '2x gtceu:quantum_processor', '2x gtceu:advanced_smd_inductor',
            '4x gtceu:advanced_smd_capacitor', chip('iv', 'ram', 4), '16x gtceu:fine_tungsten_steel_wire')
        .itemOutputs('2x gtceu:quantum_processor_assembly')
        .duration(200).EUt(2400)))

    circuit('crystal_processor_iv', 1, r => clean(r
        .itemInputs('gtceu:multilayer_fiber_reinforced_printed_circuit_board', 'gtceu:crystal_cpu', chip('iv', 'nano_cpu', 2),
            '6x gtceu:advanced_smd_capacitor', '6x gtceu:advanced_smd_transistor', '8x gtceu:fine_tungsten_steel_wire')
        .itemOutputs('2x gtceu:crystal_processor')
        .duration(200).EUt(9600)))

    // ================================= LuV (LUV chips, osmiridium + NbTi + rhodium-plated palladium) ===============
    // (Nano Mainframe: ATM9 Assembly Line recipe in circuits_for_atm.js)
    circuit('quantum_computer_luv', 2, r => clean(r
        .itemInputs('gtceu:fiber_reinforced_printed_circuit_board', '2x gtceu:quantum_processor_assembly', '8x gtceu:smd_diode',
            chip('luv', 'nor', 4), chip('luv', 'ram', 16), '32x gtceu:fine_osmiridium_wire')
        .itemOutputs('gtceu:quantum_processor_computer')
        .duration(400).EUt(2400)))

    circuit('quantum_computer_luv_asmd', 2, r => clean(r
        .itemInputs('gtceu:fiber_reinforced_printed_circuit_board', '2x gtceu:quantum_processor_assembly', '2x gtceu:advanced_smd_diode',
            chip('luv', 'nor', 4), chip('luv', 'ram', 16), '32x gtceu:fine_osmiridium_wire')
        .itemOutputs('gtceu:quantum_processor_computer')
        .duration(200).EUt(2400)))

    circuit('crystal_assembly_luv', 2, r => clean(r
        .itemInputs('gtceu:multilayer_fiber_reinforced_printed_circuit_board', '2x gtceu:crystal_processor', '4x gtceu:advanced_smd_inductor',
            '8x gtceu:advanced_smd_capacitor', chip('luv', 'ram', 24), '16x gtceu:fine_niobium_titanium_wire')
        .itemOutputs('2x gtceu:crystal_processor_assembly')
        .duration(400).EUt(9600)))

    circuit('wetware_processor_luv', 1, r => clean(r
        .itemInputs('gtceu:neuro_processing_unit', 'gtceu:crystal_cpu', chip('luv', 'nano_cpu', 1), '8x gtceu:advanced_smd_capacitor',
            '8x gtceu:advanced_smd_transistor', '8x gtceu:fine_niobium_titanium_wire')
        .itemOutputs('2x gtceu:wetware_processor')
        .duration(200).EUt(38400)))

    circuit('wetware_processor_luv_soc', 1, r => clean(r
        .itemInputs('gtceu:neuro_processing_unit', chip('luv', 'highly_advanced_soc', 1), '8x gtceu:fine_niobium_titanium_wire',
            '8x gtceu:rhodium_plated_palladium_bolt')
        .itemOutputs('4x gtceu:wetware_processor')
        .duration(100).EUt(150000)))
})
