// AF9 - Circuit crafting, linear ladder: LV -> MV -> HV -> EV -> IV -> LuV -> ZPM -> UV -> UHV -> XPS -> NVM.
// The XPS and NVM recipes are OUT FOR NOW: both tiers are being redone. Their items, tags and quests stay; what is
// said about them below is the plan they had.
//
// Rules (load-bearing, do not soften without re-reading this file):
//   1. LINEAR: every normal circuit at tier T takes >=1x circuit of tier T-1 as a direct
//      item input, and inside a tier the circuits chain in order (assembly takes the
//      integrated, processor takes the assembly). SoC shortcuts never skip a tier.
//   2. SILICON FIRST: the value sits in lithographed dies and packages (node-matched
//      chips, asic/edram/photonic packages), boards are carriers, wires/bolts/passives
//      shrink every tier and collapse in lean recipes. MV keeps GT solders, HV-UV uses
//      high-grade solder (tin stays as budget), wetware uses living solder, XPS/NVM
//      uses plasma solder (tin stays as dead budget option).
//   3. LEAN STAGES: reaching the end of a stage unlocks a second recipe for everything
//      below it: same chain, 1x previous circuit, packages instead of loose chips,
//      quarter wires/bolts, no loose passives, 2x output or half time, gated by one
//      consumed input you only have at that stage end:
//        Stage 1 @HV end:  MV lean, gate af9:asic_chip (200nm phosphorus)
//        Stage 2 @LuV end: HV+EV+IV lean, gate af9:vpu_chip (65nm immersion naquadria)
//        Stage 3 @UHV end: LuV+ZPM+UV lean, gate af9:tpu_chip + quantanium (20nm EUV)
//        Stage 4 @XPS end: UHV+XPS lean, gate af9:photonic_ic_chip + af9:spin_logic_chip
//        Stage 5 @NVM end: XPS+NVM lean, gate af9:memristor_chip + af9:quantum_dot_ic_chip
//      Lean ids are af9:<id>_lean (tin) and af9:<id>_lean_high_grade / _lean_living /
//      _lean_plasma for the canonical solder. The normal recipe always stays.
//
// What is here, in order:
//   1. helpers (tin/solder pairs, high-grade, living, plasma; chip stacks; cleanroom)
//   2. circuit alloy dusts (Al-Si, Kovar, Pt-Ir, high-grade solder)
//   3. MV normal (bootstrap, chip-light) + MV lean (Stage 1)
//   4. HV normal (linear, node-matched) + HV lean (Stage 2)
//   5. EV normal + EV lean (Stage 2)
//   6. IV normal + IV lean (Stage 2)
//   7. LuV normal (incl. pack Nano Mainframe AL) + LuV lean (Stage 3)
//   8. ZPM normal (crystal computer, quantum mainframe AL, wetware assembly) + ZPM lean (Stage 3)
//   9. UV normal (crystal mainframe AL, wetware computer) + UV lean (Stage 3)
//   10. UHV normal (wetware mainframe AL + plasma Mk2 path) + UHV lean (Stage 4) + AI Acceleration Card
//   11. Pico circuits (Advanced Circuit Manufacturer); the XPS and NVM recipes that were here are out for now
//   13. packages (asic_package, photonic_package; edram packages live in photolithography.js)
//   14. tags (gtceu:circuits/xps, gtceu:circuits/nvm)
// Everything else is GT's own. Spec: docs/semiconductor-factory.md

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    // The Pico circuits are made at UHV voltage on a lot of amps: 300 A (UHV recipes of the other scripts take 100 A).
    // That is what the Dyson Swarm's power is for (docs/dyson-swarm.md).
    const PICO_AMPS = 300

    // ================================= 1. helpers =================================
    const newCircuit = (id, solder, build) => {
        build(event.recipes.gtceu.circuit_assembler(`af9:${id}`)).inputFluids(Fluid.of('gtceu:tin', 144 * solder))
        build(event.recipes.gtceu.circuit_assembler(`af9:${id}_soldering_alloy`)).inputFluids(Fluid.of('gtceu:soldering_alloy', 72 * solder))
    }
    const circuit = (id, solder, build) => {
        event.remove({ id: `gtceu:circuit_assembler/${id}` })
        event.remove({ id: `gtceu:circuit_assembler/${id}_soldering_alloy` })
        newCircuit(id, solder, build)
    }
    // HV-UV: full replacement of soldering alloy with the high-grade solder (tin stays as budget option).
    const newHighCircuit = (id, solder, build) => {
        build(event.recipes.gtceu.circuit_assembler(`af9:${id}`)).inputFluids(Fluid.of('gtceu:tin', 144 * solder))
        build(event.recipes.gtceu.circuit_assembler(`af9:${id}_high_grade`)).inputFluids(Fluid.of('gtceu:high_grade_solder', 72 * solder))
    }
    const highCircuit = (id, solder, build) => {
        event.remove({ id: `gtceu:circuit_assembler/${id}` })
        event.remove({ id: `gtceu:circuit_assembler/${id}_soldering_alloy` })
        newHighCircuit(id, solder, build)
    }
    // Wetware: living solder only (reflow would kill the cells). Tin stays as a dead budget option.
    const newLivingCircuit = (id, solder, build) => {
        build(event.recipes.gtceu.circuit_assembler(`af9:${id}`)).inputFluids(Fluid.of('gtceu:tin', 144 * solder))
        build(event.recipes.gtceu.circuit_assembler(`af9:${id}_living`)).inputFluids(Fluid.of('gtceu:living_solder', 72 * solder))
    }
    const livingCircuit = (id, solder, build) => {
        event.remove({ id: `gtceu:circuit_assembler/${id}` })
        event.remove({ id: `gtceu:circuit_assembler/${id}_soldering_alloy` })
        newLivingCircuit(id, solder, build)
    }
    // XPS/NVM: plasma solder canonical (atomic deposition wastes nothing: half the fluid),
    // tin stays as a dead budget option.
    const newPlasmaCircuit = (id, solder, build) => {
        build(event.recipes.gtceu.circuit_assembler(`af9:${id}`)).inputFluids(Fluid.of('gtceu:tin', 144 * solder))
        build(event.recipes.gtceu.circuit_assembler(`af9:${id}_plasma`)).inputFluids(Fluid.of('gtceu:plasma_solder', 36 * solder))
    }
    // Assembly-line replacement (mainframes ZPM+): remove GT's line, if any, then build ours.
    const assemblyLine = (id, build) => {
        event.remove({ id: `gtceu:assembly_line/${id}` })
        build(event.recipes.gtceu.assembly_line(`af9:${id}`))
    }
    const circuitAssembler = (id, build) => newCircuit(id, 1, build)
    const chipIn = (modeId, chipId, count) => AF9_WAFERS.chipStack(chipId, count)
    const chip = (tier, chipId, count) => AF9_WAFERS.chipStack(chipId, count)
    const clean = recipe => recipe.cleanroom(CleanroomType.CLEANROOM)

    // ================================= 2. the metals of the circuits =================================
    const alloys = [
        { id: 'aluminium_silicon', inputs: ['16x gtceu:aluminium_dust', 'gtceu:silicon_dust'], count: 17, circuit: 2, eut: VA[GTValues.LV] },
        { id: 'kovar', inputs: ['6x gtceu:iron_dust', '3x gtceu:nickel_dust', '2x gtceu:cobalt_dust'], count: 11, circuit: 3, eut: VA[GTValues.LV] },
        { id: 'platinum_iridium', inputs: ['9x gtceu:platinum_dust', 'gtceu:iridium_dust'], count: 10, circuit: 2, eut: VA[GTValues.HV] },
        { id: 'high_grade_solder', inputs: ['9x gtceu:tin_dust', 'gtceu:bismuth_dust', 'gtceu:indium_dust', 'gtceu:silver_dust'], count: 12, circuit: 12, eut: VA[GTValues.MV] }
    ]
    alloys.forEach(a => {
        event.recipes.gtceu.mixer(`af9:${a.id}_dust`)
            .itemInputs(a.inputs)
            .circuit(a.circuit)
            .itemOutputs(`${a.count}x gtceu:${a.id}_dust`)
            .duration(a.count * 30)
            .EUt(a.eut)
    })

    // ================================= 3. MV (LV -> MV, bootstrap) =================================
    // Good Electronic stays chip-free (electron tubes): the line needs MV circuits first.
    const replacedGtRecipes = ['electronic_circuit_mv', 'integrated_circuit_mv', 'processor_mv']
    event.remove({ id: 'gtceu:shaped/electronic_circuit_mv' })
    replacedGtRecipes.forEach(id => {
        event.remove({ id: `gtceu:circuit_assembler/${id}` })
        event.remove({ id: `gtceu:circuit_assembler/${id}_soldering_alloy` })
    })

    // (no crafting table recipe: by hand it is the Create sequenced assembly of early_circuits.js)

    circuitAssembler('good_electronic_circuit', recipe => recipe
        .itemInputs(
            'gtceu:phenolic_printed_circuit_board',
            '2x gtceu:basic_electronic_circuit',
            '2x create:electron_tube',
            '2x gtceu:copper_single_wire')
        .itemOutputs('gtceu:good_electronic_circuit')
        .duration(300)
        .EUt(GTValues.VA[GTValues.LV]))

    // Good Integrated: LV basic_integrated + Si logic dies, Al-Si wire, Kovar pins.
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

    // Microprocessor: LV microchip + CPU/RAM dies on plastic. Linear via microchip_processor.
    circuitAssembler('micro_processor', recipe => recipe
        .itemInputs(
            'gtceu:plastic_printed_circuit_board',
            'gtceu:microchip_processor',
            'gtceu:cpu_chip',
            'gtceu:ram_chip',
            '4x #gtceu:resistors',
            '4x gtceu:fine_aluminium_silicon_wire')
        .itemOutputs('2x gtceu:micro_processor')
        .duration(200)
        .EUt(60))

    // Microprocessor, APU version: one APU die does CPU+RAM, takes the LV microchip too.
    circuitAssembler('micro_processor_apu', recipe => recipe
        .itemInputs(
            'gtceu:plastic_printed_circuit_board',
            'gtceu:microchip_processor',
            'af9:apu_chip',
            '4x #gtceu:resistors',
            '4x gtceu:fine_aluminium_silicon_wire')
        .itemOutputs('3x gtceu:micro_processor')
        .duration(200)
        .EUt(60))

    // ---- MV lean (Stage 1 @HV end, gate: asic_chip). Packages instead of loose
    // passives, quarter wire, 2x output. The chain still starts at LV. ----
    newCircuit('good_integrated_circuit_lean', 1, recipe => recipe
        .itemInputs(
            'gtceu:phenolic_printed_circuit_board',
            'gtceu:basic_integrated_circuit',
            '2x gtceu:ilc_chip',
            'af9:asic_chip',
            '2x gtceu:fine_aluminium_silicon_wire',
            '2x gtceu:kovar_bolt')
        .itemOutputs('4x gtceu:good_integrated_circuit')
        .duration(200)
        .EUt(24))

    newCircuit('micro_processor_lean', 1, recipe => recipe
        .itemInputs(
            'gtceu:plastic_printed_circuit_board',
            'gtceu:microchip_processor',
            'af9:apu_chip',
            'af9:asic_chip',
            '2x gtceu:fine_aluminium_silicon_wire')
        .itemOutputs('4x gtceu:micro_processor')
        .duration(100)
        .EUt(60))

    // ================================= 4. HV (MV -> HV) =================================
    highCircuit('integrated_circuit_hv', 1, r => r
        .itemInputs('2x gtceu:good_integrated_circuit', chipIn('muv', 'ilc', 2), chipIn('muv', 'ram', 2), '4x #gtceu:transistors',
            '8x gtceu:fine_gold_wire', '8x gtceu:stainless_steel_bolt')
        .itemOutputs('gtceu:advanced_integrated_circuit')
        .duration(800).EUt(VA[GTValues.LV]))

    highCircuit('processor_assembly_hv', 2, r => r
        .itemInputs('gtceu:plastic_printed_circuit_board', '2x gtceu:micro_processor', '4x #gtceu:inductors', '8x #gtceu:capacitors',
            chip('hv', 'ram', 4), '8x gtceu:fine_gold_wire')
        .itemOutputs('2x gtceu:micro_processor_assembly')
        .duration(400).EUt(VA[GTValues.MV]))

    // Nano: takes the HV assembly (linear), nano_cpu die, SMD, Au wire.
    highCircuit('nano_processor_hv', 1, r => clean(r
        .itemInputs('gtceu:epoxy_printed_circuit_board', 'gtceu:micro_processor_assembly', chip('hv', 'nano_cpu', 1),
            '8x gtceu:smd_resistor', '8x gtceu:smd_capacitor', '8x gtceu:fine_gold_wire')
        .itemOutputs('2x gtceu:nano_processor')
        .duration(200).EUt(600)))

    highCircuit('nano_processor_hv_asmd', 1, r => clean(r
        .itemInputs('gtceu:epoxy_printed_circuit_board', 'gtceu:micro_processor_assembly', chip('hv', 'nano_cpu', 1),
            '2x gtceu:advanced_smd_resistor', '2x gtceu:advanced_smd_capacitor', '8x gtceu:fine_gold_wire')
        .itemOutputs('2x gtceu:nano_processor')
        .duration(100).EUt(600)))

    // SoC shortcut, linearised: still takes the HV assembly, then the SoC die does the discretes.
    highCircuit('nano_processor_hv_soc', 1, r => clean(r
        .itemInputs('gtceu:epoxy_printed_circuit_board', 'gtceu:micro_processor_assembly', chip('hv', 'advanced_soc', 1),
            '4x gtceu:fine_gold_wire', '4x gtceu:stainless_steel_bolt')
        .itemOutputs('4x gtceu:nano_processor')
        .duration(50).EUt(9600)))

    // ---- HV lean (Stage 2 @LuV end, gate: vpu_chip). 1x previous circuit, packages
    // over loose RAM, quarter metals, 2x output. ----
    newHighCircuit('integrated_circuit_hv_lean', 1, r => clean(r
        .itemInputs('gtceu:good_integrated_circuit', chip('hv', 'nand_memory', 2), 'af9:vpu_chip',
            '2x gtceu:fine_gold_wire', '2x gtceu:stainless_steel_bolt')
        .itemOutputs('2x gtceu:advanced_integrated_circuit')
        .duration(400).EUt(VA[GTValues.LV])))

    newHighCircuit('processor_assembly_hv_lean', 2, r => clean(r
        .itemInputs('gtceu:plastic_printed_circuit_board', 'gtceu:micro_processor', 'af9:asic_package',
            'af9:vpu_chip', '2x gtceu:fine_gold_wire')
        .itemOutputs('4x gtceu:micro_processor_assembly')
        .duration(200).EUt(VA[GTValues.MV])))

    newHighCircuit('nano_processor_hv_lean', 1, r => clean(r
        .itemInputs('gtceu:epoxy_printed_circuit_board', 'gtceu:micro_processor_assembly', chip('hv', 'nano_cpu', 1),
            'af9:vpu_chip', '2x gtceu:fine_gold_wire')
        .itemOutputs('4x gtceu:nano_processor')
        .duration(100).EUt(600)))

    // ================================= 5. EV (HV -> EV) =================================
    highCircuit('workstation_ev', 2, r => clean(r
        .itemInputs('gtceu:plastic_printed_circuit_board', '2x gtceu:micro_processor_assembly', '4x #gtceu:diodes',
            chipIn('huv', 'ram', 4), '16x gtceu:fine_platinum_wire', '16x gtceu:titanium_bolt')
        .itemOutputs('gtceu:micro_processor_computer')
        .duration(400).EUt(VA[GTValues.MV])))

    highCircuit('nano_processor_assembly_ev', 2, r => clean(r
        .itemInputs('gtceu:epoxy_printed_circuit_board', '2x gtceu:nano_processor', '4x gtceu:smd_inductor', '8x gtceu:smd_capacitor',
            chip('ev', 'ram', 8), '16x gtceu:fine_platinum_iridium_wire')
        .itemOutputs('2x gtceu:nano_processor_assembly')
        .duration(400).EUt(600)))

    highCircuit('nano_processor_assembly_ev_asmd', 2, r => clean(r
        .itemInputs('gtceu:epoxy_printed_circuit_board', '2x gtceu:nano_processor', 'gtceu:advanced_smd_inductor',
            '2x gtceu:advanced_smd_capacitor', chip('ev', 'ram', 8), '16x gtceu:fine_platinum_iridium_wire')
        .itemOutputs('2x gtceu:nano_processor_assembly')
        .duration(200).EUt(600)))

    // Quantum: takes the EV nano assembly (linear) + qbit/nano dies.
    highCircuit('quantum_processor_ev', 1, r => clean(r
        .itemInputs('gtceu:fiber_reinforced_printed_circuit_board', 'gtceu:nano_processor_assembly', chip('ev', 'qbit_cpu', 1),
            chip('ev', 'nano_cpu', 1), '12x gtceu:smd_capacitor', '12x gtceu:fine_platinum_iridium_wire')
        .itemOutputs('2x gtceu:quantum_processor')
        .duration(200).EUt(2400)))

    highCircuit('quantum_processor_ev_asmd', 1, r => clean(r
        .itemInputs('gtceu:fiber_reinforced_printed_circuit_board', 'gtceu:nano_processor_assembly', chip('ev', 'qbit_cpu', 1),
            chip('ev', 'nano_cpu', 1), '3x gtceu:advanced_smd_capacitor', '12x gtceu:fine_platinum_iridium_wire')
        .itemOutputs('2x gtceu:quantum_processor')
        .duration(100).EUt(2400)))

    // SoC shortcut, linearised: takes the EV nano assembly too.
    highCircuit('quantum_processor_ev_soc', 1, r => clean(r
        .itemInputs('gtceu:fiber_reinforced_printed_circuit_board', 'gtceu:nano_processor_assembly', chip('ev', 'advanced_soc', 1),
            '12x gtceu:fine_platinum_iridium_wire', '8x gtceu:titanium_bolt')
        .itemOutputs('4x gtceu:quantum_processor')
        .duration(50).EUt(38400)))

    // ---- EV lean (Stage 2 @LuV end, gate: vpu_chip) ----
    newHighCircuit('workstation_ev_lean', 2, r => clean(r
        .itemInputs('gtceu:plastic_printed_circuit_board', 'gtceu:micro_processor_assembly', 'af9:asic_package',
            'af9:vpu_chip', '4x gtceu:fine_platinum_wire', '4x gtceu:titanium_bolt')
        .itemOutputs('2x gtceu:micro_processor_computer')
        .duration(200).EUt(VA[GTValues.MV])))

    newHighCircuit('nano_processor_assembly_ev_lean', 2, r => clean(r
        .itemInputs('gtceu:epoxy_printed_circuit_board', 'gtceu:nano_processor', chip('ev', 'soc', 1),
            'af9:vpu_chip', '4x gtceu:fine_platinum_iridium_wire')
        .itemOutputs('4x gtceu:nano_processor_assembly')
        .duration(200).EUt(600)))

    newHighCircuit('quantum_processor_ev_lean', 1, r => clean(r
        .itemInputs('gtceu:fiber_reinforced_printed_circuit_board', 'gtceu:nano_processor_assembly', chip('ev', 'qbit_cpu', 1),
            'af9:vpu_chip', '3x gtceu:fine_platinum_iridium_wire')
        .itemOutputs('4x gtceu:quantum_processor')
        .duration(100).EUt(2400)))

    // ================================= 6. IV (EV -> IV) =================================
    highCircuit('mainframe_iv', 4, r => clean(r
        .itemInputs('2x gtceu:tungsten_steel_frame', '2x gtceu:micro_processor_computer', '8x #gtceu:inductors', '16x #gtceu:capacitors',
            chip('iv', 'ram', 16), '16x gtceu:tungsten_single_wire')
        .itemOutputs('gtceu:micro_processor_mainframe')
        .duration(800).EUt(VA[GTValues.HV])))

    highCircuit('mainframe_iv_asmd', 4, r => clean(r
        .itemInputs('2x gtceu:tungsten_steel_frame', '2x gtceu:micro_processor_computer', '2x gtceu:advanced_smd_inductor',
            '4x gtceu:advanced_smd_capacitor', chip('iv', 'ram', 16), '16x gtceu:tungsten_single_wire')
        .itemOutputs('gtceu:micro_processor_mainframe')
        .duration(400).EUt(VA[GTValues.HV])))

    highCircuit('nano_computer_iv', 2, r => clean(r
        .itemInputs('gtceu:epoxy_printed_circuit_board', '2x gtceu:nano_processor_assembly', '8x gtceu:smd_diode',
            chip('iv', 'nor', 4), chip('iv', 'ram', 16), '16x gtceu:fine_tungsten_steel_wire')
        .itemOutputs('gtceu:nano_processor_computer')
        .duration(400).EUt(600)))

    highCircuit('nano_computer_iv_asmd', 2, r => clean(r
        .itemInputs('gtceu:epoxy_printed_circuit_board', '2x gtceu:nano_processor_assembly', '2x gtceu:advanced_smd_diode',
            chip('iv', 'nor', 4), chip('iv', 'ram', 16), '16x gtceu:fine_tungsten_steel_wire')
        .itemOutputs('gtceu:nano_processor_computer')
        .duration(200).EUt(600)))

    highCircuit('quantum_assembly_iv', 2, r => clean(r
        .itemInputs('gtceu:fiber_reinforced_printed_circuit_board', '2x gtceu:quantum_processor', '8x gtceu:smd_inductor',
            '16x gtceu:smd_capacitor', chip('iv', 'ram', 4), '16x gtceu:fine_tungsten_steel_wire')
        .itemOutputs('2x gtceu:quantum_processor_assembly')
        .duration(400).EUt(2400)))

    highCircuit('quantum_assembly_iv_asmd', 2, r => clean(r
        .itemInputs('gtceu:fiber_reinforced_printed_circuit_board', '2x gtceu:quantum_processor', '2x gtceu:advanced_smd_inductor',
            '4x gtceu:advanced_smd_capacitor', chip('iv', 'ram', 4), '16x gtceu:fine_tungsten_steel_wire')
        .itemOutputs('2x gtceu:quantum_processor_assembly')
        .duration(200).EUt(2400)))

    // Crystal: takes the IV quantum assembly (linear) + crystal/nano dies.
    highCircuit('crystal_processor_iv', 1, r => clean(r
        .itemInputs('gtceu:multilayer_fiber_reinforced_printed_circuit_board', 'gtceu:quantum_processor_assembly',
            'gtceu:crystal_cpu', chip('iv', 'nano_cpu', 2), '6x gtceu:advanced_smd_capacitor', '8x gtceu:fine_tungsten_steel_wire')
        .itemOutputs('2x gtceu:crystal_processor')
        .duration(200).EUt(9600)))

    // ---- IV lean (Stage 2 @LuV end, gate: vpu_chip; cache via edram packages) ----
    newHighCircuit('mainframe_iv_lean', 4, r => clean(r
        .itemInputs('2x gtceu:tungsten_steel_frame', 'gtceu:micro_processor_computer', '4x af9:edram_cpu_package',
            'af9:vpu_chip', '4x gtceu:tungsten_single_wire')
        .itemOutputs('2x gtceu:micro_processor_mainframe')
        .duration(400).EUt(VA[GTValues.HV])))

    newHighCircuit('nano_computer_iv_lean', 2, r => clean(r
        .itemInputs('gtceu:epoxy_printed_circuit_board', 'gtceu:nano_processor_assembly', chip('iv', 'nor', 4),
            '2x af9:edram_cpu_package', 'af9:vpu_chip', '4x gtceu:fine_tungsten_steel_wire')
        .itemOutputs('2x gtceu:nano_processor_computer')
        .duration(200).EUt(600)))

    newHighCircuit('quantum_assembly_iv_lean', 2, r => clean(r
        .itemInputs('gtceu:fiber_reinforced_printed_circuit_board', 'gtceu:quantum_processor', '2x af9:edram_soc_package',
            'af9:vpu_chip', '4x gtceu:fine_tungsten_steel_wire')
        .itemOutputs('4x gtceu:quantum_processor_assembly')
        .duration(200).EUt(2400)))

    newHighCircuit('crystal_processor_iv_lean', 1, r => clean(r
        .itemInputs('gtceu:multilayer_fiber_reinforced_printed_circuit_board', 'gtceu:quantum_processor_assembly',
            'gtceu:crystal_cpu', 'af9:vpu_chip', '2x gtceu:fine_tungsten_steel_wire')
        .itemOutputs('4x gtceu:crystal_processor')
        .duration(100).EUt(9600)))

    // ================================= 7. LuV (IV -> LuV) =================================
    highCircuit('quantum_computer_luv', 2, r => clean(r
        .itemInputs('gtceu:fiber_reinforced_printed_circuit_board', '2x gtceu:quantum_processor_assembly', '8x gtceu:smd_diode',
            chip('luv', 'nor', 4), chip('luv', 'ram', 16), '32x gtceu:fine_osmiridium_wire')
        .itemOutputs('gtceu:quantum_processor_computer')
        .duration(400).EUt(2400)))

    highCircuit('quantum_computer_luv_asmd', 2, r => clean(r
        .itemInputs('gtceu:fiber_reinforced_printed_circuit_board', '2x gtceu:quantum_processor_assembly', '2x gtceu:advanced_smd_diode',
            chip('luv', 'nor', 4), chip('luv', 'ram', 16), '32x gtceu:fine_osmiridium_wire')
        .itemOutputs('gtceu:quantum_processor_computer')
        .duration(200).EUt(2400)))

    highCircuit('crystal_assembly_luv', 2, r => clean(r
        .itemInputs('gtceu:multilayer_fiber_reinforced_printed_circuit_board', '2x gtceu:crystal_processor', '4x gtceu:advanced_smd_inductor',
            '8x gtceu:advanced_smd_capacitor', chip('luv', 'ram', 24), '16x gtceu:fine_niobium_titanium_wire')
        .itemOutputs('2x gtceu:crystal_processor_assembly')
        .duration(400).EUt(9600)))

    // Wetware: takes the LuV crystal assembly (linear) + NPU/crystal/nano dies.
    livingCircuit('wetware_processor_luv', 1, r => clean(r
        .itemInputs('gtceu:neuro_processing_unit', 'gtceu:crystal_processor_assembly', 'gtceu:crystal_cpu', chip('luv', 'nano_cpu', 1),
            '8x gtceu:advanced_smd_capacitor', '8x gtceu:fine_niobium_titanium_wire')
        .itemOutputs('2x gtceu:wetware_processor')
        .duration(200).EUt(38400)))

    // Wetware SoC, linearised: LuV-native VPU does the discretes, still takes the assembly.
    livingCircuit('wetware_processor_luv_soc', 1, r => clean(r
        .itemInputs('gtceu:neuro_processing_unit', 'gtceu:crystal_processor_assembly', 'af9:vpu_chip',
            '8x gtceu:fine_niobium_titanium_wire', '8x gtceu:rhodium_plated_palladium_bolt')
        .itemOutputs('4x gtceu:wetware_processor')
        .duration(100).EUt(150000)))

    // eDRAM cache variants: extra, faster recipes beside the RAM ones.
    newHighCircuit('quantum_computer_luv_edram', 2, r => clean(r
        .itemInputs('gtceu:fiber_reinforced_printed_circuit_board', '2x gtceu:quantum_processor_assembly', '8x gtceu:smd_diode',
            chip('luv', 'nor', 4), '4x af9:edram_cpu_package', '32x gtceu:fine_osmiridium_wire')
        .itemOutputs('gtceu:quantum_processor_computer')
        .duration(200).EUt(2400)))

    newHighCircuit('quantum_computer_luv_edram_asmd', 2, r => clean(r
        .itemInputs('gtceu:fiber_reinforced_printed_circuit_board', '2x gtceu:quantum_processor_assembly', '2x gtceu:advanced_smd_diode',
            chip('luv', 'nor', 4), '4x af9:edram_cpu_package', '32x gtceu:fine_osmiridium_wire')
        .itemOutputs('gtceu:quantum_processor_computer')
        .duration(100).EUt(2400)))

    newHighCircuit('crystal_assembly_luv_edram', 2, r => clean(r
        .itemInputs('gtceu:multilayer_fiber_reinforced_printed_circuit_board', '2x gtceu:crystal_processor', '4x gtceu:advanced_smd_inductor',
            '8x gtceu:advanced_smd_capacitor', '6x af9:edram_soc_package', '16x gtceu:fine_niobium_titanium_wire')
        .itemOutputs('2x gtceu:crystal_processor_assembly')
        .duration(200).EUt(9600)))

    // Nano Mainframe: the pack's own Assembly Line recipe (IV nano computer -> LuV).
    event.remove({ id: 'gtceu:assembly_line/nano_processor_mainframe' })
    event.recipes.gtceu.assembly_line('af9:nano_mainframe_luv')
        .itemInputs(
            '2x gtceu:tungsten_steel_frame',
            '2x gtceu:nano_processor_computer',
            '4x gtceu:quantum_processor_computer',
            '8x af9:edram_soc_package',
            '16x gtceu:fine_niobium_titanium_wire',
            '16x gtceu:rhodium_plated_palladium_bolt')
        .inputFluids(
            Fluid.of('gtceu:high_grade_solder', 576),
            Fluid.of('gtceu:polybenzimidazole', 576))
        .itemOutputs('gtceu:nano_processor_mainframe')
        .stationResearch(b => b
            .researchStack(Item.of('gtceu:nano_processor_computer'))
            .CWUt(32)
            .EUt(VA[GTValues.IV]))
        .duration(1200)
        .EUt(VA[GTValues.LuV])

    // ---- LuV lean (Stage 3 @UHV end, gate: tpu_chip). Packages over loose RAM,
    // quarter metals, 2x output. ----
    newHighCircuit('quantum_computer_luv_lean', 2, r => clean(r
        .itemInputs('gtceu:fiber_reinforced_printed_circuit_board', 'gtceu:quantum_processor_assembly',
            '2x af9:edram_cpu_package', 'af9:tpu_chip', '8x gtceu:fine_osmiridium_wire')
        .itemOutputs('2x gtceu:quantum_processor_computer')
        .duration(200).EUt(2400)))

    newHighCircuit('crystal_assembly_luv_lean', 2, r => clean(r
        .itemInputs('gtceu:multilayer_fiber_reinforced_printed_circuit_board', 'gtceu:crystal_processor',
            '3x af9:edram_soc_package', 'af9:tpu_chip', '4x gtceu:fine_niobium_titanium_wire')
        .itemOutputs('4x gtceu:crystal_processor_assembly')
        .duration(200).EUt(9600)))

    newLivingCircuit('wetware_processor_luv_lean', 1, r => clean(r
        .itemInputs('gtceu:neuro_processing_unit', 'gtceu:crystal_processor_assembly', 'af9:vpu_chip',
            'af9:tpu_chip', '2x gtceu:fine_niobium_titanium_wire')
        .itemOutputs('4x gtceu:wetware_processor')
        .duration(100).EUt(38400)))

    // ================================= 8. ZPM (LuV -> ZPM) =================================
    // GT's ZPM recipes are replaced wholesale: crystal computer (assembler), quantum
    // mainframe (assembly line), wetware assembly (assembler). All take LuV circuits.
    // Both id shapes are removed (GT's exact suffix per tier is version-dependent;
    // removing a missing id is a harmless no-op, leaving one would be a bypass).
    ;['crystal_processor_computer', 'crystal_processor_computer_zpm'].forEach(id => {
        event.remove({ id: `gtceu:circuit_assembler/${id}` })
        event.remove({ id: `gtceu:circuit_assembler/${id}_soldering_alloy` })
    })
    ;['quantum_processor_mainframe', 'quantum_processor_mainframe_zpm'].forEach(id => {
        event.remove({ id: `gtceu:assembly_line/${id}` })
    })
    ;['wetware_processor_assembly', 'wetware_processor_assembly_zpm'].forEach(id => {
        event.remove({ id: `gtceu:circuit_assembler/${id}` })
        event.remove({ id: `gtceu:circuit_assembler/${id}_soldering_alloy` })
    })

    // Crystal computer: LuV crystal assembly + ZPM-native HASoC + NbTi.
    highCircuit('crystal_computer_zpm', 2, r => clean(r
        .itemInputs('gtceu:multilayer_fiber_reinforced_printed_circuit_board', '2x gtceu:crystal_processor_assembly',
            chip('zpm', 'highly_advanced_soc', 2), '8x gtceu:advanced_smd_capacitor', '16x gtceu:fine_niobium_titanium_wire')
        .itemOutputs('gtceu:crystal_processor_computer')
        .duration(400).EUt(VA[GTValues.IV])))

    // Quantum mainframe (AL): LuV quantum computer + ZPM crystal computer + TMD logic.
    assemblyLine('quantum_mainframe_zpm', r => r
        .itemInputs(
            '2x gtceu:tritanium_frame',
            '2x gtceu:quantum_processor_computer',
            '2x gtceu:crystal_processor_computer',
            '8x af9:tmd_logic_chip',
            '16x gtceu:fine_naquadah_alloy_wire',
            '8x gtceu:tritanium_bolt')
        .inputFluids(
            Fluid.of('gtceu:high_grade_solder', 576),
            Fluid.of('gtceu:polybenzimidazole', 576))
        .itemOutputs('gtceu:quantum_processor_mainframe')
        .stationResearch(b => b
            .researchStack(Item.of('gtceu:quantum_processor_computer'))
            .CWUt(48)
            .EUt(VA[GTValues.LuV]))
        .duration(1200)
        .EUt(VA[GTValues.ZPM]))

    // Wetware assembly: LuV wetware + ZPM crystal computer + NPU, living solder.
    livingCircuit('wetware_assembly_zpm', 2, r => clean(r
        .itemInputs('2x gtceu:wetware_processor', 'gtceu:crystal_processor_computer', 'gtceu:neuro_processing_unit',
            '8x gtceu:advanced_smd_capacitor', '16x gtceu:fine_naquadah_alloy_wire')
        .itemOutputs('gtceu:wetware_processor_assembly')
        .duration(400).EUt(VA[GTValues.LuV])))

    // ---- ZPM lean (Stage 3 @UHV end, gate: tpu_chip + quantanium) ----
    newHighCircuit('crystal_computer_zpm_lean', 2, r => clean(r
        .itemInputs('gtceu:multilayer_fiber_reinforced_printed_circuit_board', 'gtceu:crystal_processor_assembly',
            chip('zpm', 'highly_advanced_soc', 2), 'af9:tpu_chip', '4x gtceu:fine_niobium_titanium_wire')
        .itemOutputs('2x gtceu:crystal_processor_computer')
        .duration(200).EUt(VA[GTValues.IV])))

    event.recipes.gtceu.assembly_line('af9:quantum_mainframe_zpm_lean')
        .itemInputs(
            '2x gtceu:tritanium_frame',
            'gtceu:quantum_processor_computer',
            'gtceu:crystal_processor_computer',
            '4x af9:tmd_logic_chip',
            'af9:tpu_chip',
            'gtceu:quantanium_dust',
            '8x gtceu:fine_naquadah_alloy_wire')
        .inputFluids(
            Fluid.of('gtceu:high_grade_solder', 288),
            Fluid.of('gtceu:polybenzimidazole', 288))
        .itemOutputs('2x gtceu:quantum_processor_mainframe')
        .duration(600)
        .EUt(VA[GTValues.ZPM])

    newLivingCircuit('wetware_assembly_zpm_lean', 2, r => clean(r
        .itemInputs('gtceu:wetware_processor', 'gtceu:crystal_processor_computer', 'af9:tpu_chip',
            '4x gtceu:fine_naquadah_alloy_wire')
        .itemOutputs('2x gtceu:wetware_processor_assembly')
        .duration(200).EUt(VA[GTValues.LuV])))

    // ================================= 9. UV (ZPM -> UV) =================================
    // Crystal mainframe (AL): ZPM crystal computer + UV-native TPU.
    event.remove({ id: 'gtceu:assembly_line/crystal_processor_mainframe' })
    event.remove({ id: 'gtceu:assembly_line/crystal_processor_mainframe_uv' })
    assemblyLine('crystal_mainframe_uv', r => r
        .itemInputs(
            '2x gtceu:tritanium_frame',
            '2x gtceu:crystal_processor_computer',
            '4x af9:tpu_chip',
            '8x af9:memristor_chip',
            '16x gtceu:fine_tritanium_wire',
            '8x gtceu:europium_plate')
        .inputFluids(
            Fluid.of('gtceu:high_grade_solder', 1152),
            Fluid.of('gtceu:polybenzimidazole', 1152))
        .itemOutputs('gtceu:crystal_processor_mainframe')
        .stationResearch(b => b
            .researchStack(Item.of('gtceu:crystal_processor_computer'))
            .CWUt(64)
            .EUt(VA[GTValues.ZPM]))
        .duration(1600)
        .EUt(VA[GTValues.UV]))

    // Wetware supercomputer: ZPM wetware assembly + living solder, sterile.
    event.remove({ id: 'gtceu:circuit_assembler/wetware_processor_computer' })
    event.remove({ id: 'gtceu:circuit_assembler/wetware_processor_computer_uv' })
    event.remove({ id: 'gtceu:circuit_assembler/wetware_processor_computer_uv_soldering_alloy' })
    event.recipes.gtceu.circuit_assembler('af9:wetware_processor_computer_living')
        .itemInputs(
            'gtceu:wetware_printed_circuit_board',
            '2x gtceu:wetware_processor_assembly',
            '8x gtceu:advanced_smd_capacitor',
            '8x gtceu:advanced_smd_transistor',
            '32x gtceu:fine_tritanium_wire')
        .inputFluids(Fluid.of('gtceu:living_solder', 288))
        .itemOutputs('gtceu:wetware_processor_computer')
        .cleanroom(CleanroomType.STERILE_CLEANROOM)
        .duration(400)
        .EUt(VA[GTValues.UV])

    // ---- UV lean (Stage 3 @UHV end, gate: tpu_chip + quantanium) ----
    event.recipes.gtceu.assembly_line('af9:crystal_mainframe_uv_lean')
        .itemInputs(
            '2x gtceu:tritanium_frame',
            'gtceu:crystal_processor_computer',
            '2x af9:tpu_chip',
            '4x af9:memristor_chip',
            'gtceu:quantanium_dust',
            '8x gtceu:fine_tritanium_wire')
        .inputFluids(
            Fluid.of('gtceu:high_grade_solder', 576),
            Fluid.of('gtceu:polybenzimidazole', 576))
        .itemOutputs('2x gtceu:crystal_processor_mainframe')
        .duration(800)
        .EUt(VA[GTValues.UV])

    event.recipes.gtceu.circuit_assembler('af9:wetware_processor_computer_lean_living')
        .itemInputs(
            'gtceu:wetware_printed_circuit_board',
            'gtceu:wetware_processor_assembly',
            'af9:tpu_chip',
            '4x gtceu:advanced_smd_capacitor',
            '8x gtceu:fine_tritanium_wire')
        .inputFluids(Fluid.of('gtceu:living_solder', 144))
        .itemOutputs('2x gtceu:wetware_processor_computer')
        .cleanroom(CleanroomType.STERILE_CLEANROOM)
        .duration(200)
        .EUt(VA[GTValues.UV])

    // ================================= 10. UHV (UV -> UHV) =================================
    // UHV wetware mainframe: no assembly line version left (both AF9 bills removed); GT's own stays off.
    // The only path is plasma soldering in the Orbital Array Mk2 (solders.js).
    event.remove({ id: 'gtceu:assembly_line/wetware_mainframe_uhv' })

    // AI Acceleration Card: TPU + memristor on a wetware board, traced in 128 fine sanguinite wire.
    // UHV circuit assembler, plasma solder canonical (tin stays as dead budget option). The compute
    // card of the new DTPF (docs/dtpf.md).
    newPlasmaCircuit('ai_acceleration_card', 4, r => clean(r
        .itemInputs('gtceu:wetware_printed_circuit_board', 'af9:tpu_chip', 'af9:memristor_chip',
            '128x gtceu:fine_sanguinite_wire')
        .itemOutputs('af9:ai_acceleration_card')
        .duration(600).EUt(VA[GTValues.UHV])))

    // ================================= 11. Pico circuits =================================
    // GTNH's Pico components, made in the Advanced Circuit Manufacturer (advanced_circuit_manufacturer: up to 16 items
    // and 4 fluids a recipe, computation through its hatch, UHV on 300 A through its laser hatch) from chromodynium
    // and the finest chips. The XPS and NVM processors that took them are out for now (see the header), so nothing
    // takes a Pico circuit at the moment.
    const pico = (id, spec) => {
        const r = event.recipes.gtceu.advanced_circuit_manufacturer(`af9:${id}`)
            .itemInputs(spec.items)
            .itemOutputs(spec.out)
            .CWUt(spec.cwu)
            .duration(spec.duration)
            .EUt(VA[GTValues.UHV], PICO_AMPS)
        spec.fluids.forEach(f => r.inputFluids(f))
    }
    // the board: a wetware board traced in fine chromodynium wire
    pico('pico_board', {
        items: ['gtceu:wetware_printed_circuit_board', '2x gtceu:chromodynium_plate', '8x gtceu:fine_chromodynium_wire'],
        fluids: [Fluid.of('gtceu:plasma_solder', 144)],
        out: 'af9:pico_board', cwu: 32, duration: 400 })
    // cleansed in ultrapure water and noble gas, every stray particle gone
    pico('cleansed_pico_board', {
        items: ['af9:pico_board'],
        fluids: [Fluid.of('gtceu:distilled_water', 2000), Fluid.of('gtceu:argon', 1000)],
        out: 'af9:cleansed_pico_board', cwu: 32, duration: 300 })
    // the CPU: the memory and compute dies of the NVM tier set on the clean board, bonded with fine chromodynium wire
    pico('pico_cpu', {
        items: ['af9:cleansed_pico_board', '2x af9:tpu_chip', '4x af9:memristor_chip', '4x af9:quantum_dot_ic_chip',
            '2x gtceu:chromodynium_plate', '16x gtceu:fine_chromodynium_wire'],
        fluids: [Fluid.of('gtceu:plasma_solder', 576), Fluid.of('gtceu:polybenzimidazole', 288)],
        out: 'af9:pico_cpu', cwu: 64, duration: 800 })
    // the CPU with its photonic and spin-logic dies put in order
    pico('organized_pico_circuit', {
        items: ['af9:pico_cpu', '8x af9:photonic_ic_chip', '8x af9:spin_logic_chip', '8x gtceu:fine_sanguinite_wire'],
        fluids: [Fluid.of('gtceu:plasma_solder', 288), Fluid.of('gtceu:polybenzimidazole', 144)],
        out: 'af9:organized_pico_circuit', cwu: 96, duration: 600 })
    // the housing of the rack
    pico('processed_pico_circuit_casing', {
        items: ['2x gtceu:chromodynium_plate', 'gtceu:tritanium_frame', '16x gtceu:polybenzimidazole_foil'],
        fluids: [Fluid.of('gtceu:polybenzimidazole', 576)],
        out: 'af9:processed_pico_circuit_casing', cwu: 32, duration: 400 })
    // four organized circuits in their casing
    pico('pico_circuit_rack', {
        items: ['4x af9:organized_pico_circuit', 'af9:processed_pico_circuit_casing', '32x gtceu:fine_sanguinite_wire'],
        fluids: [Fluid.of('gtceu:plasma_solder', 1440), Fluid.of('gtceu:polybenzimidazole', 1152)],
        out: 'af9:pico_circuit_rack', cwu: 128, duration: 1200 })

    // ================================= 13. packages =================================
    // Silicon interposers: dies flip-chipped to a laminate with tier wire. The circuit
    // takes one package instead of 4-16 loose chips; the litho stays the cost.
    event.recipes.gtceu.assembler('af9:asic_package')
        .itemInputs('af9:asic_chip', '2x gtceu:ram_chip', 'gtceu:epoxy_plate', '4x gtceu:fine_gold_wire')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 72))
        .itemOutputs('af9:asic_package')
        .duration(400)
        .EUt(VA[GTValues.HV])
        .cleanroom(CleanroomType.CLEANROOM)

    event.recipes.gtceu.assembler('af9:photonic_package')
        .itemInputs('af9:photonic_ic_chip', '2x af9:spin_logic_chip', 'gtceu:europium_plate', '4x gtceu:fine_sanguinite_wire')
        .inputFluids(Fluid.of('gtceu:plasma_solder', 36))
        .itemOutputs('af9:photonic_package')
        .duration(400)
        .EUt(VA[GTValues.UHV])
        .cleanroom(CleanroomType.CLEANROOM)
})

ServerEvents.tags('item', event => {
    event.add('gtceu:circuits/xps', ['af9:xps_processor', 'af9:xps_processor_mainframe'])
    event.add('gtceu:circuits/nvm', ['af9:nvm_processor', 'af9:nvm_processor_mainframe'])
})
