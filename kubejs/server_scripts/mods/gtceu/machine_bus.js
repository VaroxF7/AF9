// AF9 - Crafting of the machine bus (startup_scripts/gtceu/machine_bus.js, af9-core com.af9.core.bus). MV, like GT's
// Central Monitor it plugs into: the Bus Connector and the module run on an MCU (kubejs:mcu_chip, silicon wafers).

ServerEvents.recipes(allthemods => {
    const VA = GTValues.VA

    // Polycat Cable: four twisted pairs of annealed copper in a polyethylene jacket
    allthemods.recipes.gtceu.assembler('af9:polycat_cable')
        .itemInputs('8x gtceu:fine_annealed_copper_wire', '2x gtceu:polyethylene_foil')
        .itemOutputs('8x af9:polycat_cable')
        .duration(100)
        .EUt(VA[GTValues.LV])

    // Bus Connector: an MV hull with the port, its controller (two MCUs) and a circuit
    allthemods.recipes.gtceu.assembler('af9:bus_connector')
        .itemInputs('gtceu:mv_machine_hull', '2x kubejs:mcu_chip', '4x af9:polycat_cable', '#gtceu:circuits/mv')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 144))
        .itemOutputs('gtceu:mv_bus_connector')
        .duration(200)
        .EUt(VA[GTValues.MV])

    // Machine Bus Module: built like GT's Text Module, with an MCU and a port
    allthemods.recipes.gtceu.assembler('af9:machine_bus_module')
        .itemInputs('gtceu:plastic_printed_circuit_board', 'kubejs:mcu_chip', '2x af9:polycat_cable',
            '4x gtceu:fine_red_alloy_wire', '#gtceu:circuits/mv')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 144))
        .itemOutputs('af9:machine_bus_module')
        .duration(400)
        .EUt(VA[GTValues.MV])
})
