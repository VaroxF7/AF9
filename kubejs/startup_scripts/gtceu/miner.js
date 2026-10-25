// This File has been authored by AllTheMods Staff, or a Community contributor for use in AllTheMods - AllTheMods 9.
// As all AllTheMods packs are licensed under All Rights Reserved, this file is not allowed to be used in any public packs not released by the AllTheMods Team, without explicit permission.
// AF9 - overrides ATM9's startup_scripts/gtceu/miner.js (same path, so the sync replaces it). Only the Void Miner (MK1)
// multiblock and its recipe type stay; the Dimension Scanner machines, their world_data_scanner recipe type and the
// kubejs:overworld_data / nether_data / end_data items that ATM9 registered here are gone (not registered at all now).
// Recipes: server_scripts/mods/gtceu/miner.js. MK2 / MK3 and the mode types: startup_scripts/gtceu/void_mining.js.

GTCEuStartupEvents.registry("gtceu:machine", allthemods => {

    allthemods.create("void_miner", "multiblock")
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeType("void_miner")
        .recipeModifier(GTRecipeModifiers.OC_NON_PERFECT)
        .appearanceBlock(GTBlocks.CASING_TITANIUM_STABLE)
        .pattern((definition) =>
            FactoryBlockPattern.start()
                .aisle("XXX", "#F#", "#F#", "#F#", "###", "###", "###")
                .aisle("XXX", "FCF", "FCF", "FCF", "#F#", "#F#", "#F#")
                .aisle("XSX", "#F#", "#F#", "#F#", "###", "###", "###")
                .where("S", Predicates.controller(Predicates.blocks(definition.get())))
                .where("X", Predicates.blocks("gtceu:stable_machine_casing")
                    .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                    .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1)))
                .where("C", Predicates.blocks("gtceu:stable_machine_casing"))
                .where("F", Predicates.blocks("gtceu:titanium_frame"))
                .where("#", Predicates.any())
                .build()
        )
        .workableCasingModel("gtceu:block/casings/solid/machine_casing_stable_titanium", "gtceu:block/multiblock/large_miner")

})

GTCEuStartupEvents.registry("gtceu:recipe_type", allthemods => {

    allthemods.create("void_miner")
        .setEUIO("in")
        .setSlotOverlay(false, false, GuiTextures.SOLIDIFIER_OVERLAY)
        .setMaxIOSize(2, 4, 2, 0)
        .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.SCIENCE)

})
