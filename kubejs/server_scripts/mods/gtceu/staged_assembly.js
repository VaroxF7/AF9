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

    // ---- Components as an extra path (GT's assembler ingredients, no Powah glue) ----
    // Same parts as the assembler / orb (orb_recipes.js), staged for the hall's perfect OC.
    // 2 layers each: frame first, then belt / seals / gears / electronics.
    // LV
    staged('af9:staged_lv_conveyor', {
        eu: VA[GTValues.LV],
        outputs: ['gtceu:lv_conveyor_module'],
        layers: [
            { items: ['2x gtceu:lv_electric_motor', 'gtceu:tin_single_cable'], duration: 100 },
            { items: ['6x #forge:plates/rubber'], duration: 100 },
        ],
    })
    staged('af9:staged_lv_pump', {
        eu: VA[GTValues.LV],
        outputs: ['gtceu:lv_electric_pump'],
        layers: [
            { items: ['gtceu:lv_electric_motor', 'gtceu:tin_single_cable', 'gtceu:bronze_normal_fluid_pipe'], duration: 100 },
            { items: ['#forge:screws/tin', '#forge:rotors/tin', '2x #forge:rings/rubber'], duration: 100 },
        ],
    })
    staged('af9:staged_lv_piston', {
        eu: VA[GTValues.LV],
        outputs: ['gtceu:lv_electric_piston'],
        layers: [
            { items: ['gtceu:lv_electric_motor', '2x #forge:rods/steel', '2x gtceu:tin_single_cable'], duration: 100 },
            { items: ['3x #forge:plates/steel', '#forge:small_gears/steel'], duration: 100 },
        ],
    })
    staged('af9:staged_lv_arm', {
        eu: VA[GTValues.LV],
        outputs: ['gtceu:lv_robot_arm'],
        layers: [
            { items: ['2x gtceu:lv_electric_motor', '2x #forge:rods/steel', '3x gtceu:tin_single_cable'], duration: 150 },
            { items: ['gtceu:lv_electric_piston', '#gtceu:circuits/lv'], duration: 150 },
        ],
    })
    staged('af9:staged_lv_emitter', {
        eu: VA[GTValues.LV],
        outputs: ['gtceu:lv_emitter'],
        layers: [
            { items: ['4x #forge:rods/brass', '2x gtceu:tin_single_cable'], duration: 100 },
            { items: ['2x #gtceu:circuits/lv', '#forge:gems/quartzite'], duration: 100 },
        ],
    })
    staged('af9:staged_lv_sensor', {
        eu: VA[GTValues.LV],
        outputs: ['gtceu:lv_sensor'],
        layers: [
            { items: ['#forge:rods/brass', '4x #forge:plates/steel'], duration: 100 },
            { items: ['#gtceu:circuits/lv', '#forge:gems/quartzite'], duration: 100 },
        ],
    })
    staged('af9:staged_lv_field', {
        eu: VA[GTValues.LV],
        outputs: ['gtceu:lv_field_generator'],
        layers: [
            { items: ['#forge:ender_pearls', '2x #forge:plates/steel'], duration: 300 },
            { items: ['2x #gtceu:circuits/lv', '4x gtceu:manganese_phosphide_quadruple_wire'], duration: 300 },
        ],
    })

    // MV
    staged('af9:staged_mv_conveyor', {
        eu: VA[GTValues.MV],
        outputs: ['gtceu:mv_conveyor_module'],
        layers: [
            { items: ['2x gtceu:mv_electric_motor', 'gtceu:copper_single_cable'], duration: 100 },
            { items: ['6x #forge:plates/rubber'], duration: 100 },
        ],
    })
    staged('af9:staged_mv_pump', {
        eu: VA[GTValues.MV],
        outputs: ['gtceu:mv_electric_pump'],
        layers: [
            { items: ['gtceu:mv_electric_motor', 'gtceu:copper_single_cable', 'gtceu:steel_normal_fluid_pipe'], duration: 100 },
            { items: ['#forge:screws/bronze', '#forge:rotors/bronze', '2x #forge:rings/rubber'], duration: 100 },
        ],
    })
    staged('af9:staged_mv_piston', {
        eu: VA[GTValues.MV],
        outputs: ['gtceu:mv_electric_piston'],
        layers: [
            { items: ['gtceu:mv_electric_motor', '2x #forge:rods/aluminium', '2x gtceu:copper_single_cable'], duration: 100 },
            { items: ['3x #forge:plates/aluminium', '#forge:small_gears/aluminium'], duration: 100 },
        ],
    })
    staged('af9:staged_mv_arm', {
        eu: VA[GTValues.MV],
        outputs: ['gtceu:mv_robot_arm'],
        layers: [
            { items: ['2x gtceu:mv_electric_motor', '2x #forge:rods/aluminium', '3x gtceu:copper_single_cable'], duration: 150 },
            { items: ['gtceu:mv_electric_piston', '#gtceu:circuits/mv'], duration: 150 },
        ],
    })
    staged('af9:staged_mv_emitter', {
        eu: VA[GTValues.MV],
        outputs: ['gtceu:mv_emitter'],
        layers: [
            { items: ['4x #forge:rods/electrum', '2x gtceu:copper_single_cable'], duration: 100 },
            { items: ['2x #gtceu:circuits/mv', '#forge:flawless_gems/emerald'], duration: 100 },
        ],
    })
    staged('af9:staged_mv_sensor', {
        eu: VA[GTValues.MV],
        outputs: ['gtceu:mv_sensor'],
        layers: [
            { items: ['#forge:rods/electrum', '4x #forge:plates/aluminium'], duration: 100 },
            { items: ['#gtceu:circuits/mv', '#forge:flawless_gems/emerald'], duration: 100 },
        ],
    })
    staged('af9:staged_mv_field', {
        eu: VA[GTValues.MV],
        outputs: ['gtceu:mv_field_generator'],
        layers: [
            { items: ['minecraft:ender_eye', '2x #forge:plates/aluminium'], duration: 300 },
            { items: ['2x #gtceu:circuits/mv', '4x gtceu:magnesium_diboride_quadruple_wire'], duration: 300 },
        ],
    })

    // HV
    staged('af9:staged_hv_conveyor', {
        eu: VA[GTValues.HV],
        outputs: ['gtceu:hv_conveyor_module'],
        layers: [
            { items: ['2x gtceu:hv_electric_motor', 'gtceu:gold_single_cable'], duration: 100 },
            { items: ['6x #forge:plates/rubber'], duration: 100 },
        ],
    })
    staged('af9:staged_hv_pump', {
        eu: VA[GTValues.HV],
        outputs: ['gtceu:hv_electric_pump'],
        layers: [
            { items: ['gtceu:hv_electric_motor', 'gtceu:gold_single_cable', 'gtceu:stainless_steel_normal_fluid_pipe'], duration: 100 },
            { items: ['#forge:screws/steel', '#forge:rotors/steel', '2x #forge:rings/rubber'], duration: 100 },
        ],
    })
    staged('af9:staged_hv_piston', {
        eu: VA[GTValues.HV],
        outputs: ['gtceu:hv_electric_piston'],
        layers: [
            { items: ['gtceu:hv_electric_motor', '2x #forge:rods/stainless_steel', '2x gtceu:gold_single_cable'], duration: 100 },
            { items: ['3x #forge:plates/stainless_steel', '#forge:small_gears/stainless_steel'], duration: 100 },
        ],
    })
    staged('af9:staged_hv_arm', {
        eu: VA[GTValues.HV],
        outputs: ['gtceu:hv_robot_arm'],
        layers: [
            { items: ['2x gtceu:hv_electric_motor', '2x #forge:rods/stainless_steel', '3x gtceu:gold_single_cable'], duration: 150 },
            { items: ['gtceu:hv_electric_piston', '#gtceu:circuits/hv'], duration: 150 },
        ],
    })
    staged('af9:staged_hv_emitter', {
        eu: VA[GTValues.HV],
        outputs: ['gtceu:hv_emitter'],
        layers: [
            { items: ['4x #forge:rods/chromium', '2x gtceu:gold_single_cable'], duration: 100 },
            { items: ['2x #gtceu:circuits/hv', 'minecraft:ender_eye'], duration: 100 },
        ],
    })
    staged('af9:staged_hv_sensor', {
        eu: VA[GTValues.HV],
        outputs: ['gtceu:hv_sensor'],
        layers: [
            { items: ['#forge:rods/chromium', '4x #forge:plates/stainless_steel'], duration: 100 },
            { items: ['#gtceu:circuits/hv', 'minecraft:ender_eye'], duration: 100 },
        ],
    })
    staged('af9:staged_hv_field', {
        eu: VA[GTValues.HV],
        outputs: ['gtceu:hv_field_generator'],
        layers: [
            { items: ['gtceu:quantum_eye', '2x #forge:plates/stainless_steel'], duration: 300 },
            { items: ['2x #gtceu:circuits/hv', '4x gtceu:mercury_barium_calcium_cuprate_quadruple_wire'], duration: 300 },
        ],
    })
})
