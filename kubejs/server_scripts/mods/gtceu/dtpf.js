// AF9 - the Dimensionally Transcendent Plasma Forge: its recipe and what it makes (machine and structure:
// startup_scripts/gtceu/dtpf.js, af9-core PlasmaForgeMachine). Spec: docs/dtpf.md
//
// Two plasmas ionised here from their dust and Endion, and forged back into matter with a yield above what the
// furnaces give: the forge's own pull. Idontknowium plasma is forged here too, out of the star matter plasma and the
// anti-matter plasma the Mega Fusion Reactor fuses (fusion_reactor.js). The running-time ramp (-50 % EU/t, -25 % time after
// half an hour) comes on top.

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    // UHV recipes draw 100 A of UHV power (Pico-scale work takes 300 A in the Advanced Circuit Manufacturer).
    // The controller below is built in the Advanced Circuit Manufacturer like the Pico circuits: UHV on 300 A.
    const UHV_AMPS = 100
    const forge = event.recipes.gtceu

    // The controller: UHV hull in a frame of mk3 fusion casing, held by field generators,
    // wired with sanguinite, computed with AI cards + qubit CPUs + eDRAM. Made in AF9's Advanced
    // Circuit Manufacturer (up to 16 items / 4 fluids; computation through its hatch; UHV on 300 A
    // through its laser hatch, like the Pico circuits in circuits_af9.js).
    // NOTE: GT registers no uhv_field_generator while its high-tier content is off (as it is in the
    // pack: tools/lint/data/gt-dev-only.txt; every UHV build uses uv_field_generator instead, e.g.
    // uhv_superconductor.js), so the field generators are UV.
    // The sanguinite is 8 stacks of 64 (a bus slot holds 64),
    // 8 inputs of 64x + 7 single inputs = 15 item inputs of 16 max.
    event.recipes.gtceu.advanced_circuit_manufacturer('af9:dtpf')
        .itemInputs('16x #gtceu:circuits/uhv', 'gtceu:uhv_machine_hull', '8x gtceu:fusion_casing_mk3',
            '2x gtceu:uv_field_generator', '4x af9:ai_acceleration_card', '32x gtceu:qbit_cpu_chip',
            '16x af9:edram_chip',
            '64x gtceu:fine_sanguinite_wire', '64x gtceu:fine_sanguinite_wire',
            '64x gtceu:fine_sanguinite_wire', '64x gtceu:fine_sanguinite_wire',
            '64x gtceu:fine_sanguinite_wire', '64x gtceu:fine_sanguinite_wire',
            '64x gtceu:fine_sanguinite_wire', '64x gtceu:fine_sanguinite_wire')
        .inputFluids(Fluid.of('gtceu:plasma_solder', 5760))
        .itemOutputs('gtceu:dtpf')
        .CWUt(128)
        .duration(1200)
        .EUt(VA[GTValues.UHV], 300)

    // ---- Ionisation: dust and Endion in, plasma out ----
    forge.plasma_forge('af9:dtpf/strange_matter_plasma')
        .itemInputs('8x gtceu:strange_matter_dust')
        .inputFluids(Fluid.of('gtceu:endion', 2000))
        .outputFluids(Fluid.of('gtceu:strange_matter_plasma', 2000))
        .duration(600)
        .EUt(VA[GTValues.UHV], UHV_AMPS)
    forge.plasma_forge('af9:dtpf/chromodynium_plasma')
        .itemInputs('4x gtceu:chromodynium_dust')
        .inputFluids(Fluid.of('gtceu:endion', 4000))
        .outputFluids(Fluid.of('gtceu:chromodynium_plasma', 2000))
        .duration(800)
        .EUt(VA[GTValues.UHV], UHV_AMPS)
    forge.plasma_forge('af9:dtpf/carbon_plasma')
        .itemInputs('8x gtceu:carbon_dust')
        .inputFluids(Fluid.of('gtceu:endion', 2000))
        .outputFluids(Fluid.of('gtceu:carbon_plasma', 2000))
        .duration(600)
        .EUt(VA[GTValues.UHV], UHV_AMPS)

    // ---- Forging: plasma back into matter, half as much again as the dust it came from ----
    // 2,000 mB of chromodynium plasma (4 dust) -> 6 plates
    forge.plasma_forge('af9:dtpf/chromodynium_plates')
        .inputFluids(Fluid.of('gtceu:chromodynium_plasma', 2000))
        .itemOutputs('6x gtceu:chromodynium_plate')
        .duration(400)
        .EUt(VA[GTValues.UHV], UHV_AMPS)
    // strange matter plasma hardens tritanium: 4 ingots -> 6 plates
    forge.plasma_forge('af9:dtpf/tritanium_plates')
        .itemInputs('4x gtceu:tritanium_ingot')
        .inputFluids(Fluid.of('gtceu:strange_matter_plasma', 500))
        .itemOutputs('6x gtceu:tritanium_plate')
        .duration(300)
        .EUt(VA[GTValues.UHV], UHV_AMPS)
    // and neutronium: 4 ingots -> 6 plates
    forge.plasma_forge('af9:dtpf/neutronium_plates')
        .itemInputs('4x gtceu:neutronium_ingot')
        .inputFluids(Fluid.of('gtceu:strange_matter_plasma', 1000))
        .itemOutputs('6x gtceu:neutronium_plate')
        .duration(500)
        .EUt(VA[GTValues.UHV], UHV_AMPS)

    // ---- Idontknowium: nobody knows what it is. Star matter and anti-matter held together in the forge's field ----
    // 2,000 mB star matter plasma (the Mega Fusion Reactor's) + 2,000 mB anti-matter plasma (the Mega's too) -> 1,000 mB at 12,000 K
    forge.plasma_forge('af9:dtpf/idontknowium_plasma')
        .inputFluids(Fluid.of('gtceu:star_matter_plasma', 2000), Fluid.of('gtceu:anti_matter_plasma', 2000))
        .outputFluids(Fluid.of('gtceu:idontknowium_plasma', 1000))
        .duration(1200)
        .EUt(VA[GTValues.UHV], UHV_AMPS)

    // ---- Star Matter Plasma Steel: the NVM board steel, forged as a superstate ----
    // star matter plasma + 4 steel -> 1,000 mB of superstate plasma (the Plasma Transformer
    // condenses it to plates at XPS: plasma_transformer.js)
    forge.plasma_forge('af9:dtpf/star_matter_plasma_steel_plasma')
        .itemInputs('4x gtceu:steel_ingot')
        .inputFluids(Fluid.of('gtceu:star_matter_plasma', 1000))
        .outputFluids(Fluid.of('gtceu:star_matter_plasma_steel_plasma', 1000))
        .duration(800)
        .EUt(VA[GTValues.UHV], UHV_AMPS)

    // ---- Nano particles: carbon plasma caught in a magnetic trap, after GTNH ----
    // the trap is not used up (like the quark-synthesis traps, it comes back empty)
    forge.plasma_forge('af9:dtpf/nano_particles')
        .notConsumable('af9:magnetic_trap')
        .inputFluids(Fluid.of('gtceu:carbon_plasma', 1000))
        .itemOutputs('4x af9:nano_particles')
        .duration(600)
        .EUt(VA[GTValues.UHV], UHV_AMPS)

    // ---- Mk2 refit: the Mk1 controller turned with the Plasma Heater's heart ----
    // the heater is integrated (sacrificed) into the Mk2: half the osmium for four times the amperage
    event.recipes.gtceu.assembly_line('af9:dtpf_mk2')
        .itemInputs('gtceu:dtpf', 'gtceu:plasma_heater', '4x gtceu:fusion_casing_mk3',
            '2x gtceu:uv_field_generator', '4x gtceu:double_tungsten_steel_plate')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 2304), Fluid.of('gtceu:niobium_titanium', 2304))
        .itemOutputs('gtceu:dtpf_mk2')
        .duration(1200)
        .EUt(VA[GTValues.UHV])
})
