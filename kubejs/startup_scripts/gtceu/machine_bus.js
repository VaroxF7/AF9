// AF9 - The machine bus: the Bus Connector (a machine's, a Central Monitor's or a Bus Controller's port on the bus) and
// the Bus Controller. Behaviour, the Optical Bus Cable and the Central Monitor's Machine Bus Module: af9-core
// com.af9.core.bus. Recipes: server_scripts/mods/gtceu/machine_bus.js. Spec: docs/machine-bus.md
//
// A machine takes one Bus Connector wherever it takes hatches (the AF9 multiblocks: the lithography machines, the SMC
// fab multiblocks, the Particle Accelerator, the Supercooling Cryostat); GT's Central Monitor takes one in its wall
// (af9-core adds it to the wall). The connector is an optical reception hatch too (computation and data), so GT's
// Assembly Line, Research Station, Data Bank and Network Switch take it where their reception hatch goes: the machine
// then draws its CWU/t and research over the bus. Optical Bus Cable joins their front faces, and GT's transmitter
// hatches (HPCA / Network Switch computation, Data Bank research) facing the cable put their CWU/t and research on it.
// A bus serves at most 16 machines and carries at most 1024 CWU/t (research without limit).
//
// The Bus Controller runs up to 4 buses (a Bus Connector on each); its Interconnect Hatch links it, over Optical Bus
// Cable, to the other controllers' Interconnect Hatches on the same run: one network sharing computation, research,
// machines and inputs.

const $BusConnector = Java.loadClass('com.af9.core.bus.BusConnectorPartMachine')
const $BusController = Java.loadClass('com.af9.core.bus.BusControllerMachine')
const $BusInterconnect = Java.loadClass('com.af9.core.bus.BusInterconnectPartMachine')

GTCEuStartupEvents.registry('gtceu:machine', allthemods => {
    allthemods.create('bus_connector', 'custom')
        .tiers(GTValues.MV)
        .machine((holder, tier) => new $BusConnector(holder))
        .definition((tier, builder) => {
            builder
                .langValue('Bus Connector')
                .rotationState(RotationState.ALL)
                .abilities($BusConnector.BUS_CONNECTOR, PartAbility.OPTICAL_DATA_RECEPTION,
                    PartAbility.COMPUTATION_DATA_RECEPTION)
                ['overlayTieredHullModel(net.minecraft.resources.ResourceLocation)'](
                    'af9:block/machine/part/bus_connector')
                ['tooltips(net.minecraft.network.chat.Component[])']([0, 1, 2, 3].map(i =>
                    Component.translatable(`af9.bus_connector.tooltip.${i}`)))
        })

    // Interconnect Hatch: a Bus Controller's link to other Bus Controllers (Optical Bus Cable from its front face)
    allthemods.create('interconnect_hatch', 'custom')
        .tiers(GTValues.MV)
        .machine((holder, tier) => new $BusInterconnect(holder))
        .definition((tier, builder) => {
            builder
                .langValue('Interconnect Hatch')
                .rotationState(RotationState.ALL)
                .abilities($BusInterconnect.BUS_INTERCONNECT)
                ['overlayTieredHullModel(net.minecraft.resources.ResourceLocation)'](
                    'af9:block/machine/part/bus_interconnect')
                ['tooltips(net.minecraft.network.chat.Component[])']([0, 1, 2].map(i =>
                    Component.translatable(`af9.interconnect_hatch.tooltip.${i}`)))
        })

    // Bus Controller (MV): the bus's PLC. It takes items and fluids (plain or ME buses and hatches), picks a recipe
    // for each machine on its buses and keeps each one supplied with a run of ingredients. Up to 4 Bus Connectors (4
    // buses) and 1 Interconnect Hatch. 3 x 3 x 3 of solid steel casing, aisles back -> front (controller), rows
    // bottom -> top; parts anywhere on the shell.
    allthemods.create('bus_controller', 'multiblock')
        .machine(holder => new $BusController(holder))
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeType(GTRecipeTypes.DUMMY_RECIPES)
        .appearanceBlock(GTBlocks.CASING_STEEL_SOLID)
        ['tooltips(net.minecraft.network.chat.Component[])']([0, 1, 2, 3].map(i =>
            Component.translatable(`af9.bus_controller.tooltip.${i}`)))
        .pattern(definition => FactoryBlockPattern.start()
            .aisle('CCC', 'CCC', 'CCC')
            .aisle('CCC', 'C#C', 'CCC')
            .aisle('CCC', 'CSC', 'CCC')
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count))
            .where('C', Predicates.blocks(GTBlocks.CASING_STEEL_SOLID.get())
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(8, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(4, 1))
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities($BusConnector.BUS_CONNECTOR).setMaxGlobalLimited(4, 1))
                .or(Predicates.abilities($BusInterconnect.BUS_INTERCONNECT).setMaxGlobalLimited(1, 1)))
            .where('#', Predicates.any())
            .build())
        .workableCasingModel('gtceu:block/casings/solid/machine_casing_solid_steel',
            'gtceu:block/multiblock/network_switch')
})
