// AF9 - Recipes of the lithography process around the print (items, materials and machines:
// startup_scripts/gtceu/litho_process.js and air_conditioning.js; behaviour: AF9 Core LithoMachine).
// Spec: docs/semiconductor-factory.md §18

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // ---- Air Conditioning Hatches (MV-IV) ----
    // A compressor (pump), a fan (motor), the condenser (tier plates) and the control circuits in a machine hull. Every
    // part is made with the machines of the tier before, like the line's own parts.
    const hatches = [
        ['mv', 'gtceu:aluminium_plate', GTValues.MV],
        ['hv', 'gtceu:stainless_steel_plate', GTValues.HV],
        ['ev', 'gtceu:titanium_plate', GTValues.EV],
        ['iv', 'gtceu:tungsten_steel_plate', GTValues.IV]
    ]
    hatches.forEach(([tier, plate, voltage]) => {
        event.recipes.gtceu.assembler(`af9:${tier}_air_conditioning_hatch`)
            .circuit(1)   // the Scanner's recipe holds all of this and more (at IV): a programmed circuit tells them apart
            .itemInputs(`gtceu:${tier}_machine_hull`, `gtceu:${tier}_electric_pump`, `gtceu:${tier}_electric_motor`,
                `2x #gtceu:circuits/${tier}`, `4x ${plate}`)
            .inputFluids(Fluid.of('gtceu:soldering_alloy', 144))
            .itemOutputs(`gtceu:${tier}_air_conditioning_hatch`)
            .duration(200)
            .EUt(VA[voltage])
    })

    // ---- Calibration Wafer ----
    // A blank silicon wafer with chrome alignment marks: resist it, expose the marks, etch the chrome. Four from one.
    event.recipes.gtceu.assembler('af9:calibration_wafer')
        .itemInputs('gtceu:silicon_wafer', 'gtceu:chromium_plate')
        .inputFluids(Fluid.of('gtceu:photoresist', 100))
        .itemOutputs('4x kubejs:calibration_wafer')
        .duration(300)
        .EUt(VA[GTValues.MV])
})

// Each part is its own handler: a recipe that fails to load (an item id of another mod, a typo) stops only its own part.

// ======================================================================================================
// Chemistry (materials: startup_scripts/gtceu/litho_process.js). All in the SMC fab machines, like the
// rest of the fab chemistry; non-thermal recipes from HV power on need a clean room.
// ======================================================================================================
ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const gt = event.recipes.gtceu
    const MV = VA[GTValues.MV], HV = VA[GTValues.HV], IV = VA[GTValues.IV]

    // ---- RCA clean and the resist strip ----
    // SC-1 1:1:5 and SC-2 1:1:6 (ammonia / HCl, peroxide, water by volume), piranha 3:1 (sulfuric acid, peroxide)
    gt.fab_blending('af9:sc1_solution')
        .inputFluids(Fluid.of('gtceu:ammonia', 1000), Fluid.of('gtceu:hydrogen_peroxide', 1000),
            Fluid.of('gtceu:distilled_water', 5000))
        .outputFluids(Fluid.of('gtceu:sc1_solution', 7000))
        .duration(100)
        .EUt(MV)
    gt.fab_blending('af9:sc2_solution')
        .inputFluids(Fluid.of('gtceu:hydrochloric_acid', 1000), Fluid.of('gtceu:hydrogen_peroxide', 1000),
            Fluid.of('gtceu:distilled_water', 6000))
        .outputFluids(Fluid.of('gtceu:sc2_solution', 8000))
        .duration(100)
        .EUt(MV)
    gt.fab_blending('af9:piranha_solution')
        .inputFluids(Fluid.of('gtceu:sulfuric_acid', 3000), Fluid.of('gtceu:hydrogen_peroxide', 1000))
        .outputFluids(Fluid.of('gtceu:piranha_solution', 4000))
        .duration(100)
        .EUt(MV)
    // The spent strip is sulfuric acid with the dissolved resist: lime turns the acid into gypsum
    gt.fab_synthesis('af9:spent_piranha_neutralisation')
        .itemInputs('gtceu:calcium_hydroxide_dust')
        .inputFluids(Fluid.of('gtceu:spent_piranha', 1000))
        .itemOutputs('gtceu:gypsum_dust')
        .outputFluids(Fluid.of('minecraft:water', 500))
        .duration(200)
        .EUt(MV)

    // ---- Ethyl lactate ----
    // 2 CH3CH2OH + O2 -> 2 CH3CHO + 2 H2O over copper
    gt.fab_synthesis('af9:acetaldehyde')
        .notConsumable('gtceu:copper_dust')
        .inputFluids(Fluid.of('gtceu:ethanol', 1000), Fluid.of('gtceu:oxygen', 500))
        .outputFluids(Fluid.of('gtceu:acetaldehyde', 1000), Fluid.of('minecraft:water', 1000))
        .duration(200)
        .EUt(MV)
    // CH3CHO + HCN -> CH3CH(OH)CN, base catalysed
    gt.fab_synthesis('af9:lactonitrile')
        .notConsumable('gtceu:sodium_hydroxide_dust')
        .inputFluids(Fluid.of('gtceu:acetaldehyde', 1000), Fluid.of('gtceu:hydrogen_cyanide', 1000))
        .outputFluids(Fluid.of('gtceu:lactonitrile', 1000))
        .duration(200)
        .EUt(MV)
    // CH3CH(OH)CN + 2 H2O + H2SO4 -> CH3CH(OH)COOH + NH4HSO4
    gt.fab_synthesis('af9:lactic_acid')
        .inputFluids(Fluid.of('gtceu:lactonitrile', 1000), Fluid.of('minecraft:water', 2000),
            Fluid.of('gtceu:sulfuric_acid', 1000))
        .itemOutputs('gtceu:ammonium_bisulfate_dust')
        .outputFluids(Fluid.of('gtceu:lactic_acid', 1000))
        .duration(300)
        .EUt(MV)
    // Fischer esterification: the acid catalyst stays
    gt.fab_synthesis('af9:ethyl_lactate')
        .inputFluids(Fluid.of('gtceu:lactic_acid', 1000), Fluid.of('gtceu:ethanol', 1000))
        .notConsumableFluid(Fluid.of('gtceu:sulfuric_acid', 100))
        .outputFluids(Fluid.of('gtceu:ethyl_lactate', 1000), Fluid.of('minecraft:water', 1000))
        .duration(300)
        .EUt(MV)

    // ---- BARC ----
    // Nitration of naphthalene (mixed acid: the sulfuric acid stays), then polymer and dye in ethyl lactate
    gt.fab_synthesis('af9:nitronaphthalene')
        .circuit(4)
        .inputFluids(Fluid.of('gtceu:naphthalene', 1000), Fluid.of('gtceu:nitric_acid', 1000))
        .notConsumableFluid(Fluid.of('gtceu:sulfuric_acid', 100))
        .itemOutputs('gtceu:nitronaphthalene_dust')
        .outputFluids(Fluid.of('minecraft:water', 1000))
        .duration(300)
        .EUt(MV)
    gt.fab_blending('af9:barc')
        .itemInputs('gtceu:methacrylate_resin_dust', 'gtceu:nitronaphthalene_dust')
        .inputFluids(Fluid.of('gtceu:ethyl_lactate', 4000))
        .outputFluids(Fluid.of('gtceu:barc', 4000))
        .duration(200)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // ---- TARC, the etch plasma, and the coater's waste ----
    // TARC, the top anti-reflective coat of the immersion nodes: a fluoropolymer (PTFE's monomer) in PGMEA
    gt.fab_blending('af9:tarc')
        .inputFluids(Fluid.of('gtceu:tetrafluoroethylene', 500), Fluid.of('gtceu:propylene_glycol_methyl_ether_acetate', 1000))
        .outputFluids(Fluid.of('gtceu:tarc', 1500))
        .duration(200)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)
    // C + 2 F2 -> CF4
    gt.fab_synthesis('af9:tetrafluoromethane')
        .itemInputs('gtceu:carbon_dust')
        .inputFluids(Fluid.of('gtceu:fluorine', 2000))
        .outputFluids(Fluid.of('gtceu:tetrafluoromethane', 1000))
        .duration(200)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)
    // The etch plasma every print burns: CF4 etches the oxide, chlorine the silicon and metals, oxygen cleans the
    // polymer off, argon carries the discharge
    gt.fab_blending('af9:etch_plasma_gas')
        .inputFluids(Fluid.of('gtceu:tetrafluoromethane', 500), Fluid.of('gtceu:chlorine', 250),
            Fluid.of('gtceu:argon', 2000), Fluid.of('gtceu:oxygen', 250))
        .outputFluids(Fluid.of('gtceu:etch_plasma_gas', 3000))
        .duration(100)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)
    // What the coater spins off is mostly resist solvent: distilled back to PGMEA, 60 % of it (the rest is polymer and
    // the solvent that left with it: never all of it)
    gt.fab_fractionation('af9:recover_resist_solvent')
        .inputFluids(Fluid.of('gtceu:spent_resist_solvent', 1000))
        .itemOutputs('gtceu:carbon_dust')
        .outputFluids(Fluid.of('gtceu:propylene_glycol_methyl_ether_acetate', 600))
        .duration(200)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // ---- Coater Track (the machine: startup_scripts/gtceu/litho_process.js; the coating recipes: photolithography.js) ----
    // MV like the Line: the first (350 nm) print needs a coated wafer
    gt.assembler('af9:wafer_coater')
        .itemInputs('gtceu:mv_machine_hull', '2x gtceu:mv_electric_pump', '2x gtceu:mv_electric_motor',
            'gtceu:mv_robot_arm', '4x #gtceu:circuits/mv', '8x gtceu:stainless_steel_plate',
            '4x kubejs:plascrete_pipe_casing')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 288))
        .itemOutputs('gtceu:wafer_coater')
        .duration(400)
        .EUt(MV)

    // ---- Metal-oxo EUV resists ----
    // The same tin-free idea as the tin-oxo resist, with the zirconium and hafnium the zircon chain makes: hafnium
    // clusters absorb EUV the best (a quarter more resist per batch), zirconium is the plentiful one.
    gt.fab_synthesis('af9:zirconium_oxo_resist')
        .inputFluids(Fluid.of('gtceu:zirconium_tetrachloride', 1000), Fluid.of('gtceu:methacrylic_acid', 2000),
            Fluid.of('gtceu:propylene_glycol_methyl_ether_acetate', 4000), Fluid.of('gtceu:ultrapure_water', 1000))
        .outputFluids(Fluid.of('gtceu:euv_photoresist', 4000), Fluid.of('gtceu:hydrochloric_acid', 2000))
        .duration(600)
        .EUt(IV)
        .cleanroom(CleanroomType.CLEANROOM)
    gt.fab_synthesis('af9:hafnium_oxo_resist')
        .inputFluids(Fluid.of('gtceu:hafnium_tetrachloride', 1000), Fluid.of('gtceu:methacrylic_acid', 2000),
            Fluid.of('gtceu:propylene_glycol_methyl_ether_acetate', 4000), Fluid.of('gtceu:ultrapure_water', 1000))
        .outputFluids(Fluid.of('gtceu:euv_photoresist', 5000), Fluid.of('gtceu:hydrochloric_acid', 2000))
        .duration(600)
        .EUt(IV)
        .cleanroom(CleanroomType.CLEANROOM)
})

// ======================================================================================================
// The new chip families (chips.js): the functional layers their cards use. Thermal steps in the SMC thermal furnace
// (blast temperature, no clean room), the rest from HV power on in a clean room.
// ======================================================================================================
ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const gt = event.recipes.gtceu
    const HV = VA[GTValues.HV], EV = VA[GTValues.EV], LuV = VA[GTValues.LuV]

    // Acoustic wave: the piezo films. 2 Al + 2 NH3 -> 2 AlN + 3 H2 (reactive sputtering / MOCVD); Li + Nb + O2 solid
    // state, as the lithium niobate crystals SAW filters are cut from
    gt.fab_cvd('af9:aluminium_nitride')
        .itemInputs('2x gtceu:aluminium_dust')
        .inputFluids(Fluid.of('gtceu:ammonia', 2000))
        .itemOutputs('2x gtceu:aluminium_nitride_dust')
        .outputFluids(Fluid.of('gtceu:hydrogen', 3000))
        .blastFurnaceTemp(1400)
        .duration(300)
        .EUt(EV)
    gt.fab_calcination('af9:lithium_niobate')
        .itemInputs('gtceu:lithium_dust', 'gtceu:niobium_dust')
        .inputFluids(Fluid.of('gtceu:oxygen', 3000))
        .itemOutputs('gtceu:lithium_niobate_dust')
        .blastFurnaceTemp(1500)
        .duration(400)
        .EUt(EV)

    // Photonics: the silicon nitride waveguide. 3 Si + 4 NH3 -> Si3N4 + 6 H2 (LPCVD)
    gt.fab_cvd('af9:silicon_nitride')
        .itemInputs('3x gtceu:silicon_dust')
        .inputFluids(Fluid.of('gtceu:ammonia', 4000))
        .itemOutputs('gtceu:silicon_nitride_dust')
        .outputFluids(Fluid.of('gtceu:hydrogen', 6000))
        .blastFurnaceTemp(1300)
        .duration(400)
        .EUt(HV)

    // Spintronics: the CoFeB free layer of the tunnel junction (sputtered from an alloy target)
    gt.fab_blending('af9:cobalt_iron_boron')
        .itemInputs('gtceu:cobalt_dust', 'gtceu:iron_dust', 'gtceu:boron_dust')
        .itemOutputs('3x gtceu:cobalt_iron_boron_dust')
        .duration(200)
        .EUt(EV)
        .cleanroom(CleanroomType.CLEANROOM)

    // 2D materials: WSe2 by chemical vapour transport, hexagonal boron nitride from boron and ammonia
    gt.fab_cvd('af9:tungsten_diselenide')
        .itemInputs('gtceu:tungsten_dust', '2x gtceu:selenium_dust')
        .itemOutputs('gtceu:tungsten_diselenide_dust')
        .blastFurnaceTemp(1100)
        .duration(400)
        .EUt(LuV)
    gt.fab_cvd('af9:boron_nitride')
        .itemInputs('gtceu:boron_dust')
        .inputFluids(Fluid.of('gtceu:ammonia', 1000))
        .itemOutputs('gtceu:boron_nitride_dust')
        .outputFluids(Fluid.of('gtceu:hydrogen', 1500))
        .blastFurnaceTemp(1300)
        .duration(400)
        .EUt(LuV)

    // Neuromorphic: Ge2Sb2Te5, the phase-change material of PCM cells (the memristor crossbars)
    gt.fab_blending('af9:gst_alloy')
        .itemInputs('2x gtceu:germanium_dust', '2x gtceu:antimony_dust', '5x gtceu:tellurium_dust')
        .itemOutputs('9x gtceu:gst_alloy_dust')
        .duration(300)
        .EUt(LuV)
        .cleanroom(CleanroomType.CLEANROOM)

    // Quantum dots: CdSe nanocrystals by hot injection into a solvent
    gt.fab_synthesis('af9:quantum_dot_colloid')
        .itemInputs('gtceu:cadmium_dust', 'gtceu:selenium_dust')
        .inputFluids(Fluid.of('gtceu:dimethylbenzene', 1000))
        .outputFluids(Fluid.of('gtceu:quantum_dot_colloid', 1000))
        .duration(400)
        .EUt(LuV)
        .cleanroom(CleanroomType.CLEANROOM)
})

// ======================================================================================================
// Cards of the three new tiers (af9-core ComputeCard): the chips of the new families on a multilayer board. Photonic:
// photonic ICs (processors) and spintronic memory; Atomic: 2D-material logic and memristor memory; Sub-atomic:
// quantum-dot chips. Made at UV, in a clean room; the Sub-atomic ones also take the quantum-dot colloid (an
// assembler: the circuit assembler has one fluid slot).
// ======================================================================================================
ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const board = 'gtceu:multilayer_fiber_reinforced_printed_circuit_board'
    const wire = '8x gtceu:fine_yttrium_barium_cuprate_wire'
    const solder = Fluid.of('gtceu:soldering_alloy', 144)
    // [id, inputs besides the board and the wire, quantum-dot colloid]
    const cards = [
        ['photonic_cpu', ['2x kubejs:photonic_ic_chip', '4x gtceu:silicon_nitride_dust'], 0],
        ['photonic_gpu', ['4x kubejs:photonic_ic_chip', '8x gtceu:silicon_nitride_dust'], 0],
        ['photonic_ram', ['4x kubejs:spin_logic_chip', '2x gtceu:cobalt_iron_boron_dust'], 0],
        ['atomic_cpu', ['2x kubejs:tmd_logic_chip', '2x gtceu:tungsten_diselenide_dust', '2x gtceu:boron_nitride_dust'],
            0],
        ['atomic_gpu', ['4x kubejs:tmd_logic_chip', '4x gtceu:tungsten_diselenide_dust', '4x gtceu:boron_nitride_dust'],
            0],
        ['atomic_ram', ['4x kubejs:memristor_chip', '2x gtceu:gst_alloy_dust'], 0],
        ['subatomic_cpu', ['2x kubejs:quantum_dot_ic_chip'], 250],
        ['subatomic_gpu', ['4x kubejs:quantum_dot_ic_chip'], 500],
        ['subatomic_ram', ['2x kubejs:quantum_dot_ic_chip', '4x kubejs:memristor_chip'], 250]
    ]
    // the CPU, GPU and RAM card of a tier take the same things in different amounts: the programmed circuit (1 CPU,
    // 2 GPU, 3 RAM) decides which one the machine makes
    cards.forEach(([id, inputs, colloid]) => {
        const recipe = event.recipes.gtceu.circuit_assembler(`af9:${id}_card`)
            .circuit({ cpu: 1, gpu: 2, ram: 3 }[id.split('_')[1]])
            .itemInputs([board].concat(inputs, [wire]))
            .itemOutputs(`af9:${id}_card`)
            .duration(400)
            .EUt(VA[GTValues.UV])
            .cleanroom(CleanroomType.CLEANROOM)
        // GT's assembler and circuit assembler have ONE fluid slot: the sub-atomic cards print their quantum dots from the
        // colloid (ink) instead of soldering them
        if (colloid > 0) recipe.inputFluids(Fluid.of('gtceu:quantum_dot_colloid', colloid))
        else recipe.inputFluids(solder)
    })
})

// ======================================================================================================
// Metrology Station (startup_scripts/gtceu/litho_process.js, af9-core MetrologyStationMachine)
// ======================================================================================================
ServerEvents.recipes(event => {
    const VA = GTValues.VA
    // The tool: sensors and an emitter for the measuring, a robot arm for the wafer stage, MCUs and a Bus Connector's
    // cable for the link to the machines
    event.recipes.gtceu.assembler('af9:metrology_station')
        .itemInputs('gtceu:hv_machine_hull', '2x gtceu:hv_sensor', 'gtceu:hv_emitter', '4x #gtceu:circuits/hv',
            'gtceu:hv_robot_arm', '4x kubejs:mcu_chip', '8x gtceu:stainless_steel_plate', '4x af9:optical_bus_cable')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 288))
        .itemOutputs('gtceu:metrology_station')
        .duration(400)
        .EUt(VA[GTValues.HV])

    // A run: the reference wafer goes under the microscope (and comes back nine times in ten), the measurements are
    // evaluated with 24 CWU/t of computation over the machine bus
    event.recipes.gtceu.metrology('af9:metrology_run')
        .itemInputs('kubejs:calibration_wafer')
        .inputFluids(Fluid.of('gtceu:distilled_water', 100))
        .chancedOutput('kubejs:calibration_wafer', 9000, 0)
        .CWUt(24)
        .duration(600)
        .EUt(VA[GTValues.HV])
})
