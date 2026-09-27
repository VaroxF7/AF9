// AF9 - HV to LuV circuits are built from the metals of their own tier (the ones the Circuits quest page lists per
// tier). The chips are GT's plain chips: the lithography machines print plain wafer items, and a chip is a chip
// whatever substrate it was cut from (higher substrates just give more chips per wafer).
//
// Metals (all makeable with the previous tier's machines):
//   HV  gold bond wire, stainless steel bolts
//   EV  Platinum-Iridium fine wire (bootstrap: plain platinum, iridium is EV-era), titanium bolts
//   IV  tungstensteel fine wire and frames, tungsten busbars (single wire)
//   LuV osmiridium fine wire, niobium-titanium superconductor wire (the LuV cable), rhodium-plated palladium bolts
// The LuV Nano Mainframe is ATM9's Assembly Line recipe (circuits_for_atm.js).
//
// Every recipe below is otherwise GT's own (CircuitRecipes.java, GTCEu 7.2.0, harderCircuitRecipes off).

ServerEvents.recipes(allthemods => {
    const VA = GTValues.VA
    // plain GT chips; the tier / mode arguments only document which circuit tier a recipe belongs to
    const chipIn = (modeId, chipId, count) => AF9_WAFERS.chipStack(chipId, count)
    const chip = (tier, chipId, count) => AF9_WAFERS.chipStack(chipId, count)

    // A tin (144 mB x multiplier) and a soldering alloy (72 mB x multiplier) version, the same pair GT's generator
    // makes (KubeJS recipes skip it)
    const newCircuit = (id, solder, build) => {
        build(allthemods.recipes.gtceu.circuit_assembler(`af9:${id}`)).inputFluids(Fluid.of('gtceu:tin', 144 * solder))
        build(allthemods.recipes.gtceu.circuit_assembler(`af9:${id}_soldering_alloy`)).inputFluids(Fluid.of('gtceu:soldering_alloy', 72 * solder))
    }
    // Replaces GT's recipe and its generated soldering alloy copy with that pair
    const circuit = (id, solder, build) => {
        allthemods.remove({ id: `gtceu:circuit_assembler/${id}` })
        allthemods.remove({ id: `gtceu:circuit_assembler/${id}_soldering_alloy` })
        newCircuit(id, solder, build)
    }
    const clean = recipe => recipe.cleanroom(CleanroomType.CLEANROOM)

    // ================================= HV (gold + stainless steel) =================================
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

    // ================================= EV (platinum + titanium) =================================
    // bootstrap: plain platinum wire
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

    // ================================= IV (tungstensteel + tungsten) =================================
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

    // ================================= LuV (osmiridium + NbTi + rhodium-plated palladium) ===============
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

    // ---- eDRAM packages (kubejs:edram_cpu/soc_package): extra, faster recipes beside the RAM ones ----
    // A package is a CPU or SoC die with its eDRAM cache on one laminate; one replaces four RAM chips, in half the time
    newCircuit('quantum_computer_luv_edram', 2, r => clean(r
        .itemInputs('gtceu:fiber_reinforced_printed_circuit_board', '2x gtceu:quantum_processor_assembly', '8x gtceu:smd_diode',
            chip('luv', 'nor', 4), '4x kubejs:edram_cpu_package', '32x gtceu:fine_osmiridium_wire')
        .itemOutputs('gtceu:quantum_processor_computer')
        .duration(200).EUt(2400)))

    newCircuit('quantum_computer_luv_edram_asmd', 2, r => clean(r
        .itemInputs('gtceu:fiber_reinforced_printed_circuit_board', '2x gtceu:quantum_processor_assembly', '2x gtceu:advanced_smd_diode',
            chip('luv', 'nor', 4), '4x kubejs:edram_cpu_package', '32x gtceu:fine_osmiridium_wire')
        .itemOutputs('gtceu:quantum_processor_computer')
        .duration(100).EUt(2400)))

    newCircuit('crystal_assembly_luv_edram', 2, r => clean(r
        .itemInputs('gtceu:multilayer_fiber_reinforced_printed_circuit_board', '2x gtceu:crystal_processor', '4x gtceu:advanced_smd_inductor',
            '8x gtceu:advanced_smd_capacitor', '6x kubejs:edram_soc_package', '16x gtceu:fine_niobium_titanium_wire')
        .itemOutputs('2x gtceu:crystal_processor_assembly')
        .duration(200).EUt(9600)))
    // (the Nano Mainframe's eDRAM version: circuits_for_atm.js)
})
