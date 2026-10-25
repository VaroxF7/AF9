// AF9 - the Dimensionally Transcendent Plasma Forge: its recipe and what it makes (machine and structure:
// startup_scripts/gtceu/dtpf.js, af9-core PlasmaForgeMachine). Spec: docs/dtpf.md
//
// Two plasmas ionised here from their dust and Endion, and forged back into matter with a yield above what the
// furnaces give: the forge's own pull. Idontknowium plasma is forged here too, out of the star matter plasma and the
// anti-matter plasma the Mega Fusion Reactor fuses (fusion_reactor.js). The running-time ramp (-50 % EU/t, -25 % time after
// half an hour) comes on top. All of them run 1200 s at UHV at the lowest, on a laser hatch (one primary input), while the
// hall's magnetic field is held with osmium plasma and 10 A of LuV auxiliary power (af9-core PlasmaForgeMachine).
// Every forging recipe carries a forge_heat in Kelvin: the field is 1,000 K, 10,000 K with a Superheating Element module.

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    // UHV recipes draw 100 A of UHV power (Pico-scale work takes 300 A in the Advanced Circuit Manufacturer).
    // The controller below is built in the Advanced Circuit Manufacturer like the Pico circuits: UHV on 300 A.
    const UHV_AMPS = 100
    // Every forging recipe runs 1200 s at UHV at the lowest (perfect overclocks shorten it, the running-time ramp more)
    const FORGE_TICKS = 1200 * 20
    const forge = event.recipes.gtceu

    // The controller: UHV hull in a frame of transcendent casing, held by field generators,
    // wired with sanguinite, computed with AI cards + qubit CPUs + eDRAM. Made in AF9's Advanced
    // Circuit Manufacturer (up to 16 items / 4 fluids; computation through its hatch; UHV on 300 A
    // through its laser hatch, like the Pico circuits in circuits_af9.js).
    // NOTE: GT registers no uhv_field_generator while its high-tier content is off (as it is in the
    // pack: tools/lint/data/gt-dev-only.txt; every UHV build uses uv_field_generator instead, e.g.
    // uhv_superconductor.js), so the field generators are UV.
    // The sanguinite is 8 stacks of 64 (a bus slot holds 64),
    // 8 inputs of 64x + 7 single inputs = 15 item inputs of 16 max.
    event.recipes.gtceu.advanced_circuit_manufacturer('af9:dtpf')
        .itemInputs('16x #gtceu:circuits/uhv', 'gtceu:uhv_machine_hull', '8x af9:dtpf_transcendent_casing',
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

    // ---- The hall's blocks (structure: startup_scripts/gtceu/dtpf.js; textures GTNH's, tools/textures/dtpf_blocks.py) ----
    // the research item: a neutronium seed grown on a UV emitter
    event.recipes.gtceu.assembler('af9:dimensional_seed')
        .itemInputs('2x gtceu:neutronium_plate', 'gtceu:uv_emitter', 'gtceu:tritanium_plate')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 144))
        .itemOutputs('af9:dimensional_seed')
        .duration(400)
        .EUt(VA[GTValues.UV])
    // The casings: an Assembly Line at UV, researched on the Dimensional Seed. Zeron 100 parts and mk3 fusion casings,
    // four fluids of the ZPM and UV tiers (high grade solder, polybenzimidazole, soldering alloy and the molten
    // ruthenium trinium americium neutronate). Nothing here needs the forge's own products, so the first hall can be built from scratch.
    event.recipes.gtceu.assembly_line('af9:dtpf_transcendent_casing')
        .itemInputs('4x gtceu:fusion_casing_mk3', '16x gtceu:zeron_100_plate', '8x gtceu:zeron_100_rod',
            '2x gtceu:uv_field_generator', '2x gtceu:uv_emitter')
        .inputFluids(Fluid.of('gtceu:molten_ruthenium_trinium_americium_neutronate', 1152),
            Fluid.of('gtceu:soldering_alloy', 1152),
            Fluid.of('gtceu:high_grade_solder', 1152),
            Fluid.of('gtceu:polybenzimidazole', 1152))
        .itemOutputs('8x af9:dtpf_transcendent_casing')
        .stationResearch(b => b
            .researchStack(Item.of('af9:dimensional_seed'))
            .CWUt(64)
            .EUt(VA[GTValues.ZPM]))
        .duration(600)
        .EUt(VA[GTValues.UV])
    // the injection deck: the one casing that takes hatches. Assembly Line at UV, wound with spools (dearer than fine wire);
    // four fluids of the UV to UHV tiers, among them the cryogenics and the resonating fluix nobody used yet
    event.recipes.gtceu.assembly_line('af9:dtpf_injection_casing')
        .itemInputs('4x af9:dtpf_transcendent_casing', '32x gtceu:yttrium_barium_cuprate_wire_spool', '4x gtceu:uv_emitter',
            '12x gtceu:dense_ruthenium_trinium_americium_neutronate_plate', '16x gtceu:zeron_100_screw',
            '64x gtceu:zeron_100_ring')
        .inputFluids(Fluid.of('gtceu:dense_xenon', 2000), Fluid.of('gtceu:dense_argon', 2000),
            Fluid.of('gtceu:dense_endion', 1000), Fluid.of('gtceu:liquid_resonating_fluix', 1152))
        .itemOutputs('8x af9:dtpf_injection_casing')
        .stationResearch(b => b
            .researchStack(Item.of('af9:dtpf_transcendent_casing'))
            .CWUt(64)
            .EUt(VA[GTValues.ZPM]))
        .duration(800)
        .EUt(VA[GTValues.UV])
    // the module slots' casing: a plate of Inconel 738 on a transcendent casing (the controllers' recipes are the pack owner's)
    event.recipes.gtceu.assembler('af9:dtpf_module_casing')
        .itemInputs('af9:dtpf_transcendent_casing', '4x gtceu:inconel_738_plate')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 288))
        .itemOutputs('2x af9:dtpf_module_casing')
        .duration(200)
        .EUt(VA[GTValues.UV])
    // the bridge: lit while the forge works
    event.recipes.gtceu.assembler('af9:dtpf_bridge_casing')
        .itemInputs('2x af9:dtpf_transcendent_casing', 'gtceu:uv_emitter', 'gtceu:uv_sensor',
            '16x gtceu:fine_endionite_wire')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 288))
        .itemOutputs('2x af9:dtpf_bridge_casing')
        .duration(240)
        .EUt(VA[GTValues.UV])

    // The coils come at UHV, in the Assembly Line: a Resonant Endion coil (Boule Melting's best) wound anew round a
    // field generator and UHV circuits; the resonance coil is a containment coil pushed further with neutronium
    event.recipes.gtceu.assembly_line('af9:dtpf_containment_coil')
        .itemInputs('af9:resonant_endion_coil_block', '8x gtceu:tritanium_plate', 'gtceu:uv_field_generator',
            '2x #gtceu:circuits/uhv', '32x gtceu:fine_sanguinite_wire')
        .inputFluids(Fluid.of('gtceu:plasma_solder', 1152), Fluid.of('gtceu:supercooled_endion', 1000))
        .itemOutputs('2x af9:dtpf_containment_coil')
        .duration(600)
        .EUt(VA[GTValues.UHV])
    event.recipes.gtceu.assembly_line('af9:dtpf_resonance_coil')
        .itemInputs('af9:dtpf_containment_coil', '4x gtceu:neutronium_plate', '2x gtceu:uv_field_generator',
            '2x #gtceu:circuits/uhv', '32x gtceu:fine_endionite_wire')
        .inputFluids(Fluid.of('gtceu:plasma_solder', 1152), Fluid.of('gtceu:strange_matter_plasma', 500),
            Fluid.of('gtceu:supercooled_endion', 1000))
        .itemOutputs('af9:dtpf_resonance_coil')
        .duration(800)
        .EUt(VA[GTValues.UHV])

    // ---- Ionisation: dust and Endion in, plasma out ----
    forge.plasma_forge('af9:dtpf/strange_matter_plasma')
        .addData('forge_heat', 3000)
        .itemInputs('8x gtceu:strange_matter_dust')
        .inputFluids(Fluid.of('gtceu:endion', 2000))
        .outputFluids(Fluid.of('gtceu:strange_matter_plasma', 2000))
        .duration(FORGE_TICKS)
        .EUt(VA[GTValues.UHV], UHV_AMPS)
    forge.plasma_forge('af9:dtpf/chromodynium_plasma')
        .addData('forge_heat', 4000)
        .itemInputs('4x gtceu:chromodynium_dust')
        .inputFluids(Fluid.of('gtceu:endion', 4000))
        .outputFluids(Fluid.of('gtceu:chromodynium_plasma', 2000))
        .duration(FORGE_TICKS)
        .EUt(VA[GTValues.UHV], UHV_AMPS)
    forge.plasma_forge('af9:dtpf/carbon_plasma')
        .addData('forge_heat', 1000)
        .itemInputs('8x gtceu:carbon_dust')
        .inputFluids(Fluid.of('gtceu:endion', 2000))
        .outputFluids(Fluid.of('gtceu:carbon_plasma', 2000))
        .duration(FORGE_TICKS)
        .EUt(VA[GTValues.UHV], UHV_AMPS)

    // ---- Forging: plasma back into matter, half as much again as the dust it came from ----
    // 2,000 mB of chromodynium plasma (4 dust) -> 6 plates
    forge.plasma_forge('af9:dtpf/chromodynium_plates')
        .addData('forge_heat', 2000)
        .inputFluids(Fluid.of('gtceu:chromodynium_plasma', 2000))
        .itemOutputs('6x gtceu:chromodynium_plate')
        .duration(FORGE_TICKS)
        .EUt(VA[GTValues.UHV], UHV_AMPS)
    // strange matter plasma hardens tritanium: 4 ingots -> 6 plates
    forge.plasma_forge('af9:dtpf/tritanium_plates')
        .addData('forge_heat', 2000)
        .itemInputs('4x gtceu:tritanium_ingot')
        .inputFluids(Fluid.of('gtceu:strange_matter_plasma', 500))
        .itemOutputs('6x gtceu:tritanium_plate')
        .duration(FORGE_TICKS)
        .EUt(VA[GTValues.UHV], UHV_AMPS)
    // and neutronium: 4 ingots -> 6 plates
    forge.plasma_forge('af9:dtpf/neutronium_plates')
        .addData('forge_heat', 6000)
        .itemInputs('4x gtceu:neutronium_ingot')
        .inputFluids(Fluid.of('gtceu:strange_matter_plasma', 1000))
        .itemOutputs('6x gtceu:neutronium_plate')
        .duration(FORGE_TICKS)
        .EUt(VA[GTValues.UHV], UHV_AMPS)

    // ---- Idontknowium: nobody knows what it is. Star matter and anti-matter held together in the forge's field ----
    // 2,000 mB star matter plasma (the Mega Fusion Reactor's) + 2,000 mB anti-matter plasma (the Mega's too) -> 1,000 mB at 12,000 K
    forge.plasma_forge('af9:dtpf/idontknowium_plasma')
        .addData('forge_heat', 10000)
        .inputFluids(Fluid.of('gtceu:star_matter_plasma', 2000), Fluid.of('gtceu:anti_matter_plasma', 2000))
        .outputFluids(Fluid.of('gtceu:idontknowium_plasma', 1000))
        .duration(FORGE_TICKS)
        .EUt(VA[GTValues.UHV], UHV_AMPS)

    // ---- Star Matter Plasma Steel: the NVM board steel, forged as a superstate ----
    // star matter plasma + 4 steel -> 1,000 mB of superstate plasma (the Plasma Transformer
    // condenses it to plates at XPS: plasma_transformer.js)
    forge.plasma_forge('af9:dtpf/star_matter_plasma_steel_plasma')
        .addData('forge_heat', 8000)
        .itemInputs('4x gtceu:steel_ingot')
        .inputFluids(Fluid.of('gtceu:star_matter_plasma', 1000))
        .outputFluids(Fluid.of('gtceu:star_matter_plasma_steel_plasma', 1000))
        .duration(FORGE_TICKS)
        .EUt(VA[GTValues.UHV], UHV_AMPS)

    // ---- Nano particles: carbon plasma caught in a magnetic trap, after GTNH ----
    // the trap is not used up (like the quark-synthesis traps, it comes back empty)
    forge.plasma_forge('af9:dtpf/nano_particles')
        .addData('forge_heat', 1000)
        .notConsumable('af9:magnetic_trap')
        .inputFluids(Fluid.of('gtceu:carbon_plasma', 1000))
        .itemOutputs('4x af9:nano_particles')
        .duration(FORGE_TICKS)
        .EUt(VA[GTValues.UHV], UHV_AMPS)

    // ---- Mk2 refit: the Mk1 controller turned with a UV hull ----
    // half the osmium for four times the amperage
    event.recipes.gtceu.assembly_line('af9:dtpf_mk2')
        .itemInputs('gtceu:dtpf', 'gtceu:uv_machine_hull', '4x af9:dtpf_bridge_casing',
            '2x af9:dtpf_resonance_coil', '2x gtceu:uv_field_generator', '4x gtceu:double_tungsten_steel_plate')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 2304), Fluid.of('gtceu:niobium_titanium', 2304))
        .itemOutputs('gtceu:dtpf_mk2')
        .duration(1200)
        .EUt(VA[GTValues.UHV])
})
