// This File has been authored by AllTheMods Staff, or a Community contributor for use in AllTheMods - AllTheMods 9.
// As all AllTheMods packs are licensed under All Rights Reserved, this file is not allowed to be used in any public packs not released by the AllTheMods Team, without explicit permission.

ServerEvents.recipes(allthemods => {
    //////////////// machine controller ////////////////
    allthemods.shaped('gtceu:greenhouse', ['AWA', 'ASA', 'WAW'],
        {
            A: '#gtceu:circuits/mv',
            W: 'gtceu:copper_single_cable',
            S: 'gtceu:solid_machine_casing'
        }).id('gtceu:shaped/greenhouse')

    //////////////// greenhouse recipes ////////////////

    function grow(id, input, output, duration, boostItem, boosted) {
        if (boosted) {
            allthemods.recipes.gtceu.greenhouse(id)
                .circuit(2)
                .notConsumable(InputItem.of(input))
                .itemInputs(boostItem)
                .inputFluids(Fluid.of('minecraft:water', 1000))
                .itemOutputs(output)
                .duration(duration / 2)
                .EUt(MV)
        } else {
            allthemods.recipes.gtceu.greenhouse(id)
                .circuit(1)
                .notConsumable(InputItem.of(input))
                .inputFluids(Fluid.of('minecraft:water', 1000))
                .itemOutputs(output)
                .duration(duration)
                .EUt(MV)
        }
    }

    function grow_max(id, input, output, duration, boostItem, boosted, scale) {
        if (boosted) {
            allthemods.recipes.gtceu.greenhouse(id)
                .circuit(4)
                .notConsumable(InputItem.of(input))
                .itemInputs(boostItem)
                .inputFluids(Fluid.of('minecraft:water', 1000 * scale))
                .itemOutputs(output)
                .duration(duration / 2)
                .EUt(UHV)
        } else {
            allthemods.recipes.gtceu.greenhouse(id)
                .circuit(3)
                .notConsumable(InputItem.of(input))
                .inputFluids(Fluid.of('minecraft:water', 1000 * scale))
                .itemOutputs(output)
                .duration(duration)
                .EUt(UHV)
        }
    }

    // AF9: the Greenhouse only grows trees. The base pack gives it a recipe for every Botany Pots crop and every Mystical
    // Agriculture seed; crops and seeds are the Agriculture Simulation Bed's now (agri_sim.js). This override of the base
    // pack's greenhouse_recipes.js (the sync replaces the pack's copy, the original goes to the backup folder) keeps the
    // controller recipe, the rubber sapling and the Botany Pots recipes whose seed is a sapling or propagule; the
    // Mystical Agriculture seeds, the other crops and flowers are left out.
    const trees = /sapling|propagule/
    let treeRecipes = 0
    let skippedRecipes = 0
    allthemods.forEachRecipe({ type: 'botanypots:crop' }, rawRecipe => {
        let recipe = JSON.parse(rawRecipe.json)
        let duration = recipe.growthTicks
        let drops = recipe.drops // array of objects { chance, output { item }, minRolls, maxRolls }
        let seed = recipe.seed
        if (!seed.hasOwnProperty('item')) { skippedRecipes++; return } // pitcher plant, chorus, torchflower
        let input = recipe.seed.item
        let affix = input.split(':')
        if (!trees.test(input)) { skippedRecipes++; return }
        if ((rawRecipe.getId().split(':')[0].includes('botanytrees') && input.includes('croptopia')) || rawRecipe.getId().split(':')[0].includes('supplementaries')) {
            // duplicate recipes
            return
        }
        var recipeMAXBuilder = allthemods.recipes.gtceu.greenhouse('kubejs:gtceu/greenhouse/' + affix[0] + "/" + affix[1] + "_max")
        var recipeBuilder = allthemods.recipes.gtceu.greenhouse('kubejs:gtceu/greenhouse/' + affix[0] + "/" + affix[1])
        recipeBuilder.EUt(MV)
            .notConsumable(InputItem.of(input))
            .inputFluids(Fluid.of('minecraft:water', 1000))
            .duration(duration / 4)
            .circuit(0)
        recipeMAXBuilder.EUt(UHV)
            .notConsumable(InputItem.of(input))
            .inputFluids(Fluid.of('minecraft:water', 1000 * 1310720))
            .duration(duration / 4 * 1024 / 15)
            .circuit(5)
        drops.forEach((drop) => {
            if (drop.hasOwnProperty('maxRolls')) {
                recipeBuilder.chancedOutput(Item.of(drop.output.item, drop.maxRolls), drop.chance * 10000, drop.chance * 10000)
                recipeMAXBuilder.itemOutputs(Item.of(drop.output.item, drop.maxRolls * 1310720))
            } else {
                recipeBuilder.chancedOutput(drop.output.item, drop.chance * 10000, drop.chance * 10000)
                recipeMAXBuilder.itemOutputs(Item.of(drop.output.item, 1310720))
            }
        })
        treeRecipes++
    })
    console.info('greenhouse_recipes.js (AF9 override): ' + treeRecipes + ' Botany Pots tree recipes added, ' + skippedRecipes + ' crop recipes left out, Mystical Agriculture seeds left out, rubber sapling kept')

    //////////////// GT Rubber trees ////////////////
    let scale = 163840
    grow('rubber_sapling', 'gtceu:rubber_sapling', ['32x gtceu:rubber_log', '8x gtceu:sticky_resin', '4x gtceu:rubber_sapling'], 9600.0 / 2, 'minecraft:air', false)
    grow('rubber_sapling_boosted', 'gtceu:rubber_sapling', ['64x gtceu:rubber_log', '16x gtceu:sticky_resin', '4x gtceu:rubber_sapling'], 9600.0 / 2, '4x gtceu:fertilizer', true)
    grow_max('rubber_sapling_max', 'gtceu:rubber_sapling', [Item.of('gtceu:rubber_log', 32 * scale), Item.of('gtceu:sticky_resin', 8 * scale), Item.of('gtceu:rubber_sapling', 2 * scale)], 9600.0 / 2 * 1024 / 120, 'minecraft:air', false, scale)
    grow_max('rubber_sapling_max_boosted', 'gtceu:rubber_sapling', [Item.of('gtceu:rubber_log', 64 * scale), Item.of('gtceu:sticky_resin', 16 * scale), Item.of('gtceu:rubber_sapling', 4 * scale)], 9600.0 / 2 * 1024 / 120, Item.of('gtceu:fertilizer', 4 * scale), true, scale)
})

// This File has been authored by AllTheMods Staff, or a Community contributor for use in AllTheMods - AllTheMods 9.
// As all AllTheMods packs are licensed under All Rights Reserved, this file is not allowed to be used in any public packs not released by the AllTheMods Team, without explicit permission.
