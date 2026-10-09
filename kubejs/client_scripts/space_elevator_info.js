// AF9 - the Space Elevator's set-up, as an information page of the recipe viewers (on the tower and on the Mining Modules).
JEIEvents.information(event => {
    const text = [
        'The Space Elevator flies expeditions with its Mining Modules (MK-1 to MK-3), as many at once as the motors power.',
        'Each module needs a Mining Drone of its tier (not used up), a drill head and a crate for the cargo (both used up), hydrogen and the supercooled coolant of the drone, and the energy: all of it comes through the hatches of the tower and the module.',
        'The tower also needs computation: data hatches on an optical cable from a computation source. Its screen says how much the modules in the slots need (MK-1 20, MK-2 60, MK-3 120 CWU/t each) and how much hydrogen and coolant they use up a second. Without it no module flies.',
        'Energy hatches, wireless energy receivers and laser hatches (up to 2) all power the tower; an energy hatch switched to Auxiliary does not.',
        'The ore comes home in the output buses; the ores of each drone tier are on the recipe pages.'
    ]
    event.add(['gtceu:space_elevator', 'gtceu:space_mining_module_mk1', 'gtceu:space_mining_module_mk2',
        'gtceu:space_mining_module_mk3'], text)
})
