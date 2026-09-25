// AF9 - Fab chemistry: the long, real-world supply chains behind the Photolithography Line
// Recipes: server_scripts/mods/gtceu/fab_chemistry.js. Spec: docs/semiconductor-factory.md §6.11-6.15
//
//   1. Electronic-grade silicon (MV)  quartz -> MG-Si -> trichlorosilane -> Siemens polysilicon -> CZ boules
//   2. Fluorochemicals (MV/HV)        fluorspar -> HF -> KF.2HF electrolysis -> F2; Simons ECF -> triflic acid
//   3. Air gases (MV/HV)              cold box -> argon, neon, krypton, xenon -> KrF / ArF laser premix
//   4. KrF resist (HV)                t-BOC polyhydroxystyrene + photoacid generator + amine quencher in PGMEA
//   5. ArF resist (EV)                methacrylate terpolymer from the acetone cyanohydrin route
//
// Every material is a real compound or a real process stream. Only formulas are given (no components), so GT adds
// no electrolyzer or centrifuge shortcut that would skip a chain.

GTCEuStartupEvents.registry('gtceu:material', allthemods => {
    // [id, form, colour, formula]; form: dust, liquid, gas or ingot
    const materials = [
        // ---- 1. Electronic-grade silicon ----
        ['high_purity_quartz', 'dust', 0xf4f2ee, 'SiO2'],                       // acid-leached quartzite
        ['metallurgical_grade_silicon', 'dust', 0x7d8087, 'Si'],               // 98-99 %, from the arc furnace
        ['crude_chlorosilanes', 'liquid', 0xc9ccb8, '(SiHCl3)(SiCl4)(SiH2Cl2)'], // hydrochlorination product
        ['trichlorosilane', 'liquid', 0xdadfe0, 'SiHCl3'],                      // TCS, bp 32 C
        ['silicon_tetrachloride', 'liquid', 0xd2d8d6, 'SiCl4'],                  // STC, bp 58 C
        ['dichlorosilane', 'gas', 0xe2e7e4, 'SiH2Cl2'],                          // DCS, bp 8 C
        ['boron_trichloride', 'gas', 0xd9e3d0, 'BCl3'],                          // the boron impurity, bp 13 C
        ['electronic_grade_trichlorosilane', 'liquid', 0xe8eef0, 'SiHCl3'],      // 9N, boron/phosphorus adsorbed
        ['siemens_feed_gas', 'gas', 0xd8e2e8, '(SiHCl3)(H2)4'],                 // TCS vapour in hydrogen
        ['siemens_vent_gas', 'gas', 0xc4ccc4, '(H2)(HCl)(SiHCl3)(SiCl4)'],       // off-gas of the bell-jar reactor
        ['polysilicon', 'ingot', 0x8a93a3, 'Si'],                                // Siemens rods, 11N
        ['silicon_etchant', 'liquid', 0xe8e0a8, '(HNO3)(HF)(CH3COOH)'],          // mixed-acid chunk etch
        ['electronic_grade_silicon', 'dust', 0x9aa6b8, 'Si'],                    // etched poly chunks, CZ charge

        // ---- 2. Fluorochemicals ----
        ['crude_hydrogen_fluoride', 'liquid', 0xd6dccf, '(HF)(H2SO4)(H2O)'],   // kiln gas, condensed
        ['potassium_fluoride', 'dust', 0xefefe8, 'KF'],
        ['potassium_bifluoride_electrolyte', 'liquid', 0xe0e6d8, 'KF(HF)2'],   // molten KF.2HF, 90 C
        ['crude_fluorine', 'gas', 0xd9e38a, '(F2)(HF)'],                        // cell gas, ~10 % HF
        ['sodium_fluoride', 'dust', 0xf2f2ec, 'NaF'],
        ['sodium_bifluoride', 'dust', 0xe6ece0, 'NaHF2'],                        // spent HF trap
        ['methanesulfonic_acid', 'liquid', 0xe0e2d6, 'CH3SO3H'],
        ['sulfur_dichloride', 'liquid', 0xb33a2a, 'SCl2'],                       // cherry-red liquid
        ['thionyl_chloride', 'liquid', 0xe8d890, 'SOCl2'],
        ['methanesulfonyl_chloride', 'liquid', 0xe4dcc0, 'CH3SO2Cl'],
        ['methanesulfonyl_fluoride', 'liquid', 0xe2e4d0, 'CH3SO2F'],
        ['simons_cell_electrolyte', 'liquid', 0xd8e0cc, '(CH3SO2F)(HF)3'],       // substrate in anhydrous HF
        ['trifluoromethanesulfonyl_fluoride', 'gas', 0xdde6de, 'CF3SO2F'],
        ['potassium_triflate', 'dust', 0xf0f0ea, 'CF3SO3K'],

        // ---- 3. Air gases ----
        ['crude_argon', 'gas', 0xb8d8f0, '(Ar)(O2)(N2)'],                        // side draw, ~95 % Ar
        ['crude_neon', 'gas', 0xf0b8a8, '(Ne)(He)(N2)(H2)'],                     // non-condensables of the condenser
        ['neon_helium_mixture', 'gas', 0xf4c8b8, '(Ne)(He)'],
        ['krypton_xenon_concentrate', 'gas', 0xc8d0f0, '(O2)(Kr)(Xe)(CH4)'],     // from the oxygen sump
        ['crude_krypton_xenon', 'gas', 0xbcc4ec, '(O2)(Kr)(Xe)(CO2)(N2O)'],      // hydrocarbons burnt out
        ['purified_krypton_xenon', 'gas', 0xb0b8e8, '(O2)(Kr)(Xe)'],

        // ---- 4. KrF resist ----
        ['hydroxyacetophenone', 'dust', 0xece4cc, 'HOC6H4COCH3'],                // 4-HAP
        ['acetoxyacetophenone', 'dust', 0xe8e0c8, 'CH3COOC6H4COCH3'],           // 4-AAP
        ['palladium_chloride', 'dust', 0x8a5a3a, 'PdCl2'],
        ['palladium_on_carbon', 'dust', 0x2a2a2e, '(Pd)(C)'],                    // 5 % Pd/C hydrogenation catalyst
        ['acetoxyphenyl_methyl_carbinol', 'liquid', 0xe6e0cc, 'CH3COOC6H4CH(OH)CH3'],
        ['acetoxystyrene', 'liquid', 0xe8e4d0, 'CH3COOC6H4CH=CH2'],             // 4-ASM monomer
        ['hydrazine', 'liquid', 0xe4ecf2, 'N2H4'],                               // Olin-Raschig process
        ['acetone_cyanohydrin', 'liquid', 0xe0d8b8, '(CH3)2C(OH)CN'],
        ['hydrazobisisobutyronitrile', 'dust', 0xf0ecdf, 'C8H14N4'],
        ['azobisisobutyronitrile', 'dust', 0xf6f4ee, 'C8H12N4'],                 // AIBN radical initiator
        ['poly_acetoxystyrene', 'dust', 0xeee8d6, '(C10H10O2)n'],
        ['isobutylene', 'gas', 0xe8ecd8, '(CH3)2C=CH2'],
        ['acidic_ion_exchange_resin', 'dust', 0x9a5a2a, '(C8H7SO3H)n'],          // sulfonated polystyrene beads
        ['tert_butanol', 'liquid', 0xecf0ea, '(CH3)3COH'],
        ['sodium_tert_butoxide', 'dust', 0xf2eee4, '(CH3)3CONa'],
        ['phosgene', 'gas', 0xe8ecc8, 'COCl2'],
        ['di_tert_butyl_dicarbonate', 'liquid', 0xf0ece0, '((CH3)3COCO)2O'],    // Boc anhydride
        ['tboc_polyhydroxystyrene', 'dust', 0xe6dcc0, '(C8H8O)n(C5H8O2)m'],     // the KrF resin, ~30 % protected
        ['aluminium_chloride', 'dust', 0xf0ecd8, 'AlCl3'],
        ['diphenyl_sulfoxide', 'dust', 0xf2f0e8, '(C6H5)2SO'],
        ['vanadyl_pyrophosphate', 'dust', 0x3c5a6a, '(VO)2P2O7'],                 // VPO butane oxidation catalyst
        ['maleic_anhydride', 'dust', 0xf4f2ea, 'C4H2O3'],
        ['tetrahydrofuran', 'liquid', 0xe6eef0, 'C4H8O'],
        ['phenylmagnesium_chloride', 'liquid', 0x8c7a5a, 'C6H5MgCl(C4H8O)'],      // Grignard reagent in THF
        ['triphenylsulfonium_chloride', 'dust', 0xf0eee6, '(C6H5)3SCl'],
        ['butanol', 'liquid', 0xe8eee4, 'C4H9OH'],
        ['tributylamine', 'liquid', 0xe4e8d4, '(C4H9)3N'],                       // base quencher
        ['tetraethyl_orthosilicate', 'liquid', 0xe8ecf0, 'Si(OC2H5)4'],         // TEOS
        ['titanium_silicalite', 'dust', 0xeeeef4, '(TiO2)(SiO2)50'],             // TS-1 epoxidation catalyst
        ['propylene_oxide', 'liquid', 0xe6ecee, 'C3H6O'],
        ['unfiltered_krf_photoresist', 'liquid', 0xc8a458],

        // ---- 5. ArF resist ----
        ['methacrylamide_sulfate', 'liquid', 0xcfc6a0, 'CH2=C(CH3)CONH2(H2SO4)'],
        ['methacrylic_acid', 'liquid', 0xe6eae0, 'CH2=C(CH3)COOH'],
        ['tert_butyl_methacrylate', 'liquid', 0xe8ece4, 'CH2=C(CH3)COOC(CH3)3'], // the acid-labile monomer
        ['ammonium_bisulfate', 'dust', 0xf2f2ea, 'NH4HSO4'],
        ['unfiltered_arf_photoresist', 'liquid', 0xd8cc98],

        // ---- MUV chemistry support ----
        ['molybdenum_trioxide', 'dust', 0xe0e4a0, 'MoO3'],
        ['iron_molybdate', 'dust', 0xa89a4a, 'Fe2(MoO4)3'],                      // Formox formaldehyde catalyst
        ['tetramethylammonium_chloride_solution', 'liquid', 0xd6ecf2, '(CH3)4NCl(H2O)']
    ]
    materials.forEach(entry => {
        const [id, form, color, formula] = entry
        const material = allthemods.create(id)
        if (form === 'dust') material.dust()
        else if (form === 'ingot') material.ingot().iconSet(GTMaterialIconSet.METALLIC)
        else if (form === 'gas') material.gas()
        else material.liquid()
        material.color(color)
        if (formula) material.formula(formula)
    })

    // ---- Materials the Photolithography Line consumes directly ----
    // Excimer laser premixes: about 1 % rare gas and 0.1 % fluorine in a neon buffer. The discharge slowly uses up
    // the fluorine, so the gas is topped up while the laser runs.
    allthemods.create('krf_excimer_gas').gas().color(0xd6c8f2).formula('(Ne)(Kr)(F2)')
    allthemods.create('arf_excimer_gas').gas().color(0xc6d8f2).formula('(Ne)(Ar)(F2)')

    // Chemically amplified resists. DNQ-novolac stops working below ~300 nm (novolac turns opaque, DNQ barely
    // bleaches); here light frees an acid from the photoacid generator (PAG) and in the post-exposure bake each acid
    // unblocks hundreds of polymer groups. The amine quencher stops the acid from wandering into dark areas.
    allthemods.create('trifluoromethanesulfonic_acid').liquid().color(0xe9edf0).formula('CF3SO3H')
    allthemods.create('triphenylsulfonium_triflate').dust().color(0xf2f0ea).formula('(C6H5)3S(CF3SO3)')
    // KrF polymer backbone: transparent at 248 nm; its phenol groups carry the t-BOC protection
    allthemods.create('polyhydroxystyrene').dust().color(0xefe6d2).formula('(C8H8O)n')
    // ArF: aromatic rings absorb 193 nm, so ArF resists are built on methacrylates instead
    allthemods.create('methyl_methacrylate').liquid().color(0xe4eef2).formula('C5H8O2')
    // ArF resin: MMA / tert-butyl methacrylate / methacrylic acid terpolymer (IBM's first 193 nm resist platform)
    allthemods.create('methacrylate_resin').dust().color(0xdfe8ea).formula('(C5H8O2)n(C8H14O2)m(C4H6O2)k')
    allthemods.create('propylene_glycol_methyl_ether').liquid().color(0xe6f0ea).formula('C4H10O2')
    allthemods.create('propylene_glycol_methyl_ether_acetate').liquid().color(0xdcebe6).formula('C6H12O3')
    allthemods.create('krf_photoresist').liquid().color(0xd9b45c)
    allthemods.create('arf_photoresist').liquid().color(0xe6dcaa)

    // Immersion film for LUV: 18 MOhm cm, degassed. Water (n = 1.44 at 193 nm) lets the lens reach NA 1.35.
    allthemods.create('ultrapure_water').liquid().color(0x8cc4ff).formula('H2O')
})
