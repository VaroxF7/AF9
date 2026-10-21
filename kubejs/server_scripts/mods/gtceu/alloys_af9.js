// AF9 - The 15 alloys Janne picked (2026-10-16): mixes, the rhenium / hafnium / chronium chains, the hearth and forge
// casts. Materials: af9-core, registry/AF9Materials (alloys()). Ore: vein_ceres.js (chronite), the exotic asteroid
// (stellarite, neutronite shard). Void miner: miner.js (asteroids circuit 8). Spec: docs/alloys.md
//
//   EBF alloys     a mixer makes the dust, GT's EBF recipe (from the blast property) melts it:
//                  duralumin monel vanadium spring steel cupro-beryllium wolfram invar rheniloy albeme
//                  cobalt-molybdenum supersteel titanium aluminide hafnitinol (zincarry is a paste: dust only)
//   chains         rhenium: molybdenite roast flue dust -> perrhenate -> hydrogen reduction -> Ni3Re master alloy
//                  hafnium: HfCl4 (the zircon line) + magnesium (Kroll)
//                  chronium: chronite -> sulfate leach -> carbonate -> oxide -> calcium reduction
//   hearth         chronium alloy, triniar (the Sanguinite Hearth Furnace, 6000 K, supercooled coolant)
//   plasma forge   stellarium, pulsium (the DTPF, UHV)

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const UHV_AMPS = 48
    const gt = event.recipes.gtceu

    // ---- the mixes: dust to dust, then GT's EBF melts them ----
    const mix = (id, inputs, output, circuit, tier, seconds) => {
        gt.mixer(`af9:alloys/${id}`)
            .itemInputs(inputs)
            .itemOutputs(output)
            .circuit(circuit)
            .duration(seconds * 20)
            .EUt(VA[tier])
    }
    // 9 : 1 : 1 aluminium, copper, magnesium
    mix('duralumin', ['9x gtceu:aluminium_dust', 'gtceu:copper_dust', 'gtceu:magnesium_dust'],
        '11x gtceu:duralumin_dust', 1, GTValues.MV, 20)
    // 2 : 1 nickel, copper (the natural ratio of the Cu-Ni sulfides)
    mix('monel', ['2x gtceu:nickel_dust', 'gtceu:copper_dust'], '3x gtceu:monel_dust', 2, GTValues.MV, 10)
    mix('vanadium_spring_steel', ['8x gtceu:steel_dust', 'gtceu:vanadium_dust', 'gtceu:chromium_dust'],
        '10x gtceu:vanadium_spring_steel_dust', 3, GTValues.HV, 20)
    mix('cupro_beryllium', ['24x gtceu:copper_dust', 'gtceu:beryllium_dust'], '25x gtceu:cupro_beryllium_dust',
        4, GTValues.HV, 30)
    // invar is 2 iron : 1 nickel, so 6 invar are 4 iron + 2 nickel; the dust is GT's invar, 6 of it
    mix('wolfram_invar', ['6x gtceu:invar_dust', 'gtceu:tungsten_dust'], '7x gtceu:wolfram_invar_dust', 5,
        GTValues.HV, 20)
    mix('albeme', ['3x gtceu:aluminium_dust', '2x gtceu:beryllium_dust'], '5x gtceu:albeme_dust', 6,
        GTValues.EV, 20)
    mix('cobalt_molybdenum_supersteel', ['8x gtceu:steel_dust', '2x gtceu:cobalt_dust', 'gtceu:molybdenum_dust'],
        '11x gtceu:cobalt_molybdenum_supersteel_dust', 7, GTValues.EV, 25)
    mix('titanium_aluminide', ['gtceu:titanium_dust', 'gtceu:aluminium_dust'], '2x gtceu:titanium_aluminide_dust',
        8, GTValues.EV, 20)
    // nitinol is 1 : 1 nickel, titanium; hafnium 1 in 9
    mix('hafnitinol', ['4x gtceu:nickel_dust', '4x gtceu:titanium_dust', 'gtceu:hafnium_dust'],
        '9x gtceu:hafnitinol_dust', 9, GTValues.IV, 30)
    // the cell paste: zinc and silver wetted with mercury (a sealed hatch in the mixer)
    gt.mixer('af9:alloys/zincarry')
        .itemInputs('4x gtceu:zinc_dust', 'gtceu:silver_dust')
        .inputFluids(Fluid.of('gtceu:mercury', 1000))
        .itemOutputs('6x gtceu:zincarry_dust')
        .circuit(10)
        .duration(300)
        .EUt(VA[GTValues.HV])

    // ---- rhenium: the Re2O7 that leaves a molybdenite roaster with the flue gas ----
    // 4 MoS2 + 14 O2... in the pack's units: the roast of the fab line (fab_calcination, photolithography.js) with the
    // flue dust caught as a chance output
    gt.electric_blast_furnace('af9:alloys/molybdenite_roast_flue_dust')
        .itemInputs('4x gtceu:molybdenite_dust')
        .inputFluids(Fluid.of('gtceu:oxygen', 28000))
        .itemOutputs('4x gtceu:molybdenum_trioxide_dust')
        .chancedOutput('gtceu:rhenium_heptoxide_dust', 2500, 0)
        .outputFluids(Fluid.of('gtceu:sulfur_dioxide', 8000))
        .blastFurnaceTemp(1200)
        .duration(400)
        .EUt(VA[GTValues.HV])
    // Re2O7 + 2 NH3 + H2O -> 2 NH4ReO4 (the perrhenate crystallises out of the scrubber liquor)
    gt.chemical_reactor('af9:alloys/ammonium_perrhenate')
        .itemInputs('gtceu:rhenium_heptoxide_dust')
        .inputFluids(Fluid.of('gtceu:ammonia', 2000), Fluid.of('minecraft:water', 1000))
        .itemOutputs('2x gtceu:ammonium_perrhenate_dust')
        .duration(200)
        .EUt(VA[GTValues.HV])
    // 2 NH4ReO4 + 7 H2 -> 2 Re + 8 H2O + 2 NH3 (hydrogen reduction at 1,200 K; the ammonia goes up the stack)
    gt.electric_blast_furnace('af9:alloys/rhenium_reduction')
        .itemInputs('2x gtceu:ammonium_perrhenate_dust')
        .inputFluids(Fluid.of('gtceu:hydrogen', 7000))
        .itemOutputs('2x gtceu:rhenium_dust')
        .outputFluids(Fluid.of('gtceu:steam', 8000))
        .blastFurnaceTemp(1200)
        .duration(500)
        .EUt(VA[GTValues.EV])
    // the master alloy first (rhenium would not wet the melt), in hydrogen at 2,000 K: 2 Re + 6 Ni
    gt.electric_blast_furnace('af9:alloys/rhenium_nickel_master')
        .itemInputs('2x gtceu:rhenium_dust', '6x gtceu:nickel_dust')
        .inputFluids(Fluid.of('gtceu:hydrogen', 1000))
        .itemOutputs('8x gtceu:rhenium_nickel_master_alloy_dust')
        .blastFurnaceTemp(2000)
        .duration(400)
        .EUt(VA[GTValues.EV])
    mix('rheniloy', ['8x gtceu:rhenium_nickel_master_alloy_dust', 'gtceu:tantalum_dust', 'gtceu:tungsten_dust'],
        '10x gtceu:rheniloy_dust', 11, GTValues.EV, 30)

    // ---- the superalloys (docs/superalloys.md): 20 dust of each, circuits 12-17; Kovar's mix is in circuits_af9.js ----
    // 625: Ni 60, Cr 20, Mo 10, Fe 5, Nb 5
    mix('inconel_625', ['12x gtceu:nickel_dust', '4x gtceu:chromium_dust', '2x gtceu:molybdenum_dust',
        'gtceu:iron_dust', 'gtceu:niobium_dust'], '20x gtceu:inconel_625_dust', 12, GTValues.HV, 25)
    // 718: Ni 50, Cr 20, Fe 20, Nb 5, Mo 5
    mix('inconel_718', ['10x gtceu:nickel_dust', '4x gtceu:chromium_dust', '4x gtceu:iron_dust',
        'gtceu:niobium_dust', 'gtceu:molybdenum_dust'], '20x gtceu:inconel_718_dust', 13, GTValues.HV, 25)
    // 738: Ni 60, Cr 15 (15 nichrome dust are 12 Ni + 3 Cr), Co 10, Al, Ti, W 5 each
    mix('inconel_738', ['15x gtceu:nichrome_dust', '2x gtceu:cobalt_dust', 'gtceu:tungsten_dust',
        'gtceu:titanium_dust', 'gtceu:aluminium_dust'], '20x gtceu:inconel_738_dust', 14, GTValues.EV, 30)
    // Havar: Co 40, Cr 20, Ni 15, Fe 15, W 10
    mix('havar', ['8x gtceu:cobalt_dust', '4x gtceu:chromium_dust', '3x gtceu:nickel_dust', '3x gtceu:iron_dust',
        '2x gtceu:tungsten_dust'], '20x gtceu:havar_dust', 15, GTValues.HV, 25)
    // Stellite 1: Co 50, Cr 30, W 15, C 5; Stellite 12: Co 55, Cr 30, W 10, C 5
    mix('stellite_1', ['10x gtceu:cobalt_dust', '6x gtceu:chromium_dust', '3x gtceu:tungsten_dust',
        'gtceu:carbon_dust'], '20x gtceu:stellite_1_dust', 16, GTValues.HV, 25)
    mix('stellite_12', ['11x gtceu:cobalt_dust', '6x gtceu:chromium_dust', '2x gtceu:tungsten_dust',
        'gtceu:carbon_dust'], '20x gtceu:stellite_12_dust', 17, GTValues.EV, 30)

    // ---- hafnium: Kroll reduction of the hafnium tetrachloride the zircon line distils (electronics_metallurgy.js) ----
    // HfCl4 + 2 Mg -> Hf + 2 MgCl2 (the EBF's gas tier is the argon blanket)
    gt.electric_blast_furnace('af9:alloys/hafnium_kroll')
        .itemInputs('2x gtceu:magnesium_dust')
        .inputFluids(Fluid.of('gtceu:hafnium_tetrachloride', 1000))
        .itemOutputs('gtceu:hafnium_dust', '2x gtceu:magnesium_chloride_dust')
        .blastFurnaceTemp(1100)
        .duration(500)
        .EUt(VA[GTValues.EV])

    // ---- chronium: chronite, a rare-earth niobate of the Ceres belt ----
    // leach: the pyrochlore is opened by hot sulfuric acid, the niobium stays behind
    gt.chemical_reactor('af9:alloys/chronite_leach')
        .itemInputs('2x gtceu:chronite_dust')
        .inputFluids(Fluid.of('gtceu:sulfuric_acid', 3000))
        .chancedOutput('gtceu:niobium_dust', 5000, 0)
        .outputFluids(Fluid.of('gtceu:rare_earth_sulfate_solution', 2000))
        .duration(400)
        .EUt(VA[GTValues.EV])
    // soda ash precipitates the rare earths as carbonates (the spent sulfate liquor goes to the filtration plant)
    gt.chemical_reactor('af9:alloys/rare_earth_carbonate')
        .itemInputs('3x gtceu:soda_ash_dust')
        .inputFluids(Fluid.of('gtceu:rare_earth_sulfate_solution', 2000))
        .itemOutputs('2x gtceu:rare_earth_carbonate_dust')
        .duration(300)
        .EUt(VA[GTValues.EV])
    // calcination: carbonate to oxide, carbon dioxide off
    gt.electric_blast_furnace('af9:alloys/chronite_oxide')
        .itemInputs('2x gtceu:rare_earth_carbonate_dust')
        .itemOutputs('2x gtceu:chronite_oxide_mix_dust')
        .outputFluids(Fluid.of('gtceu:carbon_dioxide', 3000))
        .blastFurnaceTemp(1300)
        .duration(300)
        .EUt(VA[GTValues.EV])
    // reduction: calcium takes the oxygen (the lime slag is swept off), argon over the melt
    gt.electric_blast_furnace('af9:alloys/chronium_reduction')
        .itemInputs('gtceu:chronite_oxide_mix_dust', '3x gtceu:calcium_dust')
        .inputFluids(Fluid.of('gtceu:argon', 1000))
        .itemOutputs('2x gtceu:chronium_dust')
        .blastFurnaceTemp(1900)
        .duration(500)
        .EUt(VA[GTValues.EV])

    // ---- the Sanguinite Hearth Furnace: coolant first (fluid_in_0), the hearth preheated to 6,000 K ----
    // 3 ceranium + 1 chronium: one ingot's worth is 144 mB, four dust 576 mB
    gt.sanguinite_hearth('af9:alloys/chronium_alloy')
        .itemInputs('3x gtceu:ceranium_dust', 'gtceu:chronium_dust')
        .circuit(8)
        .inputFluids('#af9:coolant/xenon 1000')
        .outputFluids(Fluid.of('gtceu:chronium_alloy', 576))
        .blastFurnaceTemp(6000)
        .duration(1200)
        .EUt(VA[GTValues.UV])
    // 5 trinium + 2 stellarite + 1 naquadria
    gt.sanguinite_hearth('af9:alloys/triniar')
        .itemInputs('5x gtceu:trinium_dust', '2x gtceu:stellarite_dust', 'gtceu:naquadria_dust')
        .circuit(8)
        .inputFluids('#af9:coolant/xenon 1000')
        .outputFluids(Fluid.of('gtceu:triniar', 1152))
        .blastFurnaceTemp(6000)
        .duration(1400)
        .EUt(VA[GTValues.UV])

    // ---- the Plasma Forge (the DTPF): iron plasma and the exotic ores ----
    // 5 Fe (plasma) + 2 stellarite + 1 quantanium: eight ingots' worth, 1152 mB
    gt.plasma_forge('af9:alloys/stellarium')
        .itemInputs('2x gtceu:stellarite_dust', 'gtceu:quantanium_dust')
        .inputFluids(Fluid.of('gtceu:iron_plasma', 5000))
        .outputFluids(Fluid.of('gtceu:stellarium', 1152))
        .duration(900)
        .EUt(VA[GTValues.UHV], UHV_AMPS)
    // 4 stellarium + 2 neutronite shard, struck by strange matter and chromodynium plasma: five ingots' worth
    gt.plasma_forge('af9:alloys/pulsium')
        .itemInputs('4x gtceu:stellarium_ingot', '2x gtceu:neutronite_shard_dust')
        .inputFluids(Fluid.of('gtceu:strange_matter_plasma', 1000), Fluid.of('gtceu:chromodynium_plasma', 500))
        .outputFluids(Fluid.of('gtceu:pulsium', 720))
        .duration(1200)
        .EUt(VA[GTValues.UHV], UHV_AMPS)
})
