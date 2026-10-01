// AF9 - Asteroid fission (materials, items, machine: startup_scripts/gtceu/asteroid_fission.js). Spec: docs/asteroid-fission.md
//
// 1. Veins:   Brannerite (the uranium ore) and three metal ores in the asteroids of af9:asteroid_field; GT's old uranium
//             veins (pitchblende, uraninite) switched off
// 2. Uranium: ore -> leach -> yellowcake -> UF6 (GT enriches it) -> pellets -> fuel rods
// 3. Reactor: FX-1 Reactor: rod + water + NaK -> spent rod + supercritical steam + hot NaK; the Vacuum Freezer cools the
//             alloy; GT's Large Steam Turbines (and Extreme Reactors') turn the steam into power
// 4. Plutonium: spent rod -> macerator (cladding off) -> nitric acid -> centrifuge: plutonium, uranium back, fission
//             products. GT's two shortcuts to plutonium (U-238 and Pu-239 centrifuging) are gone.

// ---- 1. Veins ----
const $AsteroidChemicalHelper = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper')
const $AsteroidTagPrefix = Java.loadClass('com.gregtechceu.gtceu.api.data.tag.TagPrefix')
GTCEuServerEvents.oreVeins(event => {
    // The veins of the asteroid layer; each 3 x 3 chunks of the field hold one of them (weights). Ore only grows where
    // there is rock, and an asteroid is mostly empty space around it, so the clusters are big and exposed to the
    // void (no air discard): the ore shows on the surface of the rocks.
    const asteroidVein = (id, weight, size, density, material) => {
        event.add(id, builder => {
            builder.clusterSize(size)
                .weight(weight)
                .density(density)
                .discardChanceOnAirExposure(0.0)
                .layer('af9_asteroid')
                .dimensions('af9:asteroid_field')
                .heightRangeUniform(0, 110)
                // withBlock and withNetherBlock, not withMaterial: GT 7.2.0 sends the veins to every joining player with a
                // codec that reads the block, deep and nether block fields, and withMaterial leaves them empty ("Invalid
                // player data", no world loads). withBlock sets the first two.
                .standardVeinGenerator(generator => generator
                    .withBlock(() => $AsteroidChemicalHelper.getBlock($AsteroidTagPrefix.ore, GTMaterials.get(material)))
                    .withNetherBlock(() => $AsteroidChemicalHelper.getBlock($AsteroidTagPrefix.ore, GTMaterials.get(material))))
        })
    }
    asteroidVein('af9:asteroid_brannerite_vein', 60, 120, 0.55, 'brannerite')
    asteroidVein('af9:asteroid_pentlandite_vein', 20, 100, 0.5, 'pentlandite')
    asteroidVein('af9:asteroid_magnetite_vein', 15, 100, 0.5, 'magnetite')
    asteroidVein('af9:asteroid_cooperite_vein', 10, 90, 0.45, 'cooperite')

    // The old ways to uranium: the veins with pitchblende or uraninite are switched off (weight 0), wherever the pack put
    // them
    const uranium = ['pitchblende', 'uraninite']
    event.modifyAll((id, vein) => {
        if (String(id).startsWith('af9:')) return
        let isUranium = false
        try {
            vein.veinGenerator().getAllMaterials().forEach(material => {
                if (uranium.includes(String(material.getName()))) isUranium = true
            })
        } catch (error) {
            // a generator without materials has no uranium in it
        }
        if (isUranium) vein.weight(0)
    })
})

// ---- Tags ----
ServerEvents.tags('item', event => {
    // what RadiationWatch (af9-core) warns about besides GT's radioactive materials
    event.add('af9:radioactive', ['kubejs:fx_fuel_pellet', 'kubejs:fx_fuel_rod', 'kubejs:fx_spent_fuel_rod'])
})
ServerEvents.tags('fluid', event => {
    // Extreme Reactors' turbines take it as a vapor (af9-core ExtremeReactorsCompat)
    event.add('forge:supercritical_steam', 'gtceu:supercritical_steam')
})

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const V = GTValues.V

    // ---- 1b. The shortcuts to plutonium ----
    // GT: U-238 dust gives a little tiny plutonium (and tiny U-235), Pu-239 dust gives Pu-241. Both closed: the plutonium
    // comes from the reactor. The U-235 part stays.
    event.remove({ id: 'gtceu:centrifuge/uranium_238_separation' })
    event.remove({ id: 'gtceu:centrifuge/plutonium_239_separation' })
    event.recipes.gtceu.centrifuge('af9:uranium_238_separation')
        .itemInputs('gtceu:uranium_dust')
        .chancedOutput('gtceu:tiny_uranium_235_dust', 2300, 0)
        .duration(800)
        .EUt(320)

    // ---- 2. Uranium ----
    // The leach: sulfuric acid takes the uranium out of the purified ore (the titanium stays behind as rutile, thorium
    // too in small amounts). Two dust of ore per two ore dust: 1000 mB of solution.
    event.recipes.gtceu.chemical_reactor('af9:brannerite_leach')
        .itemInputs('2x gtceu:brannerite_dust')
        .inputFluids(Fluid.of('gtceu:sulfuric_acid', 2000))
        .itemOutputs('gtceu:rutile_dust')
        .chancedOutput('gtceu:thorium_dust', 1500, 0)
        .outputFluids(Fluid.of('gtceu:uranyl_sulfate_solution', 1000))
        .duration(240)
        .EUt(VA[GTValues.MV])
    // Ammonia precipitates it: yellowcake, and the acid comes back diluted
    event.recipes.gtceu.chemical_reactor('af9:yellowcake_precipitation')
        .inputFluids(Fluid.of('gtceu:uranyl_sulfate_solution', 1000))
        .inputFluids(Fluid.of('gtceu:ammonia', 1000))
        .itemOutputs('3x gtceu:yellowcake_dust')
        .outputFluids(Fluid.of('gtceu:diluted_sulfuric_acid', 1000))
        .duration(200)
        .EUt(VA[GTValues.MV])
    // Yellowcake to uranium hexafluoride: GT's own enrichment (centrifuge, electrolyzer) takes it from there to U-235
    // and U-238 dust (GT: 3 uraninite, the same fluids)
    event.recipes.gtceu.chemical_reactor('af9:uranium_hexafluoride_from_yellowcake')
        .itemInputs('3x gtceu:yellowcake_dust')
        .inputFluids(Fluid.of('gtceu:hydrofluoric_acid', 4000))
        .inputFluids(Fluid.of('gtceu:fluorine', 2000))
        .outputFluids(Fluid.of('gtceu:uranium_hexafluoride', 1000))
        .outputFluids(Fluid.of('gtceu:water', 2000))
        .duration(200)
        .EUt(VA[GTValues.MV])

    // Pellets: natural uranium oxide with a little U-235 to start the chain reaction, sintered in oxygen (Kanthal coils)
    event.recipes.gtceu.electric_blast_furnace('af9:fx_fuel_pellets')
        .itemInputs('12x gtceu:uranium_dust', '4x gtceu:tiny_uranium_235_dust')
        .inputFluids(Fluid.of('gtceu:oxygen', 8000))
        .itemOutputs('4x kubejs:fx_fuel_pellet')
        .blastFurnaceTemp(1800)
        .duration(400)
        .EUt(VA[GTValues.HV])
    // Rods: four pellets in a zirconium cladding (zircon chain, electronics_metallurgy.js)
    event.recipes.gtceu.assembler('af9:fx_fuel_rod')
        .itemInputs('4x kubejs:fx_fuel_pellet', 'gtceu:zirconium_ingot')
        .itemOutputs('kubejs:fx_fuel_rod')
        .duration(200)
        .EUt(VA[GTValues.HV])

    // ---- 3. The reactor ----
    // One rod per 1,200 ticks (a minute); 61,440 mB of supercritical steam (51 mB/t, 4,096 EU/t in four Large Steam
    // Turbines at full rotor power) for 640 mB of water, EV: 1,920 EU/t. The coolant goes through the core and comes out
    // hot, in equal amounts. Overclocking (IV hatches) runs it faster.
    event.recipes.gtceu.fx1_reactor('af9:fx1_fuel_cycle')
        .itemInputs('kubejs:fx_fuel_rod')
        .inputFluids(Fluid.of('gtceu:distilled_water', 640))
        .inputFluids(Fluid.of('gtceu:sodium_potassium', 1000))
        .itemOutputs('kubejs:fx_spent_fuel_rod')
        .outputFluids(Fluid.of('gtceu:supercritical_steam', 61440))
        .outputFluids(Fluid.of('gtceu:hot_sodium_potassium', 1000))
        .duration(1200)
        .EUt(VA[GTValues.EV])
    // The Vacuum Freezer gives the heat to space (well, to its coolant loop): hot NaK back to NaK
    event.recipes.gtceu.vacuum_freezer('af9:cool_hot_sodium_potassium')
        .inputFluids(Fluid.of('gtceu:hot_sodium_potassium', 1000))
        .outputFluids(Fluid.of('gtceu:sodium_potassium', 1000))
        .duration(100)
        .EUt(VA[GTValues.HV])
    // Power: GT's steam turbine recipe type, so every steam turbine of GT runs it (the Large Steam Turbine from HV: it
    // needs the recipe's 512 EU/t below its own maximum). 80 EU per mB (steam: 0.5). A trickle of water comes out.
    event.recipes.gtceu.steam_turbine('af9:supercritical_steam')
        .inputFluids(Fluid.of('gtceu:supercritical_steam', 128))
        .outputFluids(Fluid.of('gtceu:distilled_water', 1))
        .duration(20)
        .EUt(-V[GTValues.HV])

    // ---- 4. Plutonium ----
    // The cladding comes off (the macerator: zirconium back), the fuel dissolves in nitric acid, the centrifuge splits
    // plutonium (a part of it Pu-241), uranium (recycled into new pellets) and fission products off, and the acid is
    // recovered.
    event.recipes.gtceu.macerator('af9:decladding_spent_fuel_rod')
        .itemInputs('kubejs:fx_spent_fuel_rod')
        .itemOutputs('4x gtceu:irradiated_fuel_dust', 'gtceu:zirconium_dust')
        .duration(200)
        .EUt(VA[GTValues.HV])
    event.recipes.gtceu.chemical_reactor('af9:dissolve_irradiated_fuel')
        .itemInputs('4x gtceu:irradiated_fuel_dust')
        .inputFluids(Fluid.of('gtceu:nitric_acid', 3000))
        .outputFluids(Fluid.of('gtceu:spent_fuel_solution', 3000))
        .duration(300)
        .EUt(VA[GTValues.HV])
    event.recipes.gtceu.centrifuge('af9:separate_spent_fuel')
        .inputFluids(Fluid.of('gtceu:spent_fuel_solution', 3000))
        .itemOutputs('gtceu:plutonium_dust', '8x gtceu:uranium_dust')
        .chancedOutput('gtceu:plutonium_241_dust', 3000, 0)
        .chancedOutput('2x gtceu:tiny_uranium_235_dust', 6000, 0)
        .chancedOutput('gtceu:neodymium_dust', 1500, 0)
        .chancedOutput('gtceu:molybdenum_dust', 1500, 0)
        .outputFluids(Fluid.of('gtceu:nitric_acid', 2000))
        .duration(600)
        .EUt(VA[GTValues.EV])
})
