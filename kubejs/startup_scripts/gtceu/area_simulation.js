// AF9 - Area Simulation Chambers (ASC): renewable ores before the Space Elevator. Spec: docs/area-simulation.md.
// Recipes: server_scripts/mods/gtceu/area_simulation.js.
//
// A tiered single block (HV-LuV, perfect overclocks, no parallel hatch). It grows raw ore on Anode Rods out of
// three gases: hydrogen, carbon dioxide and lithium gas. Seven areas on a programmed circuit (1-7): Overworld,
// Nether, End, Moon, Mars, Venus and Asteroids. Harder areas take more gas, more rods and a higher voltage.
// No drones, no data sticks: set the circuit and feed it.

const $ASCMachine = Java.loadClass('com.gregtechceu.gtceu.api.machine.SimpleTieredMachine')

GTCEuStartupEvents.registry('gtceu:material', event => {
    event.create('lithium_gas')
        .gas()
        .color(0xc9b8ff)
        .formula('Li')
})

StartupEvents.registry('item', event => {
    event.create('anode_rod')
        .displayName('Anode Rod')
        .tooltip('§7A copper-jacketed carbon rod: ore condenses on it out of the simulation gases.')
        .tooltip('§7Used up by the Area Simulation Chamber.')
    // Area Data, one per area: kept, picks the area (a programmed circuit picks the ore in it)
    const areas = [
        ['overworld', 'Overworld'],
        ['nether', 'Nether'],
        ['end', 'End'],
        ['moon', 'Moon'],
        ['mars', 'Mars'],
        ['venus', 'Venus'],
        ['asteroids', 'Asteroids']
    ]
    areas.forEach(([id, name]) => {
        event.create(`area_data_${id}`)
            .displayName(`Area Data: ${name}`)
            .tooltip('§7Kept: picks the area in an Area Simulation Chamber.')
    })
})

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    event.create('area_simulation')
        .category('af9_simulation')
        .setEUIO('in')
        .setMaxIOSize(3, 3, 3, 0)
        .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.ELECTROLYZER)
})

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }

    // Single blocks, HV-LuV. Crafted from GT's electrolyzer of the same tier (server area_simulation.js).
    event.create('area_simulation_chamber', 'custom')
        .tiers(GTValues.HV, GTValues.EV, GTValues.IV, GTValues.LuV)
        .tankScalingFunction(tier => 16000)
        .machine((holder, tier, tankScaling) => new $ASCMachine(holder, tier, tankScaling))
        .definition((tier, builder) => {
            builder
                .langValue(`${GTValues.VLVH[tier]} Area Simulation Chamber ${GTValues.VLVT[tier]}`)
                .recipeTypes([GTRecipeTypes.get('area_simulation')])
                .recipeModifiers([GTRecipeModifiers.OC_PERFECT])
                .editableUI($ASCMachine.EDITABLE_UI_CREATOR.apply(GTCEu.id('area_simulation_chamber'),
                    GTRecipeTypes.get('area_simulation')))
                .workableTieredHullModel(GTCEu.id('block/machines/electrolyzer'))
                ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.area_simulation_chamber.tooltip', 4))
        })
})
