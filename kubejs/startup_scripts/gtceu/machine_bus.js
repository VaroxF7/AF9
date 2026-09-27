// AF9 - The machine bus: the Bus Connector (a machine's or a Central Monitor's port on the bus). Behaviour, the Polycat
// Cable and the Central Monitor's Machine Bus Module: af9-core com.af9.core.bus. Recipes:
// server_scripts/mods/gtceu/machine_bus.js. Spec: docs/machine-bus.md
//
// A machine takes one Bus Connector wherever it takes hatches (the AF9 multiblocks: the lithography machines, the SMC
// fab multiblocks, the Particle Accelerator, the Supercooling Cryostat); GT's Central Monitor takes one in its wall
// (af9-core adds it to the wall). Polycat Cable joins their front faces.

const $BusConnector = Java.loadClass('com.af9.core.bus.BusConnectorPartMachine')

GTCEuStartupEvents.registry('gtceu:machine', allthemods => {
    allthemods.create('bus_connector', 'custom')
        .tiers(GTValues.MV)
        .machine((holder, tier) => new $BusConnector(holder))
        .definition((tier, builder) => {
            builder
                .langValue('Bus Connector')
                .rotationState(RotationState.ALL)
                .abilities($BusConnector.BUS_CONNECTOR)
                ['overlayTieredHullModel(net.minecraft.resources.ResourceLocation)'](
                    'af9:block/machine/part/bus_connector')
                ['tooltips(net.minecraft.network.chat.Component[])']([0, 1, 2].map(i =>
                    Component.translatable(`af9.bus_connector.tooltip.${i}`)))
        })
})
