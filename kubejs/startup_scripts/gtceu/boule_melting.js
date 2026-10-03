// AF9 - Boule Melting: the Electric Blast Furnace's second machine mode (AF9 Core adds it to GT's EBF, see
// af9-core BouleMelting). Spec: docs/semiconductor-factory.md
//
// Czochralski growth of the substrate boules, ten times the material of GT's boules: 10 melt charges (the dopants
// already blended in, so a boule fits the EBF's three input slots) + one seed crystal + one crucible, under a
// protective gas. Energy: 2x GT's EU/t for silicon, 4x phosphorus, 6x naquadah, 8x neutronium (as amps).
// Endion coils speed the mode up: Endion -25 % time and up to 2 parallels, Resonant Endion -50 % and up to 4.
//
// Endion: a heavy noble gas found only in the End's air; the centrifuge and the distillation tower separate it
// from Ender Air (server_scripts/mods/gtceu/boule_melting.js). The materials, the coils and the items: AF9 Core
// (com.af9.core.registry).

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    // melt charges, seed crystal, crucible + protective gas -> boule
    event.create('boule_melting')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(3, 1, 1, 0)
        .setProgressBar(GuiTextures.PROGRESS_BAR_CRYSTALLIZATION, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.FURNACE)
})
