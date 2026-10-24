// AF9 - LV and MV Quadruple Input / Output Hatch. GT's quad hatches start at EV; these are the same part (GT's own
// FluidHatchPartMachine with four tanks, registered like GT's registerFluidHatches: the quad pipe overlay and BOTH the
// plain IMPORT_FLUIDS / EXPORT_FLUIDS ability and the 4X one, which is what multiblock patterns ask for) at LV and MV. Recipes: server_scripts/mods/gtceu/quad_hatches.js
// Ids: gtceu:<lv|mv>_input_hatch_4x, gtceu:<lv|mv>_output_hatch_4x (GT's naming).

const $QuadFluidHatch = Java.loadClass('com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine')
const $QuadIO = Java.loadClass('com.gregtechceu.gtceu.api.capability.recipe.IO')

GTCEuStartupEvents.registry('gtceu:machine', event => {
    // GT's initial tank size of a quad hatch (it scales it with the tier itself)
    const initialCapacity = $QuadFluidHatch.INITIAL_TANK_CAPACITY_4X || 2000

    event.create('input_hatch_4x', 'custom')
        .tiers(GTValues.LV, GTValues.MV)
        .machine((holder, tier) => new $QuadFluidHatch(holder, tier, $QuadIO.IN, initialCapacity, 4))
        .definition((tier, builder) => {
            builder
                .langValue(`${GTValues.VN[tier]} Quadruple Input Hatch`)
                .rotationState(RotationState.ALL)
                ['colorOverlayTieredHullModel(java.lang.String,java.lang.String,java.lang.String)'](
                    'overlay_pipe_in_emissive', 'overlay_pipe_4x', 'overlay_fluid_hatch')
                .abilities(PartAbility.IMPORT_FLUIDS, PartAbility.IMPORT_FLUIDS_4X)
                ['tooltips(net.minecraft.network.chat.Component[])']([
                    Component.translatable('gtceu.machine.fluid_hatch.import.tooltip'),
                    Component.translatable('gtceu.universal.tooltip.fluid_storage_capacity_mult', 4,
                        `${initialCapacity << tier}`)])
        })

    event.create('output_hatch_4x', 'custom')
        .tiers(GTValues.LV, GTValues.MV)
        .machine((holder, tier) => new $QuadFluidHatch(holder, tier, $QuadIO.OUT, initialCapacity, 4))
        .definition((tier, builder) => {
            builder
                .langValue(`${GTValues.VN[tier]} Quadruple Output Hatch`)
                .rotationState(RotationState.ALL)
                ['colorOverlayTieredHullModel(java.lang.String,java.lang.String,java.lang.String)'](
                    'overlay_pipe_out_emissive', 'overlay_pipe_4x', 'overlay_fluid_hatch')
                .abilities(PartAbility.EXPORT_FLUIDS, PartAbility.EXPORT_FLUIDS_4X)
                ['tooltips(net.minecraft.network.chat.Component[])']([
                    Component.translatable('gtceu.machine.fluid_hatch.export.tooltip'),
                    Component.translatable('gtceu.universal.tooltip.fluid_storage_capacity_mult', 4,
                        `${initialCapacity << tier}`)])
        })
})
