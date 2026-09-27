ServerEvents.recipes(event => {
    // AF9: the LuV Nano Mainframe also needs 16 RAM chips
    event.recipes.gtceu.assembly_line("circuit_1_iv_luv")
    .itemInputs("gtceu:micro_processor_mainframe","16x gtceu:rtm_alloy_double_cable","16x gtceu:double_osmiridium_plate","64x gtceu:fine_osmiridium_wire","64x gtceu:fine_osmiridium_wire","16x gtceu:nano_processor_computer","mekanism:qio_drive_hyper_dense","16x gtceu:annealed_copper_octal_wire","32x gtceu:microchip_processor","64x gtceu:smd_capacitor","32x gtceu:smd_inductor","16x gtceu:ram_chip")
    .inputFluids("gtceu:indium 4000")
    .itemOutputs("gtceu:nano_processor_mainframe")
    .duration(1600).EUt(GTValues.VA[GTValues.LuV])

    // AF9: an extra, faster Nano Mainframe with 4 eDRAM CPU Packages (a CPU die with its eDRAM cache) instead of the
    // 16 RAM chips (tiered_circuits.js: the other eDRAM recipes)
    event.recipes.gtceu.assembly_line("af9:nano_processor_mainframe_edram")
    .itemInputs("gtceu:micro_processor_mainframe","16x gtceu:rtm_alloy_double_cable","16x gtceu:double_osmiridium_plate","64x gtceu:fine_osmiridium_wire","64x gtceu:fine_osmiridium_wire","16x gtceu:nano_processor_computer","mekanism:qio_drive_hyper_dense","16x gtceu:annealed_copper_octal_wire","32x gtceu:microchip_processor","64x gtceu:smd_capacitor","32x gtceu:smd_inductor","4x kubejs:edram_cpu_package")
    .inputFluids("gtceu:indium 4000")
    .itemOutputs("gtceu:nano_processor_mainframe")
    .duration(800).EUt(GTValues.VA[GTValues.LuV])

    event.recipes.gtceu.assembly_line("circuit_2_luv_zpm")
    .itemInputs("gtceu:nano_processor_mainframe","16x gtceu:samarium_iron_arsenic_oxide_hex_wire","64x gtceu:data_stick","64x gtceu:data_stick","64x gtceu:fine_rhodium_wire","16x gtceu:quantum_processor_computer","mekanism:qio_drive_time_dilating","32x gtceu:microchip_processor","64x gtceu:smd_capacitor","64x gtceu:smd_inductor","64x gtceu:normal_optical_pipe","64x gtceu:normal_optical_pipe")
    .inputFluids("gtceu:stellite_100 10000","mekanismgenerators:fusion_fuel 10000")
    .itemOutputs("gtceu:quantum_processor_mainframe")
    .duration(1600).EUt(GTValues.VA[GTValues.ZPM])

    event.recipes.gtceu.assembly_line("circuit_3")
    .itemInputs("gtceu:quantum_processor_mainframe","4x gtceu:indium_tin_barium_titanium_cuprate_hex_wire","16x gtceu:double_trinium_plate","64x gtceu:uhpic_chip","16x gtceu:crystal_processor_computer","mekanism:qio_drive_supermassive","64x gtceu:microchip_processor","64x gtceu:microchip_processor","64x gtceu:microchip_processor","64x gtceu:smd_diode","64x gtceu:advanced_smd_inductor","64x gtceu:normal_optical_pipe","64x gtceu:normal_laser_pipe")
    .inputFluids("gtceu:europium 1000")
    .itemOutputs("gtceu:crystal_processor_mainframe")
    .duration(1600).EUt(GTValues.VA[GTValues.UV])
    .stationResearch(b => b.researchStack('gtceu:crystal_processor_computer').CWUt(10,384000).EUt(12200))

    // AF9: the UHV Wetware Mainframe also needs 16 TPUs (the AI accelerator, chip_uses.js)
    event.recipes.gtceu.assembly_line("circuit_4_uv")
    .itemInputs("gtceu:crystal_processor_mainframe","8x gtceu:uranium_rhodium_dinaquadide_hex_wire","16x gtceu:tritanium_frame","64x gtceu:uhpic_chip","64x gtceu:uhpic_chip","16x gtceu:wetware_processor_computer","mekanism:qio_drive_supermassive","64x gtceu:polybenzimidazole_foil","64x gtceu:advanced_smd_diode","32x gtceu:enriched_naquadah_trinium_europium_duranide_double_wire","64x gtceu:advanced_smd_diode","64x gtceu:advanced_smd_resistor","64x gtceu:normal_optical_pipe","64x gtceu:normal_laser_pipe","16x kubejs:tpu_chip")
    .inputFluids("gtceu:europium 80000","gtceu:soldering_alloy 40000")
    .itemOutputs("gtceu:wetware_processor_mainframe")
    .duration(1600).EUt(GTValues.VA[GTValues.UV])
    .stationResearch(b => b.researchStack('gtceu:wetware_processor_computer').CWUt(10,384000).EUt(12200))
})


// all removals
ServerEvents.recipes(e => {
    const toRemoveId = [
       "gtceu:circuit_assembler/nano_mainframe_luv_soldering_alloy","gtceu:circuit_assembler/nano_mainframe_luv_asmd","gtceu:circuit_assembler/nano_mainframe_luv_asmd_soldering_alloy","gtceu:circuit_assembler/nano_mainframe_luv",
       "gtceu:circuit_assembler/quantum_mainframe_zpm","gtceu:circuit_assembler/quantum_mainframe_zpm_asmd_soldering_alloy","gtceu:circuit_assembler/quantum_mainframe_zpm_soldering_alloy","gtceu:circuit_assembler/quantum_mainframe_zpm_asmd",
       "gtceu:assembly_line/crystal_mainframe_uv","gtceu:assembly_line/wetware_mainframe_uhv"
    ];   
    toRemoveId.forEach(element => {
    e.remove({ id: element});
    })
})