// AF9 - the Dimensionally Transcendent Plasma Forge: its recipe and what it makes (machine and structure:
// startup_scripts/gtceu/dtpf.js, af9-core PlasmaForgeMachine). Spec: docs/dtpf.md
//
// Two plasmas, ionised here from their dust and Endion, and forged back into matter with a yield above what the
// furnaces give: the forge's own pull. The running-time ramp (-50 % EU/t, -25 % time after half an hour) comes on top.

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const forge = event.recipes.gtceu

    // The controller: a UV hull in a frame of fusion casing and coils, with the field generators that hold the plasma
    forge.assembler('af9:dtpf')
        .itemInputs('gtceu:uv_machine_hull', '4x gtceu:fusion_casing_mk2', '8x gtceu:superconducting_coil',
            '4x #gtceu:circuits/uhv', '2x gtceu:uv_field_generator', '16x gtceu:chromodynium_plate')
        .inputFluids(Fluid.of('gtceu:plasma_solder', 1440))
        .itemOutputs('gtceu:dtpf')
        .duration(1200)
        .EUt(VA[GTValues.UV])

    // ---- Ionisation: dust and Endion in, plasma out ----
    forge.plasma_forge('af9:dtpf/strange_matter_plasma')
        .itemInputs('8x gtceu:strange_matter_dust')
        .inputFluids(Fluid.of('gtceu:endion', 2000))
        .outputFluids(Fluid.of('gtceu:strange_matter_plasma', 2000))
        .duration(600)
        .EUt(VA[GTValues.UHV], 2)
    forge.plasma_forge('af9:dtpf/chromodynium_plasma')
        .itemInputs('4x gtceu:chromodynium_dust')
        .inputFluids(Fluid.of('gtceu:endion', 4000))
        .outputFluids(Fluid.of('gtceu:chromodynium_plasma', 2000))
        .duration(800)
        .EUt(VA[GTValues.UHV], 2)

    // ---- Forging: plasma back into matter, half as much again as the dust it came from ----
    // 2,000 mB of chromodynium plasma (4 dust) -> 6 plates
    forge.plasma_forge('af9:dtpf/chromodynium_plates')
        .inputFluids(Fluid.of('gtceu:chromodynium_plasma', 2000))
        .itemOutputs('6x gtceu:chromodynium_plate')
        .duration(400)
        .EUt(VA[GTValues.UHV], 2)
    // strange matter plasma hardens tritanium: 4 ingots -> 6 plates
    forge.plasma_forge('af9:dtpf/tritanium_plates')
        .itemInputs('4x gtceu:tritanium_ingot')
        .inputFluids(Fluid.of('gtceu:strange_matter_plasma', 500))
        .itemOutputs('6x gtceu:tritanium_plate')
        .duration(300)
        .EUt(VA[GTValues.UHV], 2)
    // and neutronium: 4 ingots -> 6 plates
    forge.plasma_forge('af9:dtpf/neutronium_plates')
        .itemInputs('4x gtceu:neutronium_ingot')
        .inputFluids(Fluid.of('gtceu:strange_matter_plasma', 1000))
        .itemOutputs('6x gtceu:neutronium_plate')
        .duration(500)
        .EUt(VA[GTValues.UHV], 2)
})
