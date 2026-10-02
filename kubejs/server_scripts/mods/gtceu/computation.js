// AF9 - Crafting of the computation arrays (startup_scripts/gtceu/computation.js, af9-core com.af9.core.compute): the
// Computer Racks, the N1 arrays, the Server Casing, the MV Coolant Hatch and the rack cards. Everything for the MV
// array comes before the Photolithography Line (no chips): the Tube cards run on vacuum tubes and magnetic core memory.
// Spec: docs/computation.md §2

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const solder = mb => Fluid.of('gtceu:soldering_alloy', mb)

    // ---- Server Casing: aluminium panels on a steel frame ----
    event.shaped('2x kubejs:server_casing', ['PHP', 'PFP', 'PWP'], {
        P: 'gtceu:aluminium_plate', F: 'gtceu:steel_frame', H: '#forge:tools/hammers', W: '#forge:tools/wrenches'
    }).id('af9:shaped/server_casing')
    event.recipes.gtceu.assembler('af9:server_casing')
        .itemInputs('6x gtceu:aluminium_plate', 'gtceu:steel_frame')
        .circuit(6)
        .itemOutputs('2x kubejs:server_casing')
        .duration(50)
        .EUt(16)

    // ---- MV Coolant Hatch: the MV array's, before the cryostat (lined with polyethylene, filled with distilled
    // water instead of supercooled hydrogen) ----
    event.recipes.gtceu.assembler('af9:mv_coolant_hatch')
        .itemInputs('gtceu:mv_input_hatch', '2x gtceu:mv_electric_pump', 'gtceu:frostproof_machine_casing',
            '4x gtceu:polyethylene_plate')
        .inputFluids(Fluid.of('gtceu:distilled_water', 1000))
        .itemOutputs('gtceu:mv_coolant_hatch')
        .duration(400)
        .EUt(VA[GTValues.MV])

    // ---- Computer Racks ----
    event.recipes.gtceu.assembler('af9:mv_computer_rack')
        .itemInputs('gtceu:mv_machine_hull', '2x #gtceu:circuits/mv', '2x gtceu:mv_electric_motor',
            '4x gtceu:fine_borosilicate_glass_wire', '4x gtceu:aluminium_plate')
        .inputFluids(solder(144))
        .itemOutputs('gtceu:mv_computer_rack')
        .duration(200)
        .EUt(VA[GTValues.MV])
    event.recipes.gtceu.assembler('af9:luv_computer_rack')
        .itemInputs('gtceu:luv_machine_hull', '2x #gtceu:circuits/luv', '2x gtceu:luv_electric_motor',
            '4x gtceu:fine_borosilicate_glass_wire', '4x gtceu:rhodium_plated_palladium_plate')
        .inputFluids(solder(576))
        .itemOutputs('gtceu:luv_computer_rack')
        .duration(400)
        .EUt(VA[GTValues.LuV])
        .cleanroom(CleanroomType.CLEANROOM)

    // ---- The arrays ----
    event.recipes.gtceu.assembler('af9:n1_computation_array')
        .itemInputs('gtceu:mv_machine_hull', '4x #gtceu:circuits/mv', '4x kubejs:server_casing',
            '4x gtceu:mv_electric_pump', '2x gtceu:mv_electric_motor', '8x gtceu:fine_borosilicate_glass_wire')
        .inputFluids(solder(288))
        .itemOutputs('gtceu:n1_computation_array')
        .duration(600)
        .EUt(VA[GTValues.MV])
    event.recipes.gtceu.assembler('af9:n1_supercomputer_array')
        .itemInputs('gtceu:luv_machine_hull', '4x #gtceu:circuits/luv', '8x gtceu:computer_casing',
            '4x gtceu:computer_heat_vent', '4x gtceu:luv_electric_pump', '2x gtceu:luv_field_generator',
            '16x gtceu:fine_borosilicate_glass_wire')
        .inputFluids(solder(1152))
        .itemOutputs('gtceu:n1_supercomputer_array')
        .duration(1200)
        .EUt(VA[GTValues.LuV])
        .cleanroom(CleanroomType.CLEANROOM)

    // ---- Cards (circuit assembler): a board, the processor or memory, wire of the tier ----
    // [id, board, [inputs], tier voltage, cleanroom]
    const cards = [
        // Tube (MV): vacuum tubes; magnetic core memory (magnetic iron rings threaded on copper)
        ['tube_cpu', 'gtceu:phenolic_printed_circuit_board',
            ['6x gtceu:vacuum_tube', '2x gtceu:basic_electronic_circuit', '4x gtceu:fine_copper_wire'], GTValues.MV, false],
        ['tube_gpu', 'gtceu:phenolic_printed_circuit_board',
            ['12x gtceu:vacuum_tube', 'gtceu:good_electronic_circuit', '4x #gtceu:resistors', '8x gtceu:fine_copper_wire'],
            GTValues.MV, false],
        ['tube_ram', 'gtceu:phenolic_printed_circuit_board',
            ['4x gtceu:magnetic_iron_rod', '16x gtceu:fine_copper_wire'], GTValues.MV, false],
        // Silicon (HV): 350 nm chips
        ['silicon_cpu', 'gtceu:plastic_printed_circuit_board',
            ['2x gtceu:cpu_chip', '4x #gtceu:capacitors', '8x gtceu:fine_gold_wire'], GTValues.HV, false],
        ['silicon_gpu', 'gtceu:plastic_printed_circuit_board',
            ['2x kubejs:apu_chip', '4x #gtceu:transistors', '8x gtceu:fine_gold_wire'], GTValues.HV, false],
        ['silicon_ram', 'gtceu:plastic_printed_circuit_board',
            ['4x gtceu:ram_chip', '8x gtceu:fine_gold_wire'], GTValues.HV, false],
        // Nano (IV)
        ['nano_cpu', 'gtceu:epoxy_printed_circuit_board',
            ['2x gtceu:nano_cpu_chip', '4x gtceu:smd_capacitor', '8x gtceu:fine_platinum_wire'], GTValues.IV, true],
        ['nano_gpu', 'gtceu:epoxy_printed_circuit_board',
            ['2x gtceu:advanced_soc', '4x gtceu:smd_transistor', '8x gtceu:fine_platinum_wire'], GTValues.IV, true],
        ['nano_ram', 'gtceu:epoxy_printed_circuit_board',
            ['4x kubejs:edram_chip', '8x gtceu:fine_platinum_wire'], GTValues.IV, true],
        // Quantum (LuV)
        ['quantum_cpu', 'gtceu:fiber_reinforced_printed_circuit_board',
            ['2x gtceu:qbit_cpu_chip', '4x gtceu:advanced_smd_capacitor', '8x gtceu:fine_osmiridium_wire'],
            GTValues.LuV, true],
        ['quantum_gpu', 'gtceu:fiber_reinforced_printed_circuit_board',
            ['2x kubejs:vpu_chip', '4x gtceu:advanced_smd_transistor', '8x gtceu:fine_osmiridium_wire'],
            GTValues.LuV, true],
        ['quantum_ram', 'gtceu:fiber_reinforced_printed_circuit_board',
            ['4x kubejs:mram_chip', '8x gtceu:fine_osmiridium_wire'], GTValues.LuV, true],
        // Tensor (UV): the AI accelerator and ferroelectric memory
        ['tensor_cpu', 'gtceu:multilayer_fiber_reinforced_printed_circuit_board',
            ['2x gtceu:crystal_cpu', '4x gtceu:advanced_smd_capacitor', '8x gtceu:fine_yttrium_barium_cuprate_wire'],
            GTValues.UV, true],
        ['tensor_gpu', 'gtceu:multilayer_fiber_reinforced_printed_circuit_board',
            ['2x kubejs:tpu_chip', '4x gtceu:advanced_smd_transistor', '8x gtceu:fine_yttrium_barium_cuprate_wire'],
            GTValues.UV, true],
        ['tensor_ram', 'gtceu:multilayer_fiber_reinforced_printed_circuit_board',
            ['4x kubejs:feram_chip', '8x gtceu:fine_yttrium_barium_cuprate_wire'], GTValues.UV, true]
    ]
    cards.forEach(([id, board, inputs, tier, clean]) => {
        const recipe = event.recipes.gtceu.circuit_assembler(`af9:${id}_card`)
            .itemInputs([board].concat(inputs))
            .inputFluids(solder(144))
            .itemOutputs(`af9:${id}_card`)
            .duration(400)
            .EUt(VA[tier])
        if (clean) recipe.cleanroom(CleanroomType.CLEANROOM)
        // GT's processor assembly (HV) holds all the RAM card takes, and the HBM Sticks and Stacks (crafting_cpu.js) hold
        // everything the RAM cards take: a programmed circuit tells them apart
        if (id === 'silicon_ram' || id === 'nano_ram') recipe.circuit(3)
    })
})
