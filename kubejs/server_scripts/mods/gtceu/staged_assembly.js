// AF9 - Staged Assembly recipes (startup_scripts/gtceu/staged_assembly.js).
//
// A staged recipe is written as one recipe whose inputs arrive in layers. The staged() helper below appends every
// layer's inputs in order and records which content index belongs to which layer in the recipe's data (read by
// AF9 Core's StagedRecipes); the machine splits it back into stages at runtime and runs them one after the other.
// Inputs outside the layers (globalItems, globalFluids) go into every stage: energy always does.
//
// Limits (the JEI page fits this much): 1-7 layers, at most 5 item + fluid inputs per layer.

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const CompoundTag = Java.loadClass('net.minecraft.nbt.CompoundTag')
    const ListTag = Java.loadClass('net.minecraft.nbt.ListTag')
    const IntTag = Java.loadClass('net.minecraft.nbt.IntTag')

    const intList = values => {
        const list = new ListTag()
        values.forEach(v => list.add(IntTag.valueOf(v)))
        return list
    }

    // staged(id, spec): spec = { eu, outputs: [...], globalItems, globalFluids, layers: [
    //   { items: [...], fluids: ['<id> <mB>', ...], duration } ] }
    const staged = (id, spec) => {
        const layers = spec.layers || []
        if (layers.length < 1 || layers.length > 7) {
            console.error(`staged ${id}: needs 1-7 layers, has ${layers.length}`)
            return
        }
        let bad = false
        layers.forEach((layer, i) => {
            const inputs = (layer.items || []).length + (layer.fluids || []).length
            if (inputs < 1 || inputs > 5) {
                console.error(`staged ${id}: layer ${i + 1} has ${inputs} inputs (needs 1-5)`)
                bad = true
            }
        })
        if (bad || !spec.outputs || !spec.outputs.length || !(spec.eu > 0)) {
            console.error(`staged ${id}: needs outputs and a positive eu`)
            return
        }

        const recipe = event.recipes.gtceu.staged_assembly(id)
        const itemCount = []
        const fluidCount = []
        const takeItems = list => {
            const indices = []
            ;(list || []).forEach(entry => {
                recipe.itemInputs(entry)
                indices.push(itemCount.length)
                itemCount.push(entry)
            })
            return indices
        }
        const takeFluids = list => {
            const indices = []
            ;(list || []).forEach(entry => {
                const m = String(entry).match(/^(.*?)\s+(\d+)$/)
                recipe.inputFluids(m ? Fluid.of(m[1], Number(m[2])) : Fluid.of(String(entry), 1000))
                indices.push(fluidCount.length)
                fluidCount.push(entry)
            })
            return indices
        }
        const globalItems = takeItems(spec.globalItems)
        const globalFluids = takeFluids(spec.globalFluids)
        let duration = 0
        const layersTag = new ListTag()
        layers.forEach(layer => {
            const items = takeItems(layer.items)
            const fluids = takeFluids(layer.fluids)
            const ticks = layer.duration && layer.duration > 0 ? layer.duration : 200
            duration += ticks
            const tag = new CompoundTag()
            tag.putInt('duration', ticks)
            tag.put('items', intList(items))
            tag.put('fluids', intList(fluids))
            layersTag.add(tag)
        })
        const tag = new CompoundTag()
        tag.put('layers', layersTag)
        tag.put('globalItems', intList(globalItems))
        tag.put('globalFluids', intList(globalFluids))
        recipe.addData('af9_staged', tag)
        recipe.itemOutputs(spec.outputs)
        recipe.duration(duration)
        recipe.EUt(spec.eu)
    }

    // ---- The machine and its cover ----
    // The controller, crafted at LV like GT's assembler: robot arms, conveyors and LV circuits around an LV assembler
    event.shaped('gtceu:staged_assembly', ['RCR', 'VAV', 'WCW'], {
        R: 'gtceu:lv_robot_arm', C: '#gtceu:circuits/lv', V: 'gtceu:lv_conveyor_module', A: 'gtceu:lv_assembler',
        W: 'gtceu:tin_single_cable'
    }).id('af9:staged_assembly')
    // The staged step detector: GT's activity detector cover that reads the stage, a comparator for the signal strength
    event.shapeless('af9:staged_step_detector', ['gtceu:activity_detector_cover', 'minecraft:comparator'])
        .id('af9:staged_step_detector')

    // ---- Demo: an LV robot arm in three stages (items only) ----
    staged('af9:staged_demo_arm', {
        eu: VA[GTValues.LV],
        outputs: ['gtceu:lv_robot_arm'],
        layers: [
            { items: ['2x gtceu:steel_plate', 'gtceu:lv_electric_motor'], duration: 200 },
            { items: ['2x gtceu:copper_single_cable', 'gtceu:lv_sensor'], duration: 200 },
            { items: ['gtceu:epoxy_printed_circuit_board'], duration: 200 },
        ],
    })

    // ---- Demo: an MV conveyor module in two stages (a frame and lubricant run through both) ----
    staged('af9:staged_demo_conveyor', {
        eu: VA[GTValues.MV],
        outputs: ['gtceu:mv_conveyor_module'],
        globalItems: ['gtceu:aluminium_frame'],
        globalFluids: ['gtceu:lubricant 500'],
        layers: [
            { items: ['4x gtceu:aluminium_plate', '2x gtceu:stainless_steel_rod'], duration: 300 },
            { items: ['gtceu:mv_electric_piston', '4x gtceu:gold_single_cable'], duration: 300 },
        ],
    })
})
