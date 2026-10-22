// AF9 - Bio Vat: GTNH's Bacterial Vat (BartWorks MTEBioVat in GT5-Unofficial, MIT; credits in CREDITS-textures.md,
// licence in licenses/) in GTCEu. It replaces the Bacteria Breeding Station: the same 5 x 4 x 5 shell of clean
// stainless casing with a 3 x 3 x 2 tank behind glass (the original's shape: controller front-bottom-centre, hatches in
// the casings), and the original's rules (af9-core BioVatMachine): the glass of the tank sets which recipes run
// (tempered 1, laminated 2, fusion 3), a run is multiplied by how full the output hatch is, and the culture is seen
// standing in the tank (af9-core BioVatRender). The controller face is the original's (distillation tower overlay).
// Biochaff fed here grows the cultures of the Water Filtration Plant's LuV/ZPM biosorption modes. Recipes:
// server_scripts/mods/gtceu/bio_vat.js
//
// Rows bottom -> top in each aisle, aisles back -> front. Fluid hatches have a maximum only, never a required count
// (setMaxGlobalLimited(max, preview count)).

const $BioVatMachine = Java.loadClass('com.af9.core.machine.BioVatMachine')
const $BioVatModels = Java.loadClass('com.af9.core.machine.AF9MachineModels')

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    event.create('bio_vat')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(2, 0, 2, 1)
        .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.CHEMICAL)
})

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }

    event.create('bio_vat', 'multiblock')
        .machine(holder => new $BioVatMachine(holder))
        .langValue('Bio Vat')
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('bio_vat')])
        // glass gate and fill bonus first, then perfect overclocks
        .recipeModifiers([$BioVatMachine.VAT, GTRecipeModifiers.OC_PERFECT])
        .appearanceBlock(GTBlocks.CASING_STAINLESS_CLEAN)
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.bio_vat.tooltip', 4))
        .pattern(definition => FactoryBlockPattern.start()
            .aisle('XXXXX', 'GGGGG', 'GGGGG', 'XXXXX')
            .aisle('XXXXX', 'GAAAG', 'GAAAG', 'XXXXX')
            .aisle('XXXXX', 'GAAAG', 'GAAAG', 'XXXXX')
            .aisle('XXXXX', 'GAAAG', 'GAAAG', 'XXXXX')
            .aisle('XXSXX', 'GGGGG', 'GGGGG', 'XXXXX')
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            .where('X', Predicates.blocks(GTBlocks.CASING_STAINLESS_CLEAN.get())
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
            // the three glasses, one tier each; the weakest pane sets the vat's tier (BioVatMachine)
            .where('G', Predicates.blocks('gtceu:tempered_glass', 'gtceu:laminated_glass', 'gtceu:fusion_glass'))
            .where('A', Predicates.air())
            .build())
        // clean stainless casing with GTNH's distillation tower face, and the culture standing in the tank
        .model($BioVatModels.workableCasingWithBioVat(
            'gtceu:block/casings/solid/machine_casing_clean_stainless_steel',
            'gtceu:block/multiblock/bio_vat', 0.5, 2.0))
        .hasBER(true)
})
