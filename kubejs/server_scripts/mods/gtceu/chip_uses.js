// AF9 - Where AF9's own chips (startup_scripts/gtceu/chips.js) go into other mods' recipes: chips added to GT's and
// AE2's recipes, and alternative recipes that take a chip. The uses inside AF9's own recipes sit with those recipes
// (wireless energy hatches, MV circuits, tiered circuits, fab machines, void miner, Orbital Lithography Station).
// Spec: docs/semiconductor-factory.md §8.

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    // Appends items to a recipe of another mod (GT's recipes keep their id, research and everything else)
    const addInputs = (id, items) => event.forEachRecipe({ id: id }, recipe => {
        items.forEach(item => recipe.itemInputs(item))
    })

    // ---- RF Transceiver: the radio of every wireless link ----
    // AE2's Wireless Receiver, the part of the Wireless Access Point and of every wireless terminal
    event.remove({ id: 'ae2:network/wireless_part' })
    event.shaped('ae2:wireless_receiver', [' F ', 'IQI', 'IRI'], {
        F: 'ae2:fluix_pearl', I: '#forge:ingots/iron', Q: 'ae2:quartz_fiber', R: 'af9:rf_transceiver_chip'
    }).id('af9:ae2/wireless_receiver')

    // ---- MCU: the small controller in GT's logic parts ----
    // GT's logic covers take one (they had no circuit: the lever / redstone parts and an iron plate)
    const logicCovers = ['cover_machine_controller', 'cover_activity_detector', 'cover_item_detector',
        'cover_fluid_detector']
    logicCovers.forEach(id => addInputs(`gtceu:assembler/${id}`, ['af9:mcu_chip']))
    // LV and MV robot arms and sensors: an MCU instead of the circuit, beside GT's recipes (the circuit versions stay:
    // the first ones come before the Photolithography Line)
    const mcuParts = [
        // tier, cable, rod, plate, sensor rod, sensor gem (GT's crafting components of the tier)
        ['lv', 'tin', 'steel', 'steel', 'brass', '#forge:gems/quartzite'],
        ['mv', 'copper', 'aluminium', 'aluminium', 'electrum', '#forge:flawless_gems/emerald']]
    mcuParts.forEach(([t, cable, rod, plate, sensorRod, gem]) => {
        event.recipes.gtceu.assembler(`af9:${t}_robot_arm_mcu`)
            .itemInputs(`3x gtceu:${cable}_single_cable`, `2x gtceu:${rod}_rod`, `2x gtceu:${t}_electric_motor`,
                `gtceu:${t}_electric_piston`, 'af9:mcu_chip')
            .itemOutputs(`gtceu:${t}_robot_arm`)
            .duration(100)
            .EUt(VA[GTValues.LV])
        event.recipes.gtceu.assembler(`af9:${t}_sensor_mcu`)
            .itemInputs(`gtceu:${sensorRod}_rod`, `4x gtceu:${plate}_plate`, 'af9:mcu_chip', gem)
            .itemOutputs(`gtceu:${t}_sensor`)
            .duration(100)
            .EUt(VA[GTValues.LV])
    })

    // ---- ASIC: the mining ASIC ----
    // GT's Large Miners and Fluid Drilling Rigs (HV and up: the MV rig comes before phosphorus wafers, it stays as it
    // is); the Void Miner takes them in miner.js
    const miningAsics = [['ev_large_miner', 2], ['iv_large_miner', 4], ['luv_large_miner', 8],
        ['hv_fluid_drilling_rig', 2], ['ev_fluid_drilling_rig', 4]]
    miningAsics.forEach(([id, count]) => addInputs(`gtceu:assembler/${id}`, [`${count}x af9:asic_chip`]))

    // ---- MRAM: data storage that keeps its data without power ----
    // Faster recipes beside GT's for its research data parts: MRAM for the memory chips, a CPU chip, wire and the
    // part's body (no research: they are made from the MRAM, not researched)
    const solders = [['', 'gtceu:tin', 288], ['_soldering_alloy', 'gtceu:soldering_alloy', 144]]
    solders.forEach(([suffix, fluid, mb]) => {
        // GT: 2 HV circuits, 4 RAM, 32 NOR, 64 NAND, 32 platinum wire, 400 ticks
        event.recipes.gtceu.circuit_assembler(`af9:data_orb_mram${suffix}`)
            .itemInputs('gtceu:epoxy_printed_circuit_board', 'gtceu:cpu_chip', '4x af9:mram_chip',
                '16x gtceu:fine_platinum_wire')
            .inputFluids(Fluid.of(fluid, mb))
            .itemOutputs('gtceu:data_orb')
            .cleanroom(CleanroomType.CLEANROOM)
            .duration(200)
            .EUt(1200)
    })
    // GT: an assembly line (8 LuV circuits, a Data Orb, 128 fine wire, 4 optical pipes, 16 ITBTC wire), 1200 ticks
    event.recipes.gtceu.assembler('af9:data_bank_mram')
        .itemInputs('gtceu:computer_casing', '16x af9:mram_chip', '2x gtceu:cpu_chip',
            '32x gtceu:fine_niobium_titanium_wire', '4x gtceu:normal_optical_pipe')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 288))
        .itemOutputs('gtceu:data_bank')
        .cleanroom(CleanroomType.CLEANROOM)
        .duration(600)
        .EUt(6000)
    // GT: an assembly line (LuV input bus, 4 Data Orbs, 4 ZPM circuits), 400 ticks
    event.recipes.gtceu.assembler('af9:advanced_data_access_hatch_mram')
        .itemInputs('gtceu:luv_input_bus', '8x af9:mram_chip', '2x gtceu:cpu_chip',
            '32x gtceu:fine_niobium_titanium_wire')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 576))
        .itemOutputs('gtceu:advanced_data_access_hatch')
        .cleanroom(CleanroomType.CLEANROOM)
        .duration(200)
        .EUt(6000)

    // ---- VPU: image processing of GT's LuV sensing parts (the Orbital Lithography Station: photolithography.js) ----
    addInputs('gtceu:assembly_line/sensor_luv', ['2x af9:vpu_chip'])
    // LuV Scanner: GT's shape, two VPUs in place of the bottom two of its four ZPM circuits
    event.remove({ output: 'gtceu:luv_scanner' })
    event.shaped('gtceu:luv_scanner', ['CEC', 'WHW', 'VSV'], {
        C: '#gtceu:circuits/zpm', E: 'gtceu:luv_emitter', W: 'gtceu:niobium_titanium_single_cable',
        H: 'gtceu:luv_machine_hull', S: 'gtceu:luv_sensor', V: 'af9:vpu_chip'
    }).id('af9:luv_scanner')

    // ---- TPU: the AI accelerator (the pack's UHV Wetware Mainframe recipe takes them too) ----
    // The HPCA's Advanced Computation Component: the computation of the 7 nm and 1 nm prints and of the research
    addInputs('gtceu:assembler/hpca_advanced_computation_component', ['4x af9:tpu_chip'])
})
