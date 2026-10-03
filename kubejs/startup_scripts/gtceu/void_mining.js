// AF9 - Void Miner modes: the miner's four areas as recipe types (the machine itself stays GT's: VoidMinerMachine
// takes it over in af9-core). Recipes: server_scripts/mods/gtceu/miner.js. Modes 1-4: Overworld, Nether, End,
// Asteroids; a programmed circuit picks the ore in the area. Drilling fluid in, tenfold raw ore out.

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    const types = [
        'void_mining_overworld',
        'void_mining_nether',
        'void_mining_end',
        'void_mining_asteroids'
    ]
    types.forEach(id => {
        event.create(id)
            .category('multiblock')
            .setEUIO('in')
            .setMaxIOSize(1, 4, 1, 0)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
            .setSound(GTSoundEntries.CHEMICAL)
    })
})
