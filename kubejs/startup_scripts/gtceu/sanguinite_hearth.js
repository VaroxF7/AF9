// AF9 - Sanguinite Hearth Furnace: the standalone smelter of the bright-red UHV superconductor.
// Spec: docs/uhv-superconductor.md. Recipes: server_scripts/mods/gtceu/uhv_superconductor.js.
// Controller behaviour: AF9 Core (af9-core/, SanguiniteHearthMachine).
//
// A 1:1 copy of GT's Rotary Hearth Furnace (gtceu:mega_blast_furnace) with one decisive difference: it runs only
// its own recipe type (gtceu:sanguinite_hearth), never the EBF family. The EBF cannot smelt sanguinite at all:
// the hot-ingot print lives on the hearth's type and the material's auto EBF recipe is removed. The structure
// stays identical for now (a later pass gives the hearth its own look); the new gameplay is thermal:
//   - the hearth is a heat mass: it preheats toward its coils' maximum (coil temperature + 100 K per energy hatch
//     tier above MV, the EBF's own display maths) over a minute while switched on and powered, and cools over
//     four minutes without power or while switched off. Prints only start preheated (HEARTH_GATE: 13000 K), so a
//     cold hearth makes you wait once, then an enabled, powered hearth holds its heat on a trickle and back-to-back
//     smelts start at once. Breaking the structure vents it back to room temperature.
//   - automate it by keeping it switched on under power (an ME level emitter on the crude stock, or a clock) and
//     feeding it through the buses: a parallel hatch multiplies the 14-ingot prints, batch mode folds overclocked
//     runs. Resonant Endion coils (12600 K) with UV hatches reach 13100 K: just past the smelt.

const $SanguiniteHearthMachine = Java.loadClass('com.af9.core.machine.SanguiniteHearthMachine')
const $HearthDirection = Java.loadClass('com.gregtechceu.gtceu.api.pattern.util.RelativeDirection')

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    // 14 crude sanguinite + circuit 10 in, supercooled endion in, 14 hot ingots out. The temperature rides along
    // as the recipe's blastFurnaceTemp (the CoilWorkable gate reads it); the hearth's own heat is HEARTH_GATE.
    event.create('sanguinite_hearth')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(2, 1, 1, 0)
        .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.FURNACE)
})

GTCEuStartupEvents.registry('gtceu:machine', event => {
    // Loaded here, not at the top: forcing the casing holder's class init while KubeJS itself is still
    // constructing (before GT's materials exist) kills GTBlocks' own init with an NPE. In this callback
    // the registries are up. (The GCYM casings live in GTBlocks' sister registry: GTBlocks has no
    // high-temperature smelting casing.)
    const $HearthCasings = Java.loadClass('com.gregtechceu.gtceu.common.data.GCYMBlocks')
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }

    // The Rotary Hearth Furnace's structure, unchanged (GT's mega_blast_furnace pattern, same axes): 13 slices
    // front -> back, 17 rows bottom -> top, 13 columns. Slices mirror at the middle one (the 7th). The controller
    // sits in the last slice's second row, like GT's.
    event.create('sanguinite_hearth_furnace', 'multiblock')
        .machine(holder => new $SanguiniteHearthMachine(holder))
        .langValue('Sanguinite Hearth Furnace')
        .rotationState(RotationState.ALL)
        .recipeTypes([GTRecipeTypes.get('sanguinite_hearth')])
        // HEARTH_GATE: only preheated (13000 K) with the recipe's full EU/t; a parallel hatch multiplies the
        // 14-ingot prints; perfect overclocks above that; then batch mode. EBF recipes never run here (own type).
        .recipeModifiers([$SanguiniteHearthMachine.HEARTH_GATE, GTRecipeModifiers.PARALLEL_HATCH,
            GTRecipeModifiers.OC_PERFECT, GTRecipeModifiers.BATCH_MODE])
        .appearanceBlock($HearthCasings.CASING_HIGH_TEMPERATURE_SMELTING)
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.sanguinite_hearth_furnace.tooltip', 9))
        .pattern(definition => FactoryBlockPattern.start($HearthDirection.FRONT, $HearthDirection.UP,
            $HearthDirection.RIGHT)
            .aisle('##XXXXXXXXX##', '##XXXXXXXXX##', '#############', '#############', '#############',
                '#############', '#############', '#############', '#############', '#############',
                '#############', '#############', '#############', '#############', '#############',
                '#############', '#############')
            .aisle('#XXXXXXXXXXX#', '#XXXXXXXXXXX#', '###F#####F###', '###F#####F###', '###FFFFFFF###',
                '#############', '#############', '#############', '#############', '#############',
                '####FFFFF####', '#############', '#############', '#############', '#############',
                '#############', '#############')
            .aisle('XXXXXXXXXXXXX', 'XXXXVVVVVXXXX', '##F#######F##', '##F#######F##', '##FFFHHHFFF##',
                '##F#######F##', '##F#######F##', '##F#######F##', '##F#######F##', '##F#######F##',
                '##FFFHHHFFF##', '#############', '#############', '#############', '#############',
                '#############', '###TTTTTTT###')
            .aisle('XXXXXXXXXXXXX', 'XXXXXXXXXXXXX', '#F####P####F#', '#F####P####F#', '#FFHHHPHHHFF#',
                '######P######', '######P######', '######P######', '######P######', '######P######',
                '##FHHHPHHHF##', '######P######', '######P######', '######P######', '######P######',
                '######P######', '##TTTTPTTTT##')
            .aisle('XXXXXXXXXXXXX', 'XXVXXXXXXXVXX', '####BBPBB####', '####TITIT####', '#FFHHHHHHHFF#',
                '####BITIB####', '####CCCCC####', '####CCCCC####', '####CCCCC####', '####BITIB####',
                '#FFHHHHHHHFF#', '####BITIB####', '####CCCCC####', '####CCCCC####', '####CCCCC####',
                '####BITIB####', '##TTTTPTTTT##')
            .aisle('XXXXXXXXXXXXX', 'XXVXXXXXXXVXX', '####BAAAB####', '####IAAAI####', '#FHHHAAAHHHF#',
                '####IAAAI####', '####CAAAC####', '####CAAAC####', '####CAAAC####', '####IAAAI####',
                '#FHHHAAAHHHF#', '####IAAAI####', '####CAAAC####', '####CAAAC####', '####CAAAC####',
                '####IAAAI####', '##TTTTPTTTT##')
            .aisle('XXXXXXXXXXXXX', 'XXVXXXXXXXVXX', '###PPAAAPP###', '###PTAAATP###', '#FHPHAAAHPHF#',
                '###PTAAATP###', '###PCAAACP###', '###PCAAACP###', '###PCAAACP###', '###PTAAATP###',
                '#FHPHAAAHPHF#', '###PTAAATP###', '###PCAAACP###', '###PCAAACP###', '###PCAAACP###',
                '###PTAAATP###', '##TPPPMPPPT##')
            .aisle('XXXXXXXXXXXXX', 'XXVXXXXXXXVXX', '####BAAAB####', '####IAAAI####', '#FHHHAAAHHHF#',
                '####IAAAI####', '####CAAAC####', '####CAAAC####', '####CAAAC####', '####IAAAI####',
                '#FHHHAAAHHHF#', '####IAAAI####', '####CAAAC####', '####CAAAC####', '####CAAAC####',
                '####IAAAI####', '##TTTTPTTTT##')
            .aisle('XXXXXXXXXXXXX', 'XXVXXXXXXXVXX', '####BBPBB####', '####TITIT####', '#FFHHHHHHHFF#',
                '####BITIB####', '####CCCCC####', '####CCCCC####', '####CCCCC####', '####BITIB####',
                '#FFHHHHHHHFF#', '####BITIB####', '####CCCCC####', '####CCCCC####', '####CCCCC####',
                '####BITIB####', '##TTTTPTTTT##')
            .aisle('XXXXXXXXXXXXX', 'XXXXXXXXXXXXX', '#F####P####F#', '#F####P####F#', '#FFHHHPHHHFF#',
                '######P######', '######P######', '######P######', '######P######', '######P######',
                '##FHHHPHHHF##', '######P######', '######P######', '######P######', '######P######',
                '######P######', '##TTTTPTTTT##')
            .aisle('XXXXXXXXXXXXX', 'XXXXVVVVVXXXX', '##F#######F##', '##F#######F##', '##FFFHHHFFF##',
                '##F#######F##', '##F#######F##', '##F#######F##', '##F#######F##', '##F#######F##',
                '##FFFHHHFFF##', '#############', '#############', '#############', '#############',
                '#############', '###TTTTTTT###')
            .aisle('#XXXXXXXXXXX#', '#XXXXXXXXXXX#', '###F#####F###', '###F#####F###', '###FFFFFFF###',
                '#############', '#############', '#############', '#############', '#############',
                '####FFFFF####', '#############', '#############', '#############', '#############',
                '#############', '#############')
            .aisle('##XXXXXXXXX##', '##XXXXSXXXX##', '#############', '#############', '#############',
                '#############', '#############', '#############', '#############', '#############',
                '#############', '#############', '#############', '#############', '#############',
                '#############', '#############')
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            // hatches and buses go on any high-temperature smelting casing (maximums only, the AF9 convention;
            // GT's 360-casing minimum is dropped)
            .where('X', Predicates.blocks($HearthCasings.CASING_HIGH_TEMPERATURE_SMELTING.get())
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(4, 2))
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(4, 1))
                .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
            .where('C', Predicates.heatingCoils())
            .where('M', Predicates.abilities(PartAbility.MUFFLER))
            .where('F', Predicates.blocks('gtceu:naquadah_alloy_frame'))
            .where('H', Predicates.blocks($HearthCasings.CASING_HIGH_TEMPERATURE_SMELTING.get()))
            .where('T', Predicates.blocks('gtceu:robust_machine_casing'))
            .where('B', Predicates.blocks(GTBlocks.FIREBOX_TUNGSTENSTEEL.get()))
            .where('P', Predicates.blocks('gtceu:tungstensteel_pipe_casing'))
            .where('I', Predicates.blocks('gtceu:extreme_engine_intake_casing'))
            .where('V', Predicates.blocks('gtceu:heat_vent'))
            .where('A', Predicates.air())
            .where('#', Predicates.any())
            .build())
        .workableCasingModel('gtceu:block/casings/gcym/high_temperature_smelting_casing',
            'gtceu:block/multiblock/gcym/mega_blast_furnace')
})
