ServerEvents.recipes(event => {
    // FLuix Line
    event.recipes.gtceu.polarizer('charged_certus_new')
    .itemInputs('gtceu:certus_quartz_gem')
    .itemOutputs('ae2:charged_certus_quartz_crystal')
    .duration(150).EUt(32)

    event.recipes.gtceu.polarizer('charged_certus_new_ae2')
    .itemInputs('ae2:certus_quartz_crystal')
    .itemOutputs('ae2:charged_certus_quartz_crystal')
    .duration(150).EUt(32)

    event.recipes.gtceu.chemical_reactor('fluix_dust_synthesis')
    .itemInputs('4x gtceu:nether_quartz_dust', '4x minecraft:redstone')
    .inputFluids(Fluid.of('gtceu:distilled_water', 1000))
    .itemOutputs('4x ae2:fluix_dust')
    .duration(200).EUt(60)
    
    event.recipes.gtceu.autoclave('fluix_crytsal_synthetic')
    .itemInputs('ae2:charged_certus_quartz_crystal', '4x ae2:fluix_dust')
    .inputFluids(Fluid.of('gtceu:distilled_water', 1200))
    .itemOutputs('ae2:fluix_crystal')
    .duration(150).EUt(60)
    
    // AE2 - Processor Line
    event.recipes.gtceu.assembler("ae2_processor_1")
     .itemInputs("ae2:printed_silicon","ae2:printed_calculation_processor","#gtceu:circuits/mv","4x #gtceu:resistors","2x gtceu:fine_electrum_wire")
     .itemOutputs("ae2:calculation_processor")
     .inputFluids([Fluid.of("gtceu:red_alloy", 144)])
     .duration(100).EUt(120)

     event.recipes.gtceu.assembler("ae2_processor_2")
     .itemInputs("ae2:printed_silicon","ae2:printed_logic_processor","#gtceu:circuits/mv","4x #gtceu:resistors","2x gtceu:fine_electrum_wire")
     .itemOutputs("ae2:logic_processor")
     .inputFluids([Fluid.of("gtceu:red_alloy", 144)])
     .duration(100).EUt(120)

     event.recipes.gtceu.assembler("ae2_processor_3")
     .itemInputs("ae2:printed_silicon","ae2:printed_engineering_processor","#gtceu:circuits/mv","4x #gtceu:resistors","2x gtceu:fine_electrum_wire")
     .itemOutputs("ae2:engineering_processor")
     .inputFluids([Fluid.of("gtceu:red_alloy", 144)])
     .duration(100).EUt(120)

     event.recipes.gtceu.assembler("ae2_processor_4")
     .itemInputs("ae2:printed_silicon","megacells:printed_accumulation_processor","#gtceu:circuits/ev","8x #gtceu:resistors","4x gtceu:fine_electrum_wire")
     .itemOutputs("megacells:accumulation_processor")
     .inputFluids([Fluid.of("gtceu:silver", 1000)])
     .duration(200).EUt(480)

    // Printed Versions
    event.recipes.gtceu.forming_press("ae2:printed_vrsion_1")
    .itemInputs("gtceu:certus_quartz_plate")
    .itemOutputs("ae2:printed_calculation_processor")
    .notConsumable("ae2:calculation_processor_press")
    .duration(100).EUt(120)

    event.recipes.gtceu.forming_press("ae2:printed_vrsion_2")
    .itemInputs("gtceu:diamond_plate")
    .itemOutputs("ae2:printed_engineering_processor")
    .notConsumable("ae2:engineering_processor_press")
    .duration(100).EUt(120)

    event.recipes.gtceu.forming_press("ae2:printed_vrsion_3")
    .itemInputs("gtceu:electrum_plate")
    .itemOutputs("ae2:printed_logic_processor")
    .notConsumable("ae2:logic_processor_press")
    .duration(100).EUt(120)

    event.recipes.gtceu.forming_press("ae2:printed_vrsion_4")
    .itemInputs("gtceu:silicon_plate")
    .itemOutputs("ae2:printed_silicon")
    .notConsumable("ae2:silicon_press")
    .duration(100).EUt(120)

    // Controller & Drive & Assembler AE2
    event.recipes.gtceu.assembler("ae2_controller")
    .itemInputs("4x ae2:fluix_glass_cable","2x ae2:logic_processor","2x ae2:calculation_processor","2x ae2:engineering_processor","8x gtceu:stainless_steel_bolt")
    .itemOutputs("ae2:controller")
    .duration(125).EUt(60)

    event.recipes.gtceu.assembler("ae2_drive")
    .itemInputs("8x gtceu:stainless_steel_bolt","4x gtceu:stainless_steel_ingot","2x ae2:calculation_processor","2x ae2:fluix_glass_cable")
    .itemOutputs("ae2:drive")
    .duration(125).EUt(60)

    event.recipes.gtceu.assembler("ae2_assembler")
    .itemInputs("8x gtceu:stainless_steel_bolt","4x gtceu:stainless_steel_ingot","2x ae2:annihilation_core","2x ae2:formation_core","2x ae2:quartz_glass")
    .itemOutputs("ae2:molecular_assembler")
    .duration(125).EUt(60)

    // Controller & Drive & Assembler - Refined Storage
    event.recipes.gtceu.assembler("refined_controller")
    .itemInputs("4x refinedstorage:advanced_processor","16x refinedstorage:quartz_enriched_iron","16x refinedstorage:processor_binding","8x gtceu:steel_bolt")
    .itemOutputs("refinedstorage:controller")
    .duration(125).EUt(32).circuit(1)

    event.recipes.gtceu.assembler("refined_drive")
    .itemInputs("4x refinedstorage:advanced_processor","4x gtceu:steel_ingot","4x refinedstorage:processor_binding","8x refinedstorage:cable")
    .itemOutputs("refinedstorage:disk_drive")
    .duration(125).EUt(32).circuit(2)

    event.recipes.gtceu.assembler("refined_assembler")
    .itemInputs("8x refinedstorage:advanced_processor","8x refinedstorage:construction_core","4x refinedstorage:processor_binding","8x refinedstorage:destruction_core")
    .itemOutputs("refinedstorage:crafter")
    .duration(125).EUt(32).circuit(3)
})

ServerEvents.recipes(event => {
  // Components for AE2
  // 1k & 4k Storage Component AE2
    event.recipes.gtceu.circuit_assembler("1k_component_recipe")
    .itemInputs("4x #gtceu:circuits/hv","8x gtceu:fine_cobalt_wire","gtceu:certus_quartz_plate","2x ae2:logic_processor")
    .itemOutputs("ae2:cell_component_1k")
    .inputFluids([Fluid.of("gtceu:tin",144)])
    .duration(200).EUt(GTValues.VA[GTValues.MV])

    event.recipes.gtceu.circuit_assembler("4k_component_recipe")
    .itemInputs("3x ae2:cell_component_1k","16x gtceu:fine_cobalt_wire","4x gtceu:fine_electrum_wire","gtceu:plastic_printed_circuit_board")
    .itemOutputs("ae2:cell_component_4k")
    .inputFluids([Fluid.of("gtceu:silver",144)])
    .duration(200).EUt(GTValues.VA[GTValues.MV])

    event.recipes.gtceu.circuit_assembler("16k_component_cac_2")
    .itemInputs("3x ae2:cell_component_4k","gtceu:soc","64x gtceu:fine_borosilicate_glass_wire","16x gtceu:microchip_processor")
    .itemOutputs("ae2:cell_component_16k")
    .inputFluids([Fluid.of("gtceu:polytetrafluoroethylene",288)])
    .duration(300).EUt(GTValues.VA[GTValues.HV]).cleanroom(CleanroomType.CLEANROOM)

    event.recipes.gtceu.circuit_assembler("64k_component_cac_2")
    .itemInputs("3x ae2:cell_component_16k","gtceu:soc","16x gtceu:osmiridium_dust","gtceu:epoxy_circuit_board","64x gtceu:microchip_processor")
    .itemOutputs("ae2:cell_component_64k")
    .inputFluids([Fluid.of("gtceu:polytetrafluoroethylene",500)])
    .duration(300).EUt(GTValues.VA[GTValues.HV]).cleanroom(CleanroomType.CLEANROOM)

    event.recipes.gtceu.assembler("advcm_rcp_1")
   .itemInputs("32x gtceu:nano_cpu_chip","16x gtceu:ram_chip","8x mekanism:qio_drive_base","128x gtceu:fine_borosilicate_glass_wire","64x gtceu:microchip_processor")
   .inputFluids([Fluid.of("gtceu:reinforced_epoxy_resin",3000)])
   .itemOutputs("ae2:cell_component_256k")
   .EUt(GTValues.VA[GTValues.IV]).duration(200).cleanroom(CleanroomType.CLEANROOM)

   event.recipes.gtceu.assembly_line("advcm_rcp_3")
   .itemInputs("8x mekanism:qio_drive_supermassive","gtceu:multilayer_fiber_reinforced_circuit_board","256x gtceu:fine_borosilicate_glass_wire","64x gtceu:microchip_processor","32x gtceu:qbit_cpu_chip","8x mekanism:pallet_polonium")
   .inputFluids([Fluid.of("gtceu:reinforced_epoxy_resin",16000)])
   .itemOutputs("megacells:cell_component_1m")
   .EUt(GTValues.VA[GTValues.ZPM]).duration(500)

    // Wires
    event.recipes.gtceu.assembler("ae2_quartz_fiber")
    .itemInputs("6x gtceu:fine_borosilicate_glass_wire")
    .itemOutputs("3x ae2:quartz_fiber")
    .duration(250).EUt(60)

    event.recipes.gtceu.assembler("ae2_quartz_glass_wire")
    .itemInputs("2x ae2:quartz_fiber","4x ae2:fluix_dust")
    .itemOutputs("ae2:fluix_glass_cable")
    .duration(250).EUt(120)

    // silicon recipe new ig
    event.recipes.gtceu.electric_blast_furnace("silicon_dust_to_silicon")
    .itemInputs("gtceu:silicon_dust")
    .itemOutputs("8x ae2:silicon")
    .inputFluids([Fluid.of("mekanism:oxygen",2500)])
    .duration(50).EUt(256).blastFurnaceTemp(1800).circuit(4)

    event.recipes.gtceu.electric_blast_furnace("quartz_to_silicon")
    .itemInputs("minecraft:quartz")
    .itemOutputs("ae2:silicon")
    .inputFluids([Fluid.of("mekanism:oxygen",1000)])
    .duration(50).EUt(125).blastFurnaceTemp(1800).circuit(2)

    event.recipes.gtceu.electric_blast_furnace("silicon_boule_new")
    .itemInputs("64x gtceu:silicon_dust","gtceu:calcium_hydroxide_dust")
    .itemOutputs("gtceu:silicon_boule")
    .inputFluids([Fluid.of("mekanism:oxygen",4000)])
    .duration(7000).EUt(125).blastFurnaceTemp(2700).circuit(15)

    event.recipes.gtceu.arc_furnace("silicon_for_the_poor")
    .itemInputs("16x minecraft:quartz")
    .itemOutputs("ae2:silicon")
    .inputFluids([Fluid.of("mekanism:oxygen",1000)])
    .duration(500).EUt(32)

    event.recipes.gtceu.arc_furnace("silicon_for_the_poor_2")
    .itemInputs("4x tinyredstone:silicon_compound")
    .itemOutputs("ae2:silicon")
    .inputFluids([Fluid.of("mekanism:oxygen",500)])
    .duration(400).EUt(32)

    event.recipes.gtceu.arc_furnace("silicon_for_the_poor_3")
    .itemInputs("2x gtceu:certus_quartz_dust")
    .itemOutputs("ae2:silicon")
    .inputFluids([Fluid.of("gtceu:xenon",150)])
    .duration(350).EUt(125)

    // progression changes that are needed to progress
    event.recipes.gtceu.assembler("cleanroom_new")
    .itemInputs("gtceu:mv_machine_hull","2x gtceu:steel_rotor","2x gtceu:mv_electric_motor","4x gtceu:circuits/mv","3x gtceu:item_filter")
    .itemOutputs("gtceu:cleanroom")
    .duration(150).EUt(32)

})

// mekanism changes
ServerEvents.recipes(e => {
    e.recipes.gtceu.assembler("energy_tablet_copper_fix")
    .itemInputs("8x gtceu:fine_gold_wire","16x gtceu:fine_copper_wire","8x gtceu:cobalt_dust","8x gtceu:graphite_dust")
    .itemOutputs("mekanism:energy_tablet")
    .inputFluids([Fluid.of("gtceu:glue",80)])
    .duration(100).EUt(30)

    e.recipes.gtceu.assembler("energy_tablet_copper_fix_1")
    .itemInputs("8x gtceu:fine_gold_wire","16x gtceu:fine_copper_wire","8x gtceu:cobalt_dust","8x gtceu:graphite_dust","gtceu:double_stainless_steel_plate")
    .itemOutputs("4x mekanism:energy_tablet")
    .inputFluids([Fluid.of("gtceu:glue",90)])
    .duration(50).EUt(20)

    e.recipes.gtceu.assembler("energy_tablet_copper_fix_2")
    .itemInputs("8x gtceu:fine_gold_wire","16x gtceu:fine_copper_wire","8x gtceu:cobalt_dust","8x gtceu:graphite_dust","gtceu:double_steel_plate")
    .itemOutputs("2x mekanism:energy_tablet")
    .inputFluids([Fluid.of("gtceu:glue",150)])
    .duration(80).EUt(30)

    e.recipes.gtceu.assembler("qio_drive_basic")
    .itemInputs("gtceu:soc","4x mekanism:ultimate_control_circuit","4x gtceu:titanium_ingot","64x gtceu:ram_chip","4x mekanism:pellet_polonium")
    .itemOutputs("mekanism:qio_drive_base")
    .duration(150).EUt(125)

    e.recipes.gtceu.assembler("circuit_immersive")
    .itemInputs("gtceu:phenolic_circuit_board","4x gtceu:double_copper_plate","8x gtceu:fine_gold_wire")
    .itemOutputs("immersiveengineering:circuit_board")
    .duration(50).EUt(32)

    e.recipes.gtceu.centrifuge("new_glue")
    .itemInputs("4x minecraft:slime_ball")
    .outputFluids([Fluid.of("gtceu:glue",72)])
    .duration(50).EUt(32)

    e.recipes.gtceu.centrifuge("new_glue_1")
    .itemInputs("tconstruct:sky_slime_ball")
    .outputFluids([Fluid.of("gtceu:glue",72)])
    .duration(50).EUt(32)

    e.recipes.gtceu.centrifuge("new_glue_2")
    .itemInputs("tconstruct:ender_slime_ball")
    .outputFluids([Fluid.of("gtceu:glue",72)])
    .duration(50).EUt(32)

    e.recipes.gtceu.centrifuge("new_glue_3")
    .itemInputs("tconstruct:ichor_slime_ball")
    .outputFluids([Fluid.of("gtceu:glue",72)])
    .duration(50).EUt(32)

  e.shaped(
  Item.of('mekanism:energy_tablet', 1),
  [
    'ABA',
    'BCB',
    'ABA'
  ],
  {
    A: 'mekanism:alloy_infused',
    B: 'gtceu:fine_copper_wire',
    C: 'immersiveengineering:circuit_board'
  })

  e.shaped(
  Item.of('mekanism:energy_tablet', 1),
  [
    'ABA',
    'BCB',
    'DFD'
  ],
  {
    A: "powah:energized_steel_block",
    B: "#forge:dusts/lithium",
    C: "gtceu:cobalt_block",
    F: "gtceu:soc",
    D: 'mekanism:alloy_reinforced'
  })

e.custom({
  "type": "powah:energizing",
  "ingredients": [
	{"item": "gtceu:quantum_star"},
	{"item": "allthecompressed:redstone_alloy_block_1x"},
	{"item": "allthecompressed:redstone_alloy_block_1x"},
	{"item": "mekanism:pellet_antimatter"}
  ],
  "energy": 500000,
  "result": {
	"item": "powah:crystal_nitro"
    }
  })

  // energy tablet powah
  e.custom({
  "type": "powah:energizing",
  "ingredients": [
	{"item": "allthecompressed:copper_block_1x"},
	{"item": "mekanism:dust_lithium"},
  {"item": "mekanism:dust_lithium"},
  {"item": "gtceu:cobalt_brass_block"},
	{"item": "powah:energized_steel_block"},
	{"item": "powah:energized_steel_block"}
  ],
  "energy": 225000,
  "result": {
	"item": "mekanism:energy_tablet"
    }
  })

  e.custom({
  "type": "powah:energizing",
  "ingredients": [
	{"item": "allthecompressed:copper_block_1x"},
	{"item": "gtceu:lithium_dust"},
  {"item": "gtceu:lithium_dust"},
  {"item": "gtceu:cobalt_brass_block"},
	{"item": "powah:energized_steel_block"},
	{"item": "powah:energized_steel_block"}
  ],
  "energy": 225000,
  "result": {
	"item": "mekanism:energy_tablet"
    }
  })

  e.custom({
  "type": "powah:energizing",
  "ingredients": [
	{"item": "mekanism:energy_tablet"},
	{"item": "mekanism:energy_tablet"},
  {"item": "mekanism:energy_tablet"},
  {"item": "gtceu:hv_lithium_battery"},
	{"item": "gtceu:hv_lithium_battery"},
	{"item": "gtceu:simple_soc"}
  ],
  "energy": 112000,
  "result": {
	"item": "mekanism:basic_induction_cell"
    }
  })
})

// all removals
ServerEvents.recipes(e => {
    const toRemoveId = [
        "advanced_ae:accumulation_processor_chamber","megacells:inscriber/accumulation_processor","gtceu:forming_press/megacells/accumulation_circuit","advanced_ae:engineering_processor_chamber",
        "ae2:inscriber/engineering_processor","gtceu:forming_press/ae2/engineering_circuit","advanced_ae:calculation_processor_chamber","ae2:inscriber/calculation_processor","gtceu:forming_press/ae2/calculation_circuit",
        "advanced_ae:logic_processor_chamber","ae2:inscriber/logic_processor","gtceu:forming_press/ae2/logic_circuit","advanced_ae:fluixcrystals","advanced_ae:fluixcrystalfromdust","create:mixing/compat/ae2/fluix_crystal",
        "ae2:transform/fluix_crystals","ae2:network/blocks/controller","ae2:network/blocks/storage_drive","ae2:network/crafting/molecular_assembler","expatternprovider:cutter/silicon","expatternprovider:cutter/logic","expatternprovider:cutter/engineering",
        "expatternprovider:cutter/calculation","expatternprovider:cutter/accumulation","refinedstorage:controller","refinedstorage:crafter","refinedstorage:disk_drive","gtceu:electric_blast_furnace/silicon_boule","ae2:network/cells/item_storage_components_cell_1k_part",
        "ae2:network/cells/item_storage_components_cell_4k_part","ae2:network/cells/item_storage_components_cell_16k_part","ae2:network/cells/item_storage_components_cell_64k_part","ae2:network/cells/item_storage_components_cell_256k_part","megacells:cells/cell_component_1m",
        "mekanism:energy_tablet","gtceu:shaped/cleanroom","powah:energizing/nitro_crystal","mysticalagradditions:essence/nitro_crystal","mekanism:induction/cell/basic"
    ];   
    toRemoveId.forEach(element => {
    e.remove({ id: element});
    })
})
ServerEvents.recipes(e => {
    e.remove({ output: "ae2:silicon" })
})


// battery recipes
