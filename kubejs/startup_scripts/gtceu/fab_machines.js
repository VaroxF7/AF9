// AF9 - the fab recipe types: the semiconductor chemistry of the plant. Spec: docs/semiconductor-factory.md §11.
// Recipes: server_scripts/mods/gtceu/fab_chemistry.js and photolithography.js.
//
// The types are modes of the normal GT machines (AF9 Core, com.af9.core.fab.FabModes): the chemistry of the
// Chemical Reactor and the Large Chemical Reactor, the separations of the Distillery and the Distillation Tower,
// the electrochemistry of the Electrolyzer and the Large Electrolysis Chamber, the heat of the Arc Furnace and the
// Electric Blast Furnace. GT's mode tab switches between them.
//
//   Chemical Reactor, Large Chemical Reactor   synthesis, blending, wet processing
//   Distillery                                fractionation, purification
//   Distillation Tower                        distillation, cryogenic rectification, fractionation, purification
//   Electrolyzer, Large Electrolysis Chamber   electrolysis, electrofluorination
//   Arc Furnace, Electric Blast Furnace        calcination, CVD, crystal growth
//
// The thermal modes carry the temperature they need, like GT's blast furnace recipes; the Arc Furnace has the
// heat of its tier (MV 1800 K, +900 K per tier), the Electric Blast Furnace GT's coil rules.
// The recipes of HV and above need a clean room, like every other clean-room recipe of the pack: the machine stands
// in a GT Cleanroom.

// Slot layouts [items in, items out, fluids in, fluids out]: the layout of the recipe type. A machine takes
// the slots of its biggest mode, so every mode has room for its inputs and outputs.
GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    const fabTypes = [
        ['fab_synthesis', [3, 2, 4, 3], GuiTextures.PROGRESS_BAR_ARROW_MULTIPLE, GTSoundEntries.CHEMICAL],
        ['fab_blending', [3, 2, 4, 3], GuiTextures.PROGRESS_BAR_MIXER, GTSoundEntries.MIXER],
        ['fab_wet_processing', [3, 2, 4, 3], GuiTextures.PROGRESS_BAR_BATH, GTSoundEntries.BATH],
        ['fab_distillation', [1, 1, 2, 6], GuiTextures.PROGRESS_BAR_ARROW_MULTIPLE, GTSoundEntries.CHEMICAL],
        ['fab_cryogenic_rectification', [1, 1, 2, 6], GuiTextures.PROGRESS_BAR_ARROW_MULTIPLE, GTSoundEntries.COOLING],
        ['fab_fractionation', [2, 1, 3, 2], GuiTextures.PROGRESS_BAR_ARROW_MULTIPLE, GTSoundEntries.BOILER],
        ['fab_purification', [2, 1, 3, 2], GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.CHEMICAL],
        ['fab_electrolysis', [2, 2, 3, 4], GuiTextures.PROGRESS_BAR_EXTRACT, GTSoundEntries.ELECTROLYZER],
        ['fab_electrofluorination', [2, 2, 3, 4], GuiTextures.PROGRESS_BAR_EXTRACT, GTSoundEntries.ELECTROLYZER],
        ['fab_calcination', [3, 2, 2, 2], GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.FURNACE],
        ['fab_cvd', [3, 2, 2, 2], GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.FURNACE],
        ['fab_crystal_growth', [3, 2, 2, 2], GuiTextures.PROGRESS_BAR_CRYSTALLIZATION, GTSoundEntries.FURNACE],
    ]
    fabTypes.forEach(([id, io, bar, sound]) => {
        event.create(id)
            .category('af9_fab')
            .setEUIO('in')
            .setMaxIOSize(io[0], io[1], io[2], io[3])
            .setProgressBar(bar, FillDirection.LEFT_TO_RIGHT)
            .setSound(sound)
    })
})
