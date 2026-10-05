// AF9 - Fab chemistry recipes (materials: af9-core, registry/AF9Materials)
// Real industrial routes, one machine step per real unit operation. Spec: docs/semiconductor-factory.md §6.11-6.15
//
// All of it runs only in the SMC fab machines (startup_scripts/gtceu/fab_machines.js, spec §11): GT's Chemical
// Reactor, Large Chemical Reactor, mixer, blast furnace and distillation tower have none of these recipes. Non-thermal
// recipes from HV power on need a clean room (the fab multiblocks bring their own).
// Tiers follow what a player can build at that point: the MV chains (silicon, HF, argon) run in MV single blocks
// (the Fractionating Still takes column recipes one cut at a time); the fab multiblocks come with HV circuits, which
// the Photolithography Line has to print first.
// Within each fab recipe type no recipe holds all inputs of a circuit-less other one (the lookup could pick it).

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const LV = VA[GTValues.LV], MV = VA[GTValues.MV], HV = VA[GTValues.HV], EV = VA[GTValues.EV]
    const gt = event.recipes.gtceu

    // Column recipes run whole in the SMC Rectification Column. The SMC Fractionating Still (single block) takes one
    // cut per run instead: circuit = cut number, a quarter of the power, twice the time, the other cuts are lost (as
    // GT's distillery does with Distillation Tower recipes). Clean room from HV, as for all non-thermal fab recipes.
    const column = (type, id, input, cuts, duration, eut) => {
        const recipe = gt[type](id).inputFluids(input).outputFluids(cuts).duration(duration).EUt(eut)
        if (eut >= HV) recipe.cleanroom(CleanroomType.CLEANROOM)
        cuts.forEach((cut, i) => {
            const cutEUt = Math.max(LV, eut / 4)
            const still = gt.fab_fractionation(`${id}_cut_${i + 1}`)
                .circuit(i + 1)
                .inputFluids(input)
                .outputFluids(cut)
                .duration(duration * 2)
                .EUt(cutEUt)
            if (cutEUt >= HV) still.cleanroom(CleanroomType.CLEANROOM)
        })
    }

    // =============================================================================================================
    // 1. ELECTRONIC-GRADE SILICON (MV): quartz -> 99 % MG-Si -> trichlorosilane -> Siemens polysilicon (11N) -> CZ
    // =============================================================================================================

    // Acid leaching strips iron, aluminium and calcium from the quartz surface
    gt.fab_wet_processing('af9:high_purity_quartz')
        .itemInputs('gtceu:quartzite_dust')
        .inputFluids(Fluid.of('gtceu:hydrochloric_acid', 250))
        .itemOutputs('gtceu:high_purity_quartz_dust')
        .outputFluids(Fluid.of('gtceu:diluted_hydrochloric_acid', 250))
        .duration(200)
        .EUt(LV)

    // Submerged arc furnace: SiO2 + 2 C -> Si + 2 CO
    gt.fab_calcination('af9:metallurgical_grade_silicon')
        .itemInputs('gtceu:high_purity_quartz_dust', '2x gtceu:coke_dust')
        .itemOutputs('gtceu:metallurgical_grade_silicon_dust')
        .outputFluids(Fluid.of('gtceu:carbon_monoxide', 2000))
        .blastFurnaceTemp(1800)
        .duration(400)
        .EUt(MV)

    // Fluidized-bed hydrochlorination at 300 C, copper catalysed: Si + 3 HCl -> SiHCl3 + H2 (plus SiCl4, SiH2Cl2 and
    // the MG-Si's boron as BCl3)
    gt.fab_synthesis('af9:crude_chlorosilanes')
        .itemInputs('gtceu:metallurgical_grade_silicon_dust')
        .notConsumable('gtceu:copper_dust')
        .inputFluids(Fluid.of('gtceu:hydrochloric_acid', 3000))
        .outputFluids(Fluid.of('gtceu:crude_chlorosilanes', 1000), Fluid.of('gtceu:hydrogen', 1000))
        .duration(300)
        .EUt(MV)

    // Fractional distillation. At MV, the Fractionating Still runs each cut on its own (circuit 1-4).
    column('fab_distillation', 'af9:chlorosilane_distillation', Fluid.of('gtceu:crude_chlorosilanes', 1000),
        [Fluid.of('gtceu:trichlorosilane', 850), Fluid.of('gtceu:silicon_tetrachloride', 100),
         Fluid.of('gtceu:dichlorosilane', 40), Fluid.of('gtceu:boron_trichloride', 10)],
        300, MV)

    // Polishing to 9N is gone with the Siemens unit (below): TCS now burns to fumed silica instead.
    // Redistribution over the carbon bed: SiH2Cl2 + SiCl4 -> 2 SiHCl3
    gt.fab_synthesis('af9:dichlorosilane_redistribution')
        .notConsumable('gtceu:activated_carbon_dust')
        .inputFluids(Fluid.of('gtceu:dichlorosilane', 1000), Fluid.of('gtceu:silicon_tetrachloride', 1000))
        .outputFluids(Fluid.of('gtceu:trichlorosilane', 2000))
        .duration(200)
        .EUt(MV)

    // Hot-wire reduction of the boron cut: 2 BCl3 + 3 H2 -> 2 B + 6 HCl. The CZ puller's p-type dopant.
    gt.fab_synthesis('af9:boron_from_trichloride')
        .inputFluids(Fluid.of('gtceu:boron_trichloride', 1000), Fluid.of('gtceu:hydrogen', 3000))
        .itemOutputs('gtceu:boron_dust')
        .outputFluids(Fluid.of('gtceu:hydrochloric_acid', 3000))
        .duration(200)
        .EUt(MV)

    // Polysilicon comes only from Moon Sand now (below): the Siemens bell jar, its feed gas, the vent gas
    // recovery and the STC hydroconversion loop are gone. TCS burns to fumed silica instead (circuit 3).

    // Or burn it in a hydrogen flame into fumed silica: SiCl4 + 2 H2 + O2 -> SiO2 + 4 HCl
    gt.fab_synthesis('af9:fumed_silica')
        .circuit(2)
        .inputFluids(Fluid.of('gtceu:silicon_tetrachloride', 1000), Fluid.of('gtceu:hydrogen', 2000), Fluid.of('gtceu:oxygen', 2000))
        .itemOutputs('gtceu:silicon_dioxide_dust')
        .outputFluids(Fluid.of('gtceu:hydrochloric_acid', 4000))
        .duration(200)
        .EUt(MV)

    // TCS burns the same way (the Siemens unit is gone, so TCS has no CVD to go to)
    gt.fab_synthesis('af9:tcs_fumed_silica')
        .circuit(3)
        .inputFluids(Fluid.of('gtceu:trichlorosilane', 1000), Fluid.of('gtceu:hydrogen', 2000), Fluid.of('gtceu:oxygen', 2000))
        .itemOutputs('gtceu:silicon_dioxide_dust')
        .outputFluids(Fluid.of('gtceu:hydrochloric_acid', 4000))
        .duration(200)
        .EUt(MV)

    // Crushed rods pick up metal from the hammers; a nitric/hydrofluoric/acetic acid etch takes the surface off
    gt.fab_blending('af9:silicon_etchant')
        .inputFluids(Fluid.of('gtceu:nitric_acid', 1000), Fluid.of('gtceu:hydrofluoric_acid', 1000))
        .outputFluids(Fluid.of('gtceu:silicon_etchant', 2000))
        .duration(100)
        .EUt(LV)

    gt.fab_wet_processing('af9:electronic_grade_silicon')
        .itemInputs('gtceu:polysilicon_dust')
        .inputFluids(Fluid.of('gtceu:silicon_etchant', 100))
        .itemOutputs('gtceu:electronic_grade_silicon_dust')
        .duration(100)
        .EUt(MV)

    // Czochralski growth: the boules are pulled in the EBF's Boule Melting mode from melt charges (the electronic-grade
    // silicon above with its dopants, SMC blending), a seed crystal (SMC crystal growth) and a crucible. Artemite = p-type
    // (the CMOS substrate, ppm-level), phosphorus = n-type. All in boule_melting.js.

    // =============================================================================================================
    // 1b. MOON SAND POLYSILICON (MV): the only polysilicon there is. Sand is crushed to silica, L-01 (the light
    // cut of Lunar Air, the Moon's air) is bound into it as Moon Sand, and a chemical bath under oxygen turns
    // Moon Sand into polysilicon dust: a sand block is 8 polysilicon. Ad Astra moon sand grinds straight to
    // Moon Sand dust (the first wafers: a shovel on the Moon, then the L-01 chain takes over).
    // =============================================================================================================

    // Nomifactory-style: sand is just silica waiting to be crushed
    event.recipes.gtceu.macerator('af9:sand_silica')
        .itemInputs('minecraft:sand')
        .itemOutputs('2x gtceu:silicon_dioxide_dust')
        .duration(100)
        .EUt(LV)

    // The Moon's own sand grinds straight to Moon Sand dust
    event.recipes.gtceu.macerator('af9:moon_sand_grinding')
        .itemInputs('ad_astra:moon_sand')
        .itemOutputs('gtceu:moon_sand_dust')
        .duration(100)
        .EUt(LV)

    // L-01 activation: silica + the light lunar gas -> Moon Sand
    gt.fab_synthesis('af9:moon_sand')
        .itemInputs('2x gtceu:silicon_dioxide_dust')
        .inputFluids(Fluid.of('gtceu:l_01', 1000))
        .itemOutputs('2x gtceu:moon_sand_dust')
        .duration(200)
        .EUt(MV)

    // The bath: Moon Sand under oxygen -> polysilicon dust
    gt.fab_wet_processing('af9:moon_sand_polysilicon')
        .itemInputs('gtceu:moon_sand_dust')
        .inputFluids(Fluid.of('gtceu:oxygen', 1000))
        .itemOutputs('4x gtceu:polysilicon_dust')
        .duration(200)
        .EUt(MV)

    // Lunar Air distillation (distillation tower, like liquid Ender Air): argon and radon off the sides,
    // L-01 as the light product
    event.recipes.gtceu.distillation_tower('af9:distill_lunar_air')
        .inputFluids(Fluid.of('gtceu:lunar_air', 4000))
        .outputFluids(Fluid.of('gtceu:l_01', 2600), Fluid.of('gtceu:argon', 1000), Fluid.of('gtceu:radon', 160))
        .duration(600)
        .EUt(MV)

    // =============================================================================================================
    // 2. FLUOROCHEMICALS: fluorspar -> HF (MV) -> fluorine by KF.2HF electrolysis (HV); triflic acid by Simons ECF
    // =============================================================================================================

    // Rotary kiln, 250 C: CaF2 + H2SO4 -> CaSO4 + 2 HF
    gt.fab_synthesis('af9:crude_hydrogen_fluoride')
        .itemInputs('gtceu:fluorite_dust')
        .inputFluids(Fluid.of('gtceu:sulfuric_acid', 1000))
        .itemOutputs('gtceu:gypsum_dust')
        .outputFluids(Fluid.of('gtceu:crude_hydrogen_fluoride', 2000))
        .duration(300)
        .EUt(MV)

    // Scrubbing and distillation to anhydrous HF (bp 19.5 C)
    column('fab_distillation', 'af9:anhydrous_hydrogen_fluoride', Fluid.of('gtceu:crude_hydrogen_fluoride', 2000),
        [Fluid.of('gtceu:hydrofluoric_acid', 1800), Fluid.of('gtceu:sulfuric_acid', 150),
         Fluid.of('minecraft:water', 50)],
        200, MV)

    // KOH + HF -> KF + H2O
    gt.fab_synthesis('af9:potassium_fluoride')
        .itemInputs('gtceu:potassium_hydroxide_dust')
        .inputFluids(Fluid.of('gtceu:hydrofluoric_acid', 1000))
        .itemOutputs('gtceu:potassium_fluoride_dust')
        .outputFluids(Fluid.of('minecraft:water', 1000))
        .duration(100)
        .EUt(MV)

    // Molten KF.2HF, the electrolyte of every industrial fluorine cell (Moissan's discovery, 1886)
    gt.fab_blending('af9:potassium_bifluoride_electrolyte')
        .itemInputs('gtceu:potassium_fluoride_dust')
        .inputFluids(Fluid.of('gtceu:hydrofluoric_acid', 2000))
        .outputFluids(Fluid.of('gtceu:potassium_bifluoride_electrolyte', 1000))
        .duration(200)
        .EUt(MV)

    // Carbon anodes (graphite would flake apart), steel cathodes, 90 C: 2 HF -> H2 + F2; the KF stays behind
    gt.fab_electrolysis('af9:fluorine_electrolysis')
        .notConsumable('gtceu:carbon_dust')
        .inputFluids(Fluid.of('gtceu:potassium_bifluoride_electrolyte', 1000))
        .itemOutputs('gtceu:potassium_fluoride_dust')
        .outputFluids(Fluid.of('gtceu:crude_fluorine', 2000), Fluid.of('gtceu:hydrogen', 2000))
        .duration(400)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // NaF pellets trap the HF: NaF + HF -> NaHF2; heating gives the HF back
    gt.fab_synthesis('af9:sodium_fluoride')
        .itemInputs('gtceu:sodium_hydroxide_dust')
        .inputFluids(Fluid.of('gtceu:hydrofluoric_acid', 1000))
        .itemOutputs('gtceu:sodium_fluoride_dust')
        .outputFluids(Fluid.of('minecraft:water', 1000))
        .duration(100)
        .EUt(MV)

    gt.fab_purification('af9:fluorine_purification')
        .itemInputs('gtceu:sodium_fluoride_dust')
        .inputFluids(Fluid.of('gtceu:crude_fluorine', 2000))
        .itemOutputs('gtceu:sodium_bifluoride_dust')
        .outputFluids(Fluid.of('gtceu:fluorine', 1800))
        .duration(100)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    gt.fab_calcination('af9:sodium_bifluoride_regeneration')
        .itemInputs('gtceu:sodium_bifluoride_dust')
        .itemOutputs('gtceu:sodium_fluoride_dust')
        .outputFluids(Fluid.of('gtceu:hydrofluoric_acid', 200))
        .blastFurnaceTemp(700)
        .duration(100)
        .EUt(MV)

    // Triflic acid, the acid of the photoacid generator.
    // Grillo process: CH4 + SO3 -> CH3SO3H, started by a little peroxide
    gt.fab_synthesis('af9:methanesulfonic_acid')
        .inputFluids(Fluid.of('gtceu:methane', 1000), Fluid.of('gtceu:sulfur_trioxide', 1000), Fluid.of('gtceu:hydrogen_peroxide', 50))
        .outputFluids(Fluid.of('gtceu:methanesulfonic_acid', 1000), Fluid.of('minecraft:water', 50))
        .duration(300)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // S + Cl2 -> SCl2, then SCl2 + SO3 -> SOCl2 + SO2
    gt.fab_synthesis('af9:sulfur_dichloride')
        .itemInputs('gtceu:sulfur_dust')
        .inputFluids(Fluid.of('gtceu:chlorine', 2000))
        .outputFluids(Fluid.of('gtceu:sulfur_dichloride', 1000))
        .duration(100)
        .EUt(MV)

    gt.fab_synthesis('af9:thionyl_chloride')
        .inputFluids(Fluid.of('gtceu:sulfur_dichloride', 1000), Fluid.of('gtceu:sulfur_trioxide', 1000))
        .outputFluids(Fluid.of('gtceu:thionyl_chloride', 1000), Fluid.of('gtceu:sulfur_dioxide', 1000))
        .duration(200)
        .EUt(MV)

    // CH3SO3H + SOCl2 -> CH3SO2Cl + SO2 + HCl
    gt.fab_synthesis('af9:methanesulfonyl_chloride')
        .inputFluids(Fluid.of('gtceu:methanesulfonic_acid', 1000), Fluid.of('gtceu:thionyl_chloride', 1000))
        .outputFluids(Fluid.of('gtceu:methanesulfonyl_chloride', 1000), Fluid.of('gtceu:sulfur_dioxide', 1000),
            Fluid.of('gtceu:hydrochloric_acid', 1000))
        .duration(200)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // Halogen exchange: CH3SO2Cl + KF -> CH3SO2F + KCl
    gt.fab_synthesis('af9:methanesulfonyl_fluoride')
        .itemInputs('gtceu:potassium_fluoride_dust')
        .inputFluids(Fluid.of('gtceu:methanesulfonyl_chloride', 1000))
        .itemOutputs('gtceu:rock_salt_dust')
        .outputFluids(Fluid.of('gtceu:methanesulfonyl_fluoride', 1000))
        .duration(200)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // Simons electrochemical fluorination: the substrate dissolved in anhydrous HF, nickel anodes at 5-6 V.
    // CH3SO2F + 3 HF -> CF3SO2F + 3 H2
    gt.fab_blending('af9:simons_cell_electrolyte')
        .inputFluids(Fluid.of('gtceu:methanesulfonyl_fluoride', 1000), Fluid.of('gtceu:hydrofluoric_acid', 3000))
        .outputFluids(Fluid.of('gtceu:simons_cell_electrolyte', 4000))
        .duration(100)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    gt.fab_electrofluorination('af9:electrochemical_fluorination')
        .notConsumable('gtceu:nickel_plate')
        .inputFluids(Fluid.of('gtceu:simons_cell_electrolyte', 4000))
        .outputFluids(Fluid.of('gtceu:trifluoromethanesulfonyl_fluoride', 1000), Fluid.of('gtceu:hydrogen', 6000))
        .duration(600)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // CF3SO2F + 2 KOH -> CF3SO3K + KF + H2O
    gt.fab_synthesis('af9:potassium_triflate')
        .itemInputs('2x gtceu:potassium_hydroxide_dust')
        .inputFluids(Fluid.of('gtceu:trifluoromethanesulfonyl_fluoride', 1000))
        .itemOutputs('gtceu:potassium_triflate_dust', 'gtceu:potassium_fluoride_dust')
        .outputFluids(Fluid.of('minecraft:water', 1000))
        .duration(200)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // 2 CF3SO3K + H2SO4 -> 2 CF3SO3H + K2SO4, then vacuum distillation
    gt.fab_synthesis('af9:trifluoromethanesulfonic_acid')
        .itemInputs('2x gtceu:potassium_triflate_dust')
        .inputFluids(Fluid.of('gtceu:sulfuric_acid', 1000))
        .itemOutputs('gtceu:potassium_sulfate_dust')
        .outputFluids(Fluid.of('gtceu:trifluoromethanesulfonic_acid', 2000))
        .duration(300)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // =============================================================================================================
    // 3. AIR GASES: the cold box behind the XCDA plant. Argon (MV) for the CZ pullers, neon + krypton (HV) for the
    //    excimer lasers. Uses Cryogenic Supercooled Air (MV expansion cooler), not GT's liquid air (Vacuum Freezer).
    // =============================================================================================================

    // Double-column rectification with the side draws real plants take: crude argon from the middle of the low-pressure
    // column, crude neon from the condenser head, a krypton/xenon concentrate from the oxygen sump.
    column('fab_cryogenic_rectification', 'af9:air_rectification',
        Fluid.of('gtceu:cryogenic_supercooled_air', 4000),
        [Fluid.of('gtceu:nitrogen', 2960), Fluid.of('gtceu:oxygen', 600), Fluid.of('gtceu:crude_argon', 400),
         Fluid.of('gtceu:crude_neon', 24), Fluid.of('gtceu:krypton_xenon_concentrate', 16)],
        300, MV)

    // Deoxo: the oxygen in crude argon burns with hydrogen over palladium, the water is dried out
    gt.fab_purification('af9:argon_deoxo')
        .notConsumable('gtceu:palladium_dust')
        .inputFluids(Fluid.of('gtceu:crude_argon', 1000), Fluid.of('gtceu:hydrogen', 100))
        .outputFluids(Fluid.of('gtceu:argon', 950), Fluid.of('minecraft:water', 50))
        .duration(100)
        .EUt(MV)

    // Crude neon: hydrogen burnt off over platinum, nitrogen frozen onto activated carbon at 77 K
    gt.fab_purification('af9:crude_neon_purification')
        .notConsumable('gtceu:platinum_dust')
        .notConsumable('gtceu:activated_carbon_dust')
        .inputFluids(Fluid.of('gtceu:crude_neon', 1000), Fluid.of('gtceu:oxygen', 50))
        .outputFluids(Fluid.of('gtceu:neon_helium_mixture', 700), Fluid.of('gtceu:nitrogen', 250))
        .duration(200)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // Neon (bp 27 K) and helium (bp 4 K) split in a small cryogenic column
    column('fab_cryogenic_rectification', 'af9:neon_helium_separation', Fluid.of('gtceu:neon_helium_mixture', 1000),
        [Fluid.of('gtceu:neon', 720), Fluid.of('gtceu:helium', 280)],
        200, HV)

    // Methane and other hydrocarbons would explode in the concentrating oxygen: burnt out over platinum first
    gt.fab_purification('af9:krypton_xenon_catalytic_burner')
        .notConsumable('gtceu:platinum_dust')
        .inputFluids(Fluid.of('gtceu:krypton_xenon_concentrate', 1000))
        .outputFluids(Fluid.of('gtceu:crude_krypton_xenon', 980), Fluid.of('gtceu:carbon_dioxide', 20))
        .duration(100)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // Molecular sieve takes the CO2, N2O and water
    gt.fab_purification('af9:krypton_xenon_drying')
        .itemInputs('af9:molecular_sieve')
        .inputFluids(Fluid.of('gtceu:crude_krypton_xenon', 1000))
        .itemOutputs('af9:saturated_molecular_sieve')
        .outputFluids(Fluid.of('gtceu:purified_krypton_xenon', 1000))
        .duration(100)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    column('fab_cryogenic_rectification', 'af9:krypton_xenon_rectification',
        Fluid.of('gtceu:purified_krypton_xenon', 1000),
        [Fluid.of('gtceu:oxygen', 700), Fluid.of('gtceu:krypton', 270), Fluid.of('gtceu:xenon', 30)],
        300, HV)

    // Laser premixes (AF9: 5 % rare gas, 1 % fluorine in neon; real ones are leaner). The fluorine must be HF-free,
    // HF poisons the discharge.
    const excimerGases = [
        { id: 'krf_excimer_gas', rareGas: 'gtceu:krypton' },
        { id: 'arf_excimer_gas', rareGas: 'gtceu:argon' }
    ]
    excimerGases.forEach(gas => {
        gt.fab_blending(`af9:${gas.id}`)
            .inputFluids(Fluid.of('gtceu:neon', 940), Fluid.of(gas.rareGas, 50), Fluid.of('gtceu:fluorine', 10))
            .outputFluids(Fluid.of(`gtceu:${gas.id}`, 1000))
            .duration(200)
            .EUt(HV)
            .cleanroom(CleanroomType.CLEANROOM)
    })

    // =============================================================================================================
    // 4. KrF RESIST (HV): t-BOC polyhydroxystyrene + triphenylsulfonium triflate + tributylamine in PGMEA
    // =============================================================================================================

    // ---- Shared catalysts and reagents ----
    // Pd + Cl2 -> PdCl2; impregnated on activated carbon and reduced with hydrogen -> 5 % Pd/C
    gt.fab_synthesis('af9:palladium_chloride')
        .itemInputs('gtceu:palladium_dust')
        .inputFluids(Fluid.of('gtceu:chlorine', 2000))
        .itemOutputs('gtceu:palladium_chloride_dust')
        .duration(200)
        .EUt(MV)

    gt.fab_synthesis('af9:palladium_on_carbon')
        .itemInputs('gtceu:palladium_chloride_dust', '8x gtceu:activated_carbon_dust')
        .inputFluids(Fluid.of('gtceu:hydrogen', 2000))
        .itemOutputs('8x gtceu:palladium_on_carbon_dust')
        .outputFluids(Fluid.of('gtceu:hydrochloric_acid', 2000))
        .duration(200)
        .EUt(MV)

    // Acidic ion-exchange resin (Amberlyst type): styrene polymerized into beads, then sulfonated
    gt.fab_synthesis('af9:acidic_ion_exchange_resin')
        .itemInputs('gtceu:tiny_azobisisobutyronitrile_dust')
        .inputFluids(Fluid.of('gtceu:styrene', 1000), Fluid.of('gtceu:sulfuric_acid', 1000))
        .itemOutputs('gtceu:acidic_ion_exchange_resin_dust')
        .outputFluids(Fluid.of('minecraft:water', 1000))
        .duration(300)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // TS-1 titanium silicalite. Its silica comes from TEOS, made from the Siemens plant's silicon tetrachloride:
    // SiCl4 + 4 C2H5OH -> Si(OC2H5)4 + 4 HCl, then hydrolysed with a little titania: Si(OC2H5)4 + 2 H2O -> SiO2 + 4 EtOH
    gt.fab_synthesis('af9:tetraethyl_orthosilicate')
        .inputFluids(Fluid.of('gtceu:silicon_tetrachloride', 1000), Fluid.of('gtceu:ethanol', 4000))
        .outputFluids(Fluid.of('gtceu:tetraethyl_orthosilicate', 1000), Fluid.of('gtceu:hydrochloric_acid', 4000))
        .duration(200)
        .EUt(MV)

    gt.fab_synthesis('af9:titanium_silicalite')
        .itemInputs('gtceu:tiny_rutile_dust')
        .inputFluids(Fluid.of('gtceu:tetraethyl_orthosilicate', 1000), Fluid.of('minecraft:water', 2000))
        .itemOutputs('gtceu:titanium_silicalite_dust')
        .outputFluids(Fluid.of('gtceu:ethanol', 4000))
        .duration(600)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // ---- AIBN radical initiator ----
    // Olin-Raschig: NH2Cl + NH3 + NaOH -> N2H4 + NaCl + H2O
    gt.fab_synthesis('af9:hydrazine')
        .itemInputs('gtceu:sodium_hydroxide_dust')
        .inputFluids(Fluid.of('gtceu:monochloramine', 1000), Fluid.of('gtceu:ammonia', 1000))
        .itemOutputs('gtceu:salt_dust')
        .outputFluids(Fluid.of('gtceu:hydrazine', 1000), Fluid.of('minecraft:water', 1000))
        .duration(200)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // (CH3)2CO + HCN -> (CH3)2C(OH)CN, base catalysed. Also the start of the ArF monomers.
    gt.fab_synthesis('af9:acetone_cyanohydrin')
        .notConsumable('gtceu:sodium_hydroxide_dust')
        .inputFluids(Fluid.of('gtceu:acetone', 1000), Fluid.of('gtceu:hydrogen_cyanide', 1000))
        .outputFluids(Fluid.of('gtceu:acetone_cyanohydrin', 1000))
        .duration(200)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // 2 ACH + N2H4 -> hydrazobisisobutyronitrile + 2 H2O; chlorine oxidizes it to the azo compound
    gt.fab_synthesis('af9:hydrazobisisobutyronitrile')
        .inputFluids(Fluid.of('gtceu:acetone_cyanohydrin', 2000), Fluid.of('gtceu:hydrazine', 1000))
        .itemOutputs('gtceu:hydrazobisisobutyronitrile_dust')
        .outputFluids(Fluid.of('minecraft:water', 2000))
        .duration(200)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    gt.fab_synthesis('af9:azobisisobutyronitrile')
        .itemInputs('gtceu:hydrazobisisobutyronitrile_dust')
        .inputFluids(Fluid.of('gtceu:chlorine', 2000))
        .itemOutputs('gtceu:azobisisobutyronitrile_dust')
        .outputFluids(Fluid.of('gtceu:hydrochloric_acid', 2000))
        .duration(200)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // ---- Polyhydroxystyrene by the Hoechst Celanese 4-acetoxystyrene route ----
    // HF-catalysed acylation (Fries): C6H5OH + (CH3CO)2O -> 4-hydroxyacetophenone + CH3COOH
    gt.fab_synthesis('af9:hydroxyacetophenone')
        .inputFluids(Fluid.of('gtceu:phenol', 1000), Fluid.of('gtceu:acetic_anhydride', 1000))
        .notConsumableFluid(Fluid.of('gtceu:hydrofluoric_acid', 1000))
        .itemOutputs('gtceu:hydroxyacetophenone_dust')
        .outputFluids(Fluid.of('gtceu:acetic_acid', 1000))
        .duration(300)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // Acetylation of the phenol group (it has to survive the next steps)
    gt.fab_synthesis('af9:acetoxyacetophenone')
        .itemInputs('gtceu:hydroxyacetophenone_dust')
        .inputFluids(Fluid.of('gtceu:acetic_anhydride', 1000))
        .itemOutputs('gtceu:acetoxyacetophenone_dust')
        .outputFluids(Fluid.of('gtceu:acetic_acid', 1000))
        .duration(200)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // Ketone hydrogenation over Pd/C
    gt.fab_synthesis('af9:acetoxyphenyl_methyl_carbinol')
        .itemInputs('gtceu:acetoxyacetophenone_dust')
        .notConsumable('gtceu:palladium_on_carbon_dust')
        .inputFluids(Fluid.of('gtceu:hydrogen', 2000))
        .outputFluids(Fluid.of('gtceu:acetoxyphenyl_methyl_carbinol', 1000))
        .duration(300)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // Vapour-phase dehydration over an acidic bisulfate bed
    gt.fab_synthesis('af9:acetoxystyrene')
        .notConsumable('gtceu:sodium_bisulfate_dust')
        .inputFluids(Fluid.of('gtceu:acetoxyphenyl_methyl_carbinol', 1000))
        .outputFluids(Fluid.of('gtceu:acetoxystyrene', 1000), Fluid.of('minecraft:water', 1000))
        .duration(200)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // Free-radical polymerization, AIBN started
    gt.fab_synthesis('af9:poly_acetoxystyrene')
        .itemInputs('gtceu:tiny_azobisisobutyronitrile_dust')
        .inputFluids(Fluid.of('gtceu:acetoxystyrene', 1000))
        .itemOutputs('gtceu:poly_acetoxystyrene_dust')
        .duration(400)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // Base-catalysed methanolysis frees the phenols: the acetate leaves as methyl acetate
    gt.fab_synthesis('af9:polyhydroxystyrene')
        .itemInputs('gtceu:poly_acetoxystyrene_dust')
        .inputFluids(Fluid.of('gtceu:methanol', 1000))
        .notConsumableFluid(Fluid.of('gtceu:ammonia', 1000))
        .itemOutputs('gtceu:polyhydroxystyrene_dust')
        .outputFluids(Fluid.of('gtceu:methyl_acetate', 1000))
        .duration(300)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // ---- t-BOC protection (di-tert-butyl dicarbonate) ----
    // Skeletal isomerization of n-butene over a zeolite (ferrierite)
    gt.fab_synthesis('af9:isobutylene')
        .notConsumable('gtceu:zeolite_dust')
        .inputFluids(Fluid.of('gtceu:butene', 1000))
        .outputFluids(Fluid.of('gtceu:isobutylene', 1000))
        .duration(200)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // Hydration over the acidic resin: (CH3)2C=CH2 + H2O -> (CH3)3COH
    gt.fab_synthesis('af9:tert_butanol')
        .notConsumable('gtceu:acidic_ion_exchange_resin_dust')
        .inputFluids(Fluid.of('gtceu:isobutylene', 1000), Fluid.of('minecraft:water', 1000))
        .outputFluids(Fluid.of('gtceu:tert_butanol', 1000))
        .duration(200)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // 2 (CH3)3COH + 2 Na -> 2 (CH3)3CONa + H2
    gt.fab_synthesis('af9:sodium_tert_butoxide')
        .itemInputs('gtceu:sodium_dust')
        .inputFluids(Fluid.of('gtceu:tert_butanol', 1000))
        .itemOutputs('gtceu:sodium_tert_butoxide_dust')
        .outputFluids(Fluid.of('gtceu:hydrogen', 1000))
        .duration(100)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // CO + Cl2 -> COCl2 over activated carbon
    gt.fab_synthesis('af9:phosgene')
        .notConsumable('gtceu:activated_carbon_dust')
        .inputFluids(Fluid.of('gtceu:carbon_monoxide', 1000), Fluid.of('gtceu:chlorine', 2000))
        .outputFluids(Fluid.of('gtceu:phosgene', 1000))
        .duration(100)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // 2 NaOtBu + CO2 + COCl2 -> Boc2O + 2 NaCl (via the tert-butyl carbonate)
    gt.fab_synthesis('af9:di_tert_butyl_dicarbonate')
        .itemInputs('2x gtceu:sodium_tert_butoxide_dust')
        .inputFluids(Fluid.of('gtceu:carbon_dioxide', 1000), Fluid.of('gtceu:phosgene', 1000))
        .itemOutputs('2x gtceu:salt_dust')
        .outputFluids(Fluid.of('gtceu:di_tert_butyl_dicarbonate', 1000))
        .duration(300)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // About a third of the phenols get a t-BOC group; the acid from the PAG cuts it off again where light hit
    gt.fab_synthesis('af9:tboc_polyhydroxystyrene')
        .itemInputs('gtceu:polyhydroxystyrene_dust')
        .inputFluids(Fluid.of('gtceu:di_tert_butyl_dicarbonate', 300))
        .itemOutputs('gtceu:tboc_polyhydroxystyrene_dust')
        .outputFluids(Fluid.of('gtceu:carbon_dioxide', 300), Fluid.of('gtceu:tert_butanol', 300))
        .duration(300)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // ---- Photoacid generator: triphenylsulfonium triflate ----
    // 2 Al + 3 Cl2 -> 2 AlCl3
    gt.fab_synthesis('af9:aluminium_chloride')
        .itemInputs('gtceu:aluminium_dust')
        .inputFluids(Fluid.of('gtceu:chlorine', 3000))
        .itemOutputs('gtceu:aluminium_chloride_dust')
        .duration(200)
        .EUt(MV)

    // Friedel-Crafts: 2 C6H6 + SOCl2 -> (C6H5)2SO + 2 HCl
    gt.fab_synthesis('af9:diphenyl_sulfoxide')
        .notConsumable('gtceu:aluminium_chloride_dust')
        .inputFluids(Fluid.of('gtceu:benzene', 2000), Fluid.of('gtceu:thionyl_chloride', 1000))
        .itemOutputs('gtceu:diphenyl_sulfoxide_dust')
        .outputFluids(Fluid.of('gtceu:hydrochloric_acid', 2000))
        .duration(300)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // THF for the Grignard: n-butane -> maleic anhydride over VPO, hydrogenated to THF (Davy process)
    gt.fab_synthesis('af9:vanadyl_pyrophosphate')
        .itemInputs('2x gtceu:vanadium_dust')
        .inputFluids(Fluid.of('gtceu:phosphoric_acid', 2000), Fluid.of('gtceu:oxygen', 5000))
        .itemOutputs('gtceu:vanadyl_pyrophosphate_dust')
        .outputFluids(Fluid.of('minecraft:water', 3000))
        .duration(400)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // C4H10 + 3.5 O2 -> C4H2O3 + 4 H2O
    gt.fab_synthesis('af9:maleic_anhydride')
        .notConsumable('gtceu:vanadyl_pyrophosphate_dust')
        .inputFluids(Fluid.of('gtceu:butane', 1000), Fluid.of('gtceu:oxygen', 7000))
        .itemOutputs('gtceu:maleic_anhydride_dust')
        .outputFluids(Fluid.of('minecraft:water', 4000))
        .duration(300)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // C4H2O3 + 5 H2 -> C4H8O + 2 H2O
    gt.fab_synthesis('af9:tetrahydrofuran')
        .itemInputs('gtceu:maleic_anhydride_dust')
        .notConsumable('gtceu:palladium_on_carbon_dust')
        .inputFluids(Fluid.of('gtceu:hydrogen', 10000))
        .outputFluids(Fluid.of('gtceu:tetrahydrofuran', 1000), Fluid.of('minecraft:water', 2000))
        .duration(300)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // Grignard reagent: C6H5Cl + Mg -> C6H5MgCl, in THF
    gt.fab_synthesis('af9:phenylmagnesium_chloride')
        .itemInputs('gtceu:magnesium_dust')
        .inputFluids(Fluid.of('gtceu:chlorobenzene', 1000), Fluid.of('gtceu:tetrahydrofuran', 1000))
        .outputFluids(Fluid.of('gtceu:phenylmagnesium_chloride', 1000))
        .duration(200)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // (C6H5)2SO + C6H5MgCl + 2 HCl -> (C6H5)3S+ Cl- + MgCl2 + H2O; the THF is distilled back
    gt.fab_synthesis('af9:triphenylsulfonium_chloride')
        .itemInputs('gtceu:diphenyl_sulfoxide_dust')
        .inputFluids(Fluid.of('gtceu:phenylmagnesium_chloride', 1000), Fluid.of('gtceu:hydrochloric_acid', 2000))
        .itemOutputs('gtceu:triphenylsulfonium_chloride_dust', 'gtceu:magnesium_chloride_dust')
        .outputFluids(Fluid.of('gtceu:tetrahydrofuran', 1000), Fluid.of('minecraft:water', 1000))
        .duration(300)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // Anion metathesis: Ph3S+ Cl- + CF3SO3H -> Ph3S+ CF3SO3- + HCl
    gt.fab_synthesis('af9:triphenylsulfonium_triflate')
        .itemInputs('gtceu:triphenylsulfonium_chloride_dust')
        .inputFluids(Fluid.of('gtceu:trifluoromethanesulfonic_acid', 1000))
        .itemOutputs('gtceu:triphenylsulfonium_triflate_dust')
        .outputFluids(Fluid.of('gtceu:hydrochloric_acid', 1000))
        .duration(200)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // ---- Base quencher: tributylamine ----
    // Butyraldehyde (GT's oxo process) hydrogenated over nickel
    gt.fab_synthesis('af9:butanol')
        .notConsumable('gtceu:nickel_dust')
        .inputFluids(Fluid.of('gtceu:butyraldehyde', 1000), Fluid.of('gtceu:hydrogen', 2000))
        .outputFluids(Fluid.of('gtceu:butanol', 1000))
        .duration(200)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // 3 C4H9OH + NH3 -> (C4H9)3N + 3 H2O, amination over nickel
    gt.fab_synthesis('af9:tributylamine')
        .notConsumable('gtceu:nickel_dust')
        .inputFluids(Fluid.of('gtceu:butanol', 3000), Fluid.of('gtceu:ammonia', 1000))
        .outputFluids(Fluid.of('gtceu:tributylamine', 1000), Fluid.of('minecraft:water', 3000))
        .duration(300)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // ---- Solvent: PGMEA ----
    // HPPO process: C3H6 + H2O2 -> propylene oxide + H2O over TS-1
    gt.fab_synthesis('af9:propylene_oxide')
        .notConsumable('gtceu:titanium_silicalite_dust')
        .inputFluids(Fluid.of('gtceu:propene', 1000), Fluid.of('gtceu:hydrogen_peroxide', 1000))
        .outputFluids(Fluid.of('gtceu:propylene_oxide', 1000), Fluid.of('minecraft:water', 1000))
        .duration(200)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // Base-catalysed ring opening with methanol -> 1-methoxy-2-propanol
    gt.fab_synthesis('af9:propylene_glycol_methyl_ether')
        .notConsumable('gtceu:sodium_hydroxide_dust')
        .inputFluids(Fluid.of('gtceu:propylene_oxide', 1000), Fluid.of('gtceu:methanol', 1000))
        .outputFluids(Fluid.of('gtceu:propylene_glycol_methyl_ether', 1000))
        .duration(200)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // Esterification over the acidic resin (a sulfuric acid catalyst would clash with GT's ethenone recipe)
    gt.fab_synthesis('af9:propylene_glycol_methyl_ether_acetate')
        .notConsumable('gtceu:acidic_ion_exchange_resin_dust')
        .inputFluids(Fluid.of('gtceu:propylene_glycol_methyl_ether', 1000), Fluid.of('gtceu:acetic_acid', 1000))
        .outputFluids(Fluid.of('gtceu:propylene_glycol_methyl_ether_acetate', 1000), Fluid.of('minecraft:water', 1000))
        .duration(200)
        .EUt(HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // =============================================================================================================
    // 5. ArF RESIST (EV): MMA / tert-butyl methacrylate / methacrylic acid terpolymer, acetone cyanohydrin route
    // =============================================================================================================

    // ACH + H2SO4 -> methacrylamide sulfate (140 C)
    gt.fab_synthesis('af9:methacrylamide_sulfate')
        .inputFluids(Fluid.of('gtceu:acetone_cyanohydrin', 1000), Fluid.of('gtceu:sulfuric_acid', 1000))
        .outputFluids(Fluid.of('gtceu:methacrylamide_sulfate', 1000))
        .duration(200)
        .EUt(EV)
        .cleanroom(CleanroomType.CLEANROOM)

    // Esterification with methanol -> methyl methacrylate + NH4HSO4
    gt.fab_synthesis('af9:methyl_methacrylate')
        .inputFluids(Fluid.of('gtceu:methacrylamide_sulfate', 1000), Fluid.of('gtceu:methanol', 1000))
        .itemOutputs('gtceu:ammonium_bisulfate_dust')
        .outputFluids(Fluid.of('gtceu:methyl_methacrylate', 1000))
        .duration(200)
        .EUt(EV)
        .cleanroom(CleanroomType.CLEANROOM)

    // Hydrolysis with water -> methacrylic acid + NH4HSO4
    gt.fab_synthesis('af9:methacrylic_acid')
        .inputFluids(Fluid.of('gtceu:methacrylamide_sulfate', 1000), Fluid.of('minecraft:water', 1000))
        .itemOutputs('gtceu:ammonium_bisulfate_dust')
        .outputFluids(Fluid.of('gtceu:methacrylic_acid', 1000))
        .duration(200)
        .EUt(EV)
        .cleanroom(CleanroomType.CLEANROOM)

    // Addition of isobutylene over the acidic resin -> tert-butyl methacrylate, the acid-labile monomer
    gt.fab_synthesis('af9:tert_butyl_methacrylate')
        .notConsumable('gtceu:acidic_ion_exchange_resin_dust')
        .inputFluids(Fluid.of('gtceu:methacrylic_acid', 1000), Fluid.of('gtceu:isobutylene', 1000))
        .outputFluids(Fluid.of('gtceu:tert_butyl_methacrylate', 1000))
        .duration(200)
        .EUt(EV)
        .cleanroom(CleanroomType.CLEANROOM)

    // Spent acid regeneration: the bisulfate is cracked back to SO2 (then SO3 and sulfuric acid in GT's plant)
    gt.fab_calcination('af9:spent_acid_regeneration')
        .itemInputs('2x gtceu:ammonium_bisulfate_dust')
        .inputFluids(Fluid.of('gtceu:oxygen', 1000))
        .outputFluids(Fluid.of('gtceu:sulfur_dioxide', 2000))
        .blastFurnaceTemp(1300)
        .duration(200)
        .EUt(HV)

    // Radical terpolymerization in PGMEA
    gt.fab_synthesis('af9:methacrylate_resin')
        .itemInputs('gtceu:tiny_azobisisobutyronitrile_dust')
        .inputFluids(Fluid.of('gtceu:methyl_methacrylate', 1000), Fluid.of('gtceu:tert_butyl_methacrylate', 1000),
            Fluid.of('gtceu:methacrylic_acid', 500))
        .itemOutputs('2x gtceu:methacrylate_resin_dust')
        .duration(400)
        .EUt(EV)
        .cleanroom(CleanroomType.CLEANROOM)

    // =============================================================================================================
    // Resist formulation and point-of-use filtration (20 nm PTFE membrane)
    // =============================================================================================================
    const resists = [
        { id: 'krf_photoresist', polymer: 'gtceu:tboc_polyhydroxystyrene_dust', eut: HV },
        { id: 'arf_photoresist', polymer: 'gtceu:methacrylate_resin_dust', eut: EV }
    ]
    resists.forEach(resist => {
        gt.fab_blending(`af9:unfiltered_${resist.id}`)
            .itemInputs(resist.polymer, 'gtceu:small_triphenylsulfonium_triflate_dust')
            .inputFluids(Fluid.of('gtceu:propylene_glycol_methyl_ether_acetate', 3000), Fluid.of('gtceu:tributylamine', 10))
            .outputFluids(Fluid.of(`gtceu:unfiltered_${resist.id}`, 4000))
            .duration(400)
            .EUt(resist.eut)
            .cleanroom(CleanroomType.CLEANROOM)
        gt.fab_purification(`af9:${resist.id}`)
            .notConsumable('gtceu:fluid_filter')
            .inputFluids(Fluid.of(`gtceu:unfiltered_${resist.id}`, 4000))
            .outputFluids(Fluid.of(`gtceu:${resist.id}`, 4000))
            .duration(200)
            .EUt(resist.eut)
            .cleanroom(CleanroomType.CLEANROOM)
    })

    // =============================================================================================================
    // Immersion water (LUV): mixed-bed polishing, UV oxidation and membrane degassing of distilled water
    // =============================================================================================================
    gt.fab_purification('af9:ultrapure_water')
        .notConsumable('gtceu:fluid_filter')
        .inputFluids(Fluid.of('gtceu:distilled_water', 4000))
        .outputFluids(Fluid.of('gtceu:ultrapure_water', 4000))
        .duration(200)
        .EUt(EV)
        .cleanroom(CleanroomType.CLEANROOM)
})
