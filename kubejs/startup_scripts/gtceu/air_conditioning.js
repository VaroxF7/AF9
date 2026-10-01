// AF9 - Air Conditioning Hatch: the cooling of the Photolithography Line and Scanner. Behaviour: AF9 Core
// (com.af9.core.machine.part.AirConditioningHatchPartMachine, LithoMachine: cooling units, gate and break factor).
// Recipes: server_scripts/mods/gtceu/litho_process.js. Spec: docs/semiconductor-factory.md §18
//
// The two machines are built as clean rooms, so they cool with air. A print puts 1 (350 nm) to 16 (65 nm) cooling
// units (CU) of heat into the chamber; the hatches of the structure have to carry it away. A hatch is worth 1 CU at MV
// and doubles per tier (HV 2, EV 4, IV 8), two fit. Every doubling above the load makes prints 10% faster and breaks
// 20% fewer wafers. The warm air leaves from the hatch's front (the hook for the Temperature Update).

const $AirConditioningHatch = Java.loadClass('com.af9.core.machine.part.AirConditioningHatchPartMachine')

GTCEuStartupEvents.registry('gtceu:machine', event => {
    event.create('air_conditioning_hatch', 'custom')
        .tiers(GTValues.MV, GTValues.HV, GTValues.EV, GTValues.IV)
        .machine((holder, tier) => new $AirConditioningHatch(holder, tier))
        .definition((tier, builder) => {
            builder
                .langValue(`${GTValues.VN[tier]} Air Conditioning Hatch`)
                .rotationState(RotationState.ALL)
                .abilities($AirConditioningHatch.AIR_CONDITIONING)
                ['overlayTieredHullModel(net.minecraft.resources.ResourceLocation)'](
                    'af9:block/machine/part/air_conditioning')
                ['tooltips(net.minecraft.network.chat.Component[])']([
                    Component.translatable('af9.air_conditioning_hatch.tooltip.0'),
                    Component.translatable('af9.air_conditioning_hatch.tooltip.1'),
                    Component.translatable('af9.air_conditioning_hatch.tooltip.2'),
                    Component.translatable('af9.air_conditioning_hatch.tooltip.3')])
        })
})
