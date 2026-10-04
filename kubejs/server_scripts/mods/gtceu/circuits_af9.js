// AF9 - Circuit crafting: everything AF9 changes about how GT's circuits are made, in one file to edit.
//
// What is here, in order:
//   1. helpers (the tin / soldering alloy pair that every circuit assembler recipe gets, the chips, the cleanroom)
//   2. the metals of the circuits: the alloy dusts (Aluminium-Silicon, Kovar, Platinum-Iridium) mixed in LV / HV
//      (GT melts them in the EBF at the temperatures of af9-core's registry/AF9Materials)
//   3. MV circuits without discrete semiconductors (Good Electronic / Good Integrated circuit, Microprocessor, APU version)
//   4. HV to LuV circuits from the metals of their own tier, with plain chips (+ their SMD / SoC versions)
//   5. the eDRAM package recipes (LuV): extra recipes beside the RAM ones
// Everything else about circuits is GT's own (CircuitRecipes.java, GTCEu 7.2.0, harderCircuitRecipes off). The chips
// themselves (what a wafer yields, the lithography machines) are in photolithography.js and litho_process.js; the
// circuit-free uses of chips (robot arms, sensors, data orbs) are in chip_uses.js. Spec: docs/semiconductor-factory.md
//
// To change a circuit: edit its recipe below (inputs, outputs, duration, EUt); to take one out, delete its block. A
// circuit that GT makes by itself and is not listed here is untouched. A recipe id is af9:<id> (and af9:<id>_soldering_alloy
// for the second solder version): keep it unique.
//
// ---- MV (before this was mv_circuits.js) ----
// Transistors and diodes are replaced by lithographed chips (Photolithography Line); resistors and capacitors stay as
// board passives. Any chip works regardless of the mode it was printed in. The metal parts are MV metals (aluminium,
// the MV metal of the Circuits quest page): Aluminium-Silicon bond wire and Kovar pins (their alloy dusts: section 2 below),
// both from an LV mixer and the EBF, so they can be made before any MV machine.
//
// The Good Electronic Circuit must stay chip-free: the Photolithography Line and its MV parts need MV circuits, so at
// least one MV circuit has to be makeable before the line exists. It uses vacuum tubes (the pre-semiconductor
// rectifier) where GT used diodes. The SoC Microprocessor recipe is GT's own and stays: it has no discrete parts.
//
// ---- HV to LuV (before this was tiered_circuits.js) ----
// HV to LuV circuits are built from the metals of their own tier (the ones the Circuits quest page lists per
// tier). The chips are GT's plain chips: the lithography machines print plain wafer items, and a chip is a chip
// whatever substrate it was cut from (higher substrates just give more chips per wafer).
//
// Metals (all makeable with the previous tier's machines):
//   HV  gold bond wire, stainless steel bolts
//   EV  Platinum-Iridium fine wire (bootstrap: plain platinum, iridium is EV-era), titanium bolts
//   IV  tungstensteel fine wire and frames, tungsten busbars (single wire)
//   LuV osmiridium fine wire, niobium-titanium superconductor wire (the LuV cable), rhodium-plated palladium bolts
// The LuV Nano Mainframe is the pack's own Assembly Line recipe.
//
// Every recipe below is otherwise GT's own (CircuitRecipes.java, GTCEu 7.2.0, harderCircuitRecipes off).

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // ================================= 1. helpers =================================
    // GT's circuit assembler recipes get a tin and a soldering alloy version from GT's recipe generator, which KubeJS
    // recipes skip; this adds both the same way: a tin (144 mB x multiplier) and a soldering alloy (72 mB x multiplier)
    // version of one recipe
    const newCircuit = (id, solder, build) => {
        build(event.recipes.gtceu.circuit_assembler(`af9:${id}`)).inputFluids(Fluid.of('gtceu:tin', 144 * solder))
        build(event.recipes.gtceu.circuit_assembler(`af9:${id}_soldering_alloy`)).inputFluids(Fluid.of('gtceu:soldering_alloy', 72 * solder))
    }
    // Replaces GT's recipe and its generated soldering alloy copy with that pair
    const circuit = (id, solder, build) => {
        event.remove({ id: `gtceu:circuit_assembler/${id}` })
        event.remove({ id: `gtceu:circuit_assembler/${id}_soldering_alloy` })
        newCircuit(id, solder, build)
    }
    const circuitAssembler = (id, build) => newCircuit(id, 1, build)
    // plain GT chips; the tier / mode arguments only document which circuit tier a recipe belongs to
    const chipIn = (modeId, chipId, count) => AF9_WAFERS.chipStack(chipId, count)
    const chip = (tier, chipId, count) => AF9_WAFERS.chipStack(chipId, count)
    const clean = recipe => recipe.cleanroom(CleanroomType.CLEANROOM)

    // ================================= 2. the metals of the circuits =================================
    // Mixed one tier below the circuits that use them (LV for the MV alloys, HV for the EV one). Circuit 3 keeps Kovar
    // apart from GT's invar (circuit 1), whose inputs are a subset of Kovar's.
    const alloys = [
        { id: 'aluminium_silicon', inputs: ['16x gtceu:aluminium_dust', 'gtceu:silicon_dust'], count: 17, circuit: 2, eut: VA[GTValues.LV] },
        { id: 'kovar', inputs: ['6x gtceu:iron_dust', '3x gtceu:nickel_dust', '2x gtceu:cobalt_dust'], count: 11, circuit: 3, eut: VA[GTValues.LV] },
        { id: 'platinum_iridium', inputs: ['9x gtceu:platinum_dust', 'gtceu:iridium_dust'], count: 10, circuit: 2, eut: VA[GTValues.HV] }
    ]
    alloys.forEach(a => {
        event.recipes.gtceu.mixer(`af9:${a.id}_dust`)
            .itemInputs(a.inputs)
            .circuit(a.circuit)
            .itemOutputs(`${a.count}x gtceu:${a.id}_dust`)
            .duration(a.count * 30)
            .EUt(a.eut)
    })

    // ================================= 3. MV =================================
    // GT's versions (with diodes / transistors) and their generated solder variants
    const replacedGtRecipes = ['electronic_circuit_mv', 'integrated_circuit_mv', 'processor_mv']
    event.remove({ id: 'gtceu:shaped/electronic_circuit_mv' })
    replacedGtRecipes.forEach(id => {
        event.remove({ id: `gtceu:circuit_assembler/${id}` })
        event.remove({ id: `gtceu:circuit_assembler/${id}_soldering_alloy` })
    })

    // ---- Good Electronic Circuit (MV, bootstrap) ----
    event.shaped('gtceu:good_electronic_circuit', ['VPV', 'CBC', 'WCW'], {
        V: 'gtceu:vacuum_tube',
        P: 'gtceu:steel_plate',
        C: 'gtceu:basic_electronic_circuit',
        B: 'gtceu:phenolic_printed_circuit_board',
        W: 'gtceu:copper_single_wire'
    }).id('af9:shaped/good_electronic_circuit')

    circuitAssembler('good_electronic_circuit', recipe => recipe
        .itemInputs(
            'gtceu:phenolic_printed_circuit_board',
            '2x gtceu:basic_electronic_circuit',
            '2x gtceu:vacuum_tube',
            '2x gtceu:copper_single_wire')
        .itemOutputs('gtceu:good_electronic_circuit')
        .duration(300)
        .EUt(GTValues.VA[GTValues.LV]))

    // ---- Good Integrated Circuit (MV): logic chips instead of diodes ----
    circuitAssembler('good_integrated_circuit', recipe => recipe
        .itemInputs(
            'gtceu:phenolic_printed_circuit_board',
            '2x gtceu:basic_integrated_circuit',
            '2x gtceu:ilc_chip',
            '2x #gtceu:resistors',
            '4x gtceu:fine_aluminium_silicon_wire',
            '4x gtceu:kovar_bolt')
        .itemOutputs('2x gtceu:good_integrated_circuit')
        .duration(400)
        .EUt(24))

    // ---- Microprocessor (MV): CPU with a RAM cache instead of discrete transistors ----
    circuitAssembler('micro_processor', recipe => recipe
        .itemInputs(
            'gtceu:plastic_printed_circuit_board',
            'gtceu:cpu_chip',
            'gtceu:ram_chip',
            '4x #gtceu:resistors',
            '4x #gtceu:capacitors',
            '4x gtceu:fine_aluminium_silicon_wire')
        .itemOutputs('2x gtceu:micro_processor')
        .duration(200)
        .EUt(60))

    // ---- Microprocessor (MV), APU version: CPU and graphics on one die, with their cache; one chip does the work of
    // the CPU and its RAM, so the board takes more of them ----
    circuitAssembler('micro_processor_apu', recipe => recipe
        .itemInputs(
            'gtceu:plastic_printed_circuit_board',
            'af9:apu_chip',
            '4x #gtceu:resistors',
            '4x #gtceu:capacitors',
            '4x gtceu:fine_aluminium_silicon_wire')
        .itemOutputs('3x gtceu:micro_processor')
        .duration(200)
        .EUt(60))
    // ================================= 4. HV to LuV =================================
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
    // (Nano Mainframe: the pack's own Assembly Line recipe)
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

    // ---- eDRAM packages (af9:edram_cpu/soc_package): extra, faster recipes beside the RAM ones ----
    // A package is a CPU or SoC die with its eDRAM cache on one laminate; one replaces four RAM chips, in half the time
    newCircuit('quantum_computer_luv_edram', 2, r => clean(r
        .itemInputs('gtceu:fiber_reinforced_printed_circuit_board', '2x gtceu:quantum_processor_assembly', '8x gtceu:smd_diode',
            chip('luv', 'nor', 4), '4x af9:edram_cpu_package', '32x gtceu:fine_osmiridium_wire')
        .itemOutputs('gtceu:quantum_processor_computer')
        .duration(200).EUt(2400)))

    newCircuit('quantum_computer_luv_edram_asmd', 2, r => clean(r
        .itemInputs('gtceu:fiber_reinforced_printed_circuit_board', '2x gtceu:quantum_processor_assembly', '2x gtceu:advanced_smd_diode',
            chip('luv', 'nor', 4), '4x af9:edram_cpu_package', '32x gtceu:fine_osmiridium_wire')
        .itemOutputs('gtceu:quantum_processor_computer')
        .duration(100).EUt(2400)))

    newCircuit('crystal_assembly_luv_edram', 2, r => clean(r
        .itemInputs('gtceu:multilayer_fiber_reinforced_printed_circuit_board', '2x gtceu:crystal_processor', '4x gtceu:advanced_smd_inductor',
            '8x gtceu:advanced_smd_capacitor', '6x af9:edram_soc_package', '16x gtceu:fine_niobium_titanium_wire')
        .itemOutputs('2x gtceu:crystal_processor_assembly')
        .duration(200).EUt(9600)))
    // (the Nano Mainframe's eDRAM version: the pack's own Assembly Line recipe)

    // ================================= UV + UHV =================================
    // UV wetware supercomputer: GT's circuit assembler recipe replaced with a quantanium-soldered version.
    event.remove({ id: 'gtceu:circuit_assembler/wetware_processor_computer_uv' })
    event.remove({ id: 'gtceu:circuit_assembler/wetware_processor_computer_uv_soldering_alloy' })

    event.recipes.gtceu.circuit_assembler('af9:wetware_processor_computer_quantanium')
        .itemInputs(
            'gtceu:wetware_printed_circuit_board',
            '2x gtceu:wetware_processor_assembly',
            '8x gtceu:advanced_smd_capacitor',
            '8x gtceu:advanced_smd_transistor',
            '32x gtceu:fine_tritanium_wire')
        .inputFluids(Fluid.of('gtceu:quantanium', 288))
        .itemOutputs('gtceu:wetware_processor_computer')
        .cleanroom(CleanroomType.CLEANROOM)
        .duration(400)
        .EUt(VA[GTValues.UV])

    // UHV wetware mainframe (photonic-only): GT's assembly line (tritanium frame, 2x wetware computer, 5x 32x SMD,
    // 64x PBI foil, 32x RAM, 16x double ENTED wire, europium plates) is replaced with a photonic-only line:
    // photonic ICs + the photonic compute cards (CPU/GPU + a fuck-ton of photonic DRAM), 10x wetware
    // supercomputers, 64x double ENTED wire and 128x PBI foil. Same research as GT (scan the wetware
    // supercomputer, 96 CWU/t at UV) and UV power.
    event.remove({ id: 'gtceu:assembly_line/wetware_mainframe_uhv' })

    event.recipes.gtceu.assembly_line('af9:wetware_mainframe_uhv')
        .itemInputs(
            '2x gtceu:tritanium_frame',
            '10x gtceu:wetware_processor_computer',
            '32x af9:photonic_ic_chip',
            '16x af9:photonic_cpu_card',
            '16x af9:photonic_gpu_card',
            '32x af9:photonic_ram_card',
            '64x gtceu:enriched_naquadah_trinium_europium_duranide_double_wire',
            '128x gtceu:polybenzimidazole_foil',
            '8x gtceu:europium_plate')
        .inputFluids(
            Fluid.of('gtceu:soldering_alloy', 2880),
            Fluid.of('gtceu:polybenzimidazole', 1152))
        .itemOutputs('gtceu:wetware_processor_mainframe')
        .stationResearch(b => b
            .researchStack(Item.of('gtceu:wetware_processor_computer'))
            .CWUt(96)
            .EUt(VA[GTValues.UV]))
        .duration(2000)
        .EUt(VA[GTValues.UV])
})
