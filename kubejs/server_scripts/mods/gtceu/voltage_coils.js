// AF9 - Voltage Coils: GT's own assembler recipes, with the 16x fine wire swapped for 2x wire spool of the tier's
// material (a spool is 64 fine wires, see docs/wire-spools.md). The magnetic rod, the circuit, the duration and the
// EU/t stay as GT has them: each recipe is edited in place instead of being rewritten.

ServerEvents.recipes(event => {
    // [coil, spool material]
    const COILS = [
        ['lv', 'black_steel'],
        ['mv', 'aluminium']
    ]
    COILS.forEach(cfg => {
        let changed = 0
        event.forEachRecipe({ type: 'gtceu:assembler', output: `gtceu:${cfg[0]}_voltage_coil` }, recipe => {
            const items = recipe.json.getAsJsonObject('inputs').getAsJsonArray('item')
            for (let i = 0; i < items.size(); i++) {
                let content = items.get(i).getAsJsonObject().getAsJsonObject('content')
                if (!content.has('count') || content.get('count').getAsInt() !== 16) continue
                if (String(content.get('ingredient')).indexOf('fine_wire') < 0) continue
                content.addProperty('count', 2)
                content.add('ingredient', JsonIO.toObject({ item: `gtceu:${cfg[1]}_wire_spool` }))
                changed++
            }
        })
        if (changed !== 1) console.error(`AF9 voltage_coils: ${cfg[0]} coil expected 1 fine wire input to change, changed ${changed}`)
    })
})
