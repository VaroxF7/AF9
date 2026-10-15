// AF9 - Fiber Link Port and Logistics Hub: the brain and logistics centre of the Photolithography Scanner Mk2.
// The hub is a small 4 x 2 x 2 multiblock that stands anywhere; a Fiber Link Port in the scanner and one in the hub are
// linked with a data stick (right-click one port, then the other; shift-right-click cuts the link). Everything put into
// the hub's input buses and hatches reaches the scanner, everything the scanner makes comes out of the hub's output
// buses and hatches: items, fluids and gases. Behaviour: AF9 Core (com.af9.core.fiber).
// The Logistics Rack (2 wide x 3 high x 1 deep) adds onto the scanner's ports: the scanner takes a second Fiber Link Port,
// the rack has its own, and it brings four more buses and hatches. Same behaviour as the hub.
// Data (computation) is not carried yet: the scanner keeps its own computation hatch.

const $FiberLinkPort = Java.loadClass('com.af9.core.fiber.FiberLinkPortPartMachine')
const $LogisticsHub = Java.loadClass('com.af9.core.fiber.LogisticsHubMachine')
const $HubRelativeDirection = Java.loadClass('com.gregtechceu.gtceu.api.pattern.util.RelativeDirection')

GTCEuStartupEvents.registry('gtceu:machine', event => {
    event.create('fiber_link_port', 'custom')
        .tiers(GTValues.EV)
        .machine((holder, tier) => new $FiberLinkPort(holder, tier))
        .definition((tier, builder) => {
            builder
                .langValue('Fiber Link Port')
                .rotationState(RotationState.ALL)
                .abilities($FiberLinkPort.FIBER_LINK)
                ['overlayTieredHullModel(net.minecraft.resources.ResourceLocation)'](
                    'af9:block/machine/part/fiber_link_port')
                ['tooltips(net.minecraft.network.chat.Component[])']([
                    Component.translatable('af9.fiber_link_port.tooltip.0'),
                    Component.translatable('af9.fiber_link_port.tooltip.1'),
                    Component.translatable('af9.fiber_link_port.tooltip.2')])
        })
})

GTCEuStartupEvents.registry('gtceu:machine', event => {
    event.create('logistics_hub', 'multiblock')
        .machine(holder => new $LogisticsHub(holder))
        .langValue('Logistics Hub')
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeType(GTRecipeTypes.DUMMY_RECIPES)
        .appearanceBlock(() => Block.getBlock('gtceu:plascrete'))
        ['tooltips(net.minecraft.network.chat.Component[])']([
            Component.translatable('af9.logistics_hub.tooltip.0'),
            Component.translatable('af9.logistics_hub.tooltip.1'),
            Component.translatable('af9.logistics_hub.tooltip.2'),
            Component.translatable('af9.logistics_hub.tooltip.3')])
        // 4 wide x 2 high x 2 deep, plascrete like the scanner. Aisles front -> back, rows bottom -> top. The
        // controller is in the front, bottom row; the Fiber Link Port is the one block at the back, top row, left.
        .pattern(definition => FactoryBlockPattern.start($HubRelativeDirection.LEFT, $HubRelativeDirection.UP,
            $HubRelativeDirection.BACK)
            .aisle('CMCC', 'CCCC')
            .aisle('CCCC', 'LCCC')
            .where('M', Predicates.controller(Predicates.blocks(definition.get())))
            .where('L', Predicates.abilities($FiberLinkPort.FIBER_LINK))
            // parts have a maximum only, never a required count; any plascrete of the shell may be one
            .where('C', Predicates.blocks('gtceu:plascrete')
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(4, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(4, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(4, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(4, 1))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
            .build())
        .workableCasingModel('gtceu:block/casings/cleanroom/plascrete',
            'gtceu:block/multiblock/gcym/large_engraving_laser')
})

GTCEuStartupEvents.registry('gtceu:machine', event => {
    event.create('logistics_rack', 'multiblock')
        .machine(holder => new $LogisticsHub(holder))
        .langValue('Logistics Rack')
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeType(GTRecipeTypes.DUMMY_RECIPES)
        .appearanceBlock(() => Block.getBlock('gtceu:plascrete'))
        ['tooltips(net.minecraft.network.chat.Component[])']([
            Component.translatable('af9.logistics_rack.tooltip.0'),
            Component.translatable('af9.logistics_rack.tooltip.1'),
            Component.translatable('af9.logistics_rack.tooltip.2')])
        // 2 wide x 3 high x 1 deep, standing upright. The controller is the bottom row, left; the port the top row, left.
        .pattern(definition => FactoryBlockPattern.start($HubRelativeDirection.LEFT, $HubRelativeDirection.UP,
            $HubRelativeDirection.BACK)
            .aisle('MC', 'CC', 'LC')
            .where('M', Predicates.controller(Predicates.blocks(definition.get())))
            .where('L', Predicates.abilities($FiberLinkPort.FIBER_LINK))
            .where('C', Predicates.blocks('gtceu:plascrete')
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 0))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2, 0))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(2, 0))
                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(2, 0)))
            .build())
        .workableCasingModel('gtceu:block/casings/cleanroom/plascrete',
            'gtceu:block/multiblock/gcym/large_engraving_laser')
})
