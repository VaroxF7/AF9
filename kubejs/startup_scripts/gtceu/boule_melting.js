// AF9 - Boule Melting: the Electric Blast Furnace's second machine mode (AF9 Core adds it to GT's EBF, see
// af9-core BouleMelting). Spec: docs/semiconductor-factory.md
//
// Czochralski growth of the substrate boules, ten times the material of GT's boules: 10 melt charges (the dopants
// already blended in, so a boule fits the EBF's three input slots) + one seed crystal + one crucible, under a
// protective gas. Energy: 2x GT's EU/t for silicon, 4x phosphorus, 6x naquadah, 8x neutronium (as amps).
// Endion coils speed the mode up: Endion -25 % time and up to 2 parallels, Resonant Endion -50 % and up to 4.
//
// Endion: a heavy noble gas found only in the End's air; the centrifuge and the distillation tower separate it
// from Ender Air (server_scripts/mods/gtceu/boule_melting.js).

GTCEuStartupEvents.registry('gtceu:material', event => {
    event.create('endion')
        .gas()
        .color(0x9b6bff)
        .formula('Ed')

    // Tungstensteel and naquadah soaked in endion: the wire of the Endion coils
    event.create('endionite')
        .ingot()
        .fluid()
        .color(0x5e2a9e).secondaryColor(0x1a0b33)
        .iconSet(GTMaterialIconSet.SHINY)
        .flags(GTMaterialFlags.GENERATE_PLATE, GTMaterialFlags.GENERATE_FOIL, GTMaterialFlags.GENERATE_FINE_WIRE,
            GTMaterialFlags.GENERATE_ROD, GTMaterialFlags.GENERATE_FRAME)
        .blastTemp(5400, 'highest', GTValues.VA[GTValues.IV], 1200)
        .formula('(W2Fe2Nq)Ed')
})

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    // melt charges, seed crystal, crucible + protective gas -> boule
    event.create('boule_melting')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(3, 1, 1, 0)
        .setProgressBar(GuiTextures.PROGRESS_BAR_CRYSTALLIZATION, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.FURNACE)
})
