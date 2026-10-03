// AF9 - The platinum group metals line (materials: af9-core, registry/AF9Materials). Spec: docs/platinum-group-metals.md
//
// GT's own chain (platinum group sludge, aqua regia, centrifuge) is not touched; its sludge is one more feed here.
// Tiers follow GT's: platinum and palladium from HV, ruthenium and rhodium from EV, osmium and iridium from IV.
// One batch of 8 leach residue gives 4 platinum, 3 palladium, 1 gold, 1 rhodium, 1 ruthenium, 1 iridium and 1 osmium
// (GT's line: 1 platinum, 0.6 palladium and a third of each of the others for 6 sludge, which is 3 sulfide ore).
// Ammonium chloride, hydrochloric acid, zinc and most of the chlorine come back out of the later steps.

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const MV = VA[GTValues.MV], HV = VA[GTValues.HV], EV = VA[GTValues.EV], IV = VA[GTValues.IV]
    const gt = event.recipes.gtceu

    // =============================================================================================================
    // 1. MATTE (HV): the purified ore is smelted with a silica flux in an oxygen blast. The iron goes into the slag,
    // the sulfur leaves as sulfur dioxide (a sulfuric acid plant's feed), nickel, copper and the PGMs stay in the matte.
    // Cooperite (PtS) is nearly all platinum: twice the matte per ore, and less sulfur.
    // =============================================================================================================
    const ores = [
        // ore, ore count, matte count, SO2 (mB)
        ['cooperite', 2, 8, 2000],
        ['chalcopyrite', 4, 8, 4000],
        ['pentlandite', 4, 8, 4000],
        ['bornite', 4, 8, 4000],
        ['chalcocite', 4, 8, 3000],
        ['tetrahedrite', 4, 8, 3000]
    ]
    ores.forEach(([ore, count, matte, so2]) => {
        gt.electric_blast_furnace(`af9:pgm_matte_from_${ore}`)
            .itemInputs(`${count}x gtceu:purified_${ore}_ore`, '2x gtceu:silicon_dioxide_dust')
            .inputFluids(Fluid.of('gtceu:oxygen', 4000))
            .itemOutputs(`${matte}x gtceu:pgm_matte_dust`, '2x gtceu:dark_ash_dust')
            .outputFluids(Fluid.of('gtceu:sulfur_dioxide', so2))
            .blastFurnaceTemp(1800)
            .duration(400)
            .EUt(HV)
    })

    // GT's platinum group sludge (its nitric acid leach of the same ores) is a feed of its own: a hydrochloric wash
    // upgrades three of it to one leach residue
    gt.chemical_reactor('af9:pgm_residue_from_sludge')
        .itemInputs('3x gtceu:platinum_group_sludge_dust')
        .inputFluids(Fluid.of('gtceu:hydrochloric_acid', 1000))
        .itemOutputs('gtceu:pgm_leach_residue_dust')
        .outputFluids(Fluid.of('gtceu:diluted_hydrochloric_acid', 1000))
        .duration(200)
        .EUt(HV)

    // =============================================================================================================
    // 2. LEACH (HV): hot sulfuric acid under oxygen pressure dissolves nickel and copper (the base metal refinery's
    // feed, electrowon below); the platinum group metals stay behind as a residue of about 60 % PGM.
    // =============================================================================================================
    gt.chemical_reactor('af9:pgm_matte_leach')
        .itemInputs('4x gtceu:pgm_matte_dust')
        .inputFluids(Fluid.of('gtceu:sulfuric_acid', 4000), Fluid.of('gtceu:oxygen', 2000))
        .itemOutputs('2x gtceu:pgm_leach_residue_dust')
        .outputFluids(Fluid.of('gtceu:sulfuric_nickel_solution', 1000), Fluid.of('gtceu:sulfuric_copper_solution', 1000))
        .duration(300)
        .EUt(HV)

    // Electrowinning of the base metals; the acid is regenerated
    gt.electrolyzer('af9:pgm_nickel_electrowinning')
        .inputFluids(Fluid.of('gtceu:sulfuric_nickel_solution', 1000))
        .itemOutputs('2x gtceu:nickel_dust')
        .outputFluids(Fluid.of('gtceu:sulfuric_acid', 1000))
        .duration(200)
        .EUt(MV)
    gt.electrolyzer('af9:pgm_copper_electrowinning')
        .inputFluids(Fluid.of('gtceu:sulfuric_copper_solution', 1000))
        .itemOutputs('2x gtceu:copper_dust')
        .outputFluids(Fluid.of('gtceu:sulfuric_acid', 1000))
        .duration(200)
        .EUt(MV)

    // =============================================================================================================
    // 3. DISSOLUTION (HV): hydrochloric acid and chlorine (modern refineries do not use aqua regia) take platinum,
    // palladium and gold into solution as chloro complexes. Rhodium, iridium, ruthenium and osmium stay behind.
    // =============================================================================================================
    gt.chemical_reactor('af9:pgm_dissolution')
        .itemInputs('8x gtceu:pgm_leach_residue_dust')
        .inputFluids(Fluid.of('gtceu:hydrochloric_acid', 4000), Fluid.of('gtceu:chlorine', 2000))
        .itemOutputs('3x gtceu:pgm_insoluble_residue_dust')
        .outputFluids(Fluid.of('gtceu:platinum_group_chloride_liquor', 6000))
        .duration(400)
        .EUt(HV)

    // =============================================================================================================
    // 4. GOLD (HV): sulfur dioxide reduces the gold out of the solution: 2 HAuCl4 + 3 SO2 + 6 H2O -> 2 Au + ...
    // =============================================================================================================
    gt.chemical_reactor('af9:pgm_gold_precipitation')
        .inputFluids(Fluid.of('gtceu:platinum_group_chloride_liquor', 6000), Fluid.of('gtceu:sulfur_dioxide', 1500))
        .itemOutputs('gtceu:gold_dust')
        .outputFluids(Fluid.of('gtceu:gold_free_liquor', 6000))
        .duration(200)
        .EUt(HV)

    // =============================================================================================================
    // 5. PLATINUM (HV): Pt(IV) is the only one of the three that ammonium chloride precipitates, as (NH4)2PtCl6.
    // Palladium stays in the filtrate as PdCl4(2-). Calcined at 800 C the salt falls apart into platinum sponge,
    // ammonium chloride and hydrochloric acid (3 (NH4)2PtCl6 -> 3 Pt + 2 NH4Cl + 2 N2 + 16 HCl).
    // =============================================================================================================
    gt.chemical_reactor('af9:pgm_platinum_precipitation')
        .itemInputs('8x gtceu:ammonium_chloride_dust')
        .inputFluids(Fluid.of('gtceu:gold_free_liquor', 6000))
        .itemOutputs('4x gtceu:ammonium_hexachloroplatinate_dust')
        .outputFluids(Fluid.of('gtceu:palladium_filtrate', 5000))
        .duration(300)
        .EUt(HV)

    gt.electric_blast_furnace('af9:pgm_platinum_calcination')
        .itemInputs('4x gtceu:ammonium_hexachloroplatinate_dust')
        .itemOutputs('4x gtceu:platinum_dust', '2x gtceu:ammonium_chloride_dust')
        .outputFluids(Fluid.of('gtceu:hydrochloric_acid', 4000))
        .blastFurnaceTemp(1200)
        .duration(300)
        .EUt(HV)

    // =============================================================================================================
    // 6. PALLADIUM (HV): ammonia turns the filtrate into the colourless tetraammine, hydrochloric acid precipitates
    // the yellow diammine salt, hydrogen reduces it: Pd(NH3)2Cl2 + H2 -> Pd + 2 NH4Cl. Pure palladium sponge, and
    // all the ammonium chloride comes back.
    // =============================================================================================================
    gt.chemical_reactor('af9:pgm_palladium_ammination')
        .inputFluids(Fluid.of('gtceu:palladium_filtrate', 5000), Fluid.of('gtceu:ammonia', 4000))
        .outputFluids(Fluid.of('gtceu:palladium_tetraammine_solution', 8000))
        .duration(200)
        .EUt(HV)

    gt.chemical_reactor('af9:pgm_palladium_salt')
        .inputFluids(Fluid.of('gtceu:palladium_tetraammine_solution', 8000), Fluid.of('gtceu:hydrochloric_acid', 4000))
        .itemOutputs('3x gtceu:palladium_diammine_dichloride_dust')
        .outputFluids(Fluid.of('gtceu:diluted_hydrochloric_acid', 4000))
        .duration(200)
        .EUt(HV)

    gt.electric_blast_furnace('af9:pgm_palladium_reduction')
        .itemInputs('3x gtceu:palladium_diammine_dichloride_dust')
        .inputFluids(Fluid.of('gtceu:hydrogen', 2000))
        .itemOutputs('3x gtceu:palladium_dust', '6x gtceu:ammonium_chloride_dust')
        .blastFurnaceTemp(900)
        .duration(300)
        .EUt(HV)

    // =============================================================================================================
    // 7. RUTHENIUM AND OSMIUM
    // The insoluble residue is fused with sodium hydroxide and saltpeter (oxidising, alkaline): ruthenium and osmium
    // become the water-soluble ruthenate and osmate, rhodium and iridium oxidise to their oxides.
    // =============================================================================================================
    gt.electric_blast_furnace('af9:pgm_alkaline_fusion')
        .itemInputs('3x gtceu:pgm_insoluble_residue_dust', '3x gtceu:sodium_hydroxide_dust', '2x gtceu:saltpeter_dust')
        .itemOutputs('3x gtceu:pgm_fusion_cake_dust')
        .outputFluids(Fluid.of('gtceu:steam', 1500))
        .blastFurnaceTemp(1500)
        .duration(300)
        .EUt(HV)

    // Water takes the ruthenate and osmate; Rh2O3 and IrO2 stay as an oxide
    gt.chemical_reactor('af9:pgm_fusion_cake_leach')
        .itemInputs('3x gtceu:pgm_fusion_cake_dust')
        .inputFluids(Fluid.of('gtceu:water', 3000))
        .itemOutputs('2x gtceu:rhodium_iridium_oxide_dust')
        .outputFluids(Fluid.of('gtceu:ruthenate_osmate_liquor', 3000))
        .duration(200)
        .EUt(HV)

    // Chlorine oxidises both to the volatile tetroxides, which leave as one vapour (the vessel is closed and the
    // vapour goes through scrubbers: both are poisonous)
    gt.chemical_reactor('af9:pgm_tetroxide_distillate')
        .inputFluids(Fluid.of('gtceu:ruthenate_osmate_liquor', 3000), Fluid.of('gtceu:chlorine', 3000))
        .itemOutputs('3x gtceu:salt_dust')
        .outputFluids(Fluid.of('gtceu:platinum_group_tetroxide_vapour', 2000))
        .duration(300)
        .EUt(EV)

    // RuO4 boils at 40 C, OsO4 at 130 C: the tower splits them
    gt.distillation_tower('af9:pgm_tetroxide_separation')
        .inputFluids(Fluid.of('gtceu:platinum_group_tetroxide_vapour', 2000))
        .outputFluids(Fluid.of('gtceu:ruthenium_tetroxide_vapour', 1000), Fluid.of('gtceu:osmium_tetroxide_vapour', 1000))
        .duration(300)
        .EUt(EV)

    // Ruthenium: hydrochloric acid absorbs RuO4 (it is reduced and gives chlorine back), ammonium chloride
    // precipitates (NH4)2RuCl6, hydrogen reduces it at 900 C: (NH4)2RuCl6 + 2 H2 -> Ru + 2 NH4Cl + 4 HCl
    gt.chemical_reactor('af9:pgm_ruthenium_salt')
        .itemInputs('2x gtceu:ammonium_chloride_dust')
        .inputFluids(Fluid.of('gtceu:ruthenium_tetroxide_vapour', 1000), Fluid.of('gtceu:hydrochloric_acid', 6000))
        .itemOutputs('gtceu:ammonium_hexachlororuthenate_dust')
        .outputFluids(Fluid.of('gtceu:chlorine', 2000), Fluid.of('gtceu:water', 4000))
        .duration(300)
        .EUt(EV)

    gt.electric_blast_furnace('af9:pgm_ruthenium_reduction')
        .itemInputs('gtceu:ammonium_hexachlororuthenate_dust')
        .inputFluids(Fluid.of('gtceu:hydrogen', 4000))
        .itemOutputs('gtceu:ruthenium_dust', '2x gtceu:ammonium_chloride_dust')
        .outputFluids(Fluid.of('gtceu:hydrochloric_acid', 4000))
        .blastFurnaceTemp(1200)
        .duration(300)
        .EUt(EV)

    // Osmium: ethanol reduces OsO4 in hydrochloric acid, ammonium chloride precipitates (NH4)2OsCl6 (Fremy's salt),
    // hydrogen reduces it
    gt.chemical_reactor('af9:pgm_osmium_salt')
        .itemInputs('2x gtceu:ammonium_chloride_dust')
        .inputFluids(Fluid.of('gtceu:osmium_tetroxide_vapour', 1000), Fluid.of('gtceu:hydrochloric_acid', 6000),
            Fluid.of('gtceu:ethanol', 1000))
        .itemOutputs('gtceu:ammonium_hexachloroosmate_dust')
        .outputFluids(Fluid.of('gtceu:water', 2000))
        .duration(300)
        .EUt(IV)

    gt.electric_blast_furnace('af9:pgm_osmium_reduction')
        .itemInputs('gtceu:ammonium_hexachloroosmate_dust')
        .inputFluids(Fluid.of('gtceu:hydrogen', 4000))
        .itemOutputs('gtceu:osmium_dust', '2x gtceu:ammonium_chloride_dust')
        .outputFluids(Fluid.of('gtceu:hydrochloric_acid', 4000))
        .blastFurnaceTemp(1300)
        .duration(300)
        .EUt(IV)

    // =============================================================================================================
    // 8. IRIDIUM AND RHODIUM
    // Rh2O3 and IrO2 do not dissolve in plain acid; hot hydrochloric acid with chlorine under pressure takes them.
    // =============================================================================================================
    gt.chemical_reactor('af9:pgm_rhodium_iridium_chlorination')
        .itemInputs('2x gtceu:rhodium_iridium_oxide_dust')
        .inputFluids(Fluid.of('gtceu:hydrochloric_acid', 4000), Fluid.of('gtceu:chlorine', 2000))
        .outputFluids(Fluid.of('gtceu:rhodium_iridium_chloride_liquor', 4000))
        .duration(400)
        .EUt(EV)

    // Nitric acid oxidises Ir(III) to Ir(IV); only the IV chloro complex precipitates with ammonium chloride.
    // Rhodium(III) stays in the filtrate.
    gt.chemical_reactor('af9:pgm_iridium_precipitation')
        .itemInputs('3x gtceu:ammonium_chloride_dust')
        .inputFluids(Fluid.of('gtceu:rhodium_iridium_chloride_liquor', 4000), Fluid.of('gtceu:nitric_acid', 500))
        .itemOutputs('gtceu:ammonium_hexachloroiridate_dust')
        .outputFluids(Fluid.of('gtceu:rhodium_chloride_filtrate', 3000))
        .duration(300)
        .EUt(EV)

    gt.electric_blast_furnace('af9:pgm_iridium_reduction')
        .itemInputs('gtceu:ammonium_hexachloroiridate_dust')
        .inputFluids(Fluid.of('gtceu:hydrogen', 4000))
        .itemOutputs('gtceu:iridium_dust', '2x gtceu:ammonium_chloride_dust')
        .outputFluids(Fluid.of('gtceu:hydrochloric_acid', 4000))
        .blastFurnaceTemp(1300)
        .duration(300)
        .EUt(IV)

    // Rhodium: sodium nitrite precipitates the hexanitritorhodate, which leaves the base metals behind; hot
    // hydrochloric acid destroys the nitrite (brown fumes) and gives a pure rhodium chloride solution
    gt.chemical_reactor('af9:pgm_rhodium_nitrite')
        .itemInputs('3x gtceu:sodium_nitrite_dust')
        .inputFluids(Fluid.of('gtceu:rhodium_chloride_filtrate', 3000))
        .itemOutputs('gtceu:sodium_hexanitritorhodate_dust')
        .outputFluids(Fluid.of('gtceu:diluted_hydrochloric_acid', 3000))
        .duration(300)
        .EUt(EV)

    gt.chemical_reactor('af9:pgm_rhodium_chloride')
        .itemInputs('gtceu:sodium_hexanitritorhodate_dust')
        .inputFluids(Fluid.of('gtceu:hydrochloric_acid', 6000))
        .itemOutputs('3x gtceu:salt_dust')
        .outputFluids(Fluid.of('gtceu:rhodium_trichloride_solution', 1000), Fluid.of('gtceu:nitrogen_dioxide', 3000))
        .duration(300)
        .EUt(EV)

    // Cementation with zinc: 3 Zn + 2 RhCl3 -> 2 Rh + 3 ZnCl2. The zinc chloride is electrolysed back to zinc and chlorine.
    gt.chemical_reactor('af9:pgm_rhodium_cementation')
        .itemInputs('2x gtceu:zinc_dust')
        .inputFluids(Fluid.of('gtceu:rhodium_trichloride_solution', 1000))
        .itemOutputs('gtceu:rhodium_dust', '2x gtceu:zinc_chloride_dust')
        .duration(200)
        .EUt(EV)

    gt.electrolyzer('af9:zinc_chloride_electrolysis')
        .itemInputs('2x gtceu:zinc_chloride_dust')
        .itemOutputs('2x gtceu:zinc_dust')
        .outputFluids(Fluid.of('gtceu:chlorine', 2000))
        .duration(200)
        .EUt(MV)
})
