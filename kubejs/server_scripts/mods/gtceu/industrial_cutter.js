// AF9 - Industrial Cutting Machine: crafting. It replaces GTCEu's Large Cutter (gtceu:large_cutter), which the fork
// no longer registers (gtceu-fork patch 0013); the remove below is a no-op safety net for older fork jars.
// Machine: startup_scripts/gtceu/industrial_cutter.js (cutter + lathe modes). Quest: LuV chapter (ludicrous_voltage).

ServerEvents.recipes(event => {
    event.remove({ output: 'gtceu:large_cutter' })

    // the purple Cutting Factory Frame casing: a ring of tungsten steel plates round a tungsten steel frame
    event.shaped('2x af9:cutter_casing', ['PPP', 'PFP', 'PPP'], {
        P: 'gtceu:tungsten_steel_plate', F: 'gtceu:tungsten_steel_frame'
    }).id('af9:cutter_casing')

    // the machine: what the Large Cutter took, with the new casing (LuV circuits and motors, robot arms)
    event.shaped('gtceu:industrial_cutting_machine', ['CMC', 'RXR', 'CMC'], {
        C: '#gtceu:circuits/luv', M: 'gtceu:luv_electric_motor', R: 'gtceu:luv_robot_arm', X: 'af9:cutter_casing'
    }).id('af9:industrial_cutting_machine')
})
