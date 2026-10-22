// AF9 - Electric Implosion Compressor: the implosion compressor without explosives. A capacitor bank fires through the
// work coils and the shock wave that dynamite made is made of current and an acidic vapor (Lunar Acid Vapor, a cut of
// Lunar Air) that the discharge flashes into plasma. Recipes: server_scripts/mods/gtceu/electric_implosion.js,
// rocket_hull.js. Spec: docs/electric-implosion.md
//
//   single block   HV-LuV, one dust (and one more) in, one vapor, one plate or naquadah dust out
//   Large Electric Implosion Compressor, 3 x 3 x 3 like GT's Large Chemical Reactor: the Shock Capacitor Casing, five
//   cupronickel coil blocks as the work coils (the middle of the cross and its four arms) round a titanium frame, the
//   controller in the middle of the front. Hatches go in the casings. Parallel hatch, perfect overclocks.
// Both run the recipe type `electric_implosion_compressor`; GT's own Implosion Compressor keeps the explosive recipes.
// The Large one also holds a capacitor bank (LargeElectricImplosionMachine): charged recipes, one per normal implosion
// recipe without its explosive, fire from the bank. The naquadah recipe (4 U-235 dust + vapor -> 1 naquadah dust) is the electric implosion's transmutation.

const $LargeElectricImplosion = Java.loadClass('com.af9.core.machine.LargeElectricImplosionMachine')
const $ElectricImplosionTiered = Java.loadClass('com.gregtechceu.gtceu.api.machine.SimpleTieredMachine')

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    // the charged recipes (no explosive, no vapor; fired from the Large machine's capacitor bank): its second mode
    event.create('charged_implosion')
        .setEUIO('in')
        .setMaxIOSize(2, 2, 0, 0)
        .setProgressBar(GuiTextures.PROGRESS_BAR_COMPRESS, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.ARC)

    // dust and a second item in, the plate (and a little residue) out, the vapor in
    event.create('electric_implosion_compressor')
        .setEUIO('in')
        .setMaxIOSize(2, 2, 1, 1)
        .setProgressBar(GuiTextures.PROGRESS_BAR_COMPRESS, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.ARC)
})

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }

    // ---- The single blocks ----
    event.create('electric_implosion_compressor', 'custom')
        .tiers(GTValues.HV, GTValues.EV, GTValues.IV, GTValues.LuV)
        .tankScalingFunction(tier => 16000)
        .addDefaultTooltips(true)
        .addDefaultModel(false)
        .machine((holder, tier, tankScaling) => new $ElectricImplosionTiered(holder, tier, tankScaling))
        .definition((tier, builder) => {
            builder
                .langValue(`${GTValues.VLVH[tier]} Electric Implosion Compressor ${GTValues.VLVT[tier]}`)
                .recipeTypes([GTRecipeTypes.get('electric_implosion_compressor')])
                .recipeModifiers([GTRecipeModifiers.OC_NON_PERFECT])
                .editableUI($ElectricImplosionTiered.EDITABLE_UI_CREATOR.apply(
                    GTCEu.id('electric_implosion_compressor'), GTRecipeTypes.get('electric_implosion_compressor')))
                .workableTieredHullModel(GTCEu.id('block/machines/electric_implosion_compressor'))
                ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.electric_implosion_compressor.tooltip', 2))
        })

    // ---- The multiblock ----
    event.create('large_electric_implosion_compressor', 'multiblock')
        .machine(holder => new $LargeElectricImplosion(holder))
        .langValue('Large Electric Implosion Compressor')
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('electric_implosion_compressor'), GTRecipeTypes.get('charged_implosion')])
        .recipeModifiers([GTRecipeModifiers.PARALLEL_HATCH, GTRecipeModifiers.OC_PERFECT])
        .appearanceBlock(() => Block.getBlock('af9:shock_capacitor_casing'))
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.large_electric_implosion_compressor.tooltip', 4))
        .pattern(definition => FactoryBlockPattern.start()
            .aisle('XXX', 'XCX', 'XXX')
            .aisle('XCX', 'CPC', 'XCX')
            .aisle('XXX', 'XSX', 'XXX')
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count))
            .where('X', Predicates.blocks('af9:shock_capacitor_casing')
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1, 0))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
            .where('C', Predicates.blocks('gtceu:cupronickel_coil_block'))
            .where('P', Predicates.blocks('gtceu:titanium_frame'))
            .build())
        .workableCasingModel('af9:block/shock_capacitor_casing',
            'gtceu:block/multiblock/large_electric_implosion_compressor')
})
