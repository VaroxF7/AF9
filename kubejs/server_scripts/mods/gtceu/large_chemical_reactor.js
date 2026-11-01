// AF9 - the Large Chemical Reactor's new structure: the casings' recipes (the structure itself: startup_scripts/gtceu/large_chemical_reactor.js,
// textures tools/textures/lcr_casings.py). Six plates of the alloy round a steel frame make two casings, like GT's PTFE casings;
// the saw cuts a casing into two slopes.
ServerEvents.recipes(event => {
    const VA = GTValues.VA
    ;[['inconel_625', GTValues.HV], ['inconel_718', GTValues.EV]].forEach(alloy => {
        event.recipes.gtceu.assembler(`af9:${alloy[0]}_casing`)
            .itemInputs(`6x gtceu:${alloy[0]}_plate`, 'gtceu:steel_frame')
            .itemOutputs(`2x af9:${alloy[0]}_casing`)
            .duration(100)
            .EUt(VA[alloy[1]])
    })
    event.recipes.gtceu.cutter('af9:inconel_718_slope')
        .itemInputs('af9:inconel_718_casing')
        .inputFluids(Fluid.of('minecraft:water', 20))
        .itemOutputs('2x af9:inconel_718_slope')
        .duration(60)
        .EUt(VA[GTValues.MV])
})
