// AF9 - synthetic titanium, HV to the end of EV: titanium without an ore, until the tier 3 rocket reaches Venus and its
// ilmenite vein (vein_venus.js). Materials: af9-core, registry/AF9Materials (syntheticTitanium()).
// Spec: docs/titanium-synthetic.md
//
// Both ways end in GT's rutile dust; from there GT's own chlorination (carbon + chlorine -> TiCl4) and Kroll (blast
// furnace with magnesium) run unchanged, and so does the magnesium loop.
//
// Stage 1, HV, lunar regolith (poor: about 64 moon sand per titanium):
//   moon sand        -separator->  lunar ilmenite concentrate (+ silica)
//   2 concentrate    -EBF, H2->    reduced titania slag + iron   (FeTiO3 + H2 -> Fe + TiO2 + H2O)
//   slag + H2SO4     -reactor->    crude titanyl sulfate liquor + silica   (the sulfate process)
//   crude liquor     -centrifuge-> titanyl sulfate liquor + copperas (ferrous sulfate)
//   liquor + steam   -reactor->    metatitanic acid + dilute acid back     (hydrolysis)
//   metatitanic acid -EBF->        rutile dust                             (calcination)
//   copperas         -EBF->        hematite + SO2                          (the sulfur comes home)
// Stage 2, EV (LCR and PTFE), the red mud of bauxite (about 8 bauxite per titanium, a heavy chemical bill):
//   bauxite + NaOH + water  -LCR->      red mud + sodium aluminate liquor  (Bayer)
//   aluminate liquor        -electrolyzer-> aluminium + NaOH
//   red mud + HCl           -LCR->      leach liquor (FeCl3, TiOCl2) + silica tailings
//   diesel + H3PO4          -mixer->    extractant (tributyl phosphate in diesel)
//   leach liquor + extractant -reactor->  loaded extract + iron(III) chloride raffinate
//   loaded extract + HCl    -reactor->  strip liquor + extractant back (80 %)
//   strip liquor + steam    -reactor->  metatitanic acid + hydrochloric acid back
// Stage 3, end of EV: the vanadium-catalysed chlorination of rutile gives double the TiCl4.

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const gt = event.recipes.gtceu

    // ---- Stage 1: lunar regolith ----
    // Ilmenite is magnetic: the separator takes a small fraction out of the moon sand.
    gt.electromagnetic_separator('af9:titanium_synthetic/lunar_beneficiation')
        .itemInputs('32x ad_astra:moon_sand')
        .itemOutputs('gtceu:lunar_ilmenite_concentrate_dust', '16x gtceu:silicon_dioxide_dust')
        .chancedOutput('gtceu:iron_dust', 2500, 0)
        .duration(600)
        .EUt(VA[GTValues.HV])
    // Hydrogen takes the oxygen off the iron: FeTiO3 + H2 -> Fe + TiO2 + H2O.
    gt.electric_blast_furnace('af9:titanium_synthetic/hydrogen_reduction')
        .itemInputs('2x gtceu:lunar_ilmenite_concentrate_dust')
        .inputFluids(Fluid.of('gtceu:hydrogen', 4000))
        .itemOutputs('gtceu:reduced_titania_slag_dust', '2x gtceu:iron_dust')
        .outputFluids(Fluid.of('gtceu:steam', 4000))
        .blastFurnaceTemp(1800)
        .duration(600)
        .EUt(VA[GTValues.HV])
    // The sulfate process: hot sulfuric acid digests the slag.
    gt.chemical_reactor('af9:titanium_synthetic/sulfuric_digest')
        .itemInputs('gtceu:reduced_titania_slag_dust')
        .inputFluids(Fluid.of('gtceu:sulfuric_acid', 2000))
        .itemOutputs('gtceu:silicon_dioxide_dust')
        .outputFluids(Fluid.of('gtceu:crude_titanyl_sulfate_liquor', 1000))
        .duration(400)
        .EUt(VA[GTValues.HV])
    // The iron leaves as copperas.
    gt.centrifuge('af9:titanium_synthetic/copperas_separation')
        .inputFluids(Fluid.of('gtceu:crude_titanyl_sulfate_liquor', 1000))
        .itemOutputs('2x gtceu:ferrous_sulfate_dust')
        .outputFluids(Fluid.of('gtceu:titanyl_sulfate_liquor', 1000))
        .duration(300)
        .EUt(VA[GTValues.HV])
    // Boiling hydrolysis: titanyl sulfate and steam give the white precipitate, the acid comes back dilute.
    gt.chemical_reactor('af9:titanium_synthetic/hydrolysis')
        .inputFluids(Fluid.of('gtceu:titanyl_sulfate_liquor', 1000), Fluid.of('gtceu:steam', 4000))
        .itemOutputs('gtceu:metatitanic_acid_dust')
        .outputFluids(Fluid.of('gtceu:sulfuric_acid', 800))
        .duration(400)
        .EUt(VA[GTValues.HV])
    // Calcination: the acid loses its water and becomes rutile.
    gt.electric_blast_furnace('af9:titanium_synthetic/calcination')
        .itemInputs('2x gtceu:metatitanic_acid_dust')
        .itemOutputs('2x gtceu:rutile_dust')
        .outputFluids(Fluid.of('gtceu:steam', 2000))
        .blastFurnaceTemp(1800)
        .duration(300)
        .EUt(VA[GTValues.HV])
    // Roasting the copperas: 2 FeSO4 -> Fe2O3 + SO2 + SO3.
    gt.electric_blast_furnace('af9:titanium_synthetic/copperas_roasting')
        .itemInputs('2x gtceu:ferrous_sulfate_dust')
        .itemOutputs('gtceu:hematite_dust')
        .outputFluids(Fluid.of('gtceu:sulfur_dioxide', 2000))
        .blastFurnaceTemp(1800)
        .duration(300)
        .EUt(VA[GTValues.HV])

    // ---- Stage 2: red mud (alternative route, EV) ----
    // The Bayer process: caustic soda dissolves the alumina of the bauxite, the red mud stays behind.
    gt.large_chemical_reactor('af9:titanium_synthetic/bayer_digest')
        .itemInputs('16x gtceu:bauxite_dust', '4x gtceu:sodium_hydroxide_dust')
        .inputFluids(Fluid.of('minecraft:water', 4000))
        .itemOutputs('8x gtceu:red_mud_dust')
        .outputFluids(Fluid.of('gtceu:sodium_aluminate_liquor', 4000))
        .duration(400)
        .EUt(VA[GTValues.EV])
    gt.electrolyzer('af9:titanium_synthetic/aluminate_electrolysis')
        .inputFluids(Fluid.of('gtceu:sodium_aluminate_liquor', 4000))
        .itemOutputs('4x gtceu:aluminium_dust', '4x gtceu:sodium_hydroxide_dust')
        .outputFluids(Fluid.of('minecraft:water', 2000))
        .duration(400)
        .EUt(VA[GTValues.HV])
    // Hydrochloric acid takes iron and titanium out of the mud; the sodalite tailings stay as silica.
    gt.large_chemical_reactor('af9:titanium_synthetic/red_mud_leach')
        .itemInputs('8x gtceu:red_mud_dust')
        .inputFluids(Fluid.of('gtceu:hydrochloric_acid', 4000))
        .itemOutputs('4x gtceu:silicon_dioxide_dust')
        .outputFluids(Fluid.of('gtceu:red_mud_leach_liquor', 1000))
        .duration(500)
        .EUt(VA[GTValues.EV])
    // Solvent extraction: tributyl phosphate in diesel takes the titanium, the iron stays in the water phase.
    gt.mixer('af9:titanium_synthetic/extractant')
        .inputFluids(Fluid.of('gtceu:diesel', 1000), Fluid.of('gtceu:phosphoric_acid', 500))
        .outputFluids(Fluid.of('gtceu:titanium_extractant', 1500))
        .duration(200)
        .EUt(VA[GTValues.HV])
    gt.chemical_reactor('af9:titanium_synthetic/extraction')
        .inputFluids(Fluid.of('gtceu:red_mud_leach_liquor', 1000), Fluid.of('gtceu:titanium_extractant', 1500))
        .outputFluids(Fluid.of('gtceu:loaded_titanium_extract', 1000), Fluid.of('gtceu:iron_iii_chloride', 500))
        .duration(300)
        .EUt(VA[GTValues.EV])
    // Stripping: hydrochloric acid takes the titanium back out; four fifths of the extractant return.
    gt.chemical_reactor('af9:titanium_synthetic/stripping')
        .inputFluids(Fluid.of('gtceu:loaded_titanium_extract', 1000), Fluid.of('gtceu:hydrochloric_acid', 500))
        .outputFluids(Fluid.of('gtceu:titanium_strip_liquor', 1000), Fluid.of('gtceu:titanium_extractant', 1200))
        .duration(300)
        .EUt(VA[GTValues.EV])
    gt.chemical_reactor('af9:titanium_synthetic/strip_hydrolysis')
        .inputFluids(Fluid.of('gtceu:titanium_strip_liquor', 1000), Fluid.of('gtceu:steam', 3000))
        .itemOutputs('2x gtceu:metatitanic_acid_dust')
        .outputFluids(Fluid.of('gtceu:hydrochloric_acid', 1000))
        .duration(400)
        .EUt(VA[GTValues.EV])

    // ---- Stage 3: the catalysed chlorination (end of EV) ----
    // In the LCR, vanadium pentoxide (not used up) doubles the TiCl4 of a rutile dust over GT's 1,000 mB.
    gt.large_chemical_reactor('af9:titanium_synthetic/catalysed_chlorination')
        .itemInputs('gtceu:rutile_dust', '2x gtceu:carbon_dust')
        .notConsumable('gtceu:vanadium_pentoxide_dust')
        .inputFluids(Fluid.of('gtceu:chlorine', 4000))
        .outputFluids(Fluid.of('gtceu:titanium_tetrachloride', 2000), Fluid.of('gtceu:carbon_monoxide', 2000))
        .duration(300)
        .EUt(VA[GTValues.EV])
})
