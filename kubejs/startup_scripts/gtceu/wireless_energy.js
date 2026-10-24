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
//   LuV 16A   ZPM 100A   UV 256A   UHV 1000A

const $WirelessTransmitterHatch = Java.loadClass('com.af9.core.wireless.WirelessTransmitterHatch')
const $WirelessReceiverHatch = Java.loadClass('com.af9.core.wireless.WirelessReceiverHatch')

GTCEuStartupEvents.registry('gtceu:machine', event => {
    // hull tier -> [maximum amps, GT model of the receiver, GT model of the transmitter]
    const variants = {}
    variants[GTValues.LuV] = [16, 'energy_input_hatch_16a', 'energy_output_hatch_16a']
    variants[GTValues.ZPM] = [100, 'energy_input_hatch_64a', 'energy_output_hatch_64a']
    variants[GTValues.UV] = [256, 'laser_target_hatch', 'laser_source_hatch']
    variants[GTValues.UHV] = [1000, 'laser_target_hatch', 'laser_source_hatch']
    const tiers = [GTValues.LuV, GTValues.ZPM, GTValues.UV, GTValues.UHV]
    const tooltips = (key, amps) => [0, 1, 2].map(i => Component.translatable(`${key}.${i}`, `${amps}`))

    event.create('wireless_energy_receiver', 'custom')
        .tiers(tiers[0], tiers[1], tiers[2], tiers[3])
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

    event.create('wireless_energy_transmitter', 'custom')
        .tiers(tiers[0], tiers[1], tiers[2], tiers[3])
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

// Wireless Computation Transmitter / Receiver (behaviour: af9-core WirelessComputationHatch). The transmitter replaces GT's
// Computation Data Transmission Hatch on an HPCA, the receiver GT's Reception Hatch on any machine that uses computation.
// The ID typed on the hatch's screen pairs them; it is saved in the hatches themselves, so a restart or an unloaded chunk
// never breaks a link (the receiver finds its transmitter again as soon as that loads).
const $WirelessComputationHatch = Java.loadClass('com.af9.core.wireless.WirelessComputationHatch')

GTCEuStartupEvents.registry('gtceu:machine', event => {
    event.create('wireless_computation_receiver', 'custom')
        .tiers(GTValues.UV)
        .machine((holder, tier) => new $WirelessComputationHatch(holder, false))
        .definition((tier, builder) => {
            builder
                .langValue('Wireless Computation Reception Hatch')
                .rotationState(RotationState.ALL)
                .abilities(PartAbility.COMPUTATION_DATA_RECEPTION)
                ['overlayTieredHullModel(java.lang.String)']('computation_data_hatch')
                ['tooltips(net.minecraft.network.chat.Component[])']([
                    Component.translatable('af9.wireless_cwu_receiver.tooltip.0'),
                    Component.translatable('af9.wireless_cwu_receiver.tooltip.1')])
        })

    event.create('wireless_computation_transmitter', 'custom')
        .tiers(GTValues.UV)
        .machine((holder, tier) => new $WirelessComputationHatch(holder, true))
        .definition((tier, builder) => {
            builder
                .langValue('Wireless Computation Transmission Hatch')
                .rotationState(RotationState.ALL)
                .abilities(PartAbility.COMPUTATION_DATA_TRANSMISSION)
                ['overlayTieredHullModel(java.lang.String)']('computation_data_hatch')
                ['tooltips(net.minecraft.network.chat.Component[])']([
                    Component.translatable('af9.wireless_cwu_transmitter.tooltip.0'),
                    Component.translatable('af9.wireless_cwu_transmitter.tooltip.1')])
        })
})

// Network Node (af9-core NetworkNodeMachine): the Data Bank's structure, but its data items store computation (stick 1,000
// CWU, orb 10,000, module 100,000; up to nine data hatches). Computation reception hatches fill it, transmission hatches give
// it out. The controller face is the Network Switch's.
const $NetworkNodeMachine = Java.loadClass('com.af9.core.machine.NetworkNodeMachine')

GTCEuStartupEvents.registry('gtceu:machine', event => {
    event.create('network_node', 'multiblock')
        .langValue('Network Node')
        .machine(holder => new $NetworkNodeMachine(holder))
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.DUMMY_RECIPES])
        .appearanceBlock(() => Block.getBlock('gtceu:computer_casing'))
        ['tooltips(net.minecraft.network.chat.Component[])']([0, 1, 2, 3].map(i =>
            Component.translatable(`af9.network_node.tooltip.${i}`)))
        .pattern(definition => FactoryBlockPattern.start()
            .aisle('XDDDX', 'XDDDX', 'XDDDX')
            .aisle('XDDDX', 'XAAAX', 'XDDDX')
            .aisle('XCCCX', 'XCSCX', 'XCCCX')
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            .where('X', Predicates.blocks('gtceu:computer_heat_vent'))
            .where('D', Predicates.blocks('gtceu:computer_casing')
                .or(Predicates.abilities(PartAbility.DATA_ACCESS).setMaxGlobalLimited(9, 3))
                .or(Predicates.abilities(PartAbility.COMPUTATION_DATA_RECEPTION).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.COMPUTATION_DATA_TRANSMISSION).setMaxGlobalLimited(2, 1)))
            .where('A', Predicates.blocks('gtceu:computer_casing'))
            .where('C', Predicates.blocks('gtceu:high_power_casing')
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
            .build())
        .workableCasingModel('gtceu:block/casings/hpca/high_power_casing', 'gtceu:block/multiblock/network_switch')
})
