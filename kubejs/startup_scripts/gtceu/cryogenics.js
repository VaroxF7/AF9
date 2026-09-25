// AF9 - Cryogenics: the Supercooling Cryostat and the Coolant Hatch. Spec: docs/semiconductor-factory.md
//
// Supercooling Cryostat (HV): needs 4A of HV (two energy hatches) to run at all, perfect overclocks above that.
// Two modes, one after the other:
//   dense cooling   gas -> dense liquid (compressed and chilled; the intermediate)
//   supercooling    the dense liquid goes back in -> supercooled fluid, rated at an effective -5000 K
// Supercooled hydrogen, argon, xenon and endion (the End's noble gas, boule_melting.js) are the only fluids the
// Coolant Hatch takes; the Particle Accelerator and the Orbital Lithography Station take their fluids only through it.
// (GT fluids cannot be below 0 K, so the supercooled fluids sit at 1 K; the -5000 K is the cryostat's rating.)
// Machine behaviour: AF9 Core (SupercoolerMachine, CoolantHatchPartMachine). Recipes: server_scripts/mods/gtceu/
// cryogenics.js

const $SupercoolerMachine = Java.loadClass('com.af9.core.machine.SupercoolerMachine')
const $CryoCoolantHatch = Java.loadClass('com.af9.core.machine.part.CoolantHatchPartMachine')
const $CryoModifiers = Java.loadClass('com.af9.core.common.AF9Modifiers')

GTCEuStartupEvents.registry('gtceu:material', allthemods => {
    // gas, dense colour, supercooled colour, formula, dense liquid temperature (K)
    const gases = [
        ['hydrogen', 0x7fb2e6, 0xc8e6ff, 'H2', 14],
        ['argon', 0x3fb8c9, 0x9fefff, 'Ar', 84],
        ['xenon', 0x6a4fc9, 0xb9a3ff, 'Xe', 161],
        ['endion', 0x4a22b0, 0x8f6bff, 'Ed', 40]
    ]
    gases.forEach(([id, dense, supercooled, formula, kelvin]) => {
        allthemods.create(`dense_${id}`)
            .liquid(kelvin)
            .color(dense)
            .formula(formula)
        allthemods.create(`supercooled_${id}`)
            .liquid(1)
            .color(supercooled)
            .formula(formula)
    })
})

GTCEuStartupEvents.registry('gtceu:recipe_type', allthemods => {
    allthemods.create('dense_cooling')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(0, 0, 1, 1)
        .setProgressBar(GuiTextures.PROGRESS_BAR_COMPRESS, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.COOLING)
    allthemods.create('supercooling')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(0, 0, 1, 1)
        .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.COOLING)
})

GTCEuStartupEvents.registry('gtceu:machine', allthemods => {
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }

    // Cold box: frostproof shell, PTFE heat-exchanger coils, two-stage compressors, a cold chamber behind windows.
    // 5 x 5 x 5, aisles back -> front (controller), rows bottom -> top.
    allthemods.create('supercooling_cryostat', 'multiblock')
        .machine(holder => new $SupercoolerMachine(holder))
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('dense_cooling'), GTRecipeTypes.get('supercooling')])
        // POWER_GATE: only runs when the hatches supply the recipe's full 4A of HV; perfect overclocks above that
        .recipeModifiers([$CryoModifiers.POWER_GATE, GTRecipeModifiers.OC_PERFECT])
        .appearanceBlock(GTBlocks.CASING_ALUMINIUM_FROSTPROOF)
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.supercooling_cryostat.tooltip', 7))
        .pattern(definition => FactoryBlockPattern.start()
            .aisle('FFFFF', 'FFFFF', 'FFFFF', 'FFFFF', 'FFFFF')
            .aisle('FFFFF', 'FPPPF', 'FPKPF', 'FPPPF', 'FFFFF')
            .aisle('FFFFF', 'TP#PT', 'TK#KT', 'TP#PT', 'FFFFF')
            .aisle('FFFFF', 'FPPPF', 'FPKPF', 'FPPPF', 'FFFFF')
            .aisle('FFFFF', 'FFFFF', 'FFSFF', 'FFFFF', 'FFFFF')
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            .where('F', Predicates.blocks(GTBlocks.CASING_ALUMINIUM_FROSTPROOF.get()).setMinGlobalLimited(50)
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1).setMaxGlobalLimited(2))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMinGlobalLimited(1))
                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMinGlobalLimited(1))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1)))
            .where('P', Predicates.blocks(GTBlocks.CASING_POLYTETRAFLUOROETHYLENE_PIPE.get())) // heat exchanger
            .where('K', Predicates.blocks('gtceu:stainless_steel_gearbox'))                     // compressors
            .where('T', Predicates.blocks('gtceu:tempered_glass'))
            .where('#', Predicates.air())                                                       // cold chamber
            .build())
        .workableCasingModel('gtceu:block/casings/solid/machine_casing_frost_proof',
            'gtceu:block/multiblock/vacuum_freezer')

    // Coolant Hatch: an input hatch for supercooled fluids only. Tank: 1000 mB x 2^tier (64,000 mB at LuV).
    allthemods.create('coolant_hatch', 'custom')
        .tiers(GTValues.LuV, GTValues.ZPM, GTValues.UV, GTValues.UHV)
        .machine((holder, tier, tankScaling) => new $CryoCoolantHatch(holder, tier))
        .definition((tier, builder) => {
            builder
                .langValue(`${GTValues.VN[tier]} Coolant Hatch`)
                .rotationState(RotationState.ALL)
                ['colorOverlayTieredHullModel(java.lang.String,java.lang.String,java.lang.String)'](
                    'overlay_pipe_in_emissive', null, 'overlay_fluid_hatch')
                .abilities(PartAbility.IMPORT_FLUIDS, $CryoCoolantHatch.COOLANT_INPUT)
                ['tooltips(net.minecraft.network.chat.Component[])']([
                    Component.translatable('af9.coolant_hatch.tooltip.0'),
                    Component.translatable('af9.coolant_hatch.tooltip.1'),
                    Component.translatable('gtceu.universal.tooltip.fluid_storage_capacity', `${1000 << tier}`)])
        })
})
