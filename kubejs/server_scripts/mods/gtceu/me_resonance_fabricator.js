// AF9 - ME Resonance Fabricator: crafting of the machine, its port and its two casings, as in TerraFirmaGreg's recipes
// (Modpack-Modern, recipes.multiblocks.js and recipes.casings.js, LGPL-3.0; credits in CREDITS-textures.md).
// Machine: startup_scripts/gtceu/me_resonance_fabricator.js. TFG's cryogenized fluix (a TFG fluid) is liquid certus quartz
// here (ae2_quartz.js). The recipes it runs (gtceu:me_assembler) are not part of this script.

ServerEvents.recipes(event => {
    event.recipes.gtceu.assembler('af9:ptfe_black_casing')
        .itemInputs('6x gtceu:black_steel_plate', 'gtceu:black_steel_frame')
        .inputFluids(Fluid.of('gtceu:polytetrafluoroethylene', 216))
        .itemOutputs('2x af9:ptfe_black_casing')
        .circuit(6)
        .duration(50)
        .EUt(GTValues.VA[GTValues.LV])

    event.recipes.gtceu.assembler('af9:ae2_machine_casing')
        .itemInputs('6x gtceu:certus_quartz_plate', 'gtceu:stainless_steel_frame')
        .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144))
        .itemOutputs('2x af9:ae2_machine_casing')
        .circuit(6)
        .duration(50)
        .EUt(GTValues.VA[GTValues.LV])

    event.shaped('gtceu:me_assembler', ['ABA', 'DCD', 'ABA'], {
        A: 'ae2:charged_certus_quartz_crystal',
        B: 'ae2:spatial_pylon',
        C: '#gtceu:circuits/hv',
        D: 'af9:ptfe_black_casing'
    }).id('af9:me_assembler')

    event.recipes.gtceu.assembler('af9:me_assembler_redstone_port')
        .itemInputs('af9:ptfe_black_casing', 'gtceu:activity_detector_cover')
        .itemOutputs('gtceu:hv_me_assembler_redstone_port')
        .circuit(1)
        .duration(600)
        .EUt(GTValues.VA[GTValues.LV])
})
