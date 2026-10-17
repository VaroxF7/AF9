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
    // RAM is the bulk of every storage component: chips stack to 64, so a count splits into stacks.
    // Counts double each step; each component also eats 3x the previous one, so the RAM compounds.
    const ram = (id, count) => {
        const stacks = []
        for (let left = count; left > 0; left -= 64) stacks.push(`${Math.min(left, 64)}x ${id}`)
        return stacks
    }

    // ---- Cell components (all MV) ----
    const components = [
        { ram: ram('gtceu:ram_chip', 8), out: 'ae2:cell_component_1k', certus: 144, from: null, secondary: '4x minecraft:redstone',
            circuit: 'gtceu:good_integrated_circuit', processor: 'ae2:logic_processor', ticks: 200 },
        { ram: ram('gtceu:ram_chip', 16), out: 'ae2:cell_component_4k', certus: 288, from: 'ae2:cell_component_1k', secondary: '4x minecraft:redstone',
            circuit: 'gtceu:micro_processor', processor: 'ae2:calculation_processor', ticks: 300 },
        { ram: ram('gtceu:ram_chip', 32), out: 'ae2:cell_component_16k', certus: 576, from: 'ae2:cell_component_4k', secondary: '4x minecraft:glowstone_dust',
            circuit: 'gtceu:micro_processor', processor: 'ae2:calculation_processor', ticks: 400 },
        { ram: ram('gtceu:ram_chip', 64).concat(['16x gtceu:nand_memory_chip', '4x gtceu:micro_processor']),
            out: 'ae2:cell_component_64k', certus: 1152, from: 'ae2:cell_component_16k', secondary: '4x minecraft:glowstone_dust',
            circuit: 'gtceu:micro_processor', processor: 'ae2:calculation_processor', ticks: 500 },
        { ram: ram('gtceu:ram_chip', 128).concat(['32x gtceu:nand_memory_chip', '8x gtceu:micro_processor']),
            noGlass: true, out: 'ae2:cell_component_256k', certus: 2304, from: 'ae2:cell_component_64k', secondary: '4x ae2:sky_dust',
            circuit: 'gtceu:micro_processor', processor: 'ae2:calculation_processor', ticks: 600 }
    ]
    components.forEach(spec => {
        event.remove({ mod: 'ae2', output: spec.out })
        const inputs = (spec.from === null
            ? [spec.processor, '4x ae2:certus_quartz_crystal', spec.secondary, spec.circuit]
            : [`3x ${spec.from}`, spec.processor, spec.secondary, 'ae2:quartz_glass', spec.circuit]
        ).filter(i => !(spec.noGlass && i === 'ae2:quartz_glass')).concat(spec.ram)  // 256k: 9 slots are full without the glass
        const shortId = spec.out.split(':')[1]
        // Liquid certus quartz is the component's one fluid (the assembler takes one; the solder is in the
        // processor already), 144 mB for 1k, doubling each step.
        event.recipes.gtceu.assembler(`af9:ae2_${shortId}`)
            .itemInputs(inputs)
            .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', spec.certus))
            .itemOutputs(spec.out)
            .duration(spec.ticks).EUt(MV)
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
        const build = (recipe, fluid) => {
            recipe.itemInputs('ae2:crafting_unit', comp, circuit)
                .inputFluids(fluid).itemOutputs(out)
                .duration(ticks).EUt(MV)
        }
        soldered(`ae2_${shortId}`, build)
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
