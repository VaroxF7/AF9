// AF9 - The lithography process around the print: wafer clean-up, the finer coatings.
// The machines and their recipe types here; items, materials and behaviour: AF9 Core (registry/AF9Items,
// registry/AF9Materials, LithoMachine); recipes: server_scripts/mods/gtceu/litho_process.js. Spec:
// docs/semiconductor-factory.md §18

// ---- Coater Track ----
// The spin-coat track in front of the exposure tool (a real fab's track does the coating and the developing; here the line
// keeps the developing): HMDS prime, bottom anti-reflective coat, resist spun on, topcoat, soft bake. It turns a blank
// wafer into the coated wafer of its node, and what it spins off is spent solvent. Plain GT machine logic, no AF9 Core class.
const $CoaterMachine = Java.loadClass('com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine')
const $CoaterDirection = Java.loadClass('com.gregtechceu.gtceu.api.pattern.util.RelativeDirection')

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    // a blank wafer in, the coated wafer out; the track's chemicals in, the spent solvent out
    event.create('wafer_coating')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(2, 1, 5, 2)
        .setProgressBar(GuiTextures.PROGRESS_BAR_MIXER, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.BATH)
})

GTCEuStartupEvents.registry('gtceu:machine', event => {
    // 3 wide x 3 high x 6 long, plascrete like the other lithography machines: the front with the controller, the
    // dispense lines over the bowl (plascrete pipe casing), the spin chuck (a gearbox in the floor), the hotplate (heatproof
    // casing), the chill plate, the back. Aisles front -> back, rows bottom -> top; hatches on any plascrete.
    event.create('wafer_coater', 'multiblock')
        .langValue('Coater Track')
        .machine(holder => new $CoaterMachine(holder))
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('wafer_coating')])
        .recipeModifiers([GTRecipeModifiers.PARALLEL_HATCH, GTRecipeModifiers.OC_NON_PERFECT])
        .appearanceBlock(() => Block.getBlock('gtceu:plascrete'))
        ['tooltips(net.minecraft.network.chat.Component[])']([0, 1, 2, 3].map(i =>
            Component.translatable(`af9.wafer_coater.tooltip.${i}`)))
        .pattern(definition => FactoryBlockPattern.start($CoaterDirection.LEFT, $CoaterDirection.UP,
            $CoaterDirection.BACK)
            .aisle('CCC', 'CMC', 'CCC') // front with the controller
            .aisle('CCC', 'W#W', 'CPC') // dispense lines
            .aisle('CRC', 'W#W', 'CFC') // spin chuck
            .aisle('CHC', 'W#W', 'CFC') // hotplate
            .aisle('CCC', 'W#W', 'CFC') // chill plate
            .aisle('CCC', 'CCC', 'CCC') // back
            .where('M', Predicates.controller(Predicates.blocks(definition.get())))
            // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count))
            .where('C', Predicates.blocks('gtceu:plascrete')
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(6, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1, 0))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
            .where('P', Predicates.blocks('af9:plascrete_pipe_casing'))       // chemical dispense lines
            .where('F', Predicates.blocks('af9:plascrete_filter_casing'))     // fan filter units
            .where('R', Predicates.blocks('gtceu:stainless_steel_gearbox'))      // spin chuck
            .where('H', Predicates.blocks('gtceu:heatproof_machine_casing'))     // hotplate
            .where('W', Predicates.blocks('gtceu:cleanroom_glass'))              // windows of the track
            .where('#', Predicates.air())                                        // the track
            .build())
        .workableCasingModel('gtceu:block/casings/cleanroom/plascrete', 'gtceu:block/multiblock/network_switch')
})
