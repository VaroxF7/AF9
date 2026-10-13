// AF9 - Atmospheric Simulation Room: a pressure room after GT's Cleanroom that holds a Lunar
// atmosphere anywhere. The controller in the ceiling centre houses the Atmospheric Simulator;
// XPS and NVM circuits build in its lunar air. Recipes: server_scripts/mods/gtceu/atmospheric_simulation.js
//
// 31 x 6 x 31 room (a step past the cleanroom's 15 max), aisles back -> front, rows bottom -> top.
// The middle 27 x 4 x 29 is open air (AF9 Core watches it for unsuited players); the controller sits
// ceiling-centre, a 1 x 2 door stands front-bottom-centre. The pattern is generated below (like the
// HILE's shape table, but in a loop: thirty identical slices).

const $AtmosphereRoomMachine = Java.loadClass('com.af9.core.machine.AtmosphereRoomMachine')

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    event.create('atmospheric_simulation')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(6, 1, 2, 0)
        .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.ASSEMBLER)
})

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }

    event.create('atmospheric_simulation_room', 'multiblock')
        .machine(holder => new $AtmosphereRoomMachine(holder))
        .langValue('Atmospheric Simulation Room')
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('atmospheric_simulation')])
        .recipeModifiers([GTRecipeModifiers.OC_PERFECT])
        .appearanceBlock(GTBlocks.CASING_STAINLESS_CLEAN)
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.atmospheric_simulation_room.tooltip', 4))
        .pattern(definition => {
            // 31 wide, 6 high, 31 deep: floor + ceiling solid, glass bands in the walls, hollow middle.
            // Col 15 is the centre (controller, door); cols 2-28 are the air.
            const W = 31
            const solid = 'X'.repeat(W)
            const glassBand = 'XG' + '#'.repeat(W - 4) + 'GX'
            const ring = 'X' + '#'.repeat(W - 2) + 'X'
            const centre = 'X'.repeat(15) + '#' + 'X'.repeat(15)
            const top = 'X'.repeat(15) + 'S' + 'X'.repeat(15)
            // one aisle (one slice back -> front): rows bottom -> top
            const back = [solid, glassBand, glassBand, glassBand, ring, solid]
            const middle = [solid, glassBand, glassBand, glassBand, ring, solid]
            const front = [solid, centre, centre, solid, solid, top]
            let pattern = FactoryBlockPattern.start()
            pattern = pattern.aisle(back)
            for (let i = 0; i < 29; i++) pattern = pattern.aisle(middle)
            pattern = pattern.aisle(front)
            return pattern
                .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count))
                .where('X', Predicates.blocks(GTBlocks.CASING_STAINLESS_CLEAN.get())
                    .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                    .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                    .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(1, 1))
                    .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(2, 1))
                    .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(1, 1))
                    .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
                .where('G', Predicates.blocks('gtceu:tempered_glass'))
                .where('#', Predicates.air())
                .build()
        })
        .workableCasingModel('gtceu:block/casings/solid/machine_casing_clean_stainless_steel',
            'gtceu:block/multiblock/assembly_line')
})
