// priority: -100
// AF9 - Fixes for three broken base-pack (ATM9) GT recipes.
//
// GT 7.5 refuses a recipe that names an item the game does not have (the base pack
// was written for GT 7.2), and KubeJS counts each as a failed recipe:
//
//  1. gtceu:micro_universe_collector/max_energy_hatch
//     (mods/gtceu/micro_universe_orb_recipes.js#117): outputs
//     gtceu:max_energy_input_hatch, which GT only registers with its high-tier
//     content. That content is off in this pack (highTierContent=false;
//     tools/lint/data/gt-dev-only.txt), and the MAX tier is not part of AF9's
//     progression, so the recipe is disabled here.
//
//  2. kubejs:ore_sifting_plant/nether_quartz and
//  3. kubejs:ore_sifting_plant/nether_quartz_silked
//     (mods/gtceu/ore_processing_plant_recipes.js#38): chanced output
//     minecraft:nether_quartz, which does not exist. The quartz gem is
//     minecraft:quartz (the base pack's own certus quartz recipes use that).
//     Both are re-added below with minecraft:quartz.
//
// The base pack files still carry the typos upstream; until ATM9 fixes them they
// still log their own failures. This override runs after them (priority -100) so
// the game has the corrected recipes and no MAX-hatch recipe.

ServerEvents.recipes(event => {
    // ---- 1. MAX energy hatch: disabled (MAX tier off in this pack) ----
    event.remove({ id: 'gtceu:micro_universe_collector/max_energy_hatch' })

    // ---- 2-3. Nether quartz sifting: corrected (minecraft:quartz) ----
    // The broken base-pack recipes failed to build, so these removes are no-ops
    // until upstream fixes the typo; the re-adds below are what the game runs.
    event.remove({ id: 'kubejs:ore_sifting_plant/nether_quartz' })
    event.remove({ id: 'kubejs:ore_sifting_plant/nether_quartz_silked' })

    // Nether quartz, crushed (raw): stone dust out, quartzite and quartz
    // byproducts. Numbers are the base pack's own (chances in hundredths of a
    // percent, then the tier bonus); only the quartz gem id is fixed.
    event.recipes.gtceu.ore_sifting_plant('kubejs:nether_quartz')
        .itemInputs('128x #forge:raw_materials/nether_quartz')
        .itemOutputs('512x gtceu:stone_dust')
        .inputFluids(Fluid.of('minecraft:water', 1000 * 128 * 2))
        .chancedOutput('128x gtceu:quartzite_gem', 2800, 300)
        .chancedOutput('128x gtceu:stone_dust', 1100, 100)
        .chancedOutput('512x gtceu:quartzite_dust', 3300, 0)
        .chancedOutput('512x gtceu:exquisite_nether_quartz_gem', 900, 100)
        .chancedOutput('512x gtceu:flawless_nether_quartz_gem', 1900, 150)
        .chancedOutput('512x minecraft:quartz', 6500, 500)
        .chancedOutput('512x gtceu:pure_nether_quartz_dust', 9500, 750)
        .duration(800)
        .EUt(GTValues.VA[GTValues.ZPM])

    // Nether quartz, silked (ore): the same at twice the scale.
    event.recipes.gtceu.ore_sifting_plant('kubejs:nether_quartz_silked')
        .itemInputs('128x #forge:ores/nether_quartz')
        .itemOutputs('1152x gtceu:stone_dust')
        .inputFluids(Fluid.of('minecraft:water', 1000 * 128 * 4))
        .chancedOutput('128x gtceu:quartzite_gem', 2800, 300)
        .chancedOutput('1024x gtceu:quartzite_dust', 3300, 100)
        .chancedOutput('1024x gtceu:exquisite_nether_quartz_gem', 900, 0)
        .chancedOutput('1024x gtceu:flawless_nether_quartz_gem', 1900, 100)
        .chancedOutput('1024x minecraft:quartz', 6500, 150)
        .chancedOutput('1024x gtceu:pure_nether_quartz_dust', 9500, 500)
        .duration(800)
        .EUt(GTValues.VA[GTValues.ZPM])
})
