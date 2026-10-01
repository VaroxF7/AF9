// AF9 - The lithography process around the print: calibration, wafer clean-up, the finer coatings, the Metrology
// Station. Items and materials here; behaviour: AF9 Core (LithoMachine); recipes: server_scripts/mods/gtceu/
// litho_process.js. Spec: docs/semiconductor-factory.md §18

StartupEvents.registry('item', event => {
    // The reference wafer of a calibration run: a Line, Scanner or Orbital Station that has drifted takes one from an
    // input bus (or a Metrology Station for the machines on its bus) and aligns its optics and stages on its marks.
    event.create('calibration_wafer')
        .displayName('Calibration Wafer')
        .texture('kubejs:item/wafers/calibration_wafer')
        .tooltip('A reference wafer with alignment marks. Put it in the input bus of a lithography machine that has')
        .tooltip('drifted (below 70% calibration) and it calibrates itself, or feed a Metrology Station.')
})

// ---- Chemistry the process needs beside the track fluids ----
// Formula only, no components (so GT adds no electrolyzer or centrifuge shortcut). Recipes: server_scripts/mods/gtceu/
// litho_process.js.
GTCEuStartupEvents.registry('gtceu:material', event => {
    const materials = [
        // RCA clean, the wafer cleaning every fab starts with: SC-1 (ammonia, peroxide, water, 1:1:5) lifts particles
        // and organics, SC-2 (HCl, peroxide, water, 1:1:6) takes the metal ions off. Piranha (SPM, 3:1 sulfuric acid
        // to peroxide) strips baked resist and heavy organics.
        ['sc1_solution', 'liquid', 0xcfe8f5, '(NH3)(H2O2)(H2O)5'],
        ['sc2_solution', 'liquid', 0xe6f0d2, '(HCl)(H2O2)(H2O)6'],
        ['piranha_solution', 'liquid', 0xf2e2b8, '(H2SO4)3(H2O2)'],
        ['spent_piranha', 'liquid', 0x6b5a3c, '(H2SO4)(H2O)(C)'],

        // Ethyl lactate, the green solvent of the coatings: acetaldehyde from ethanol over copper, lactonitrile with
        // hydrogen cyanide, hydrolysed to lactic acid, esterified with ethanol.
        ['acetaldehyde', 'liquid', 0xeef3e0, 'CH3CHO'],
        ['lactonitrile', 'liquid', 0xe8eadc, 'CH3CH(OH)CN'],
        ['lactic_acid', 'liquid', 0xf0ecd8, 'CH3CH(OH)COOH'],
        ['ethyl_lactate', 'liquid', 0xe9efe2, 'CH3CH(OH)COOC2H5'],

        // BARC, the bottom anti-reflective coat under the resist (KrF and ArF): an acrylic polymer with a dye that
        // soaks up the light that passed the resist, in ethyl lactate. The dye is a nitrated naphthalene.
        ['nitronaphthalene', 'dust', 0xd9b24a, 'C10H7NO2'],
        ['barc', 'liquid', 0xc9a447, '(C5H8O2)n(C10H7NO2)(C5H10O3)'],

        // TARC, the top anti-reflective coat over the resist of the immersion nodes (65 and 50 nm): a fluoropolymer in
        // PGMEA. What a coating leaves in the bowl is spent resist solvent; it is distilled back, never all of it.
        ['tarc', 'liquid', 0xbfe3e8, '(C2F4)n(C6H12O3)'],
        ['spent_resist_solvent', 'liquid', 0x8a7f4a, '(C6H12O3)(C)'],
        // Plasma etching: carbon tetrafluoride, chlorine, argon and oxygen make the etch plasma of the print
        ['tetrafluoromethane', 'gas', 0xdfe8ef, 'CF4'],
        ['etch_plasma_gas', 'gas', 0xb48cf0, '(CF4)(Cl2)(Ar)4(O2)'],

        // The functional layers of the new chip families (chips.js): the cards that use the chips take them too.
        // Acoustic wave: the piezo films of SAW and BAW filters
        ['aluminium_nitride', 'dust', 0xb8c4d0, 'AlN'],
        ['lithium_niobate', 'dust', 0xdcd8e8, 'LiNbO3'],
        // Photonics: the silicon nitride waveguide (germanium and indium phosphide come from GT)
        ['silicon_nitride', 'dust', 0x9aa0b4, 'Si3N4'],
        // Spintronics: the free layer of the magnetic tunnel junction (the MgO barrier is GT's magnesia)
        ['cobalt_iron_boron', 'dust', 0x6c7a96, '(Co)(Fe)(B)'],
        // 2D materials: tungsten diselenide channels, hexagonal boron nitride dielectric (MoS2 is GT's molybdenite)
        ['tungsten_diselenide', 'dust', 0x4a5260, 'WSe2'],
        ['boron_nitride', 'dust', 0xf0f0f4, 'BN'],
        // Neuromorphic: the phase-change memory material
        ['gst_alloy', 'dust', 0x8a7a96, 'Ge2Sb2Te5'],
        // Quantum dots: CdSe nanocrystals in solution
        ['quantum_dot_colloid', 'liquid', 0xe0503c, '(CdSe)n(C8H10)']
    ]
    materials.forEach(([id, form, color, formula]) => {
        const material = event.create(id)
        if (form === 'dust') material.dust()
        else if (form === 'gas') material.gas()
        else material.liquid()
        material.color(color)
        if (formula) material.formula(formula)
    })
})

// ---- Metrology Station ----
// The fab's measuring tool (af9-core MetrologyStationMachine): on the machine bus, a run (a calibration wafer and 24
// CWU/t of computation) calibrates every lithography machine on its bus network and, for ten minutes after, feeds the
// measurements back into their alignment and dose: 15% fewer broken wafers.
const $MetrologyStation = Java.loadClass('com.af9.core.machine.MetrologyStationMachine')
const $MetrologyBusConnector = Java.loadClass('com.af9.core.bus.BusConnectorPartMachine')
const $MetrologyDirection = Java.loadClass('com.gregtechceu.gtceu.api.pattern.util.RelativeDirection')

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    // a calibration wafer in (and, mostly, back out), distilled water for the stage
    event.create('metrology')
        .category('multiblock')
        .setEUIO('in')
        .setMaxIOSize(1, 1, 1, 0)
        .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.ELECTROLYZER)
})

GTCEuStartupEvents.registry('gtceu:machine', event => {
    // 3 wide x 3 high x 5 long, built from plascrete like the lithography machines: a front with the controller, a
    // measuring tube (cleanroom glass on both sides, filter casings over it) with the wafer stage in it, a back. Aisles
    // front -> back, rows bottom -> top; hatches on any plascrete.
    event.create('metrology_station', 'multiblock')
        .machine(holder => new $MetrologyStation(holder))
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('metrology')])
        .appearanceBlock(() => Block.getBlock('gtceu:plascrete'))
        ['tooltips(net.minecraft.network.chat.Component[])']([0, 1, 2, 3].map(i =>
            Component.translatable(`af9.metrology_station.tooltip.${i}`)))
        .pattern(definition => FactoryBlockPattern.start($MetrologyDirection.LEFT, $MetrologyDirection.UP,
            $MetrologyDirection.BACK)
            .aisle('CCC', 'CMC', 'CCC') // front with the controller
            .aisle('CCC', 'W#W', 'CFC') // measuring tube
            .aisle('CRC', 'W#W', 'CFC') // wafer stage and microscope
            .aisle('CCC', 'W#W', 'CFC')
            .aisle('CCC', 'CCC', 'CCC') // back
            .where('M', Predicates.controller(Predicates.blocks(definition.get())))
            // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count))
            .where('C', Predicates.blocks('gtceu:plascrete')
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities($MetrologyBusConnector.BUS_CONNECTOR).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
            .where('F', Predicates.blocks('kubejs:plascrete_filter_casing'))     // fan filter units
            .where('R', Predicates.blocks('gtceu:stainless_steel_gearbox'))      // wafer stage
            .where('W', Predicates.blocks('gtceu:cleanroom_glass'))              // windows of the tube
            .where('#', Predicates.air())                                        // the tube
            .build())
        .workableCasingModel('gtceu:block/casings/cleanroom/plascrete', 'gtceu:block/multiblock/network_switch')
})

// ---- Coater Track ----
// The spin-coat track in front of the exposure tool (a real fab's track does the coating and the developing; here the line
// keeps the developing): HMDS prime, bottom anti-reflective coat, resist spun on, topcoat, soft bake. It turns a blank
// wafer into the coated wafer of its node, and what it spins off is spent solvent. Plain GT machine logic, no AF9 Core class.
const $CoaterMachine = Java.loadClass('com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine')
const $CoaterBusConnector = Java.loadClass('com.af9.core.bus.BusConnectorPartMachine')
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
                .or(Predicates.abilities($CoaterBusConnector.BUS_CONNECTOR).setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
            .where('P', Predicates.blocks('kubejs:plascrete_pipe_casing'))       // chemical dispense lines
            .where('F', Predicates.blocks('kubejs:plascrete_filter_casing'))     // fan filter units
            .where('R', Predicates.blocks('gtceu:stainless_steel_gearbox'))      // spin chuck
            .where('H', Predicates.blocks('gtceu:heatproof_machine_casing'))     // hotplate
            .where('W', Predicates.blocks('gtceu:cleanroom_glass'))              // windows of the track
            .where('#', Predicates.air())                                        // the track
            .build())
        .workableCasingModel('gtceu:block/casings/cleanroom/plascrete', 'gtceu:block/multiblock/network_switch')
})
