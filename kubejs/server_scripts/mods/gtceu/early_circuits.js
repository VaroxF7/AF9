// AF9 - the first circuits with Create: the LV and the MV circuit as a sequenced assembly, a second way beside GT's own recipes
// (the crafting table and the circuit assembler stay; nothing is taken away, so nothing is locked behind Create).
//
// Why: the early game is the part a player has no machines for yet, and GT's own chain asks for a lot of loose parts before
// the first assembler runs. With Create the same parts go onto the board by hand, one deployer or press at a time, from the
// first andesite casing on: a small line, not a recipe to look up.
//
//   LV  basic_electronic_circuit: a resin board, two resistors, two electron tubes and two red alloy wires, deployed one by
//       one and pressed (2 circuits)
//   MV  good_electronic_circuit: a phenolic board, two LV circuits, two electron tubes and two copper wires, deployed and
//       laser cut with Create: Vintage Improvements' laser as the last step (1 circuit): the same parts as the circuit assembler's recipe (circuits_af9.js)
//
// The transitional item is af9:incomplete_circuit. Create is a mod of the pack: the recipes are plain Create JSON.
//
// The parts of those circuits by Create too, so no machine is needed for them:
//   phenolic board (gtceu:phenolic_circuit_board)  basin: wood dust + 50 mB glue (GT's assembler recipe, by hand)
//   good board (gtceu:phenolic_printed_circuit_board)  basin: a phenolic board + 8 silver wires (GT's crafting recipe)
//   1x wires  the saw: an ingot of a metal that has a 1x wire gives 2 wires (the wiremill's ratio), from the forge tag
//             of the ingot, for every metal below that exists in the game

ServerEvents.recipes(event => {
    const TRANSIT = 'af9:incomplete_circuit'
    const deploy = item => ({
        type: 'create:deploying',
        ingredients: [{ item: TRANSIT }, { item: item }],
        results: [{ item: TRANSIT }]
    })
    const press = () => ({
        type: 'create:pressing',
        ingredients: [{ item: TRANSIT }],
        results: [{ item: TRANSIT }]
    })
    // Create: Vintage Improvements' laser (a step of a sequenced assembly: it implements Create's assembly interface):
    // it cuts with energy (FE) instead of pressing, and takes no more than maxChargeRate FE a tick.
    const laser = (energy, maxChargeRate) => ({
        type: 'vintageimprovements:laser_cutting',
        ingredients: [{ item: TRANSIT }],
        results: [{ item: TRANSIT }],
        energy: energy,
        maxChargeRate: maxChargeRate
    })
    const assembly = (id, base, steps, output, count) => {
        event.custom({
            type: 'create:sequenced_assembly',
            ingredient: { item: base },
            transitionalItem: { item: TRANSIT },
            sequence: steps,
            results: [{ item: output, count: count }],
            loops: 1
        }).id(id)
    }

    // ---- the boards ----
    event.custom({
        type: 'create:mixing',
        ingredients: [{ tag: 'forge:dusts/wood' }, { fluid: 'gtceu:glue', amount: 50 }],
        results: [{ item: 'gtceu:phenolic_circuit_board' }]
    }).id('af9:create/phenolic_board')

    const silver = { item: 'gtceu:silver_single_wire' }
    event.custom({
        type: 'create:mixing',
        ingredients: [{ item: 'gtceu:phenolic_circuit_board' }, silver, silver, silver, silver, silver, silver, silver,
            silver],
        results: [{ item: 'gtceu:phenolic_printed_circuit_board' }]
    }).id('af9:create/phenolic_printed_circuit_board')

    // ---- 1x wires: the saw cuts an ingot into 2 wires ----
    const metals = ['copper', 'tin', 'gold', 'silver', 'lead', 'nickel', 'zinc', 'aluminium', 'steel', 'red_alloy',
        'cupronickel', 'annealed_copper', 'electrum', 'invar', 'brass', 'bronze', 'kanthal', 'nichrome', 'titanium',
        'tungsten', 'platinum', 'iridium', 'osmium', 'manganese', 'magnesium', 'vanadium_gallium', 'niobium_titanium',
        'tin_alloy', 'battery_alloy', 'soldering_alloy', 'rtm_alloy', 'hssg', 'naquadah', 'naquadah_alloy', 'duranium',
        'tungsten_steel', 'stainless_steel', 'cobalt', 'chromium', 'neodymium', 'samarium', 'europium', 'graphene']
    metals.forEach(metal => {
        const wire = 'gtceu:' + metal + '_single_wire'
        if (!Item.exists(wire)) return
        event.custom({
            type: 'create:cutting',
            ingredients: [{ tag: 'forge:ingots/' + metal }],
            results: [{ item: wire, count: 2 }],
            processingTime: 50
        }).id('af9:create/wire_' + metal)
    })

    // ---- fine wires: Create Crafts & Additions' rolling mill (GT's wiremill ratios: a 1x wire gives 4, an ingot 8) ----
    ;['copper', 'silver', 'gold', 'annealed_copper'].forEach(metal => {
        const fine = 'gtceu:fine_' + metal + '_wire'
        if (!Item.exists(fine)) return
        event.custom({
            type: 'createaddition:rolling',
            input: { item: 'gtceu:' + metal + '_single_wire' },
            result: { item: fine, count: 4 }
        }).id('af9:create/fine_wire_' + metal + '_from_wire')
        event.custom({
            type: 'createaddition:rolling',
            input: { tag: 'forge:ingots/' + metal },
            result: { item: fine, count: 8 }
        }).id('af9:create/fine_wire_' + metal + '_from_ingot')
    })

    assembly('af9:create/basic_electronic_circuit', 'gtceu:resin_circuit_board', [
        deploy('gtceu:resistor'),
        deploy('gtceu:resistor'),
        deploy('create:electron_tube'),
        deploy('create:electron_tube'),
        deploy('gtceu:red_alloy_single_wire'),
        deploy('gtceu:red_alloy_single_wire'),
        press()
    ], 'gtceu:basic_electronic_circuit', 2)

    assembly('af9:create/good_electronic_circuit', 'gtceu:phenolic_printed_circuit_board', [
        deploy('gtceu:basic_electronic_circuit'),
        deploy('gtceu:basic_electronic_circuit'),
        deploy('create:electron_tube'),
        deploy('gtceu:fine_electrum_wire'),
        deploy('gtceu:annealed_copper_quadruple_wire'),
        deploy('gtceu:diode'),
        // the laser when Vintage Improvements is in the pack, else the press (the recipe never names a missing type)
        Platform.isLoaded('vintageimprovements') ? laser(2000, 50) : press()
    ], 'gtceu:good_electronic_circuit', 1)
})
