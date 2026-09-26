// AF9 - Wireless energy hatches (behaviour: af9-core com.af9.core.wireless). Recipes:
// server_scripts/mods/gtceu/wireless_energy.js
//
// Wireless Energy Transmitter: an output (dynamo) hatch for the Power Substation (or any multiblock that puts out
// energy). Wireless Energy Receiver: an energy input hatch for any multiblock. A data stick links them: right-click the
// transmitter to take its link, right-click a receiver to link it (shift-right-click a receiver copies its link). Any
// distance, any dimension: the energy goes through a channel saved with the world.
//
// The hatches have no voltage of their own, only a maximum amperage: the transmitter sends at the highest voltage of
// its multiblock's energy inputs (a substation fed with UV hatches sends UV; without inputs, set it on its screen), the
// receiver works at its transmitter's voltage. The hull tier of a variant is its look and crafting tier only:
//   EV 2A   IV 4A   LuV 16A   ZPM 100A   UV 256A   UHV 1000A

const $WirelessTransmitterHatch = Java.loadClass('com.af9.core.wireless.WirelessTransmitterHatch')
const $WirelessReceiverHatch = Java.loadClass('com.af9.core.wireless.WirelessReceiverHatch')

GTCEuStartupEvents.registry('gtceu:machine', allthemods => {
    // hull tier -> [maximum amps, GT model of the receiver, GT model of the transmitter]
    const variants = {}
    variants[GTValues.EV] = [2, 'energy_input_hatch', 'energy_output_hatch']
    variants[GTValues.IV] = [4, 'energy_input_hatch_4a', 'energy_output_hatch_4a']
    variants[GTValues.LuV] = [16, 'energy_input_hatch_16a', 'energy_output_hatch_16a']
    variants[GTValues.ZPM] = [100, 'energy_input_hatch_64a', 'energy_output_hatch_64a']
    variants[GTValues.UV] = [256, 'laser_target_hatch', 'laser_source_hatch']
    variants[GTValues.UHV] = [1000, 'laser_target_hatch', 'laser_source_hatch']
    const tiers = [GTValues.EV, GTValues.IV, GTValues.LuV, GTValues.ZPM, GTValues.UV, GTValues.UHV]
    const tooltips = (key, amps) => [0, 1, 2].map(i => Component.translatable(`${key}.${i}`, `${amps}`))

    allthemods.create('wireless_energy_receiver', 'custom')
        .tiers(tiers[0], tiers[1], tiers[2], tiers[3], tiers[4], tiers[5])
        .machine((holder, tier) => new $WirelessReceiverHatch(holder, tier, variants[tier][0]))
        .definition((tier, builder) => {
            builder
                .langValue(`Wireless Energy Receiver (${variants[tier][0]}A)`)
                .rotationState(RotationState.ALL)
                .abilities(PartAbility.INPUT_ENERGY)
                // GT has a String and a ResourceLocation version: name the String one (a GT part model)
                ['overlayTieredHullModel(java.lang.String)'](variants[tier][1])
                ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.wireless_receiver.tooltip',
                    variants[tier][0]))
        })

    allthemods.create('wireless_energy_transmitter', 'custom')
        .tiers(tiers[0], tiers[1], tiers[2], tiers[3], tiers[4], tiers[5])
        .machine((holder, tier) => new $WirelessTransmitterHatch(holder, tier, variants[tier][0]))
        .definition((tier, builder) => {
            builder
                .langValue(`Wireless Energy Transmitter (${variants[tier][0]}A)`)
                .rotationState(RotationState.ALL)
                .abilities(PartAbility.OUTPUT_ENERGY)
                ['overlayTieredHullModel(java.lang.String)'](variants[tier][2])
                ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.wireless_transmitter.tooltip',
                    variants[tier][0]))
        })
})
