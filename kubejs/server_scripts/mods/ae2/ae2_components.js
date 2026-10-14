// AF9 - AE2 storage components, crafting storage, and cell housings: all MV.
//
// Base AE2 storage stays at MV: cell components climb 1k -> 4k -> 16k -> 64k -> 256k on
// MV circuits only (good integrated, then micro processor), 3x previous + calculation
// processor (logic for 1k) + bulk secondary + quartz glass, in the assembler with solder
// (tin budget, soldering alloy canonical). Crafting storages add the crafting unit and the
// same MV circuit; housings are plates + quartz glass + redstone. All vanilla
// crafting-table recipes for these outputs are removed (mod-scoped, so the af9: recipes
// never match). Cells themselves (housing + component, shapeless) stay vanilla and follow
// automatically. MEGA Cells live in ae2_addons.js on the IV -> UHV ladder. Spec: docs/ae2.md

ServerEvents.recipes(event => {
    if (!Platform.isLoaded('ae2')) return
    const VA = GTValues.VA
    const MV = VA[GTValues.MV]

    // Tin budget + soldering alloy canonical pair.
    const soldered = (id, build) => {
        build(event.recipes.gtceu.assembler(`af9:${id}`), Fluid.of('gtceu:tin', 144))
        build(event.recipes.gtceu.assembler(`af9:${id}_soldering_alloy`),
            Fluid.of('gtceu:soldering_alloy', 72))
    }

    // ---- Cell components (all MV, no cleanroom) ----
    const components = [
        { out: 'ae2:cell_component_1k', from: null, secondary: '4x minecraft:redstone',
            circuit: 'gtceu:good_integrated_circuit', processor: 'ae2:logic_processor', ticks: 200 },
        { out: 'ae2:cell_component_4k', from: 'ae2:cell_component_1k', secondary: '4x minecraft:redstone',
            circuit: 'gtceu:micro_processor', processor: 'ae2:calculation_processor', ticks: 300 },
        { out: 'ae2:cell_component_16k', from: 'ae2:cell_component_4k', secondary: '4x minecraft:glowstone_dust',
            circuit: 'gtceu:micro_processor', processor: 'ae2:calculation_processor', ticks: 400 },
        { out: 'ae2:cell_component_64k', from: 'ae2:cell_component_16k', secondary: '4x minecraft:glowstone_dust',
            circuit: 'gtceu:micro_processor', processor: 'ae2:calculation_processor', ticks: 500 },
        { out: 'ae2:cell_component_256k', from: 'ae2:cell_component_64k', secondary: '4x ae2:sky_dust',
            circuit: 'gtceu:micro_processor', processor: 'ae2:calculation_processor', ticks: 600 }
    ]
    components.forEach(spec => {
        event.remove({ mod: 'ae2', output: spec.out })
        const inputs = spec.from === null
            ? [spec.processor, '4x ae2:certus_quartz_crystal', spec.secondary, spec.circuit]
            : [`3x ${spec.from}`, spec.processor, spec.secondary, 'ae2:quartz_glass', spec.circuit]
        const shortId = spec.out.split(':')[1]
        soldered(`ae2_${shortId}`, (recipe, fluid) => {
            recipe.itemInputs(inputs).inputFluids(fluid).itemOutputs(spec.out)
                .duration(spec.ticks).EUt(MV)
        })
    })

    // ---- Crafting unit + co-processor (gated shells for the storages below, MV) ----
    event.remove({ mod: 'ae2', output: 'ae2:crafting_unit' })
    soldered('ae2_crafting_unit', (recipe, fluid) => {
        recipe.itemInputs('4x gtceu:steel_plate', 'ae2:logic_processor', 'ae2:calculation_processor')
            .inputFluids(fluid).itemOutputs('ae2:crafting_unit')
            .duration(200).EUt(MV)
    })
    event.remove({ mod: 'ae2', output: 'ae2:crafting_accelerator' })
    soldered('ae2_crafting_accelerator', (recipe, fluid) => {
        recipe.itemInputs('ae2:crafting_unit', '2x ae2:engineering_processor', 'gtceu:micro_processor')
            .inputFluids(fluid).itemOutputs('ae2:crafting_accelerator')
            .duration(300).EUt(MV)
    })

    // ---- Crafting storages: unit + component + MV circuit ----
    const storages = [
        ['ae2:1k_crafting_storage', 'ae2:cell_component_1k', 'gtceu:good_integrated_circuit', 200],
        ['ae2:4k_crafting_storage', 'ae2:cell_component_4k', 'gtceu:micro_processor', 300],
        ['ae2:16k_crafting_storage', 'ae2:cell_component_16k', 'gtceu:micro_processor', 400],
        ['ae2:64k_crafting_storage', 'ae2:cell_component_64k', 'gtceu:micro_processor', 500],
        ['ae2:256k_crafting_storage', 'ae2:cell_component_256k', 'gtceu:micro_processor', 600]
    ]
    storages.forEach(([out, comp, circuit, ticks]) => {
        event.remove({ mod: 'ae2', output: out })
        const shortId = out.split(':')[1]
        soldered(`ae2_${shortId}`, (recipe, fluid) => {
            recipe.itemInputs('ae2:crafting_unit', comp, circuit)
                .inputFluids(fluid).itemOutputs(out)
                .duration(ticks).EUt(MV)
        })
    })

    // ---- Cell housings (dumb shells: plates + quartz glass + redstone, MV) ----
    const housings = [
        ['ae2:item_cell_housing', '3x gtceu:steel_plate'],
        ['ae2:fluid_cell_housing', '3x gtceu:copper_plate']
    ]
    housings.forEach(([out, plates]) => {
        event.remove({ mod: 'ae2', output: out })
        const shortId = out.split(':')[1]
        soldered(`ae2_${shortId}`, (recipe, fluid) => {
            recipe.itemInputs('2x ae2:quartz_glass', plates, '4x minecraft:redstone')
                .inputFluids(fluid).itemOutputs(out)
                .duration(200).EUt(MV)
        })
    })
})
