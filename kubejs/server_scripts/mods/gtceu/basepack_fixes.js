// AF9 - Fixes for three broken base-pack (ATM9) GT recipes.
//
// GT 7.5 refuses a recipe that names an item the game does not have (the base pack was written for GT 7.2), and KubeJS shows
// each as a script error in game. The cure is in the base pack's own two files, which this repository does not hold: the
// instance's copies carry these edits (AF9's sync leaves them alone as "edited"; a fresh pack needs them once, by hand):
//
//  1. mods/gtceu/micro_universe_orb_recipes.js: the recipe gtceu:micro_universe_collector/max_energy_hatch outputs
//     '1x gtceu:max_energy_input_hatch' (GT's MAX hatch, only with high-tier content). The Micro Universe's own hatch is
//     gtceu:max_micro_universe_energy_input_hatch (startup_scripts/gtceu/micro_universe_orb.js): output that instead.
//  2. mods/gtceu/ore_processing_plant_recipes.js: the two ore sifting recipes kubejs:ore_sifting_plant/nether_quartz and
//     ..._silked output 'minecraft:nether_quartz' (not an item): 'minecraft:quartz' instead (the 2 occurrences).
//
// Nothing is added here: an earlier version of this file re-added the two sifting recipes under the same ids, and with the
// pack's files edited that was "Duplicate added recipe" (KubeJS cannot see the pack's recipes from here to skip them).
