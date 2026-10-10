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
//       pressed (1 circuit): the same parts as the circuit assembler's recipe (circuits_af9.js)
//
// The transitional item is af9:incomplete_circuit. Create is a mod of the pack: the recipes are plain Create JSON.

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
    const assembly = (id, base, steps, output, count) => {
        event.custom({
            type: 'create:sequenced_assembly',
            ingredient: { item: base },
            transitional_item: { item: TRANSIT },
            sequence: steps,
            results: [{ item: output, count: count }],
            loops: 1
        }).id(id)
    }

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
        deploy('create:electron_tube'),
        deploy('gtceu:copper_single_wire'),
        deploy('gtceu:copper_single_wire'),
        press()
    ], 'gtceu:good_electronic_circuit', 1)
})
