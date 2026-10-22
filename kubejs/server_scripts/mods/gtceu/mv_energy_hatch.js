// priority: 0
// The MV Energy Hatch and Dynamo Hatch no longer take a ULPIC chip. GT's recipes (the crafting grid ones and the
// assembler ones) are copied without that ingredient, so what is left is the hull, the cables and the rest of GT's
// parts. The ULPIC wafer / chip is no longer a gate for the Photolithography Line (docs/semiconductor-factory.md §6.8).

ServerEvents.recipes(event => {
    const HATCHES = ['gtceu:mv_energy_input_hatch', 'gtceu:mv_energy_output_hatch']
    const CHIP = 'gtceu:ulpic_chip'
    const copies = []
    const seen = {}

    const outputsHatch = json => {
        const text = JSON.stringify(json.result || (json.outputs && json.outputs.item) || '')
        return HATCHES.some(h => text.indexOf(h) >= 0)
    }
    const take = recipe => {
        const id = String(recipe.getId())
        if (seen[id]) return
        seen[id] = true
        const json = JSON.parse(recipe.json.toString())
        if (!outputsHatch(json)) return
        let changed = false
        if (json.key && json.pattern) {
            // crafting grid: drop the key that is the chip and blank its cells
            Object.keys(json.key).forEach(k => {
                if (JSON.stringify(json.key[k]).indexOf(CHIP) < 0) return
                delete json.key[k]
                json.pattern = json.pattern.map(row => row.split(k).join(' '))
                changed = true
            })
        } else if (json.inputs && json.inputs.item) {
            // GT recipe (assembler): drop the chip's input slot
            const slotsBefore = json.inputs.item.length
            json.inputs.item = json.inputs.item.filter(slot => JSON.stringify(slot).indexOf(CHIP) < 0)
            changed = json.inputs.item.length < slotsBefore
        }
        if (changed) copies.push({ id: id, json: json })
    }

    HATCHES.forEach(h => event.forEachRecipe({ output: h }, take))
    event.forEachRecipe({ type: 'gtceu:assembler' }, take)

    copies.forEach(c => {
        const colon = c.id.indexOf(':')
        event.remove({ id: c.id })
        const made = event.custom(c.json)
        if (made && typeof made.id === 'function') made.id(`af9:${c.id.substring(colon + 1)}_no_chip`)
    })
    if (copies.length === 0) console.warn('mv_energy_hatch.js: no MV energy / dynamo hatch recipe with a ULPIC chip found')
})
