// AF9 - Crafting of the Microverse Projector and the ore recipes it runs (startup_scripts/gtceu/microverse.js, af9-core
// com.af9.core.microverse.OreCatalog). Spec: docs/microverse.md
//
// One recipe for every ore GregTech has: a Miner Drone of the ore's microverse tier is used up, the Microverse Core of the
// tier and a dust of the ore (the seed) are not, 16 raw ores come out. OreCatalog lists the ores and their tiers, so an
// ore another mod adds to GT is farmable too.

const $OreCatalog = Java.loadClass('com.af9.core.microverse.OreCatalog')

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const solder = mb => Fluid.of('gtceu:soldering_alloy', mb)

    // ---- Per tier: the voltage tier of the recipes, the plate of the drone, the parts of the core ----
    // tier -> [voltage tier name, GT voltage tier, plate, core extras, drone extras]
    const tiers = [
        { tier: 1, name: 'ev', volt: GTValues.EV, plate: 'gtceu:titanium_plate', seconds: 30,
            core: ['2x gtceu:ev_field_generator', 'gtceu:ev_emitter', '4x #gtceu:circuits/ev'] },
        { tier: 2, name: 'iv', volt: GTValues.IV, plate: 'gtceu:tungsten_steel_plate', seconds: 40,
            core: ['2x gtceu:iv_field_generator', 'gtceu:iv_emitter', '4x #gtceu:circuits/iv', 'gtceu:iv_sensor'] },
        { tier: 3, name: 'luv', volt: GTValues.LuV, plate: 'gtceu:rhodium_plated_palladium_plate', seconds: 50,
            core: ['2x gtceu:luv_field_generator', 'gtceu:luv_emitter', '4x #gtceu:circuits/luv', 'gtceu:luv_sensor',
                'gtceu:luv_robot_arm'] },
        { tier: 4, name: 'zpm', volt: GTValues.ZPM, plate: 'gtceu:naquadah_alloy_plate', seconds: 60,
            core: ['2x gtceu:zpm_field_generator', 'gtceu:zpm_emitter', '4x #gtceu:circuits/zpm', 'gtceu:zpm_sensor',
                'gtceu:zpm_robot_arm'] }
    ]

    tiers.forEach(t => {
        // Miner Drone: a robot with a motor and sensor, circuits and plates of the tier
        event.recipes.gtceu.assembler(`af9:miner_drone_mk${t.tier}`)
            .itemInputs(`gtceu:${t.name}_robot_arm`, `gtceu:${t.name}_sensor`, `gtceu:${t.name}_electric_motor`,
                `2x #gtceu:circuits/${t.name}`, `4x ${t.plate}`)
            .circuit(10 + t.tier)
            .inputFluids(solder(144 * t.tier))
            .itemOutputs(`2x kubejs:miner_drone_mk${t.tier}`)
            .duration(300)
            .EUt(VA[t.volt])

        // Microverse Core
        event.recipes.gtceu.assembler(`af9:microverse_core_mk${t.tier}`)
            .itemInputs(t.core.concat([`8x ${t.plate}`]))
            .inputFluids(solder(576 * t.tier))
            .itemOutputs(`kubejs:microverse_core_mk${t.tier}`)
            .duration(1200)
            .EUt(VA[t.volt])

        // The ores of the tier
        $OreCatalog.materials(t.tier).forEach(material => {
            const ore = $OreCatalog.ore(material)
            if (!ore) return
            event.recipes.gtceu.microverse_mining(`af9:microverse/mk${t.tier}/${material}`)
                .itemInputs(`kubejs:miner_drone_mk${t.tier}`)
                .notConsumable(`kubejs:microverse_core_mk${t.tier}`)
                .notConsumable(`gtceu:${material}_dust`)
                .itemOutputs(`16x ${ore}`)
                .duration(t.seconds * 20)
                .EUt(VA[t.volt])
        })
    })

    // ---- The Projector ----
    event.recipes.gtceu.assembler('af9:microverse_projector')
        .itemInputs('gtceu:ev_machine_hull', '4x #gtceu:circuits/ev', '4x gtceu:fusion_casing',
            '8x gtceu:fusion_glass', 'gtceu:superconducting_coil', '2x gtceu:ev_field_generator',
            '2x gtceu:ev_robot_arm')
        .inputFluids(solder(1152))
        .itemOutputs('gtceu:microverse_projector')
        .duration(800)
        .EUt(VA[GTValues.EV])
})
