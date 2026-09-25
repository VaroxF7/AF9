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

GTCEuStartupEvents.registry('gtceu:material', allthemods => {
    allthemods.create('endion')
        .gas()
        .color(0x9b6bff)
        .formula('Ed')

    // Tungstensteel and naquadah soaked in endion: the wire of the Endion coils
    allthemods.create('endionite')
        .ingot()
        .fluid()
        .color(0x5e2a9e).secondaryColor(0x1a0b33)
        .iconSet(GTMaterialIconSet.SHINY)
        .flags(GTMaterialFlags.GENERATE_PLATE, GTMaterialFlags.GENERATE_FOIL, GTMaterialFlags.GENERATE_FINE_WIRE,
            GTMaterialFlags.GENERATE_ROD, GTMaterialFlags.GENERATE_FRAME)
        .blastTemp(5400, 'highest', GTValues.VA[GTValues.IV], 1200)
        .formula('(W2Fe2Nq)Ed')
})

StartupEvents.registry('item', allthemods => {
    // substrate id, name used in the items; the boules of silicon, phosphorus, naquadah and neutronium are GT's
    const substrates = [
        ['silicon', 'Silicon'], ['phosphorus', 'Phosphorus-doped Silicon'], ['naquadah', 'Naquadah-doped Silicon'],
        ['trinium', 'Trinium-doped Silicon'], ['naquadria', 'Naquadria-doped Silicon'],
        ['neutronium', 'Neutronium-doped Silicon'], ['strange_matter', 'Strange Matter-doped Silicon'],
        ['chromodynium', 'Chromodynium']
    ]
    substrates.forEach(([id, name]) => {
        allthemods.create(`${id}_melt_charge`)
            .displayName(`${name} Melt Charge`)
            .texture(`kubejs:item/boules/${id}_melt_charge`)
            .tooltip('Polysilicon and dopants blended for one Czochralski pull. A boule takes ten.')
        allthemods.create(`${id}_seed_crystal`)
            .displayName(`${name} Seed Crystal`)
            .texture(`kubejs:item/boules/${id}_seed_crystal`)
            .tooltip('Dipped into the melt and pulled; grows into the boule. Grown in SMC crystal growth.')
    })
    const boules = [
        ['trinium_boule', 'Trinium-doped Monocrystalline Silicon Boule'],
        ['naquadria_boule', 'Naquadria-doped Monocrystalline Silicon Boule'],
        ['strange_matter_boule', 'Strange Matter-doped Monocrystalline Silicon Boule'],
        ['chromodynium_boule', 'Monocrystalline Chromodynium Boule']
    ]
    boules.forEach(([id, name]) => {
        allthemods.create(id).displayName(name).texture(`kubejs:item/boules/${id}`)
    })
    allthemods.create('fused_quartz_crucible')
        .displayName('Fused Quartz Crucible')
        .texture('kubejs:item/boules/fused_quartz_crucible')
        .tooltip('Holds the melt of one boule; it cracks when the melt cools.')
    allthemods.create('tritanium_crucible')
        .displayName('Tritanium Crucible')
        .texture('kubejs:item/boules/tritanium_crucible')
        .tooltip('For the exotic melts that would boil a quartz crucible away. One per boule.')
})

// The Endion coils: usable wherever GT takes heating coils; Boule Melting gets its bonus from them (af9-core
// BouleMelting reads the coil by these block ids).
StartupEvents.registry('block', allthemods => {
    allthemods.create('endion_coil_block', 'gtceu:coil')
        .temperature(8100)
        .level(8)
        .energyDiscount(6)
        .tier(5)
        .coilMaterial(() => GTMaterials.get('endionite'))
        .texture('kubejs:block/coils/endion_coil')
        .displayName('Endion Coil Block')
        .hardness(5)
        .resistance(6)
        .soundType('metal')
        .requiresTool(true)
        .tagBlock('minecraft:mineable/pickaxe')
    allthemods.create('resonant_endion_coil_block', 'gtceu:coil')
        .temperature(12600)
        .level(16)
        .energyDiscount(16)
        .tier(8)
        .coilMaterial(() => GTMaterials.get('endionite'))
        .texture('kubejs:block/coils/resonant_endion_coil')
        .displayName('Resonant Endion Coil Block')
        .hardness(5)
        .resistance(6)
        .soundType('metal')
        .requiresTool(true)
        .tagBlock('minecraft:mineable/pickaxe')
})

GTCEuStartupEvents.registry('gtceu:recipe_type', allthemods => {
    // melt charges, seed crystal, crucible + protective gas -> boule
    allthemods.create('boule_melting')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(3, 1, 1, 0)
        .setProgressBar(GuiTextures.PROGRESS_BAR_CRYSTALLIZATION, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.FURNACE)
})
