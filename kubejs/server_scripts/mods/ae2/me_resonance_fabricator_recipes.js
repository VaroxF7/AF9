// AF9 - ME Resonance Fabricator recipes: TerraFirmaGreg's me_assembler recipes (Core-Modern, LGPL-3.0; credits in
// CREDITS-textures.md) for AE2, ExtendedAE, MEGA Cells, ME Requester and GregTech's ME Pattern Buffer Proxy, taken over
// with their research (scanner / station), circuits and the Moon's air: most of them need an Oxygenated Environment
// (OxygenatedCondition, Ad Astra's oxygen) and the dimension ad_astra:moon, as in TFG.
//
// Differences from TFG: Cryogenized Fluix (and Fluix) become Liquid Certus Quartz (their amounts added together), the
// TFC workbench tag becomes a Crafting Table, the material info is not generated, and the three Spatial Cell Components
// (they need TFG's UV LEDs) are left out. Each section is guarded by its mod ids. Ids: af9:<mod>/<name> (GregTech prefixes the type, af9:me_assembler/<mod>/<name>).
// The recipes overlap with the pack's own MV AE2 chain (ae2_*.js); these are the Fabricator's faster, Moon-bound routes.
// Machine: kubejs/startup_scripts/gtceu/me_resonance_fabricator.js

const $OxygenatedCondition = Java.loadClass('com.af9.core.common.OxygenatedCondition')

ServerEvents.recipes(event => {
    // AE2
    event.recipes.gtceu.me_assembler('af9:tfg/crafting_card')
        .addCondition(new $OxygenatedCondition(false, true))
        .itemInputs('1x ae2:basic_card', '4x ae2:cell_component_16k', '4x minecraft:crafting_table')
        .itemOutputs('ae2:crafting_card')
        .duration(20*800)
        .EUt(GTValues.VA[GTValues.IV])
        .dimension('ad_astra:moon')
        .circuit(9)

    event.recipes.gtceu.me_assembler('af9:tfg/ae2/energy_cell')
        .addCondition(new $OxygenatedCondition(false, true))
        .itemInputs('gtceu:titanium_frame', '6x gtceu:titanium_plate', '6x gtceu:titanium_screw', 'gtceu:energy_crystal')
        .itemOutputs('ae2:energy_cell')
        .duration(20*25)
        .EUt(GTValues.VA[GTValues.MV])
        .dimension('ad_astra:moon')
        .circuit(8)

    event.recipes.gtceu.me_assembler('af9:tfg/ae2/dense_energy_cell')
        .addCondition(new $OxygenatedCondition(false, true))
        .itemInputs('8x ae2:energy_cell', '8x gtceu:silver_quadruple_wire', '4x #gtceu:circuits/ev')
        .itemOutputs('ae2:dense_energy_cell')
        .duration(20*25)
        .EUt(GTValues.VA[GTValues.MV])
        .dimension('ad_astra:moon')
        .circuit(9)

    event.recipes.gtceu.me_assembler('af9:ae2/blank_pattern_pvc')
        .addCondition(new $OxygenatedCondition(false, true))
        .itemInputs(
            '3x #forge:plates/steel',
            '2x #forge:plates/polyvinyl_chloride',
            '4x #forge:fine_wires/red_alloy',
            '#gtceu:circuits/mv')
        .itemOutputs('4x ae2:blank_pattern')
        .duration(20*10)
        .EUt(GTValues.VHA[GTValues.MV])
        .dimension('ad_astra:moon')
        .circuit(1)

    event.recipes.gtceu.me_assembler('af9:ae2/blank_pattern_ptfe')
        .addCondition(new $OxygenatedCondition(false, true))
        .itemInputs(
            '3x #forge:plates/steel',
            '2x #forge:plates/polytetrafluoroethylene',
            '4x #forge:fine_wires/red_alloy',
            '#gtceu:circuits/hv')
        .itemOutputs('16x ae2:blank_pattern')
        .duration(20*20)
        .EUt(GTValues.VHA[GTValues.HV])
        .dimension('ad_astra:moon')
        .circuit(2)

    event.recipes.gtceu.me_assembler('af9:ae2/blank_pattern_pbi')
        .addCondition(new $OxygenatedCondition(false, true))
        .itemInputs(
            '3x #forge:plates/steel',
            '2x #forge:plates/polybenzimidazole',
            '4x #forge:fine_wires/red_alloy',
            '#gtceu:circuits/ev')
        .itemOutputs('64x ae2:blank_pattern')
        .duration(20*40)
        .EUt(GTValues.VHA[GTValues.EV])
        .dimension('ad_astra:moon')
        .circuit(3)

    event.recipes.gtceu.me_assembler('af9:ae2/annihilation_core_moon')
        .addCondition(new $OxygenatedCondition(false, true))
        .itemInputs(
            '1x ae2:certus_quartz_crystal',
            '1x ae2:engineering_processor',
            '#forge:gems/quartzite')
        .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144))
        .itemOutputs('1x ae2:annihilation_core')
        .duration(20*10)
        .EUt(GTValues.VA[GTValues.MV])
        .dimension('ad_astra:moon')
        .circuit(1)

    event.recipes.gtceu.me_assembler('af9:ae2/annihilation_core_moon_bulk')
        .addCondition(new $OxygenatedCondition(false, true))
        .itemInputs(
            '1x ae2:certus_quartz_crystal',
            '1x ae2:engineering_processor',
            '#forge:gems/quartzite')
        .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144))
        .itemOutputs('8x ae2:annihilation_core')
        .duration(20*600)
        .EUt(GTValues.VA[GTValues.IV])
        .dimension('ad_astra:moon')
        .circuit(2)

    event.recipes.gtceu.me_assembler('af9:ae2/formation_core_moon')
        .addCondition(new $OxygenatedCondition(false, true))
        .itemInputs(
            '1x minecraft:quartz',
            '1x ae2:engineering_processor',
            '#forge:gems/quartzite')
        .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144))
        .itemOutputs('1x ae2:formation_core')
        .duration(20*10)
        .EUt(GTValues.VA[GTValues.MV])
        .dimension('ad_astra:moon')
        .circuit(3)

    event.recipes.gtceu.me_assembler('af9:ae2/formation_core_moon_bulk')
        .addCondition(new $OxygenatedCondition(false, true))
        .itemInputs(
            '1x minecraft:quartz',
            '1x ae2:engineering_processor',
            '#forge:gems/quartzite')
        .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144))
        .itemOutputs('8x ae2:formation_core')
        .duration(20*600)
        .EUt(GTValues.VA[GTValues.IV])
        .dimension('ad_astra:moon')
        .circuit(4)

    // ---- Cell components (all ZPM): the pack's own chip ladder, not TFG's foil/logic one ----
    // 1k is RAM + cpu chip + gold wire + redstone foil; 4k adds lpic chip + interconnects;
    // 16k and up chain (3x previous + calculation processor + glowstone/sky + quartz glass +
    // the RAM pile); the 256k is the wall: 4x 64k, ten IV circuits, fine electrum wire and
    // redstone foil on liquid certus quartz. Liquid certus doubles 144 -> 2304 mB.
    event.recipes.gtceu.me_assembler('af9:ae2/cell_component_1k')
        .addCondition(new $OxygenatedCondition(false, true))
        .itemInputs(
            '8x gtceu:ram_chip',
            '1x gtceu:cpu_chip',
            '16x gtceu:fine_gold_wire',
            '16x gtceu:redstone_foil')
        .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144))
        .itemOutputs('ae2:cell_component_1k')
        .duration(20*10)
        .EUt(GTValues.VA[GTValues.ZPM])
        .dimension('ad_astra:moon')
        .circuit(1)

    event.recipes.gtceu.me_assembler('af9:ae2/cell_component_4k')
        .addCondition(new $OxygenatedCondition(false, true))
        .itemInputs(
            '3x ae2:cell_component_1k',
            '1x gtceu:ram_chip',
            '1x gtceu:lpic_chip',
            '16x af9:interconnect')
        .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 288))
        .itemOutputs('ae2:cell_component_4k')
        .duration(20*60)
        .EUt(GTValues.VA[GTValues.ZPM])
        .dimension('ad_astra:moon')
        .circuit(2)

    event.recipes.gtceu.me_assembler('af9:ae2/cell_component_16k')
        .addCondition(new $OxygenatedCondition(false, true))
        .itemInputs(
            '3x ae2:cell_component_4k',
            '1x ae2:calculation_processor',
            '4x minecraft:glowstone_dust',
            '1x ae2:quartz_glass',
            '1x gtceu:micro_processor',
            '32x gtceu:ram_chip')
        .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 576))
        .itemOutputs('ae2:cell_component_16k')
        .duration(20*240)
        .EUt(GTValues.VA[GTValues.ZPM])
        .dimension('ad_astra:moon')
        .circuit(3)

    event.recipes.gtceu.me_assembler('af9:ae2/cell_component_64k')
        .addCondition(new $OxygenatedCondition(false, true))
        .itemInputs(
            '3x ae2:cell_component_16k',
            '1x ae2:calculation_processor',
            '4x minecraft:glowstone_dust',
            '1x ae2:quartz_glass',
            '5x gtceu:micro_processor',
            '64x gtceu:ram_chip',
            '16x gtceu:nand_memory_chip')
        .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 1152))
        .itemOutputs('ae2:cell_component_64k')
        .duration(20*260)
        .EUt(GTValues.VA[GTValues.ZPM])
        .dimension('ad_astra:moon')
        .circuit(4)

    event.recipes.gtceu.me_assembler('af9:ae2/cell_component_256k')
        .addCondition(new $OxygenatedCondition(false, true))
        .itemInputs(
            '4x ae2:cell_component_64k',
            '10x #gtceu:circuits/iv',
            '4x gtceu:cpu_chip',
            '32x gtceu:nand_memory_chip',
            '128x gtceu:fine_electrum_wire',
            '64x gtceu:redstone_foil')
        .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 2304))
        .itemOutputs('ae2:cell_component_256k')
        .duration(20*900)
        .EUt(GTValues.VA[GTValues.ZPM])
        .dimension('ad_astra:moon')
        .circuit(5)

    event.recipes.gtceu.me_assembler('af9:ae2/storage_bus')
        .addCondition(new $OxygenatedCondition(false, true))
        .itemInputs(
            '#ae2:interface',
            '1x gtceu:mv_electric_piston',
            '#ae2:smart_cable')
        .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144))
        .itemOutputs('1x ae2:storage_bus')
        .duration(20*25)
        .EUt(GTValues.VA[GTValues.MV])
        .dimension('ad_astra:moon')
        .circuit(1)

    event.recipes.gtceu.me_assembler('af9:ae2/storage_bus_moon_bulk')
        .addCondition(new $OxygenatedCondition(false, true))
        .itemInputs(
            '#ae2:interface',
            '1x gtceu:iv_electric_piston',
            '#ae2:smart_cable')
        .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144))
        .itemOutputs('8x ae2:storage_bus')
        .duration(20*25)
        .EUt(GTValues.VA[GTValues.MV])
        .dimension('ad_astra:moon')
        .circuit(2)

    event.recipes.gtceu.me_assembler('af9:ae2/import_bus')
        .addCondition(new $OxygenatedCondition(false, true))
        .itemInputs(
            '#forge:plates/stainless_steel',
            'ae2:annihilation_core',
            '2x gtceu:mv_robot_arm',
            '#ae2:smart_cable')
        .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144))
        .itemOutputs('ae2:import_bus')
        .duration(20*25)
        .EUt(GTValues.VA[GTValues.MV])
        .dimension('ad_astra:moon')
        .circuit(6)

    event.recipes.gtceu.me_assembler('af9:ae2/export_bus')
        .addCondition(new $OxygenatedCondition(false, true))
        .itemInputs(
            '#forge:plates/stainless_steel',
            'ae2:formation_core',
            '2x gtceu:mv_robot_arm',
            '#ae2:smart_cable')
        .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144))
        .itemOutputs('ae2:export_bus')
        .duration(20*25)
        .EUt(GTValues.VA[GTValues.MV])
        .dimension('ad_astra:moon')
        .circuit(6)

    event.recipes.gtceu.me_assembler('af9:ae2/crafting_unit')
        .addCondition(new $OxygenatedCondition(false, true))
        .itemInputs(
            'ae2:logic_processor',
            'ae2:engineering_processor',
            'ae2:calculation_processor',
            '#gtceu:circuits/ev',
            '6x #forge:plates/stainless_steel')
        .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144 * 5))
        .itemOutputs('ae2:crafting_unit')
        .duration(20*25)
        .EUt(GTValues.VA[GTValues.MV])
        .dimension('ad_astra:moon')
        .circuit(4)

    event.recipes.gtceu.me_assembler('af9:ae2/crafting_unit_bulk')
        .addCondition(new $OxygenatedCondition(false, true))
        .itemInputs(
            'ae2:logic_processor',
            'ae2:engineering_processor',
            'ae2:calculation_processor',
            '#gtceu:circuits/ev',
            '6x #forge:plates/tungsten_steel')
        .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144 * 5))
        .itemOutputs('4x ae2:crafting_unit')
        .duration(20*25)
        .EUt(GTValues.VA[GTValues.MV])
        .dimension('ad_astra:moon')
        .circuit(2)

    event.recipes.gtceu.me_assembler('af9:ae2/molecular_assembler_hv')
        .addCondition(new $OxygenatedCondition(false, true))
        .itemInputs(
            '1x #gtceu:circuits/hv',
            'ae2:quartz_glass',
            '1x ae2:annihilation_core',
            '1x ae2:formation_core',
            '1x gtceu:hv_robot_arm',
            '4x #forge:plates/stainless_steel')
        .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144))
        .itemOutputs('1x ae2:molecular_assembler')
        .duration(20*25)
        .EUt(GTValues.VA[GTValues.MV])
        .dimension('ad_astra:moon')
        .circuit(1)

    event.recipes.gtceu.me_assembler('af9:ae2/molecular_assembler_iv')
        .addCondition(new $OxygenatedCondition(false, true))
        .itemInputs(
            '1x #gtceu:circuits/ev',
            'ae2:quartz_glass',
            '2x ae2:annihilation_core',
            '2x ae2:formation_core',
            '1x gtceu:ev_robot_arm',
            '4x #forge:plates/tungsten_steel')
        .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144 * 8))
        .itemOutputs('8x ae2:molecular_assembler')
        .duration(20*25)
        .EUt(GTValues.VA[GTValues.MV])
        .dimension('ad_astra:moon')
        .circuit(2)

    event.recipes.gtceu.me_assembler('af9:tfg/controller')
        .addCondition(new $OxygenatedCondition(false, true))
        .itemInputs(
            '8x ae2:engineering_processor',
            '2x #gtceu:circuits/hv',
            '2x #forge:plates/titanium',
            'ae2:fluix_block')
        .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144))
        .itemOutputs('ae2:controller')
        .duration(20*25)
        .EUt(GTValues.VA[GTValues.MV])
        .dimension('ad_astra:moon')
        .circuit(7)

    event.recipes.gtceu.me_assembler('af9:tfg/controller_shulker')
        .itemInputs(
            '8x ae2:engineering_processor',
            '2x #gtceu:circuits/hv',
            '2x minecraft:shulker_shell',
            'ae2:fluix_block')
        .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144))
        .itemOutputs('ae2:controller')
        .duration(20*25)
        .EUt(GTValues.VA[GTValues.MV])
        .dimension('ad_astra:moon')
        .circuit(7)

    event.recipes.gtceu.me_assembler('af9:ae2/interface_mv_moon')
        .addCondition(new $OxygenatedCondition(false, true))
        .itemInputs(
            'gtceu:mv_conveyor_module',
            '4x #forge:rods/long/aluminium',
            '4x #forge:plates/aluminium',
            '1x ae2:annihilation_core',
            '1x ae2:formation_core')
        .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144))
        .itemOutputs('1x ae2:interface')
        .duration(20*25)
        .EUt(GTValues.VA[GTValues.MV])
        .dimension('ad_astra:moon')
        .circuit(1)

    event.recipes.gtceu.me_assembler('af9:ae2/interface_iv_moon')
        .addCondition(new $OxygenatedCondition(false, true))
        .itemInputs(
            'gtceu:iv_conveyor_module',
            '4x #forge:rods/long/tungsten_steel',
            '4x #forge:plates/tungsten_steel',
            '1x ae2:annihilation_core',
            '1x ae2:formation_core')
        .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144 * 8), Fluid.of('gtceu:argon', 144))
        .itemOutputs('8x ae2:interface')
        .duration(20*25)
        .EUt(GTValues.VA[GTValues.MV])
        .dimension('ad_astra:moon')
        .circuit(2)

    event.recipes.gtceu.me_assembler('af9:ae2/pattern_provider_mv_moon')
        .addCondition(new $OxygenatedCondition(false, true))
        .itemInputs(
            'gtceu:mv_robot_arm',
            '4x #forge:rods/long/aluminium',
            '4x #forge:plates/aluminium',
            '1x ae2:annihilation_core',
            '1x ae2:formation_core')
        .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144))
        .itemOutputs('1x ae2:pattern_provider')
        .duration(20*25)
        .EUt(GTValues.VA[GTValues.MV])
        .dimension('ad_astra:moon')
        .circuit(3)

    event.recipes.gtceu.me_assembler('af9:ae2/pattern_provider_iv_moon')
        .addCondition(new $OxygenatedCondition(false, true))
        .itemInputs(
            'gtceu:iv_robot_arm',
            '4x #forge:rods/long/tungsten_steel',
            '4x #forge:plates/tungsten_steel',
            '1x ae2:annihilation_core',
            '1x ae2:formation_core')
        .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144 * 8), Fluid.of('gtceu:argon', 144))
        .itemOutputs('8x ae2:pattern_provider')
        .duration(20*25)
        .EUt(GTValues.VA[GTValues.MV])
        .dimension('ad_astra:moon')
        .circuit(3)

    event.recipes.gtceu.me_assembler('af9:ae2/logic_processor')
        .addCondition(new $OxygenatedCondition(false, true))
        .itemInputs(
            'ae2:printed_silicon',
            'ae2:printed_logic_processor',
            '#gtceu:circuits/lv',
            '2x #gtceu:resistors',
            '1x minecraft:redstone')
        .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144))
        .itemOutputs('2x ae2:logic_processor')
        .duration(20 * 5)
        .EUt(GTValues.VA[GTValues.HV])
        .dimension('ad_astra:moon')
        .circuit(1)

    event.recipes.gtceu.me_assembler('af9:ae2/logic_processor_bulk')
        .addCondition(new $OxygenatedCondition(false, true))
        .itemInputs(
            '2x ae2:printed_silicon',
            '2x ae2:printed_logic_processor',
            '#gtceu:circuits/hv',
            'gtceu:advanced_smd_resistor',
            '1x minecraft:redstone')
        .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144))
        .itemOutputs('16x ae2:logic_processor')
        .duration(20 * 800)
        .EUt(GTValues.VA[GTValues.IV])
        .dimension('ad_astra:moon')
        .circuit(2)

    event.recipes.gtceu.me_assembler('af9:ae2/calculation_processor')
        .addCondition(new $OxygenatedCondition(false, true))
        .itemInputs(
            'ae2:printed_silicon',
            'ae2:printed_calculation_processor',
            '#gtceu:circuits/lv',
            '2x #gtceu:resistors',
            '1x minecraft:redstone')
        .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144))
        .itemOutputs('2x ae2:calculation_processor')
        .duration(20 * 10)
        .EUt(GTValues.VA[GTValues.HV])
        .dimension('ad_astra:moon')
        .circuit(3)

    event.recipes.gtceu.me_assembler('af9:ae2/calculation_processor_bulk')
        .addCondition(new $OxygenatedCondition(false, true))
        .itemInputs(
            '2x ae2:printed_silicon',
            '2x ae2:printed_calculation_processor',
            '#gtceu:circuits/hv',
            'gtceu:advanced_smd_resistor',
            '1x minecraft:redstone')
        .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144))
        .itemOutputs('16x ae2:calculation_processor')
        .duration(20 * 800)
        .EUt(GTValues.VA[GTValues.IV])
        .dimension('ad_astra:moon')
        .circuit(4)

    event.recipes.gtceu.me_assembler('af9:ae2/engineering_processor')
        .addCondition(new $OxygenatedCondition(false, true))
        .itemInputs(
            'ae2:printed_silicon',
            'ae2:printed_engineering_processor',
            '#gtceu:circuits/lv',
            '2x #gtceu:resistors',
            '1x minecraft:redstone')
        .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144))
        .itemOutputs('2x ae2:engineering_processor')
        .duration(20 * 10)
        .EUt(GTValues.VA[GTValues.HV])
        .dimension('ad_astra:moon')
        .circuit(5)

    event.recipes.gtceu.me_assembler('af9:ae2/engineering_processor_bulk')
        .addCondition(new $OxygenatedCondition(false, true))
        .itemInputs(
            '2x ae2:printed_silicon',
            '2x ae2:printed_engineering_processor',
            '#gtceu:circuits/hv',
            'gtceu:advanced_smd_resistor',
            '1x minecraft:redstone')
        .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144))
        .itemOutputs('16x ae2:engineering_processor')
        .duration(20 * 800)
        .EUt(GTValues.VA[GTValues.IV])
        .dimension('ad_astra:moon')
        .circuit(6)
    // expatternprovider
    if (Platform.isLoaded('expatternprovider')) {
        event.recipes.gtceu.me_assembler('af9:expatternprovider/tag_export_bus')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                'expatternprovider:ex_export_bus_part',
                '2x ae2:logic_processor',
                '#gtceu:circuits/ev')
            .itemOutputs('expatternprovider:tag_export_bus')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(1)

        event.recipes.gtceu.me_assembler('af9:expatternprovider/tag_storage_bus')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                'ae2:storage_bus',
                '2x ae2:logic_processor',
                '#gtceu:circuits/ulv')
            .itemOutputs('expatternprovider:tag_storage_bus')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(2)

        event.recipes.gtceu.me_assembler('af9:expatternprovider/mod_export_bus')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                'ae2:export_bus',
                '2x ae2:calculation_processor',
                '#gtceu:circuits/ev')
            .itemOutputs('expatternprovider:mod_export_bus')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(3)

        event.recipes.gtceu.me_assembler('af9:expatternprovider/mod_storage_bus')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                'ae2:storage_bus',
                '2x ae2:calculation_processor',
                '#gtceu:circuits/ulv')
            .itemOutputs('expatternprovider:mod_storage_bus')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(3)

        event.recipes.gtceu.me_assembler('af9:expatternprovider/precise_export_bus')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                'ae2:export_bus',
                '2x ae2:engineering_processor',
                '#gtceu:circuits/ev')
            .itemOutputs('expatternprovider:precise_export_bus')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(4)

        event.recipes.gtceu.me_assembler('af9:expatternprovider/precise_storage_bus')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                'ae2:storage_bus',
                '2x ae2:engineering_processor',
                '#gtceu:circuits/ulv')
            .itemOutputs('expatternprovider:precise_storage_bus')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(4)

        event.recipes.gtceu.me_assembler('af9:expatternprovider/threshold_level_emitter')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                'ae2:level_emitter',
                '2x minecraft:redstone_torch',
                '2x ae2:calculation_processor')
            .itemOutputs('expatternprovider:threshold_level_emitter')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(9)

        event.recipes.gtceu.me_assembler('af9:expatternprovider/ex_molecular_assembler_iv')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                '2x #gtceu:circuits/iv',
                '8x gtceu:cleanroom_glass',
                '4x ae2:annihilation_core',
                '4x ae2:formation_core',
                '2x gtceu:iv_robot_arm',
                'ae2:molecular_assembler')
            .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144 * 4))
            .itemOutputs('expatternprovider:ex_molecular_assembler')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(5)

        event.recipes.gtceu.me_assembler('af9:expatternprovider/ex_molecular_assembler_zpm')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                '2x #gtceu:circuits/zpm',
                '8x gtceu:cleanroom_glass',
                '4x ae2:annihilation_core',
                '4x ae2:formation_core',
                '2x gtceu:zpm_robot_arm',
                '8x ae2:molecular_assembler')
            .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144 * 16))
            .itemOutputs('8x expatternprovider:ex_molecular_assembler')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(6)

        event.recipes.gtceu.me_assembler('af9:expatternprovider/ingredient_buffer')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs('gtceu:hv_buffer', 'ae2:cell_component_1k')
            .itemOutputs('expatternprovider:ingredient_buffer')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(8)

        event.recipes.gtceu.me_assembler('af9:expatternprovider/assembler_matrix_wall_luv')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                '#forge:frames/polytetrafluoroethylene',
                'gtceu:plascrete',
                '6x gtceu:polytetrafluoroethylene_plate')
            .inputFluids(
                Fluid.of('gtceu:concrete', 144 * 4))
            .itemOutputs('expatternprovider:assembler_matrix_wall')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(1)

        event.recipes.gtceu.me_assembler('af9:expatternprovider/assembler_matrix_glass')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                '#forge:frames/polytetrafluoroethylene',
                'gtceu:cleanroom_glass',
                '6x gtceu:polytetrafluoroethylene_plate')
            .inputFluids(
                Fluid.of('gtceu:concrete', 144 * 4))
            .itemOutputs("expatternprovider:assembler_matrix_glass")
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(2)
    }
    // expatternprovider, megacells
    if (Platform.isLoaded('expatternprovider') && Platform.isLoaded('megacells')) {
        event.recipes.gtceu.me_assembler('af9:tfg/ex_interface_iv_moon')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                'gtceu:iv_conveyor_module',
                '4x gtceu:laminated_glass',
                '2x #forge:plates/tungsten_steel',
                '2x ae2:annihilation_core',
                '2x ae2:formation_core',
                '1x megacells:accumulation_processor',
                '1x #ae2:interface')
            .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144 * 8))
            .itemOutputs('1x expatternprovider:ex_interface')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(5)

        event.recipes.gtceu.me_assembler('af9:tfg/ex_interface_zpm_cr')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                'gtceu:zpm_conveyor_module',
                '4x gtceu:laminated_glass',
                '2x #forge:plates/naquadah_alloy',
                '2x ae2:annihilation_core',
                '2x ae2:formation_core',
                '1x megacells:accumulation_processor',
                '8x #ae2:interface')
            .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144 * 8))
            .itemOutputs('8x expatternprovider:ex_interface')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .circuit(6)
            .dimension('ad_astra:moon')

        event.recipes.gtceu.me_assembler('af9:expatternprovider/interface_upgrade_iv_moon')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                'gtceu:iv_conveyor_module',
                '4x gtceu:laminated_glass',
                '2x #forge:plates/tungsten_steel',
                '2x ae2:annihilation_core',
                '2x ae2:formation_core',
                '1x megacells:accumulation_processor')
            .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144 * 8))
            .itemOutputs('1x expatternprovider:interface_upgrade')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(6)

        event.recipes.gtceu.me_assembler('af9:expatternprovider/interface_upgrade_zpm')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                'gtceu:zpm_conveyor_module',
                '4x gtceu:laminated_glass',
                '2x #forge:plates/naquadah_alloy',
                '2x ae2:annihilation_core',
                '2x ae2:formation_core',
                '1x megacells:accumulation_processor')
            .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144 * 8))
            .itemOutputs('8x expatternprovider:interface_upgrade')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(5)

        event.recipes.gtceu.me_assembler('af9:expatternprovider/oversize_interface_iv_moon')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                '1x #expatternprovider:extended_interface',
                '4x megacells:accumulation_processor',
                'gtceu:luv_robot_arm',
                'gtceu:luv_fluid_regulator',
                '4x #forge:dense_plates/rhodium_plated_palladium',
                '#gtceu:circuits/luv')
            .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144 * 9))
            .itemOutputs("1x expatternprovider:oversize_interface")
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(7)

        event.recipes.gtceu.me_assembler('af9:expatternprovider/oversize_interface_zpm')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                '8x #expatternprovider:extended_interface',
                '4x megacells:accumulation_processor',
                'gtceu:zpm_robot_arm',
                'gtceu:zpm_fluid_regulator',
                '4x #forge:dense_plates/rhodium_plated_palladium',
                '#gtceu:circuits/zpm')
            .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144 * 9))
            .itemOutputs("8x expatternprovider:oversize_interface")
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(8)

        event.recipes.gtceu.me_assembler('af9:tfg/ex_pattern_provider_iv')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                'gtceu:iv_robot_arm',
                '4x gtceu:laminated_glass',
                '2x #forge:plates/tungsten_steel',
                '2x ae2:annihilation_core',
                '2x ae2:formation_core',
                '1x megacells:accumulation_processor',
                '1x #ae2:pattern_provider')
            .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144 * 8))
            .itemOutputs('expatternprovider:ex_pattern_provider')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .circuit(5)
            .dimension('ad_astra:moon')

        event.recipes.gtceu.me_assembler('af9:tfg/ex_pattern_provider_zpm')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                'gtceu:zpm_robot_arm',
                '4x gtceu:laminated_glass',
                '2x #forge:plates/naquadah_alloy',
                '2x ae2:annihilation_core',
                '2x ae2:formation_core',
                '1x megacells:accumulation_processor',
                '8x #ae2:pattern_provider')
            .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144 * 32))
            .itemOutputs('8x expatternprovider:ex_pattern_provider')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(6)

        event.recipes.gtceu.me_assembler('af9:expatternprovider/pattern_provider_upgrade_iv_moon')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                'gtceu:iv_robot_arm',
                '4x gtceu:laminated_glass',
                '2x #forge:plates/tungsten_steel',
                '2x ae2:annihilation_core',
                '2x ae2:formation_core',
                '1x megacells:accumulation_processor')
            .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144 * 8))
            .itemOutputs('1x expatternprovider:pattern_provider_upgrade')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .circuit(6)
            .dimension('ad_astra:moon')

        event.recipes.gtceu.me_assembler('af9:expatternprovider/pattern_provider_upgrade_zpm')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                'gtceu:zpm_robot_arm',
                '4x gtceu:laminated_glass',
                '2x #forge:plates/naquadah_alloy',
                '2x ae2:annihilation_core',
                '2x ae2:formation_core',
                '1x megacells:accumulation_processor')
            .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144 * 8))
            .itemOutputs('8x expatternprovider:pattern_provider_upgrade')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(5)

        event.recipes.gtceu.me_assembler('af9:expatternprovider/ex_import_bus_part')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                '1x #forge:plates/titanium',
                '2x ae2:annihilation_core',
                '2x gtceu:ev_robot_arm',
                '1x megacells:accumulation_processor')
            .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144))
            .itemOutputs('expatternprovider:ex_import_bus_part')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(1)

        event.recipes.gtceu.me_assembler('af9:expatternprovider/ex_export_bus_part')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                '1x #forge:plates/titanium',
                '2x ae2:formation_core',
                '2x gtceu:ev_robot_arm',
                '1x megacells:accumulation_processor')
            .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144))
            .itemOutputs('expatternprovider:ex_export_bus_part')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(2)

        event.recipes.gtceu.me_assembler('af9:expatternprovider/threshold_export_bus')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                'ae2:export_bus',
                '2x megacells:accumulation_processor',
                'ae2:level_emitter')
            .itemOutputs('expatternprovider:threshold_export_bus')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(4)

        event.recipes.gtceu.me_assembler('af9:expatternprovider/active_formation_plane')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                'ae2:formation_plane',
                'ae2:export_bus',
                '2x megacells:accumulation_processor')
            .itemOutputs('expatternprovider:active_formation_plane')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(8)

        event.recipes.gtceu.me_assembler('af9:expatternprovider/wireless_connect')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                '2x gtceu:iv_machine_casing',
                '4x #gtceu:circuits/iv',
                '8x ae2:wireless_receiver',
                '4x gtceu:iv_emitter',
                '4x gtceu:iv_sensor',
                '8x ae2:engineering_processor',
                '4x megacells:accumulation_processor')
            .itemOutputs('2x expatternprovider:wireless_connect')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(9)

        event.recipes.gtceu.me_assembler('af9:expatternprovider/ex_io_port')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                'gtceu:ev_machine_casing',
                'ae2:io_port',
                '2x megacells:accumulation_processor',
                '4x ae2:logic_processor',
                '4x ae2:engineering_processor',
                '4x ae2:speed_card')
            .itemOutputs('expatternprovider:ex_io_port')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(8)

        event.recipes.gtceu.me_assembler('af9:expatternprovider/ex_drive')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                'gtceu:ev_machine_casing',
                'ae2:drive',
                '2x megacells:accumulation_processor',
                '4x ae2:logic_processor',
                '4x ae2:engineering_processor',
                '2x ae2:capacity_card')
            .itemOutputs('expatternprovider:ex_drive')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(9)

        event.recipes.gtceu.me_assembler('af9:expatternprovider/drive_upgrade')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                'gtceu:ev_machine_casing',
                '2x megacells:accumulation_processor',
                '4x ae2:logic_processor',
                '4x ae2:engineering_processor',
                '2x ae2:capacity_card')
            .itemOutputs('expatternprovider:drive_upgrade')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(8)

        event.recipes.gtceu.me_assembler('af9:expatternprovider/assembler_matrix_frame_moon')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                '8x gtceu:plascrete',
                '8x gtceu:iv_machine_casing',
                '1x #gtceu:circuits/iv',
                '4x ae2:logic_processor',
                '4x megacells:accumulation_processor',
                '16x #forge:rods/niobium_nitride')
            .inputFluids(
                Fluid.of('gtceu:concrete', 144 * 8),
                Fluid.of('gtceu:liquid_certus_quartz', 144 * 16))
            .itemOutputs('8x expatternprovider:assembler_matrix_frame')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(9)

        event.recipes.gtceu.me_assembler('af9:expatternprovider/assembler_matrix_pattern')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                '#expatternprovider:extended_pattern_provider',
                '8x megacells:accumulation_processor',
                '4x gtceu:luv_robot_arm',
                '2x #forge:dense_plates/rhodium_plated_palladium',
                '2x #gtceu:circuits/luv')
            .inputFluids(
                Fluid.of('gtceu:liquid_certus_quartz', 144 * 16),
                Fluid.of('gtceu:polybenzimidazole', 1430))
            .itemOutputs('expatternprovider:assembler_matrix_pattern')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(7)

        event.recipes.gtceu.me_assembler('af9:expatternprovider/assembler_matrix_crafter')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                "#expatternprovider:oversize_interface",
                '8x megacells:accumulation_processor',
                '4x gtceu:luv_field_generator',
                '2x #forge:dense_plates/rhodium_plated_palladium',
                '2x #gtceu:circuits/luv')
            .inputFluids(
                Fluid.of('gtceu:liquid_certus_quartz', 144 * 16),
                Fluid.of('gtceu:polybenzimidazole', 1430))
            .itemOutputs('expatternprovider:assembler_matrix_crafter')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(7)

        event.recipes.gtceu.me_assembler('af9:expatternprovider/assembler_matrix_speed_luv')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                "megacells:mega_crafting_accelerator",
                '8x megacells:accumulation_processor',
                '4x gtceu:luv_conveyor_module',
                '2x #forge:dense_plates/rhodium_plated_palladium',
                '2x #gtceu:circuits/luv')
            .inputFluids(
                Fluid.of('gtceu:liquid_certus_quartz', 144 * 16),
                Fluid.of('gtceu:polybenzimidazole', 1430))
            .itemOutputs('expatternprovider:assembler_matrix_speed')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(7)
    }
    // expatternprovider, megacells, merequester
    if (Platform.isLoaded('expatternprovider') && Platform.isLoaded('megacells') && Platform.isLoaded('merequester')) {
        event.recipes.gtceu.me_assembler('af9:tfg/merequester/merequester')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                'expatternprovider:ex_pattern_provider',
                '#expatternprovider:extended_interface',
                'megacells:mega_crafting_unit',
                '4x #gtceu:circuits/luv',
                '4x #forge:double_plates/titanium_tungsten_carbide',
                '4x gtceu:exquisite_amethyst_gem')
            .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144 * 20))
            .itemOutputs('merequester:requester')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(9)
    }
    // megacells
    if (Platform.isLoaded('megacells')) {
        event.recipes.gtceu.me_assembler('af9:megacells/mega_energy_cell')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs('8x ae2:dense_energy_cell', '8x gtceu:nichrome_quadruple_wire', '4x #gtceu:circuits/iv')
            .itemOutputs('megacells:mega_energy_cell')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(9)

        event.recipes.gtceu.me_assembler('af9:tfg/mega_pattern_provider')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                'gtceu:ev_robot_arm',
                '2x #forge:rods/long/titanium',
                '4x #forge:plates/titanium',
                '2x ae2:annihilation_core',
                '2x ae2:formation_core')
            .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144*10))
            .itemOutputs(Item.of('megacells:mega_pattern_provider', 1))
            ['scannerResearch(java.util.function.UnaryOperator)'](b => b
            .researchStack(Item.of('ae2:pattern_provider'))
            .duration(20*30)
            .EUt(GTValues.VA[GTValues.HV]))
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(3)

        event.recipes.gtceu.me_assembler('af9:ae2/accumulation_processor')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                'ae2:printed_silicon',
                'megacells:printed_accumulation_processor',
                '#gtceu:circuits/iv',
                '2x #gtceu:resistors',
                '1x minecraft:redstone')
            .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144*5))
            .itemOutputs('megacells:accumulation_processor')
            .duration(20*100)
            .EUt(GTValues.VA[GTValues.EV])
            .dimension('ad_astra:moon')
            .circuit(1)

        event.recipes.gtceu.me_assembler('af9:ae2/accumulation_processor_cr')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                'ae2:printed_silicon',
                'megacells:printed_accumulation_processor',
                '#gtceu:circuits/iv',
                'gtceu:advanced_smd_resistor',
                '1x minecraft:redstone')
            .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144*5))
            .itemOutputs('4x megacells:accumulation_processor')
            .duration(20*600)
            .EUt(GTValues.VA[GTValues.EV])
            .dimension('ad_astra:moon')
            .circuit(2)

        event.recipes.gtceu.me_assembler('af9:megacells/mega_item_cell_housing')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                '1x #gtceu:circuits/iv',
                '2x #forge:fine_wires/niobium_titanium',
                '2x #forge:plates/maraging_steel_300',
                'ae2:quartz_vibrant_glass')
            .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144*6))
            .itemOutputs('megacells:mega_item_cell_housing')
            ['scannerResearch(java.util.function.UnaryOperator)'](b => b
            .researchStack(Item.of('ae2:item_cell_housing'))
            .duration(20*600)
            .EUt(GTValues.VA[GTValues.EV]))
            .duration(20*60)
            .EUt(GTValues.VA[GTValues.IV])
            .dimension('ad_astra:moon')
            .circuit(6)

        event.recipes.gtceu.me_assembler('af9:megacells/mega_fluid_cell_housing')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                '1x #gtceu:circuits/iv',
                '2x #forge:fine_wires/niobium_titanium',
                '2x #forge:plates/incoloy_ma_956',
                'ae2:quartz_vibrant_glass')
            .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144*6))
            .itemOutputs('megacells:mega_fluid_cell_housing')
            ['scannerResearch(java.util.function.UnaryOperator)'](b => b
            .researchStack(Item.of('ae2:fluid_cell_housing'))
            .duration(20*600)
            .EUt(GTValues.VA[GTValues.EV]))
            .duration(20*60)
            .EUt(GTValues.VA[GTValues.IV])
            .dimension('ad_astra:moon')
            .circuit(6)

        event.recipes.gtceu.me_assembler('af9:megacells/cell_component_1m')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                '4x #gtceu:circuits/zpm',
                '32x gtceu:hpic_chip',
                '64x ae2:calculation_processor',
                '64x ae2:logic_processor',
                '64x ae2:logic_processor',
                '16x megacells:accumulation_processor',
                '1x ae2:cell_component_256k')
            .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 4608))
            .inputFluids(Fluid.of('gtceu:polyphenylene_sulfide', 9216))
            .itemOutputs('megacells:cell_component_1m')
            ['scannerResearch(java.util.function.UnaryOperator)'](b => b
            .researchStack(Item.of('ae2:cell_component_256k'))
            .duration(20*60)
            .EUt(GTValues.VA[GTValues.LuV]))
            .duration(20*1200)
            .EUt(GTValues.VA[GTValues.LuV])
            .dimension('ad_astra:moon')
            .circuit(6)

        event.recipes.gtceu.me_assembler('af9:megacells/cell_component_4m')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                '4x #gtceu:circuits/uv',
                '64x gtceu:advanced_soc',
                '64x ae2:calculation_processor',
                '64x ae2:logic_processor',
                '64x ae2:logic_processor',
                '64x megacells:accumulation_processor',
                '1x megacells:cell_component_1m')
            .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 9216))
            .inputFluids(Fluid.of('gtceu:polyphenylene_sulfide', 18432))
            .itemOutputs('megacells:cell_component_4m')
            ['scannerResearch(java.util.function.UnaryOperator)'](b => b
            .researchStack(Item.of('megacells:cell_component_1m'))
            .duration(20*30)
            .EUt(GTValues.VA[GTValues.LuV]))
            .duration(20*3600)
            .EUt(GTValues.VA[GTValues.LuV])
            .dimension('ad_astra:moon')
            .circuit(7)

        event.recipes.gtceu.me_assembler('af9:megacells/cell_component_16m')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                '4x #gtceu:circuits/uhv',
                '64x gtceu:uhpic_chip',
                '64x ae2:engineering_processor',
                '64x ae2:logic_processor',
                '64x ae2:logic_processor',
                '64x megacells:accumulation_processor',
                '64x megacells:accumulation_processor',
                '1x megacells:cell_component_4m')
            .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 18432 + 18432))
            .inputFluids(Fluid.of('gtceu:polyphenylene_sulfide', 36864))
            .itemOutputs('megacells:cell_component_16m')
            .duration(20*4000)
            ['stationResearch(java.util.function.UnaryOperator)'](b => b
            .researchStack(Item.of('megacells:cell_component_4m'))
            .EUt(GTValues.VA[GTValues.ZPM])
            .CWUt(32))
            .EUt(GTValues.VA[GTValues.ZPM])
            .dimension('ad_astra:moon')
            .circuit(8)

        event.recipes.gtceu.me_assembler('af9:megacells/cell_component_64m')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                '8x #gtceu:circuits/uhv',
                '64x gtceu:uhpic_chip',
                '64x ae2:engineering_processor',
                '64x ae2:logic_processor',
                '64x ae2:calculation_processor',
                '64x megacells:accumulation_processor',
                '64x megacells:accumulation_processor',
                '1x megacells:cell_component_16m')
            .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 36864 + 36864))
            .inputFluids(Fluid.of('gtceu:polyphenylene_sulfide', 73728))
            .itemOutputs('megacells:cell_component_64m')
            .duration(20*6000)
            ['stationResearch(java.util.function.UnaryOperator)'](b => b
            .researchStack(Item.of('megacells:cell_component_16m'))
            .EUt(GTValues.VA[GTValues.UV])
            .CWUt(64))
            .EUt(GTValues.VA[GTValues.UV])
            .dimension('ad_astra:moon')
            .circuit(9)

        event.recipes.gtceu.me_assembler('af9:megacells/mega_crafting_unit')
            .addCondition(new $OxygenatedCondition(false, true))
            .itemInputs(
                '4x megacells:accumulation_processor',
                '4x ae2:logic_processor',
                '4x ae2:calculation_processor',
                '4x ae2:engineering_processor',
                '#gtceu:circuits/luv',
                '6x #forge:plates/titanium_tungsten_carbide',
                'ae2:crafting_unit')
            .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144*8))
            .itemOutputs('megacells:mega_crafting_unit')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .dimension('ad_astra:moon')
            .circuit(3)

        event.recipes.gtceu.me_assembler('af9:tfg/me_pattern_buffer_proxy')
            .itemInputs(
                'gtceu:luv_machine_hull',
                '2x gtceu:luv_sensor',
                '#gtceu:circuits/luv',
                'gtceu:fusion_glass',
                '2x ae2:quantum_ring',
                '64x gtceu:fine_europium_wire',
                '16x megacells:accumulation_processor')
            .inputFluids(Fluid.of('gtceu:lubricant', 500))
            .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144 * 4))
            ['stationResearch(java.util.function.UnaryOperator)'](b => b.researchStack(Item.of('gtceu:me_pattern_buffer')).EUt(GTValues.VA[GTValues.LuV]).CWUt(32))
            .itemOutputs('gtceu:me_pattern_buffer_proxy')
            .duration(20*25)
            .EUt(GTValues.VA[GTValues.MV])
            .circuit(9)
    }
})
