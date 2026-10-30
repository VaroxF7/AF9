// AF9 - Agriculture Simulation Bed: simulates the pack's seeds (Mystical Agriculture's essence seeds above all) in an
// 11 x 3 x 11 garden of soil behind corrosion-proof casing (the structure is Janne's agrisim.nbt: the controller replaces
// the nbt's orange wool block, in the south wall one block behind the front wall's notch; the Industrial Foregoing
// water condensator stands in the middle of the garden). Six machine modes (GT's mode tab), one recipe type each:
// elemental + tier 1, tiers 2, 3, 4, 5 + 6 (Mystical Agradditions) of the essence seeds, and one for every other
// seed of the pack (vanilla, Farmer's Delight, Croptopia, Thermal, ...). HV, a minute a run. The soil sets the bonus
// (rich soil, Mystical's higher farmlands: more output and less time; Thermal's Phytosoil the most), a parallel hatch
// or batch mode (never both), and the seed of the running recipe grows on every soil block, drawn at its growth stage.
// Logic: af9-core AgriSimMachine; the render: AgriSimRender. Recipes: server_scripts/mods/gtceu/agri_sim.js. Docs:
// docs/agriculture-simulation-bed.md.
//
// Rows bottom -> top in each aisle, aisles back -> front (the controller's aisle is the last but one), columns left ->
// right as seen from the front. Parts have maximums only (setMaxGlobalLimited(max, preview count)).
//   X  corrosion-proof casing (takes the hatches)      F  soil (any soil of the bed, see AgriSimMachine.SOILS)
//   W  water condensator                                A  air above the soil: the crops grow here
//   S  controller                                       (space)  anything

const $AgriSimMachine = Java.loadClass('com.af9.core.machine.AgriSimMachine')
const $AgriSimModels = Java.loadClass('com.af9.core.machine.AF9MachineModels')

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    // one seed (not a pack's circuit), water; the crop's yield and the seed back
    ;['agri_sim_elemental', 'agri_sim_tier2', 'agri_sim_tier3', 'agri_sim_tier4', 'agri_sim_tier5',
        'agri_sim_crops'].forEach(id => {
        event.create(id)
            .category('multiblock')
            .setEUIO('in')
            .setMaxIOSize(1, 2, 1, 0)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
            .setSound(GTSoundEntries.CHEMICAL)
    })
})

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }

    event.create('agri_sim', 'multiblock')
        .machine(holder => new $AgriSimMachine(holder))
        .langValue('Agriculture Simulation Bed')
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('agri_sim_elemental'), GTRecipeTypes.get('agri_sim_tier2'),
            GTRecipeTypes.get('agri_sim_tier3'), GTRecipeTypes.get('agri_sim_tier4'),
            GTRecipeTypes.get('agri_sim_tier5'), GTRecipeTypes.get('agri_sim_crops')])
        // soil bonus first, then a parallel hatch or the batch (the batch stands down for a hatch), then overclocks
        .recipeModifiers([$AgriSimMachine.SOIL, GTRecipeModifiers.PARALLEL_HATCH, $AgriSimMachine.BATCH,
            GTRecipeModifiers.OC_NON_PERFECT])
        .appearanceBlock(() => Block.getBlock('gtceu:corrosion_proof_casing'))
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.agri_sim.tooltip', 5))
        .pattern(definition => FactoryBlockPattern.start()
            .aisle('XXXXX XXXXX', 'XXXXX XXXXX', '           ')
            .aisle('XXXXXXXXXXX', 'XFFFXXXFFFX', ' AAA   AAA ')
            .aisle('XXXXXXXXXXX', 'XFFFFFFFFFX', ' AAAAAAAAA ')
            .aisle('XXXXXXXXXXX', 'XFFFFFFFFFX', ' AAAAAAAAA ')
            .aisle('XXXXXXXXXXX', 'XXFFXXXFFXX', '  AA   AA  ')
            .aisle(' XXXXXXXXX ', ' XFFXWXFFX ', '  AA   AA  ')
            .aisle('XXXXXXXXXXX', 'XXFFXXXFFXX', '  AA   AA  ')
            .aisle('XXXXXXXXXXX', 'XFFFFFFFFFX', ' AAAAAAAAA ')
            .aisle('XXXXXXXXXXX', 'XFFFFFFFFFX', ' AAAAAAAAA ')
            .aisle('XXXXXSXXXXX', 'XFFFXXXFFFX', ' AAA   AAA ')
            .aisle('XXXXX XXXXX', 'XXXXX XXXXX', '           ')
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            .where('A', Predicates.air())
            .where('W', Predicates.blocks('industrialforegoing:water_condensator'))
            // every soil of the bed (rich soil first: the preview shows the first block) (AgriSimMachine.SOILS holds the same list and the bonuses)
            .where('F', Predicates.blocks('farmersdelight:rich_soil_farmland',
                'mysticalagriculture:inferium_farmland', 'mysticalagriculture:prudentium_farmland',
                'mysticalagriculture:tertium_farmland', 'mysticalagriculture:imperium_farmland',
                'mysticalagriculture:supremium_farmland', 'thermal:phytosoil_tilled'))
            .where('X', Predicates.blocks('gtceu:corrosion_proof_casing')
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1, 0))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
            .build())
        // corrosion-proof casing with the bed's own controller face, and the crops growing on the soil
        .model($AgriSimModels.workableCasingWithAgriSim('gtceu:block/casings/gcym/corrosion_proof_casing',
            'gtceu:block/multiblock/agri_sim'))
        .hasBER(true)
})
