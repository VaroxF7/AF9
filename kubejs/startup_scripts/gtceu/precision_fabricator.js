// AF9 - High Temperature Precision Fabricator (TerraFirmaGreg's boule machine, available at MV). The seed crystal on
// its holder rod is dipped into liquid polysilicon in a quartz crucible and pulled: silicon, phosphorus, naquadah and
// neutronium boules. A small steel hall: a layer of steel firebox round the controller and a layer of solid steel casing
// over it, a steel gearbox turning the rod. Hatches: the Precision Fabricator Input Port and Output Port (an input hatch
// and bus in one, and the output bus) and one energy hatch, in the solid casings of the bottom layer. No maintenance, no
// muffler, no laser, no parallel hatch; batch mode and perfect overclocking only. Heat is no coil any more: the boule's
// voltage tier (the hatch) sets what the machine can pull.
// Recipes: server_scripts/mods/gtceu/precision_fabricator.js. Ports: af9-core PrecisionPortPartMachine.

const $PrecisionFab = Java.loadClass('com.af9.core.machine.PrecisionFabricatorMachine')
const $PrecisionPort = Java.loadClass('com.af9.core.machine.part.PrecisionPortPartMachine')
const $PrecisionIO = Java.loadClass('com.gregtechceu.gtceu.api.capability.recipe.IO')

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    // seed, dopant and the crucible in (the holder rod and the circuit are kept); the polysilicon, a dopant and a gas in
    event.create('high_temperature_precision_fabricator')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(6, 1, 3, 0)
        .setProgressBar(GuiTextures.PROGRESS_BAR_CRYSTALLIZATION, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.FURNACE)
})

GTCEuStartupEvents.registry('gtceu:machine', event => {
    // Precision Fabricator Input Port: input hatch and input bus in one. 9 fluid tanks (3 x 3), 3 item inputs, 1
    // non-consumable slot, a ghost circuit (the circuit button on the screen). Tank: 80,000 mB each.
    // Output Port: the one item output slot. One tier only, MV: the energy hatch sets the voltage the boules run at.
    event.create('precision_fabricator_input_port', 'custom')
        .tiers(GTValues.MV)
        .machine((holder, tier) => new $PrecisionPort(holder, tier, $PrecisionIO.IN))
        .definition((tier, builder) => {
            builder
                .langValue(`${GTValues.VN[tier]} Precision Fabricator Input Port`)
                .rotationState(RotationState.ALL)
                .abilities($PrecisionPort.INPUT)
                ['colorOverlayTieredHullModel(java.lang.String,java.lang.String,java.lang.String)'](
                    'overlay_pipe_in_emissive', null, 'overlay_fluid_hatch')
                ['tooltips(net.minecraft.network.chat.Component[])']([
                    Component.translatable('af9.precision_port.input.tooltip.0'),
                    Component.translatable('af9.precision_port.input.tooltip.1'),
                    Component.translatable('gtceu.universal.tooltip.fluid_storage_capacity_mult', 9,
                        `${$PrecisionPort.TANK_CAPACITY}`)])
        })
    event.create('precision_fabricator_output_port', 'custom')
        .tiers(GTValues.MV)
        .machine((holder, tier) => new $PrecisionPort(holder, tier, $PrecisionIO.OUT))
        .definition((tier, builder) => {
            builder
                .langValue(`${GTValues.VN[tier]} Precision Fabricator Output Port`)
                .rotationState(RotationState.ALL)
                .abilities($PrecisionPort.OUTPUT)
                ['colorOverlayTieredHullModel(java.lang.String,java.lang.String,java.lang.String)'](
                    'overlay_pipe_out_emissive', null, 'overlay_fluid_hatch')
                ['tooltips(net.minecraft.network.chat.Component[])']([
                    Component.translatable('af9.precision_port.output.tooltip.0')])
        })

    // The hall (3 wide x 2 high x 2 deep), aisles back -> front (the controller's last), rows bottom -> top:
    //   bottom layer: back row solid steel casings (the hatches go here), front row firebox, controller, firebox
    //   top layer:    back row solid casing, steel gearbox, solid casing; front row solid casings
    // Hatches: a maximum only, never a required count.
    event.create('high_temperature_precision_fabricator', 'multiblock')
        .langValue('High Temperature Precision Fabricator')
        .machine(holder => new $PrecisionFab(holder))
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('high_temperature_precision_fabricator')])
        .recipeModifiers([GTRecipeModifiers.OC_PERFECT, GTRecipeModifiers.BATCH_MODE])
        .appearanceBlock(GTBlocks.CASING_STEEL_SOLID)
        ['tooltips(net.minecraft.network.chat.Component[])']([0, 1, 2].map(i =>
            Component.translatable(`af9.high_temperature_precision_fabricator.tooltip.${i}`)))
        .pattern(definition => FactoryBlockPattern.start()
            .aisle('XXX', 'CGC')
            .aisle('FSF', 'CCC')
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            .where('F', Predicates.blocks(GTBlocks.FIREBOX_STEEL.get()))
            .where('G', Predicates.blocks('gtceu:steel_gearbox'))
            .where('C', Predicates.blocks(GTBlocks.CASING_STEEL_SOLID.get()))
            .where('X', Predicates.blocks(GTBlocks.CASING_STEEL_SOLID.get())
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities($PrecisionPort.INPUT).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities($PrecisionPort.OUTPUT).setMaxGlobalLimited(1, 1)))
            .build())
        .workableCasingModel('gtceu:block/casings/solid/machine_casing_solid_steel',
            'gtceu:block/multiblock/electric_blast_furnace')
})
