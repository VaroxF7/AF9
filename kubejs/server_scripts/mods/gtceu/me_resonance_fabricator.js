// AF9 - Certus Resonating Manufacturer: crafting of the machine, its Resonating Fluix Hatch and its two casings, as
// in TerraFirmaGreg's recipes (Modpack-Modern, recipes.multiblocks.js and recipes.casings.js, LGPL-3.0; credits in
// CREDITS-textures.md), brought down to MV: both casings and the controller take an MV circuit, the AE2 casing a
// steel frame instead of a stainless one (stainless is HV here), and the controller is built from the machine's own
// casing — no PTFE anywhere in the machine (TFG's black PTFE casing recipe stays as a spare block).
// Machine: startup_scripts/gtceu/me_resonance_fabricator.js. TFG's cryogenized fluix (a TFG fluid) is liquid certus
// quartz here (ae2_quartz.js). The recipes it runs (gtceu:me_assembler) are not part of this script.

ServerEvents.recipes(event => {
    event.recipes.gtceu.assembler('af9:ptfe_black_casing')
        .itemInputs('6x gtceu:black_steel_plate', 'gtceu:black_steel_frame', '#gtceu:circuits/mv')
        .inputFluids(Fluid.of('gtceu:polytetrafluoroethylene', 216))
        .itemOutputs('2x af9:ptfe_black_casing')
        .circuit(6)
        .duration(50)
        .EUt(GTValues.VA[GTValues.MV])

    event.recipes.gtceu.assembler('af9:ae2_machine_casing')
        .itemInputs('6x gtceu:certus_quartz_plate', 'gtceu:steel_frame', '#gtceu:circuits/mv')
        .inputFluids(Fluid.of('gtceu:liquid_certus_quartz', 144))
        .itemOutputs('2x af9:ae2_machine_casing')
        .circuit(6)
        .duration(50)
        .EUt(GTValues.VA[GTValues.MV])

    event.shaped('gtceu:me_assembler', ['ABA', 'DCD', 'ABA'], {
        A: 'ae2:charged_certus_quartz_crystal',
        B: 'ae2:spatial_pylon',
        C: '#gtceu:circuits/mv',
        D: 'af9:ae2_machine_casing'
    }).id('af9:me_assembler')

    // ---- The Resonating Fluix Hatch: the stabiliser input of the Manufacturer ----
    // An input hatch of the tier, lined for the Liquid Resonating Fluix and fed with the fluid itself.
    // HV-UHV (UHV takes UV pumps: GT has no UHV pump without its high-tier content)
    const hatchTiers = [['hv', 'hv'], ['ev', 'ev'], ['iv', 'iv'], ['luv', 'luv'], ['zpm', 'zpm'], ['uv', 'uv'],
        ['uhv', 'uv']]
    hatchTiers.forEach(([tier, parts], index) => {
        event.recipes.gtceu.assembler(`af9:${tier}_resonance_hatch`)
            .itemInputs(`gtceu:${tier}_input_hatch`, `2x gtceu:${parts}_electric_pump`,
                'af9:ae2_machine_casing', '2x ae2:charged_certus_quartz_crystal')
            .inputFluids(Fluid.of('gtceu:liquid_resonating_fluix', 144))
            .itemOutputs(`gtceu:${tier}_resonance_hatch`)
            .duration(400)
            .EUt(GTValues.VA[GTValues.HV + index])
    })
})
