// AF9 - Base AE2 devices, all gregified at MV.
//
// Everything the ME network is built from (machines, power, core, terminals, cables,
// buses, planes, monitors, cards, tools, spatial, quantum, shells) builds in the assembler
// at MV with solder (tin budget, soldering alloy canonical). Cables and fiber are dry
// (no fluid) single recipes. Vanilla crafting-table recipes for these outputs are removed
// mod-scoped, so the af9: recipes never match.
//
// Left vanilla on purpose: raw materials and world interaction (certus/fluix/sky dust,
// budding, charging, crushing, fluix mixing, meteorite loot, decorative compacting,
// facades, paintballs), condenser/singularity mechanics, cell assembly (housing +
// component, follows automatically), combat tools. See docs/ae2.md.

ServerEvents.recipes(event => {
    if (!Platform.isLoaded('ae2')) return
    const VA = GTValues.VA
    const MV = VA[GTValues.MV]
    const remove = out => event.remove({ mod: 'ae2', output: out })

    // Solder pair: tin budget + soldering alloy canonical.
    const mv = (id, inputs, outputs, ticks) => {
        event.recipes.gtceu.assembler(`af9:${id}`)
            .itemInputs(inputs)
            .inputFluids(Fluid.of('gtceu:tin', 144))
            .itemOutputs(outputs)
            .duration(ticks)
            .EUt(MV)
        event.recipes.gtceu.assembler(`af9:${id}_soldering_alloy`)
            .itemInputs(inputs)
            .inputFluids(Fluid.of('gtceu:soldering_alloy', 72))
            .itemOutputs(outputs)
            .duration(ticks)
            .EUt(MV)
    }
    // Dry single recipe (cables, fiber): no fluid.
    const dry = (id, inputs, outputs, ticks) => {
        event.recipes.gtceu.assembler(`af9:${id}`)
            .itemInputs(inputs)
            .itemOutputs(outputs)
            .duration(ticks)
            .EUt(MV)
    }

    // [recipe suffix, output, inputs, ticks]
    const devices = [
        // ---- Machines ----
        ['ae2_charger', 'ae2:charger',
            ['4x gtceu:iron_plate', '2x gtceu:copper_single_cable', 'ae2:quartz_glass'], 200],
        ['ae2_inscriber', 'ae2:inscriber',
            ['4x gtceu:steel_plate', 'gtceu:mv_electric_piston', 'ae2:fluix_crystal'], 200],
        ['ae2_vibration_chamber', 'ae2:vibration_chamber',
            ['minecraft:furnace', '2x gtceu:steel_plate', 'ae2:logic_processor'], 200],
        ['ae2_crank', 'ae2:crank',
            ['2x gtceu:steel_rod', 'gtceu:steel_plate'], 100],
        ['ae2_condenser', 'ae2:condenser',
            ['4x ae2:quartz_glass', 'ae2:logic_processor', '2x ae2:fluix_crystal'], 200],
        ['ae2_growth_accelerator', 'ae2:growth_accelerator',
            ['4x ae2:fluix_crystal', 'ae2:calculation_processor', '2x ae2:quartz_glass'], 200],
        ['ae2_sky_stone_tank', 'ae2:sky_stone_tank',
            ['4x ae2:sky_stone_block', '2x ae2:quartz_glass', 'ae2:logic_processor'], 200],
        ['ae2_resonance_generator', 'ae2:crystal_resonance_generator',
            ['4x ae2:fluix_crystal', '4x ae2:certus_quartz_crystal', 'ae2:calculation_processor'], 200],
        // ---- Power ----
        ['ae2_energy_acceptor', 'ae2:energy_acceptor',
            ['4x gtceu:steel_plate', '4x ae2:quartz_glass', 'ae2:logic_processor'], 200],
        ['ae2_energy_cell', 'ae2:energy_cell',
            ['4x ae2:charged_certus_quartz_crystal', '4x ae2:fluix_crystal', 'ae2:logic_processor'], 200],
        ['ae2_dense_energy_cell', 'ae2:dense_energy_cell',
            ['3x ae2:energy_cell', 'ae2:calculation_processor', '4x ae2:fluix_crystal'], 400],
        // ---- Network core ----
        ['ae2_controller', 'ae2:controller',
            ['6x ae2:sky_stone_block', 'ae2:engineering_processor', 'gtceu:micro_processor'], 400],
        ['ae2_drive', 'ae2:drive',
            ['4x gtceu:steel_plate', '2x ae2:engineering_processor', 'ae2:quartz_glass',
                'gtceu:micro_processor'], 400],
        ['ae2_chest', 'ae2:chest',
            ['4x ae2:quartz_glass', 'ae2:logic_processor', 'ae2:fluix_crystal',
                '2x gtceu:steel_plate'], 200],
        ['ae2_io_port', 'ae2:io_port',
            ['4x ae2:quartz_glass', 'ae2:logic_processor', 'ae2:calculation_processor',
                '2x gtceu:steel_plate'], 200],
        ['ae2_workbench', 'ae2:cell_workbench',
            ['4x gtceu:steel_plate', 'ae2:calculation_processor', 'ae2:quartz_glass'], 200],
        // ---- Terminals ----
        ['ae2_terminal', 'ae2:terminal',
            ['ae2:logic_processor', '4x ae2:quartz_fiber', '2x ae2:quartz_glass'], 200],
        ['ae2_crafting_terminal', 'ae2:crafting_terminal',
            ['ae2:terminal', 'ae2:calculation_processor', '2x gtceu:steel_plate'], 200],
        ['ae2_encoding_terminal', 'ae2:pattern_encoding_terminal',
            ['ae2:terminal', 'ae2:engineering_processor', '2x gtceu:steel_plate'], 200],
        ['ae2_access_terminal', 'ae2:pattern_access_terminal',
            ['ae2:terminal', 'ae2:logic_processor', 'ae2:calculation_processor'], 200],
        ['ae2_wireless_terminal', 'ae2:wireless_terminal',
            ['ae2:terminal', 'ae2:wireless_receiver', 'ae2:engineering_processor'], 200],
        ['ae2_wireless_crafting_terminal', 'ae2:wireless_crafting_terminal',
            ['ae2:crafting_terminal', 'ae2:wireless_receiver', 'ae2:engineering_processor'], 200],
        // ---- Network parts ----
        ['ae2_interface', 'ae2:interface',
            ['ae2:logic_processor', '2x ae2:quartz_glass', '2x gtceu:steel_plate',
                'ae2:fluix_pearl'], 200],
        ['ae2_pattern_provider', 'ae2:pattern_provider',
            ['ae2:logic_processor', 'ae2:calculation_processor', '2x gtceu:steel_plate',
                'ae2:fluix_pearl'], 200],
        ['ae2_molecular_assembler', 'ae2:molecular_assembler',
            ['4x ae2:quartz_glass', 'ae2:calculation_processor', 'ae2:engineering_processor',
                'gtceu:micro_processor'], 400],
        ['ae2_import_bus', 'ae2:import_bus',
            ['ae2:logic_processor', 'gtceu:mv_electric_piston', 'ae2:quartz_glass'], 200],
        ['ae2_export_bus', 'ae2:export_bus',
            ['ae2:calculation_processor', 'gtceu:mv_electric_piston', 'ae2:quartz_glass'], 200],
        ['ae2_storage_bus', 'ae2:storage_bus',
            ['ae2:logic_processor', 'ae2:calculation_processor', '2x ae2:quartz_glass',
                'ae2:fluix_crystal'], 200],
        ['ae2_formation_core', 'ae2:formation_core',
            ['ae2:logic_processor', '2x ae2:certus_quartz_crystal', 'ae2:fluix_dust'], 200],
        ['ae2_annihilation_core', 'ae2:annihilation_core',
            ['ae2:calculation_processor', '2x ae2:fluix_dust', 'ae2:certus_quartz_crystal'], 200],
        ['ae2_formation_plane', 'ae2:formation_plane',
            ['ae2:formation_core', '2x ae2:fluix_crystal', 'gtceu:steel_plate'], 200],
        ['ae2_annihilation_plane', 'ae2:annihilation_plane',
            ['ae2:annihilation_core', '2x ae2:fluix_crystal', 'gtceu:steel_plate'], 200],
        ['ae2_p2p', 'ae2:me_p2p_tunnel',
            ['ae2:engineering_processor', '2x ae2:fluix_crystal', '2x gtceu:steel_plate',
                'ae2:logic_processor'], 200],
        ['ae2_level_emitter', 'ae2:level_emitter',
            ['ae2:calculation_processor', '4x minecraft:redstone', '2x ae2:quartz_glass'], 200],
        ['ae2_energy_level_emitter', 'ae2:energy_level_emitter',
            ['ae2:calculation_processor', 'ae2:energy_cell', 'ae2:quartz_glass'], 200],
        ['ae2_toggle_bus', 'ae2:toggle_bus',
            ['ae2:level_emitter', 'minecraft:redstone', 'gtceu:steel_plate'], 200],
        ['ae2_storage_monitor', 'ae2:storage_monitor',
            ['ae2:logic_processor', '2x ae2:quartz_glass', 'minecraft:redstone'], 200],
        ['ae2_conversion_monitor', 'ae2:conversion_monitor',
            ['ae2:calculation_processor', '2x ae2:quartz_glass', 'minecraft:redstone',
                'gtceu:steel_plate'], 200],
        ['ae2_crafting_monitor', 'ae2:crafting_monitor',
            ['ae2:crafting_unit', 'ae2:cell_component_1k', 'ae2:quartz_glass'], 200],
        // ---- Cards ----
        ['ae2_capacity_card', 'ae2:capacity_card',
            ['ae2:logic_processor', '2x ae2:certus_quartz_crystal', '2x minecraft:redstone',
                'ae2:quartz_glass'], 200],
        ['ae2_crafting_card', 'ae2:crafting_card',
            ['ae2:calculation_processor', '2x minecraft:redstone', 'ae2:quartz_glass',
                'ae2:certus_quartz_crystal'], 200],
        ['ae2_energy_card', 'ae2:energy_card',
            ['2x ae2:charged_certus_quartz_crystal', '2x ae2:fluix_crystal', 'ae2:logic_processor'], 200],
        ['ae2_fuzzy_card', 'ae2:fuzzy_card',
            ['ae2:logic_processor', 'minecraft:ender_pearl', '2x minecraft:redstone'], 200],
        ['ae2_inverter_card', 'ae2:inverter_card',
            ['ae2:logic_processor', '4x minecraft:redstone', 'ae2:quartz_glass'], 200],
        ['ae2_redstone_card', 'ae2:redstone_card',
            ['ae2:calculation_processor', '2x minecraft:redstone_torch', 'ae2:quartz_glass'], 200],
        ['ae2_speed_card', 'ae2:speed_card',
            ['ae2:engineering_processor', '2x minecraft:glowstone_dust', 'minecraft:redstone'], 200],
        ['ae2_equal_card', 'ae2:equal_distribution_card',
            ['ae2:calculation_processor', '2x ae2:certus_quartz_crystal', '2x ae2:quartz_glass'], 200],
        ['ae2_void_card', 'ae2:void_card',
            ['ae2:calculation_processor', '4x ae2:fluix_dust', 'ae2:quartz_glass'], 200],
        // ---- Network tools ----
        ['ae2_network_tool', 'ae2:network_tool',
            ['ae2:logic_processor', 'ae2:calculation_processor', 'ae2:engineering_processor',
                'ae2:quartz_glass'], 200],
        ['ae2_memory_card', 'ae2:memory_card',
            ['ae2:calculation_processor', '2x minecraft:redstone', 'gtceu:steel_plate'], 200],
        ['ae2_color_applicator', 'ae2:color_applicator',
            ['2x gtceu:steel_plate', 'ae2:quartz_glass', 'minecraft:glowstone_dust'], 200],
        ['ae2_cable_anchor', 'ae2:cable_anchor',
            ['gtceu:steel_plate', 'ae2:quartz_fiber', 'minecraft:redstone'], 100],
        ['ae2_certus_wrench', 'ae2:certus_quartz_wrench',
            ['2x ae2:certus_quartz_crystal', 'gtceu:steel_plate'], 100],
        ['ae2_nether_wrench', 'ae2:nether_quartz_wrench',
            ['2x minecraft:quartz', 'gtceu:steel_plate'], 100],
        ['ae2_certus_knife', 'ae2:certus_quartz_cutting_knife',
            ['ae2:certus_quartz_crystal', '2x minecraft:stick'], 100],
        ['ae2_nether_knife', 'ae2:nether_quartz_cutting_knife',
            ['minecraft:quartz', '2x minecraft:stick'], 100],
        ['ae2_charged_staff', 'ae2:charged_staff',
            ['2x ae2:charged_certus_quartz_crystal', 'gtceu:steel_plate', 'minecraft:redstone'], 200],
        ['ae2_entropy', 'ae2:entropy_manipulator',
            ['ae2:engineering_processor', '2x ae2:fluix_crystal', '2x ae2:charged_certus_quartz_crystal'], 200],
        ['ae2_matter_cannon', 'ae2:matter_cannon',
            ['4x gtceu:steel_plate', 'ae2:engineering_processor', 'minecraft:diamond'], 200],
        ['ae2_tiny_tnt', 'ae2:tiny_tnt',
            ['2x minecraft:gunpowder', 'ae2:fluix_dust', 'minecraft:redstone'], 100],
        // ---- Spatial + quantum ----
        ['ae2_spatial_io', 'ae2:spatial_io_port',
            ['ae2:engineering_processor', 'ae2:calculation_processor', '4x ae2:quartz_glass',
                'minecraft:diamond'], 200],
        ['ae2_spatial_pylon', 'ae2:spatial_pylon',
            ['4x ae2:fluix_crystal', 'ae2:engineering_processor', '2x gtceu:steel_plate'], 200],
        ['ae2_spatial_anchor', 'ae2:spatial_anchor',
            ['ae2:spatial_pylon', 'ae2:calculation_processor', '2x minecraft:diamond'], 200],
        ['ae2_quantum_ring', 'ae2:quantum_ring',
            ['ae2:engineering_processor', '4x ae2:fluix_crystal', '2x minecraft:diamond'], 400],
        ['ae2_quantum_link', 'ae2:quantum_link',
            ['2x ae2:quantum_ring', 'ae2:calculation_processor'], 200],
        // ---- Small stuff ----
        ['ae2_blank_pattern', 'ae2:blank_pattern',
            ['2x ae2:certus_quartz_crystal', 'minecraft:redstone', 'ae2:quartz_glass',
                'ae2:fluix_dust'], 100],
        ['ae2_view_cell', 'ae2:view_cell',
            ['2x ae2:quartz_glass', 'ae2:calculation_processor', 'gtceu:steel_plate',
                'ae2:fluix_pearl'], 200],
        ['ae2_quartz_glass', 'ae2:quartz_glass',
            ['4x ae2:certus_quartz_dust', '4x minecraft:glass'], 100],
        // ---- Portable cells (chest + component + energy cell + housing) ----
        ['ae2_portable_1k', 'ae2:portable_item_cell_1k',
            ['ae2:chest', 'ae2:cell_component_1k', 'ae2:energy_cell', 'ae2:item_cell_housing'], 400],
        ['ae2_portable_4k', 'ae2:portable_item_cell_4k',
            ['ae2:chest', 'ae2:cell_component_4k', 'ae2:energy_cell', 'ae2:item_cell_housing'], 400],
        ['ae2_portable_16k', 'ae2:portable_item_cell_16k',
            ['ae2:chest', 'ae2:cell_component_16k', 'ae2:energy_cell', 'ae2:item_cell_housing'], 400],
        ['ae2_portable_64k', 'ae2:portable_item_cell_64k',
            ['ae2:chest', 'ae2:cell_component_64k', 'ae2:energy_cell', 'ae2:item_cell_housing'], 400],
        ['ae2_portable_256k', 'ae2:portable_item_cell_256k',
            ['ae2:chest', 'ae2:cell_component_256k', 'ae2:energy_cell', 'ae2:item_cell_housing'], 400],
        ['ae2_portable_fluid_1k', 'ae2:portable_fluid_cell_1k',
            ['ae2:chest', 'ae2:cell_component_1k', 'ae2:energy_cell', 'ae2:fluid_cell_housing'], 400],
        ['ae2_portable_fluid_4k', 'ae2:portable_fluid_cell_4k',
            ['ae2:chest', 'ae2:cell_component_4k', 'ae2:energy_cell', 'ae2:fluid_cell_housing'], 400],
        ['ae2_portable_fluid_16k', 'ae2:portable_fluid_cell_16k',
            ['ae2:chest', 'ae2:cell_component_16k', 'ae2:energy_cell', 'ae2:fluid_cell_housing'], 400],
        ['ae2_portable_fluid_64k', 'ae2:portable_fluid_cell_64k',
            ['ae2:chest', 'ae2:cell_component_64k', 'ae2:energy_cell', 'ae2:fluid_cell_housing'], 400],
        ['ae2_portable_fluid_256k', 'ae2:portable_fluid_cell_256k',
            ['ae2:chest', 'ae2:cell_component_256k', 'ae2:energy_cell', 'ae2:fluid_cell_housing'], 400]
    ]
    devices.forEach(([id, out, inputs, ticks]) => {
        remove(out)
        mv(id, inputs, out, ticks)
    })

    // ---- Cables (dry): only the fluix base of each family; dye variants follow ----
    const cables = [
        ['ae2_fluix_glass', 'ae2:fluix_glass_cable', ['2x ae2:quartz_fiber', 'ae2:fluix_dust'], '2x ae2:fluix_glass_cable'],
        ['ae2_fluix_covered', 'ae2:fluix_covered_cable', ['ae2:fluix_glass_cable', 'gtceu:rubber_plate'],
            '2x ae2:fluix_covered_cable'],
        ['ae2_fluix_dense', 'ae2:fluix_covered_dense_cable',
            ['4x ae2:fluix_covered_cable', 'ae2:fluix_crystal'], 'ae2:fluix_covered_dense_cable'],
        ['ae2_fluix_smart', 'ae2:fluix_smart_cable',
            ['ae2:fluix_covered_cable', 'minecraft:redstone', 'minecraft:glowstone_dust'],
            '2x ae2:fluix_smart_cable'],
        ['ae2_fluix_smart_dense', 'ae2:fluix_smart_dense_cable',
            ['ae2:fluix_covered_dense_cable', 'minecraft:redstone', 'minecraft:glowstone_dust'],
            'ae2:fluix_smart_dense_cable']
    ]
    cables.forEach(([id, out, inputs, result]) => {
        remove(out)
        dry(id, inputs, result, 100)
    })
    remove('ae2:quartz_fiber')
    dry('ae2_quartz_fiber', ['ae2:certus_quartz_crystal', '2x minecraft:glass'], '2x ae2:quartz_fiber', 100)
})
