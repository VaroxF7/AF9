// AF9 - Spooling: the recipe type of the Wiremill's second mode. 64 fine wires (or a wire and a reel) wound into a
// spool; recipes in server_scripts/mods/gtceu/wire_spools.js and circuits_af9.js. AF9 Core (SpoolingMode) adds the
// type to every Wiremill as a mode, switched with GT's mode tab.
GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    event.create('spooling')
        .setEUIO('in')
        .setMaxIOSize(2, 1, 0, 0)
        .setProgressBar(GuiTextures.PROGRESS_BAR_WIREMILL, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.MOTOR)
})
