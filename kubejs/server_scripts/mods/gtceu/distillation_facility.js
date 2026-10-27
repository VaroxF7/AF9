// AF9 - Distillation Facility and Distillation Tower Module: crafting. The Distillation Tower (gtceu:distillation_tower) is gone
// (gtceu-fork patch 0027): the facility runs its recipes. The module is the old tower's crafting, a tower of its own to place in the
// facility; the facility is an IV machine of three of them.
// Machines: startup_scripts/gtceu/distillation_facility.js. Docs: docs/distillation-facility.md.

ServerEvents.recipes(event => {
    event.remove({ output: 'gtceu:distillation_tower' })

    // a tower: what the Distillation Tower took (HV hull, large stainless pipes, EV circuits, HV pumps)
    event.shaped('gtceu:distillation_module', ['CBC', 'FMF', 'CBC'], {
        M: 'gtceu:hv_machine_hull', B: 'gtceu:stainless_steel_large_fluid_pipe', C: '#gtceu:circuits/ev',
        F: 'gtceu:hv_electric_pump'
    }).id('af9:distillation_module')

    // the facility: three towers round an IV hull, with IV circuits, pumps and motors
    event.shaped('gtceu:distillation_facility', ['MMM', 'CHC', 'PEP'], {
        M: 'gtceu:distillation_module', H: 'gtceu:iv_machine_hull', C: '#gtceu:circuits/iv',
        P: 'gtceu:iv_electric_pump', E: 'gtceu:iv_electric_motor'
    }).id('af9:distillation_facility')
})
