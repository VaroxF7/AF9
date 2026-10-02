// AF9 - Crafting of the Crafting CPU Array (startup_scripts/gtceu/crafting_cpu.js, af9-core com.af9.core.cpu): HBM Memory
// Sticks and Stacks, CPU Clusters, the CPU Rack, the array and its AE2 core block. Spec: docs/crafting-cpu.md
//
// HBM costs 8x what the memory it is built on costs: a Stick is eight Silicon RAM cards' worth (RAM chips, board, wire),
// a Stack eight Nano RAM cards' worth (eDRAM chips). A CPU Cluster is four CPUs on a board, a Superpositioned Cluster
// eight.

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const solder = mb => Fluid.of('gtceu:soldering_alloy', mb)

    // ---- HBM Memory Stick: 8 x (plastic board + 4 RAM chips + 8 gold wire), a programmed circuit tells it from the card
    event.recipes.gtceu.circuit_assembler('af9:hbm_memory_stick')
        .itemInputs('8x gtceu:plastic_printed_circuit_board', '32x gtceu:ram_chip', '64x gtceu:fine_gold_wire')
        .circuit(4)
        .inputFluids(solder(1152))
        .itemOutputs('kubejs:hbm_memory_stick')
        .duration(800)
        .EUt(VA[GTValues.HV])

    // ---- HBM Memory Stack: 8 x (epoxy board + 4 eDRAM chips + 8 platinum wire) ----
    event.recipes.gtceu.circuit_assembler('af9:hbm_memory_stack')
        .itemInputs('8x gtceu:epoxy_printed_circuit_board', '32x kubejs:edram_chip', '64x gtceu:fine_platinum_wire')
        .circuit(5)
        .inputFluids(solder(1152))
        .itemOutputs('kubejs:hbm_memory_stack')
        .duration(800)
        .EUt(VA[GTValues.IV])
        .cleanroom(CleanroomType.CLEANROOM)

    // ---- CPU Cluster: four CPUs on a board ----
    event.recipes.gtceu.circuit_assembler('af9:cpu_cluster')
        .itemInputs('gtceu:epoxy_printed_circuit_board', '4x gtceu:nano_cpu_chip', '8x gtceu:fine_platinum_wire')
        .inputFluids(solder(288))
        .itemOutputs('kubejs:cpu_cluster')
        .duration(600)
        .EUt(VA[GTValues.IV])
        .cleanroom(CleanroomType.CLEANROOM)

    // ---- CPU Superpositioned Cluster: eight quantum CPUs ----
    event.recipes.gtceu.circuit_assembler('af9:superpositioned_cpu_cluster')
        .itemInputs('gtceu:fiber_reinforced_printed_circuit_board', '8x gtceu:qbit_cpu_chip',
            '16x gtceu:fine_osmiridium_wire')
        .inputFluids(solder(576))
        .itemOutputs('kubejs:superpositioned_cpu_cluster')
        .duration(900)
        .EUt(VA[GTValues.LuV])
        .cleanroom(CleanroomType.CLEANROOM)

    // ---- The machines ----
    event.recipes.gtceu.assembler('af9:hv_cpu_rack')
        .itemInputs('gtceu:hv_machine_hull', '2x #gtceu:circuits/hv', '2x gtceu:hv_electric_motor',
            '4x af9:optical_bus_cable', '4x gtceu:stainless_steel_plate')
        .inputFluids(solder(288))
        .itemOutputs('gtceu:hv_cpu_rack')
        .duration(300)
        .EUt(VA[GTValues.HV])
    event.recipes.gtceu.assembler('af9:crafting_cpu_array')
        .itemInputs('gtceu:hv_machine_hull', '4x #gtceu:circuits/hv', '4x gtceu:advanced_computer_casing',
            '2x gtceu:hv_sensor', '2x gtceu:hv_robot_arm', '8x af9:optical_bus_cable')
        .inputFluids(solder(576))
        .itemOutputs('gtceu:crafting_cpu_array')
        .duration(600)
        .EUt(VA[GTValues.HV])
    // the array's block on the ME network: an AE2 crafting unit with an engineering processor on a GT circuit
    event.recipes.gtceu.assembler('af9:crafting_cpu_core')
        .itemInputs('ae2:crafting_unit', 'ae2:engineering_processor', 'gtceu:hv_emitter', '2x #gtceu:circuits/hv')
        .inputFluids(solder(144))
        .itemOutputs('af9:crafting_cpu_core')
        .duration(200)
        .EUt(VA[GTValues.HV])
})
