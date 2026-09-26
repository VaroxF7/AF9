// AF9 - Particle Accelerator: a storage ring for the wafers beyond neutronium. Spec: docs/semiconductor-factory.md
//
// A ring 47 blocks across and 7 high, built after GTNH's Compact Fusion Computer (its layout, GT5-Unofficial
// MTELargeFusionComputer): superconducting bending magnets inside a clean-steel shell, four glass gates at the compass
// points with the hatches, energy hatches along the ring. The magnets are cooled from Coolant Hatches only (supercooled
// fluids, cryogenics.js). Three modes:
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

    // The ring, top views of its layers (GTNH's Compact Fusion Computer): rows north -> south, the controller in the
    // south gate's outer wall facing out. C clean stainless steel casing, H superconducting coil (the bending magnets),
    // B fusion glass, F naquadah alloy frame; I a gate's hatch spot (fusion glass or a part), E an energy spot (casing
    // or an energy / laser hatch). The shell is a diamond around the magnets: 3 wide at y 1 and 5, 5 wide in between.
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

    allthemods.create('particle_accelerator', 'multiblock')
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
            // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count))
            return pattern
                .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                .where('C', Predicates.blocks('gtceu:clean_machine_casing'))
                .where('H', Predicates.blocks('gtceu:superconducting_coil'))
                .where('B', Predicates.blocks('gtceu:fusion_glass'))
                .where('F', Predicates.blocks('gtceu:naquadah_alloy_frame'))
                // the gates: item buses, coolant hatches and the maintenance hatch
                .where('I', Predicates.blocks('gtceu:fusion_glass')
                    .or(Predicates.abilities($AccelCoolantHatch.COOLANT_INPUT).setMaxGlobalLimited(2, 1))
                    .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                    .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2, 1))
                    .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
                // along the ring: the power
                .where('E', Predicates.blocks('gtceu:clean_machine_casing')
                    .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(4, 2))
                    .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(2, 0)))
                .where(' ', Predicates.any())
                .build()
        })
        .workableCasingModel('gtceu:block/casings/solid/machine_casing_clean_stainless_steel',
            'gtceu:block/multiblock/fusion_reactor')
        // the same model plus the light ring inside the storage ring, with lightning leaping into the middle; the
        // numbers live in af9-core (RING_*): 23 behind the controller, radius 15.5, lying flat
        .model($AccelMachineModels.workableCasingWithLightRing(
            'gtceu:block/casings/solid/machine_casing_clean_stainless_steel', 'gtceu:block/multiblock/fusion_reactor',
            $ParticleAcceleratorMachine.RING_UP, $ParticleAcceleratorMachine.RING_BACK,
            $ParticleAcceleratorMachine.RING_RADIUS, $ParticleAcceleratorMachine.RING_THICKNESS, 'up', true))
        .hasBER(true)
})
