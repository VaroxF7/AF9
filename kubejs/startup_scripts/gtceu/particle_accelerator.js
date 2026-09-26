// AF9 - Particle Accelerator: a linear accelerator for the wafers beyond neutronium. Spec: docs/semiconductor-factory.md
//
// Ion source -> eight superconducting RF cavity sections -> target station. The magnets and cavities are cooled from
// Coolant Hatches only (supercooled fluids, cryogenics.js). Three modes:
//   neutron irradiation   protons on a beryllium spallation target; the neutron flux transmutes neutronium-doped
//                         wafers into transmuted neutronium wafers (the 20 nm substrate)
//   heavy-ion collision   lead ions collide; the quark-gluon plasma is caught in magnetic (Penning) traps
//   quark synthesis       quark-gluon plasma condenses into strange matter, and with strange matter into chromodynium,
//                         the quark-level metal of the 1 nm wafers
// Machine behaviour: AF9 Core (ParticleAcceleratorMachine). Recipes: server_scripts/mods/gtceu/particle_accelerator.js

const $ParticleAcceleratorMachine = Java.loadClass('com.af9.core.machine.ParticleAcceleratorMachine')
const $AccelCoolantHatch = Java.loadClass('com.af9.core.machine.part.CoolantHatchPartMachine')
const $AccelModifiers = Java.loadClass('com.af9.core.common.AF9Modifiers')

GTCEuStartupEvents.registry('gtceu:material', allthemods => {
    // stable strangelets: up, down and strange quarks in one bag
    allthemods.create('strange_matter')
        .dust()
        .color(0x8a1e6a).secondaryColor(0x2a0033)
        .iconSet(GTMaterialIconSet.SHINY)
        .formula('(uds)n')

    // colour-charged quark matter held in a lattice: the metal of the 1 nm wafers
    allthemods.create('chromodynium')
        .ingot()
        .fluid()
        .color(0xff3c78).secondaryColor(0x3cffb4)
        .iconSet(GTMaterialIconSet.SHINY)
        .flags(GTMaterialFlags.GENERATE_PLATE, GTMaterialFlags.GENERATE_FOIL, GTMaterialFlags.GENERATE_ROD,
            GTMaterialFlags.GENERATE_FRAME)
        .blastTemp(12000, 'highest', GTValues.VA[GTValues.UHV], 2400)
        .formula('Qc')
})

StartupEvents.registry('item', allthemods => {
    allthemods.create('beryllium_spallation_target')
        .displayName('Beryllium Spallation Target')
        .texture('kubejs:item/accelerator/beryllium_spallation_target')
        .tooltip('Proton beam in, neutrons out. Wears out after four wafers.')
    allthemods.create('magnetic_trap')
        .displayName('Magnetic Penning Trap')
        .texture('kubejs:item/accelerator/magnetic_trap')
        .tooltip('An empty superconducting trap for quark-gluon plasma.')
    allthemods.create('qgp_trap')
        .displayName('Quark-Gluon Plasma Trap')
        .texture('kubejs:item/accelerator/qgp_trap')
        .tooltip('Quark-gluon plasma from a heavy-ion collision, held in a magnetic trap.')
})

StartupEvents.registry('block', allthemods => {
    const machineBlock = (id, name, light) => allthemods.create(id)
        .displayName(name)
        .soundType('metal')
        .hardness(5)
        .resistance(6)
        .lightLevel(light)
        .requiresTool(true)
        .tagBlock('minecraft:mineable/pickaxe')
    machineBlock('beamline_casing', 'Beamline Casing', 0)
    machineBlock('rf_cavity', 'Superconducting RF Cavity', 0.4)
    machineBlock('spallation_target_housing', 'Spallation Target Housing', 0.3)
})

GTCEuStartupEvents.registry('gtceu:recipe_type', allthemods => {
    // [id, items in, items out, progress bar, sound]; one fluid input each: the coolant
    const types = [
        ['neutron_irradiation', 2, 1, GuiTextures.PROGRESS_BAR_MASS_FAB, GTSoundEntries.SCIENCE],
        ['ion_collision', 2, 1, GuiTextures.PROGRESS_BAR_FUSION, GTSoundEntries.ARC],
        ['quark_synthesis', 2, 2, GuiTextures.PROGRESS_BAR_REPLICATOR, GTSoundEntries.REPLICATOR]
    ]
    types.forEach(([id, itemsIn, itemsOut, bar, sound]) => {
        allthemods.create(id)
            .category('multiblock')
            .setEUIO('in')
            .setMaxIOSize(itemsIn, itemsOut, 1, 0)
            .setProgressBar(bar, FillDirection.LEFT_TO_RIGHT)
            .setSound(sound)
    })
})

GTCEuStartupEvents.registry('gtceu:machine', allthemods => {
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }

    // 5 x 5 x 11, aisles back (ion source) -> front (controller at the target station), rows bottom -> top.
    allthemods.create('particle_accelerator', 'multiblock')
        .machine(holder => new $ParticleAcceleratorMachine(holder))
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('neutron_irradiation'), GTRecipeTypes.get('ion_collision'),
            GTRecipeTypes.get('quark_synthesis')])
        // POWER_GATE: only runs when the hatches supply the recipe's full EU/t
        .recipeModifiers([$AccelModifiers.POWER_GATE, GTRecipeModifiers.OC_NON_PERFECT])
        .appearanceBlock(() => Block.getBlock('kubejs:beamline_casing'))
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.particle_accelerator.tooltip', 9))
        .pattern(definition => {
            let pattern = FactoryBlockPattern.start()
                .aisle('BBBBB', 'BBBBB', 'BBIBB', 'BBBBB', 'BBBBB')          // ion source
            for (let i = 0; i < 8; i++) {
                pattern = pattern.aisle('BBBBB', 'BMMMB', 'BMRMB', 'BMMMB', 'BBBBB') // RF cavity in its magnet ring
            }
            return pattern
                .aisle('BBBBB', 'BGGGB', 'BGTGB', 'BGGGB', 'BBBBB')          // target station
                .aisle('BBBBB', 'BBBBB', 'BBSBB', 'BBBBB', 'BBBBB')          // shielding wall + controller
                .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count))
                .where('B', Predicates.blocks('kubejs:beamline_casing')
                    .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(4, 2))
                    .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(2, 0))
                    .or(Predicates.abilities($AccelCoolantHatch.COOLANT_INPUT).setMaxGlobalLimited(2, 1))
                    .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                    .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2, 1))
                    .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
                .where('I', Predicates.blocks('gtceu:fusion_coil'))                 // ion source
                .where('M', Predicates.blocks('gtceu:superconducting_coil'))        // focusing magnets
                .where('R', Predicates.blocks('kubejs:rf_cavity'))
                .where('G', Predicates.blocks('gtceu:laminated_glass'))
                .where('T', Predicates.blocks('kubejs:spallation_target_housing'))
                .build()
        })
        .workableCasingModel('kubejs:block/beamline_casing', 'gtceu:block/multiblock/fusion_reactor')
})
