// AF9 - Certus Resonating Manufacturer: TerraFirmaGreg's ME Assembler (Core-Modern, MEAssemblerMachine and
// TFGMultiMachines, LGPL-3.0; credits in CREDITS-textures.md, licence in licenses/) copied into GTCEu with a structure
// of AF9's own: a cage of AE2 casings and steel pipe casings around a core of vibrant quartz glass, the Resonating
// Fluix Hatch and the Data Access Hatch on the deck. The Liquid Resonating Fluix in the hatch is the stabiliser: while
// there is some, the machine batches its crafts; every craft cycle drinks a few millibuckets of it. No overclocks.
// Recipe type: gtceu:me_assembler (id as in TFG, so its recipes can be taken over unchanged).
// Controller logic, the stabiliser hatch and the recipe page: AF9 Core (MEResonanceFabricatorMachine,
// ResonanceHatchPartMachine). Blocks: af9:ae2_machine_casing (AF9Blocks), gtceu:steel_pipe_casing, ae2:quartz_vibrant_glass.
//
// Differences from TFG: parts have a maximum only (one energy hatch at most, one maintenance hatch at most), the machine
// has no 'moon only' rule (its recipes ask for a cleanroom instead), and the Budding Certus is gone: the fluix
// stabiliser takes its place.

const $MEFabricator = Java.loadClass('com.af9.core.machine.MEResonanceFabricatorMachine')
const $ResonanceHatch = Java.loadClass('com.af9.core.machine.part.ResonanceHatchPartMachine')

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    // 9 items in, 1 out, 3 fluids in; the research slot takes a data orb (the Data Access Hatch in the structure)
    event.create('me_assembler')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(9, 1, 3, 0)
        .setProgressBar(GuiTextures.PROGRESS_BAR_ASSEMBLER, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.ASSEMBLER)
        .setHasResearchSlot(true)
})

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }

    // Resonating Fluix Hatch: the stabiliser input of the Certus Resonating Manufacturer, and the only place
    // Liquid Resonating Fluix goes into it. HV-UHV. Tank: 1000 mB x 2^tier (8,000 mB at HV).
    event.create('resonance_hatch', 'custom')
        .tiers(GTValues.HV, GTValues.EV, GTValues.IV, GTValues.LuV, GTValues.ZPM, GTValues.UV,
            GTValues.UHV)
        .machine((holder, tier) => new $ResonanceHatch(holder, tier))
        .definition((tier, builder) => {
            builder
                .langValue(`${GTValues.VN[tier]} Resonating Fluix Hatch`)
                .rotationState(RotationState.ALL)
                ['colorOverlayTieredHullModel(java.lang.String,java.lang.String,java.lang.String)'](
                    'overlay_pipe_in_emissive', null, 'overlay_fluid_hatch')
                .abilities($ResonanceHatch.RESONANCE_INPUT)
                ['tooltips(net.minecraft.network.chat.Component[])']([
                    Component.translatable('af9.resonance_hatch.tooltip.0'),
                    Component.translatable('af9.resonance_hatch.tooltip.1'),
                    Component.translatable('gtceu.universal.tooltip.fluid_storage_capacity', `${1000 << tier}`)])
        })

    // 5 wide x 4 high x 5 deep, aisles back -> front (the controller's last), rows bottom -> top. The bottom layer
    // is the deck: AE2 casings (C) with the hatches, the Data Access Hatch (H) and the steel pipe casing (P) in the
    // middle; above it a cage of AE2 casings around a core of vibrant quartz glass (G), the walls' inside is any
    // block. Parts have a maximum only (setMaxGlobalLimited(max, preview count)).
    event.create('me_assembler', 'multiblock')
        .machine(holder => new $MEFabricator(holder))
        .langValue('Certus Resonating Manufacturer')
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('me_assembler')])
        .alwaysTryModifyRecipe(true)
        .recipeModifiers([$MEFabricator.RESONANCE])
        .appearanceBlock(() => Block.getBlock('af9:ae2_machine_casing'))
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.me_fabricator.tooltip', 5))
        .pattern(definition => FactoryBlockPattern.start()
            .aisle(' CHC ', ' C C ', ' C C ', ' C C ')
            .aisle('CCCCC', 'CCGCC', 'CCGCC', ' C C ')
            .aisle('CCPCC', 'CC CC', 'CCGCC', ' C C ')
            .aisle('CCCCC', 'CCGCC', 'CCGCC', ' C C ')
            .aisle(' CXC ', ' C C ', ' C C ', ' C C ')
            .where('X', Predicates.controller(Predicates.blocks(definition.get())))
            .where('P', Predicates.blocks('gtceu:steel_pipe_casing'))
            .where('G', Predicates.blocks('ae2:quartz_vibrant_glass'))
            .where('C', Predicates.blocks('af9:ae2_machine_casing')
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(9, 3))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(3, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities($ResonanceHatch.RESONANCE_INPUT).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(1, 1)))
            .where('H', Predicates.dataHatchPredicate(Predicates.blocks('af9:ae2_machine_casing')))
            .where(' ', Predicates.any())
            .build())
        .shapeInfos(definition => $MEFabricator.previews(definition))
        // AE2 casings with the manufacturer's own controller face
        .workableCasingModel('af9:block/ae2_machine_casing', 'gtceu:block/multiblock/me_resonator')
})
