// priority: -100
// AF9 - Fixes for three broken base-pack (ATM9) GT recipes.
//
// GT 7.5 refuses a recipe that names an item the game does not have (the base pack was written for GT 7.2), and KubeJS shows
// each as a script error in game. The cure is in the base pack's own two files, which this repository does not hold, so the
// instance copies carry these edits (AF9's sync leaves them alone as "edited"; a fresh pack needs them once):
//
//  1. mods/gtceu/micro_universe_orb_recipes.js: the recipe gtceu:micro_universe_collector/max_energy_hatch outputs
//     '1x gtceu:max_energy_input_hatch' (GT's MAX hatch, only with high-tier content). The Micro Universe's own hatch is
//     gtceu:max_micro_universe_energy_input_hatch (startup_scripts/gtceu/micro_universe_orb.js): output that instead.
//  2. mods/gtceu/ore_processing_plant_recipes.js: the two ore sifting recipes kubejs:ore_sifting_plant/nether_quartz and
//     ..._silked output 'minecraft:nether_quartz' (not an item): 'minecraft:quartz' instead (the 2 occurrences).
//
// Where a pack still has the unedited files, this script (it runs after them) adds the two sifting recipes with minecraft:quartz;
// the MAX hatch recipe has nothing to add (it only exists once the file is edited). Where the files are edited it does nothing.

ServerEvents.recipes(event => {

    // ---- 2-3. Nether quartz sifting: corrected (minecraft:quartz) ----
    // Added only where the base pack's own two recipes did not build (unedited files).
    if (event.countRecipes({ id: 'kubejs:ore_sifting_plant/nether_quartz' }) > 0) return

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
