// AF9 - SMC fab machines: the plant for semiconductor chemistry. Spec: docs/semiconductor-factory.md §11.
// Recipes: server_scripts/mods/gtceu/fab_chemistry.js and photolithography.js; crafting: fab_machines.js there.
//
// GT's own machines (Chemical Reactor, Large Chemical Reactor, mixer, blast furnace, distillation tower, ...) have
// none of these recipes: fab chemistry runs only in the four SMC families below. Each family is one multiblock and
// tiered single blocks (MV-LuV); machine modes are GT recipe types (GT's mode tab, or the console's mode tiles).
//
//   Chemistry         SMC Large Chemical Reactor      | SMC Chemical Reactor       synthesis, blending, wet processing
//   Separation        SMC Rectification Column        | SMC Fractionating Still    distillation*, cryogenic
//                                                                                   rectification*, fractionation,
//                                                                                   purification  (* column only)
//   Electrochemistry  SMC Membrane Cell Hall          | SMC Electrolytic Cell      electrolysis, electrofluorination
//   Thermal           SMC Thermal Processing Furnace  | SMC Thermal Furnace        calcination, CVD, crystal growth
//                     (horizontal tube furnace)
//
// Behaviour from AF9 Core (af9-core/, com.af9.core.fab and com.af9.core.machine.fab):
// - product changeover: the first run of a different recipe also takes the family's purge fluid and extra time
// - multiblocks with a roof of filter casings are their own clean room (sterile filters: ISO 3, both cleanroom types);
//   the MV Plascrete Filter Casing (photolithography.js) is the first choice of the preview and the auto-build
// - the SMC LCR's vessel shows its mode's fluid while it runs (SmcReactorMachine, AF9MachineModels)
// - PTFE Pipe Casings in the column (trays) and the cell hall (membranes) are parallels
// - consoles instead of GT's text display; single-block furnaces reach a fixed temperature per tier

const $FabFamily = Java.loadClass('com.af9.core.fab.FabFamily')
const $FabModifiers = Java.loadClass('com.af9.core.fab.FabModifiers')
const $FabBusConnector = Java.loadClass('com.af9.core.bus.BusConnectorPartMachine')
const $FabMultiblockMachine = Java.loadClass('com.af9.core.machine.fab.FabMultiblockMachine')
const $FabTieredMachine = Java.loadClass('com.af9.core.machine.fab.FabTieredMachine')
const $FabSimpleTieredMachine = Java.loadClass('com.gregtechceu.gtceu.api.machine.SimpleTieredMachine')
const $FabRelativeDirection = Java.loadClass('com.gregtechceu.gtceu.api.pattern.util.RelativeDirection')
const $SmcReactorMachine = Java.loadClass('com.af9.core.machine.fab.SmcReactorMachine')
const $AF9MachineModels = Java.loadClass('com.af9.core.machine.AF9MachineModels')
const $AF9Filters = Java.loadClass('com.af9.core.pattern.AF9Filters')

// Slot layouts [items in, items out, fluids in, fluids out]. A single block's slots come from its first mode, so the
// modes a single block has share one layout. Every layout keeps one fluid input free for the changeover purge.
GTCEuStartupEvents.registry('gtceu:recipe_type', allthemods => {
    const fabTypes = [
        ['fab_synthesis', [3, 2, 4, 3], GuiTextures.PROGRESS_BAR_ARROW_MULTIPLE, GTSoundEntries.CHEMICAL],
        ['fab_blending', [3, 2, 4, 3], GuiTextures.PROGRESS_BAR_MIXER, GTSoundEntries.MIXER],
        ['fab_wet_processing', [3, 2, 4, 3], GuiTextures.PROGRESS_BAR_BATH, GTSoundEntries.BATH],
        ['fab_distillation', [1, 1, 2, 6], GuiTextures.PROGRESS_BAR_ARROW_MULTIPLE, GTSoundEntries.CHEMICAL],
        ['fab_cryogenic_rectification', [1, 1, 2, 6], GuiTextures.PROGRESS_BAR_ARROW_MULTIPLE, GTSoundEntries.COOLING],
        ['fab_fractionation', [2, 1, 3, 2], GuiTextures.PROGRESS_BAR_ARROW_MULTIPLE, GTSoundEntries.BOILER],
        ['fab_purification', [2, 1, 3, 2], GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.CHEMICAL],
        ['fab_electrolysis', [2, 2, 3, 4], GuiTextures.PROGRESS_BAR_EXTRACT, GTSoundEntries.ELECTROLYZER],
        ['fab_electrofluorination', [2, 2, 3, 4], GuiTextures.PROGRESS_BAR_EXTRACT, GTSoundEntries.ELECTROLYZER],
        ['fab_calcination', [3, 2, 2, 2], GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.FURNACE],
        ['fab_cvd', [3, 2, 2, 2], GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.FURNACE],
        ['fab_crystal_growth', [3, 2, 2, 2], GuiTextures.PROGRESS_BAR_CRYSTALLIZATION, GTSoundEntries.FURNACE],
    ]
    fabTypes.forEach(([id, io, bar, sound]) => {
        allthemods.create(id)
            .category('af9_fab')
            .setEUIO('in')
            .setMaxIOSize(io[0], io[1], io[2], io[3])
            .setProgressBar(bar, FillDirection.LEFT_TO_RIGHT)
            .setSound(sound)
    })
})

GTCEuStartupEvents.registry('gtceu:machine', allthemods => {
    const types = ids => ids.map(id => GTRecipeTypes.get(id))
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Single blocks, MV-LuV. Crafted from GT's machine of the same tier (server fab_machines.js).
    // ---------------------------------------------------------------------------------------------------------------
    const growingTanks = tier => Math.min(64000, 16000 << Math.max(0, tier - GTValues.MV))
    const singles = [
        // id, English name, family, modes, GT overlay, tank size per tier, tooltip lines
        ['smc_chemical_reactor', 'SMC Chemical Reactor', 'CHEMISTRY',
            ['fab_synthesis', 'fab_blending', 'fab_wet_processing'], 'chemical_reactor', tier => 16000, 3],
        ['smc_fractionating_still', 'SMC Fractionating Still', 'SEPARATION',
            ['fab_fractionation', 'fab_purification'], 'distillery', growingTanks, 3],
        ['smc_electrolytic_cell', 'SMC Electrolytic Cell', 'ELECTROCHEMISTRY',
            ['fab_electrolysis', 'fab_electrofluorination'], 'electrolyzer', growingTanks, 3],
        ['smc_thermal_furnace', 'SMC Thermal Furnace', 'THERMAL',
            ['fab_calcination', 'fab_cvd', 'fab_crystal_growth'], 'arc_furnace', tier => 16000, 3],
    ]
    singles.forEach(([id, name, familyName, modes, overlay, tanks, lines]) => {
        const family = $FabFamily[familyName]
        const thermal = familyName === 'THERMAL'
        const modifiers = thermal
            ? [$FabModifiers.TIER_TEMPERATURE, GTRecipeModifiers.OC_NON_PERFECT, $FabModifiers.PURGE]
            : [GTRecipeModifiers.OC_NON_PERFECT, $FabModifiers.PURGE]
        allthemods.create(id, 'custom')
            .tiers(GTValues.MV, GTValues.HV, GTValues.EV, GTValues.IV, GTValues.LuV)
            .tankScalingFunction(tanks)
            .addDefaultTooltips(true)
            .addDefaultModel(false)
            .machine((holder, tier, tankScaling) => new $FabTieredMachine(holder, tier, tankScaling, family))
            .definition((tier, builder) => {
                const lineTooltips = tooltips(`af9.${id}.tooltip`, lines)
                if (thermal) {
                    lineTooltips.push(Component.translatable('af9.smc_thermal_furnace.tooltip.temperature',
                        $FabTieredMachine.temperatureOf(tier)))
                }
                builder
                    .langValue(`${GTValues.VLVH[tier]} ${name} ${GTValues.VLVT[tier]}`)
                    .recipeTypes(types(modes))
                    .recipeModifiers(modifiers)
                    .editableUI($FabSimpleTieredMachine.EDITABLE_UI_CREATOR.apply(GTCEu.id(id),
                        GTRecipeTypes.get(modes[0])))
                    .workableTieredHullModel(GTCEu.id(`block/machines/${overlay}`))
                    ['tooltips(net.minecraft.network.chat.Component[])'](lineTooltips)
            })
    })

    // ---------------------------------------------------------------------------------------------------------------
    // Multiblocks. Hatches, buses, a maintenance hatch, a parallel hatch, a laser hatch and a Bus Connector go on any
    // casing; every part has a maximum only, never a required count (setMaxGlobalLimited(max, preview count)).
    // ---------------------------------------------------------------------------------------------------------------
    const hatches = casing => Predicates.blocks(casing)
        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(4, 1))
        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(4, 1))
        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(8, 1))
        .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(8, 1))
        .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1))
        .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1, 1))
        .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1, 0))
        .or(Predicates.abilities($FabBusConnector.BUS_CONNECTOR).setMaxGlobalLimited(1, 0))

    // SMC LCR: GT's Large Chemical Reactor core (PTFE stirrer, one heating coil in the jacket) sealed in a
    // cleanroom-glass mini-environment with a fan filter unit ceiling. 5 x 5 x 4. The 3 x 3 x 2 vessel inside is open:
    // while the reactor runs, its mode's fluid shows there through the glass.
    allthemods.create('smc_large_chemical_reactor', 'multiblock')
        .machine(holder => new $SmcReactorMachine(holder))
        .langValue('SMC Large Chemical Reactor')
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes(types(['fab_synthesis', 'fab_blending', 'fab_wet_processing']))
        .recipeModifiers([$FabModifiers.STRUCTURE_PARALLEL, GTRecipeModifiers.PARALLEL_HATCH,
            $FabModifiers.COIL_DISCOUNT, GTRecipeModifiers.OC_PERFECT_SUBTICK, GTRecipeModifiers.BATCH_MODE,
            $FabModifiers.PURGE])
        .appearanceBlock(GTBlocks.CASING_PTFE_INERT)
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.smc_large_chemical_reactor.tooltip', 7))
        // aisles back -> front, rows bottom -> top
        .pattern(definition => FactoryBlockPattern.start()
            .aisle('XXXXX', 'XGGGX', 'XGGGX', 'XXXXX')
            .aisle('XXXXX', 'GACAG', 'GAAAG', 'XFFFX')
            .aisle('XXXXX', 'GCPCG', 'GAPAG', 'XFFFX')
            .aisle('XXXXX', 'GACAG', 'GAAAG', 'XFFFX')
            .aisle('XXXXX', 'XGSGX', 'XGGGX', 'XXXXX')
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            .where('X', hatches(GTBlocks.CASING_PTFE_INERT.get()))
            .where('A', Predicates.air())                                                        // reactor vessel
            .where('C', Predicates.heatingCoils().setMaxGlobalLimited(1, 1)                     // heating jacket (optional)
                .or(Predicates.air()))
            .where('P', Predicates.blocks(GTBlocks.CASING_POLYTETRAFLUOROETHYLENE_PIPE.get())) // stirrer / dip pipe
            .where('G', Predicates.blocks(GTBlocks.CLEANROOM_GLASS.get()))
            .where('F', $AF9Filters.cleanroomFilters())                                          // fan filter units
            .build())
        .workableCasingModel('gtceu:block/casings/solid/machine_casing_inert_ptfe',
            'gtceu:block/multiblock/large_chemical_reactor')
        // the same model plus the vessel's fluid while it runs, per mode
        .model($AF9MachineModels.workableCasingWithModeFluids('gtceu:block/casings/solid/machine_casing_inert_ptfe',
            'gtceu:block/multiblock/large_chemical_reactor', {
                'gtceu:fab_wet_processing': 'minecraft:water',
                'gtceu:fab_blending': 'gtceu:distilled_water',
                // Thermal's Destabilized Redstone; GT's molten redstone without Thermal
                'gtceu:fab_synthesis': Platform.isLoaded('thermal') ? 'thermal:redstone' : 'gtceu:redstone'
            }))
        .hasBER(true)

    // Rectification column: reboiler sump over a cold box, 1-8 packed trays (PTFE structured packing), clean
    // draw-off hood. 3 x 3, 3-10 high. Aisles bottom -> top, rows front -> back.
    allthemods.create('smc_rectification_column', 'multiblock')
        .machine(holder => new $FabMultiblockMachine(holder, $FabFamily.SEPARATION))
        .langValue('SMC Rectification Column')
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes(types(['fab_distillation', 'fab_cryogenic_rectification', 'fab_fractionation',
            'fab_purification']))
        .recipeModifiers([$FabModifiers.STRUCTURE_PARALLEL, GTRecipeModifiers.PARALLEL_HATCH,
            GTRecipeModifiers.OC_NON_PERFECT_SUBTICK, GTRecipeModifiers.BATCH_MODE, $FabModifiers.PURGE])
        .appearanceBlock(GTBlocks.CASING_STAINLESS_CLEAN)
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.smc_rectification_column.tooltip', 6))
        .pattern(definition => FactoryBlockPattern.start($FabRelativeDirection.RIGHT, $FabRelativeDirection.BACK,
            $FabRelativeDirection.UP)
            .aisle('XSX', 'XKX', 'XXX')                          // sump + cold box
            .aisle('XXX', 'XPX', 'XXX').setRepeatable(1, 8)      // trays
            .aisle('FFF', 'FFF', 'FFF')                          // filter hood
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            .where('X', hatches(GTBlocks.CASING_STAINLESS_CLEAN.get()))
            .where('K', Predicates.blocks(GTBlocks.CASING_ALUMINIUM_FROSTPROOF.get()))
            .where('P', Predicates.blocks(GTBlocks.CASING_POLYTETRAFLUOROETHYLENE_PIPE.get()))
            .where('F', $AF9Filters.cleanroomFilters())
            .build())
        .workableCasingModel('gtceu:block/casings/solid/machine_casing_clean_stainless_steel',
            'gtceu:block/multiblock/distillation_tower')

    // Membrane cell hall: a filter-press stack of 1-8 cells, each a perfluorinated membrane (PTFE Pipe Casing)
    // between two titanium electrodes, under a filter ceiling. 3 wide, 4 high, 3-10 deep. Aisles front (controller) ->
    // back: the controller comes before the repeatable cells, so GT's auto-build places it right.
    allthemods.create('smc_membrane_cell_hall', 'multiblock')
        .machine(holder => new $FabMultiblockMachine(holder, $FabFamily.ELECTROCHEMISTRY))
        .langValue('SMC Membrane Cell Hall')
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes(types(['fab_electrolysis', 'fab_electrofluorination']))
        .recipeModifiers([$FabModifiers.STRUCTURE_PARALLEL, GTRecipeModifiers.PARALLEL_HATCH,
            GTRecipeModifiers.OC_NON_PERFECT_SUBTICK, GTRecipeModifiers.BATCH_MODE, $FabModifiers.PURGE])
        .appearanceBlock(GTBlocks.CASING_TITANIUM_STABLE)
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.smc_membrane_cell_hall.tooltip', 6))
        .pattern(definition => FactoryBlockPattern.start($FabRelativeDirection.LEFT, $FabRelativeDirection.UP,
            $FabRelativeDirection.BACK)
            .aisle('XXX', 'XSX', 'XXX', 'FFF')                   // front end plate
            .aisle('XXX', 'EPE', 'XXX', 'FFF').setRepeatable(1, 8) // cells: electrode | membrane | electrode
            .aisle('XXX', 'XXX', 'XXX', 'FFF')                   // back end plate
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            .where('X', hatches(GTBlocks.CASING_TITANIUM_STABLE.get()))
            .where('E', Predicates.blocks('gtceu:titanium_frame'))
            .where('P', Predicates.blocks(GTBlocks.CASING_POLYTETRAFLUOROETHYLENE_PIPE.get()))
            .where('F', $AF9Filters.cleanroomFilters())
            .build())
        .workableCasingModel('gtceu:block/casings/solid/machine_casing_stable_titanium',
            'gtceu:block/multiblock/gcym/large_electrolyzer')

    // Thermal processing furnace: a horizontal tube furnace, 5 x 5 x 5. A gas cabinet at the back feeds the quartz
    // process tube, two heater zones (a ring of coils around the tube, behind tempered-glass windows) heat it, and the
    // wafer-boat load station sits behind the front, which has a window on each side of the controller. Aisles back ->
    // front, rows bottom -> top. MV machine: nothing in it needs PTFE (that comes at HV).
    // Works like GT's EBF: coil temperature + 100 K per energy tier above MV, EBF overclocks.
    allthemods.create('smc_thermal_processing_furnace', 'multiblock')
        .machine(holder => new $FabMultiblockMachine(holder, $FabFamily.THERMAL))
        .langValue('SMC Thermal Processing Furnace')
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes(types(['fab_calcination', 'fab_cvd', 'fab_crystal_growth']))
        .recipeModifiers([GTRecipeModifiers.PARALLEL_HATCH, $FabModifiers.THERMAL_OVERCLOCK,
            GTRecipeModifiers.BATCH_MODE, $FabModifiers.PURGE])
        .appearanceBlock(GTBlocks.CASING_INVAR_HEATPROOF)
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.smc_thermal_processing_furnace.tooltip', 5))
        .pattern(definition => FactoryBlockPattern.start()
            .aisle('XXXXX', 'XPPPX', 'XPPPX', 'XPPPX', 'XXXXX') // gas cabinet
            .aisle('XXXXX', 'GCCCG', 'GCTCG', 'GCCCG', 'XXXXX') // heater zone 1: coils around the tube
            .aisle('XXXXX', 'GCCCG', 'GCTCG', 'GCCCG', 'XXXXX') // heater zone 2
            .aisle('XXXXX', 'XRRRX', 'XRTRX', 'XRRRX', 'XXXXX') // wafer-boat load station
            .aisle('XXXXX', 'XGXGX', 'XGSGX', 'XGXGX', 'XXXXX') // front: windows beside the controller
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            .where('X', hatches(GTBlocks.CASING_INVAR_HEATPROOF.get()))
            .where('C', Predicates.heatingCoils())
            .where('T', Predicates.blocks('gtceu:tempered_glass'))                              // quartz process tube
            .where('G', Predicates.blocks('gtceu:tempered_glass'))                              // windows
            .where('P', Predicates.blocks('kubejs:plascrete_pipe_casing'))                      // gas lines
            .where('R', Predicates.blocks('gtceu:steel_gearbox'))                               // boat elevator
            .build())
        .workableCasingModel('gtceu:block/casings/solid/machine_casing_heatproof',
            'gtceu:block/multiblock/multi_furnace')
})
