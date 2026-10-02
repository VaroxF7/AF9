// AF9 - Particle Accelerator: a storage ring for the wafers beyond neutronium. Spec: docs/semiconductor-factory.md
//
// A ring 47 blocks across and 7 high, built after GTNH's Compact Fusion Computer (its layout, GT5-Unofficial
// MTELargeFusionComputer): an empty beam tube inside a clean-steel shell (the bending magnets), four glass gates at the compass
// points; the hatches go anywhere on the clean-steel casing (and in the gates' glass spots). The magnets are cooled from
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
const $AccelMachineModels = Java.loadClass('com.af9.core.machine.AF9MachineModels')
const $AccelSounds = Java.loadClass('com.af9.core.common.AF9Sounds')

// The two quark materials' own animated looks: Strange Matter a dark violet void with twinkling glints, Chromodynium a
// pearl metal with a sheen sweeping through its colour charge. Models and textures in af9-core
// (assets/gtceu/{models,textures}/item/material_sets/<set>, Chromodynium's block and frame in textures/block/...);
// shapes the sets lack come from GT's shiny set
GTCEuStartupEvents.registry('gtceu:material_icon_set', event => {
    event.create('strange_matter').parent(GTMaterialIconSet.SHINY)
    event.create('chromodynium').parent(GTMaterialIconSet.SHINY)
})

GTCEuStartupEvents.registry('gtceu:material', event => {
    // stable strangelets: up, down and strange quarks in one bag
    event.create('strange_matter')
        .dust()
        .color(0x8a1e6a).secondaryColor(0x2a0033)
        .iconSet('strange_matter')
        .formula('(uds)n')

    // colour-charged quark matter held in a lattice: the metal of the 1 nm wafers
    event.create('chromodynium')
        .ingot()
        .fluid()
        .color(0xff3c78).secondaryColor(0x3cffb4)
        .iconSet('chromodynium')
        .flags(GTMaterialFlags.GENERATE_PLATE, GTMaterialFlags.GENERATE_FOIL, GTMaterialFlags.GENERATE_ROD,
            GTMaterialFlags.GENERATE_FRAME)
        .blastTemp(12000, 'highest', GTValues.VA[GTValues.UHV], 2400)
        .formula('Qc')
})

StartupEvents.registry('item', event => {
    event.create('beryllium_spallation_target')
        .displayName('Beryllium Spallation Target')
        .texture('kubejs:item/accelerator/beryllium_spallation_target')
        .tooltip('Proton beam in, neutrons out. Wears out after four wafers.')
    event.create('magnetic_trap')
        .displayName('Magnetic Penning Trap')
        .texture('kubejs:item/accelerator/magnetic_trap')
        .tooltip('An empty superconducting trap for quark-gluon plasma.')
    event.create('qgp_trap')
        .displayName('Quark-Gluon Plasma Trap')
        .texture('kubejs:item/accelerator/qgp_trap')
        .tooltip('Quark-gluon plasma from a heavy-ion collision, held in a magnetic trap.')
})

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    // [id, items in, items out, progress bar, sound]; one fluid input each: the coolant. All three sound like the
    // accelerator: its own hum (af9-core AF9Sounds, the beacon hum pitched up)
    const types = [
        ['neutron_irradiation', 2, 1, GuiTextures.PROGRESS_BAR_MASS_FAB, $AccelSounds.PARTICLE_ACCELERATOR],
        ['ion_collision', 2, 1, GuiTextures.PROGRESS_BAR_FUSION, $AccelSounds.PARTICLE_ACCELERATOR],
        ['quark_synthesis', 2, 2, GuiTextures.PROGRESS_BAR_REPLICATOR, $AccelSounds.PARTICLE_ACCELERATOR]
    ]
    types.forEach(([id, itemsIn, itemsOut, bar, sound]) => {
        event.create(id)
            .category('multiblock')
            .setEUIO('in')
            .setMaxIOSize(itemsIn, itemsOut, 1, 0)
            .setProgressBar(bar, FillDirection.LEFT_TO_RIGHT)
            .setSound(sound)
    })
})

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }

    // The ring, top views of its layers (GTNH's Compact Fusion Computer): rows north -> south, the controller in the
    // south gate's outer wall facing out. C clean stainless steel casing (the bending magnets), H the beam tube (air),
    // B fusion glass, F naquadah alloy frame; I a gate's glass spot and E a casing spot (GTNH's hatch and energy spots:
    // here, like every C, they take any part). The shell is a diamond around the magnets: 3 wide at y 1 and 5, 5 wide in
    // between.
    // y = 0 and 6
    const RING_L0 = [
        '                                               ',
        '                                               ',
        '                    FCCCCCF                    ',
        '                    FCIBICF                    ',
        '                    FCCCCCF                    ',
        '                                               ',
        '                                               ',
        '                                               ',
        '                                               ',
        '                                               ',
        '                                               ',
        '                                               ',
        '                                               ',
        '                                               ',
        '                                               ',
        '                                               ',
        '                                               ',
        '                                               ',
        '                                               ',
        '                                               ',
        '  FFF                                     FFF  ',
        '  CCC                                     CCC  ',
        '  CIC                                     CIC  ',
        '  CBC                                     CBC  ',
        '  CIC                                     CIC  ',
        '  CCC                                     CCC  ',
        '  FFF                                     FFF  ',
        '                                               ',
        '                                               ',
        '                                               ',
        '                                               ',
        '                                               ',
        '                                               ',
        '                                               ',
        '                                               ',
        '                                               ',
        '                                               ',
        '                                               ',
        '                                               ',
        '                                               ',
        '                                               ',
        '                                               ',
        '                    FCCCCCF                    ',
        '                    FCIBICF                    ',
        '                    FCCCCCF                    ',
        '                                               ',
        '                                               ']
    // y = 1 and 5
    const RING_L1 = [
        '                                               ',
        '                    FCBBBCF                    ',
        '                   CC     CC                   ',
        '                CCCCC     CCCCC                ',
        '              CCCCCCC     CCCCCCC              ',
        '            CCCCCCC FCBBBCF CCCCCCC            ',
        '           CCCCC               CCCCC           ',
        '          CCCC                   CCCC          ',
        '         CCC                       CCC         ',
        '        CCC                         CCC        ',
        '       CCC                           CCC       ',
        '      CCC                             CCC      ',
        '     CCC                               CCC     ',
        '     CCC                               CCC     ',
        '    CCC                                 CCC    ',
        '    CCC                                 CCC    ',
        '   CCC                                   CCC   ',
        '   CCC                                   CCC   ',
        '   CCC                                   CCC   ',
        '  CCC                                     CCC  ',
        ' FCCCF                                   FCCCF ',
        ' C   C                                   C   C ',
        ' B   B                                   B   B ',
        ' B   B                                   B   B ',
        ' B   B                                   B   B ',
        ' C   C                                   C   C ',
        ' FCCCF                                   FCCCF ',
        '  CCC                                     CCC  ',
        '   CCC                                   CCC   ',
        '   CCC                                   CCC   ',
        '   CCC                                   CCC   ',
        '    CCC                                 CCC    ',
        '    CCC                                 CCC    ',
        '     CCC                               CCC     ',
        '     CCC                               CCC     ',
        '      CCC                             CCC      ',
        '       CCC                           CCC       ',
        '        CCC                         CCC        ',
        '         CCC                       CCC         ',
        '          CCCC                   CCCC          ',
        '           CCCCC               CCCCC           ',
        '            CCCCCCC FCBBBCF CCCCCCC            ',
        '              CCCCCCC     CCCCCCC              ',
        '                CCCCC     CCCCC                ',
        '                   CC     CC                   ',
        '                    FCBBBCF                    ',
        '                                               ']
    // y = 2 and 4
    const RING_L2 = [
        '                    FCCCCCF                    ',
        '                   CC     CC                   ',
        '                CCCCC     CCCCC                ',
        '              CCCCCHHHHHHHHHCCCCC              ',
        '            CCCCHHHCC     CCHHHCCCC            ',
        '           CCCHHCCCCC     CCCCCHHCCC           ',
        '          ECHHCCCCC FCCCCCF CCCCCHHCE          ',
        '         CCHCCCC               CCCCHCC         ',
        '        CCHCCC                   CCCHCC        ',
        '       CCHCE                       ECHCC       ',
        '      ECHCC                         CCHCE      ',
        '     CCHCE                           ECHCC     ',
        '    CCHCC                             CCHCC    ',
        '    CCHCC                             CCHCC    ',
        '   CCHCC                               CCHCC   ',
        '   CCHCC                               CCHCC   ',
        '  CCHCC                                 CCHCC  ',
        '  CCHCC                                 CCHCC  ',
        '  CCHCC                                 CCHCC  ',
        ' CCHCC                                   CCHCC ',
        'FCCHCCF                                 FCCHCCF',
        'C  H  C                                 C  H  C',
        'C  H  C                                 C  H  C',
        'C  H  C                                 C  H  C',
        'C  H  C                                 C  H  C',
        'C  H  C                                 C  H  C',
        'FCCHCCF                                 FCCHCCF',
        ' CCHCC                                   CCHCC ',
        '  CCHCC                                 CCHCC  ',
        '  CCHCC                                 CCHCC  ',
        '  CCHCC                                 CCHCC  ',
        '   CCHCC                               CCHCC   ',
        '   CCHCC                               CCHCC   ',
        '    CCHCC                             CCHCC    ',
        '    CCHCC                             CCHCC    ',
        '     CCHCE                           ECHCC     ',
        '      ECHCC                         CCHCE      ',
        '       CCHCE                       ECHCC       ',
        '        CCHCCC                   CCCHCC        ',
        '         CCHCCCC               CCCCHCC         ',
        '          ECHHCCCCC FCCCCCF CCCCCHHCE          ',
        '           CCCHHCCCCC     CCCCCHHCCC           ',
        '            CCCCHHHCC     CCHHHCCCC            ',
        '              CCCCCHHHHHHHHHCCCCC              ',
        '                CCCCC     CCCCC                ',
        '                   CC     CC                   ',
        '                    FCCCCCF                    ']
    // y = 3, the controller's layer
    const RING_L3 = [
        '                    FCIBICF                    ',
        '                   CC     CC                   ',
        '                CCCHHHHHHHHHCCC                ',
        '              CCHHHHHHHHHHHHHHHCC              ',
        '            CCHHHHHHHHHHHHHHHHHHHCC            ',
        '           CHHHHHHHCC     CCHHHHHHHC           ',
        '          CHHHHHCCC FCIBICF CCCHHHHHC          ',
        '         CHHHHCC               CCHHHHC         ',
        '        CHHHCC                   CCHHHC        ',
        '       CHHHC                       CHHHC       ',
        '      CHHHC                         CHHHC      ',
        '     CHHHC                           CHHHC     ',
        '    CHHHC                             CHHHC    ',
        '    CHHHC                             CHHHC    ',
        '   CHHHC                               CHHHC   ',
        '   CHHHC                               CHHHC   ',
        '  CHHHC                                 CHHHC  ',
        '  CHHHC                                 CHHHC  ',
        '  CHHHC                                 CHHHC  ',
        ' CHHHC                                   CHHHC ',
        'FCHHHCF                                 FCHHHCF',
        'C HHH C                                 C HHH C',
        'I HHH I                                 I HHH I',
        'B HHH B                                 B HHH B',
        'I HHH I                                 I HHH I',
        'C HHH C                                 C HHH C',
        'FCHHHCF                                 FCHHHCF',
        ' CHHHC                                   CHHHC ',
        '  CHHHC                                 CHHHC  ',
        '  CHHHC                                 CHHHC  ',
        '  CHHHC                                 CHHHC  ',
        '   CHHHC                               CHHHC   ',
        '   CHHHC                               CHHHC   ',
        '    CHHHC                             CHHHC    ',
        '    CHHHC                             CHHHC    ',
        '     CHHHC                           CHHHC     ',
        '      CHHHC                         CHHHC      ',
        '       CHHHC                       CHHHC       ',
        '        CHHHCC                   CCHHHC        ',
        '         CHHHHCC               CCHHHHC         ',
        '          CHHHHHCCC FCIBICF CCCHHHHHC          ',
        '           CHHHHHHHCC     CCHHHHHHHC           ',
        '            CCHHHHHHHHHHHHHHHHHHHCC            ',
        '              CCHHHHHHHHHHHHHHHCC              ',
        '                CCCHHHHHHHHHCCC                ',
        '                   CC     CC                   ',
        '                    FCISICF                    ']

    event.create('particle_accelerator', 'multiblock')
        .machine(holder => new $ParticleAcceleratorMachine(holder))
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('neutron_irradiation'), GTRecipeTypes.get('ion_collision'),
            GTRecipeTypes.get('quark_synthesis')])
        // POWER_GATE: only runs when the hatches supply the recipe's full EU/t
        .recipeModifiers([$AccelModifiers.POWER_GATE, GTRecipeModifiers.OC_NON_PERFECT])
        .appearanceBlock(() => Block.getBlock('gtceu:clean_machine_casing'))
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.particle_accelerator.tooltip', 9))
        .pattern(definition => {
            // aisles north -> south (the controller's aisle last), rows bottom -> top: the layers mirror at y 3
            let pattern = FactoryBlockPattern.start()
            for (let row = 0; row < 47; row++) {
                pattern = pattern.aisle(RING_L0[row], RING_L1[row], RING_L2[row], RING_L3[row], RING_L2[row],
                    RING_L1[row], RING_L0[row])
            }
            // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count)). One set of
            // them for the whole ring, so the maximums count across it: on any clean-steel casing, and in the gates'
            // glass hatch spots
            const parts = Predicates.abilities($AccelCoolantHatch.COOLANT_INPUT).setMaxGlobalLimited(2, 1)
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(4, 2))
                .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(2, 0))
            return pattern
                .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                .where('C', Predicates.blocks('gtceu:clean_machine_casing').or(parts))
                // the beam tube: empty, so the beam (the light ring) runs through it, seen through the gates' glass
                .where('H', Predicates.air())
                .where('B', Predicates.blocks('gtceu:fusion_glass'))
                .where('F', Predicates.blocks('gtceu:naquadah_alloy_frame'))
                .where('I', Predicates.blocks('gtceu:fusion_glass').or(parts))
                .where('E', Predicates.blocks('gtceu:clean_machine_casing').or(parts))
                .where(' ', Predicates.any())
                .build()
        })
        .workableCasingModel('gtceu:block/casings/solid/machine_casing_clean_stainless_steel',
            'gtceu:block/multiblock/fusion_reactor')
        // the same model plus the light ring, the beam: along the middle of the empty tube (it shows through the gates'
        // glass), no lightning; the numbers live in af9-core (RING_*): 23 behind the controller, radius 20, lying flat,
        // 2.5 out from the tube's inner face (where its sparks spit off)
        .model($AccelMachineModels.workableCasingWithLightRing(
            'gtceu:block/casings/solid/machine_casing_clean_stainless_steel', 'gtceu:block/multiblock/fusion_reactor',
            $ParticleAcceleratorMachine.RING_UP, $ParticleAcceleratorMachine.RING_BACK,
            $ParticleAcceleratorMachine.RING_RADIUS, $ParticleAcceleratorMachine.RING_THICKNESS, 'up', false,
            $ParticleAcceleratorMachine.RING_WALL))
        .hasBER(true)
})
