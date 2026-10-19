// AF9 - AE2 crafting storage and cell housings (MV assembler), plus the sweep that kills AE2's
// own cell component recipes.
//
// The cell components themselves (1k -> 4k -> 16k -> 64k -> 256k) belong to the ME Resonance
// Fabricator alone (me_resonance_fabricator_recipes.js): ZPM on the Moon, the pack's own chip
// ladder, no assembler route. Crafting storages add the crafting unit and the same MV circuit;
// housings are plates + quartz glass + redstone, in the assembler on liquid certus quartz — or
// soldering alloy, or high-grade solder at half the time. All vanilla crafting-table recipes for
// these outputs are removed (mod-scoped, so the af9: recipes never match). Cells themselves
// (housing + component, shapeless) stay vanilla and follow automatically. MEGA Cells live in
// ae2_addons.js on the IV -> UHV ladder. Spec: docs/ae2.md

ServerEvents.recipes(event => {
    if (!Platform.isLoaded('ae2')) return
    const VA = GTValues.VA
    const MV = VA[GTValues.MV]

    // Three solders: liquid certus quartz (canonical), soldering alloy (the normal one) and
    // high-grade solder, which runs the same recipe at half the time.
    const soldered = (id, ticks, build) => {
        const run = (suffix, fluid, time) => {
            const recipe = event.recipes.gtceu.assembler(`af9:${id}${suffix}`)
            build(recipe, fluid)
            recipe.duration(time).EUt(MV)
        }
        run('', Fluid.of('gtceu:liquid_certus_quartz', 144), ticks)
        run('_solder', Fluid.of('gtceu:soldering_alloy', 72), ticks)
        run('_high_grade', Fluid.of('gtceu:high_grade_solder', 72), Math.ceil(ticks / 2))
    }
    // ---- Cell components: the Fabricator's alone (ZPM on the Moon) — only kill AE2's own ----
    // The recipes live in me_resonance_fabricator_recipes.js; without this sweep AE2's vanilla
    // component recipes would come back.
    ;['1k', '4k', '16k', '64k', '256k'].forEach(tier => {
        event.remove({ mod: 'ae2', output: `ae2:cell_component_${tier}` })
    })

    // ---- Crafting unit + co-processor (gated shells for the storages below, MV) ----
    event.remove({ mod: 'ae2', output: 'ae2:crafting_unit' })
    soldered('ae2_crafting_unit', 200, (recipe, fluid) => {
        recipe.itemInputs('4x gtceu:steel_plate', 'ae2:logic_processor', 'ae2:calculation_processor')
            .inputFluids(fluid).itemOutputs('ae2:crafting_unit')
    })
    event.remove({ mod: 'ae2', output: 'ae2:crafting_accelerator' })
    soldered('ae2_crafting_accelerator', 300, (recipe, fluid) => {
        recipe.itemInputs('ae2:crafting_unit', '2x ae2:engineering_processor', 'gtceu:micro_processor')
            .inputFluids(fluid).itemOutputs('ae2:crafting_accelerator')
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
        }
        soldered(`ae2_${shortId}`, ticks, build)
    })

    // ---- Cell housings (dumb shells: plates + quartz glass + redstone, MV) ----
    const housings = [
        ['ae2:item_cell_housing', '3x gtceu:steel_plate'],
        ['ae2:fluid_cell_housing', '3x gtceu:copper_plate']
    ]
    housings.forEach(([out, plates]) => {
        event.remove({ mod: 'ae2', output: out })
        const shortId = out.split(':')[1]
        soldered(`ae2_${shortId}`, 200, (recipe, fluid) => {
            recipe.itemInputs('2x ae2:quartz_glass', plates, '4x minecraft:redstone')
                .inputFluids(fluid).itemOutputs(out)
        })
    })
})
