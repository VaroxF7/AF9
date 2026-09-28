// AF9 - Crafting of the machine bus (startup_scripts/gtceu/machine_bus.js, af9-core com.af9.core.bus). MV, like GT's
// Central Monitor it plugs into: the Bus Connector, the module and the Bus Controller run on an MCU (kubejs:mcu_chip,
// silicon wafers).

ServerEvents.recipes(allthemods => {
    const VA = GTValues.VA

    // Optical Bus Cable: borosilicate glass fibre (MV extruder) in a polyethylene jacket. GT's own Optical Fiber
    // Cable is IV and cannot branch; this one keeps the bus at MV
    allthemods.recipes.gtceu.assembler('af9:optical_bus_cable')
        .itemInputs('8x gtceu:fine_borosilicate_glass_wire', '2x gtceu:polyethylene_foil')
        .itemOutputs('8x af9:optical_bus_cable')
        .duration(100)
        .EUt(VA[GTValues.LV])

    // Bus Connector: an MV hull with the port, its controller (two MCUs) and a circuit
    allthemods.recipes.gtceu.assembler('af9:bus_connector')
        .itemInputs('gtceu:mv_machine_hull', '2x kubejs:mcu_chip', '4x af9:optical_bus_cable', '#gtceu:circuits/mv')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 144))
        .itemOutputs('gtceu:mv_bus_connector')
        .duration(200)
        .EUt(VA[GTValues.MV])

    // Machine Bus Module: built like GT's Text Module, with an MCU and a port
    allthemods.recipes.gtceu.assembler('af9:machine_bus_module')
        .itemInputs('gtceu:plastic_printed_circuit_board', 'kubejs:mcu_chip', '2x af9:optical_bus_cable',
            '4x gtceu:fine_red_alloy_wire', '#gtceu:circuits/mv')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 144))
        .itemOutputs('af9:machine_bus_module')
        .duration(400)
        .EUt(VA[GTValues.MV])

    // Interconnect Hatch: a duplex optical transceiver (an emitter and a sensor) behind two MCUs
    allthemods.recipes.gtceu.assembler('af9:interconnect_hatch')
        .itemInputs('gtceu:mv_machine_hull', '2x kubejs:mcu_chip', 'gtceu:mv_emitter', 'gtceu:mv_sensor',
            '4x af9:optical_bus_cable')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 144))
        .itemOutputs('gtceu:mv_interconnect_hatch')
        .duration(200)
        .EUt(VA[GTValues.MV])

    // ME Computation Link: AE2's calculation processors on the network side, an optical port on the back
    allthemods.recipes.gtceu.assembler('af9:me_computation_link')
        .itemInputs('2x ae2:calculation_processor', 'ae2:fluix_glass_cable', 'ae2:quartz_fiber', 'kubejs:mcu_chip',
            '4x af9:optical_bus_cable')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 144))
        .itemOutputs('af9:me_computation_link')
        .duration(200)
        .EUt(VA[GTValues.MV])

    // CWU Server: a server in a machine hull. LV runs on plain circuits; from MV on AF9's chips: APUs (CPU and GPU
    // on one die) and GT's RAM, ASICs from HV, eDRAM and MRAM at IV
    const servers = [
        ['lv', ['4x #gtceu:circuits/lv', '2x gtceu:tin_single_cable'], 'gtceu:tin', 144],
        ['mv', ['2x #gtceu:circuits/mv', '2x kubejs:apu_chip', '4x gtceu:ram_chip', '2x af9:optical_bus_cable'],
            'gtceu:soldering_alloy', 144],
        ['hv', ['2x #gtceu:circuits/hv', '4x kubejs:apu_chip', '8x gtceu:ram_chip', '2x kubejs:asic_chip',
            '2x af9:optical_bus_cable'], 'gtceu:soldering_alloy', 288],
        ['ev', ['2x #gtceu:circuits/ev', '8x kubejs:apu_chip', '16x gtceu:ram_chip', '4x kubejs:asic_chip',
            '4x af9:optical_bus_cable'], 'gtceu:soldering_alloy', 432],
        ['iv', ['2x #gtceu:circuits/iv', '8x kubejs:apu_chip', '4x kubejs:edram_chip', '4x kubejs:mram_chip',
            '8x kubejs:asic_chip', '4x af9:optical_bus_cable'], 'gtceu:soldering_alloy', 576]
    ]
    servers.forEach(([tier, parts, fluid, mb], i) => {
        allthemods.recipes.gtceu.assembler(`af9:${tier}_cwu_server`)
            .itemInputs([`gtceu:${tier}_machine_hull`].concat(parts))
            .inputFluids(Fluid.of(fluid, mb))
            .itemOutputs(`gtceu:${tier}_cwu_server`)
            .duration(200)
            .EUt(VA[GTValues.LV + i])
    })

    // Bus Controller: a PLC in an MV hull; the robot arm is what moves the ingredients
    allthemods.recipes.gtceu.assembler('af9:bus_controller')
        .itemInputs('gtceu:mv_machine_hull', '4x kubejs:mcu_chip', '2x #gtceu:circuits/mv', 'gtceu:mv_robot_arm',
            '8x af9:optical_bus_cable')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 288))
        .itemOutputs('gtceu:bus_controller')
        .duration(400)
        .EUt(VA[GTValues.MV])
})
