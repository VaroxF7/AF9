// AF9 - The platinum group metals line: the materials of the refinery that turns purified Cu-Ni sulfide ore and cooperite
// into platinum, palladium, gold, rhodium, ruthenium, iridium and osmium. Recipes: server_scripts/mods/gtceu/platinum_group.js.
// Spec: docs/platinum-group-metals.md
//
//   1. Matte        purified ore -> smelted with a silica flux in oxygen -> PGM matte (the converter matte of a real smelter)
//   2. Leach        matte + sulfuric acid + oxygen -> the nickel and copper go into solution, the PGMs stay: leach residue
//   3. Dissolution  residue + HCl + chlorine -> Pt, Pd and Au go into solution; Rh, Ir, Ru and Os stay as insoluble residue
//   4. Gold         sulfur dioxide reduces gold out of the liquor
//   5. Platinum     ammonium chloride precipitates yellow (NH4)2PtCl6, calcined to platinum sponge
//   6. Palladium    what stays in the filtrate: ammonia, then hydrochloric acid give Pd(NH3)2Cl2, hydrogen reduces it
//   7. Ru and Os    alkaline oxidising fusion, water leach, chlorine drives off RuO4 and OsO4, distillation splits them
//   8. Ir and Rh    chlorination of the oxide left from the leach, (NH4)2IrCl6 precipitates, Rh goes on as the nitrite
//
// GT's own platinum group sludge chain stays as it is; its sludge is one more feed of this line (server script).
// Every material is a real compound or process stream. Only formulas are given (no components), so GT adds no electrolyzer
// or centrifuge shortcut that would skip a step.

GTCEuStartupEvents.registry('gtceu:material', event => {
    // [id, form, colour, formula]; form: dust, liquid or gas
    const materials = [
        // ---- 1-3. Matte, leach residue, dissolution ----
        ['pgm_matte', 'dust', 0x8a7a52, '(Ni,Cu,Fe)xSy(PGM)'],                // converter matte, slowly cooled and ground
        ['pgm_leach_residue', 'dust', 0x5c5a4a, '(Pt,Pd,Au,Rh,Ir,Ru,Os)'],      // what the acid leach leaves: ~60 % PGM
        ['pgm_insoluble_residue', 'dust', 0x4a4a46, '(Rh,Ir,Ru,Os)'],            // refractory metals the chlorine does not take
        ['platinum_group_chloride_liquor', 'liquid', 0xc0601a, 'H2PtCl6(H2PdCl4)(HAuCl4)'],
        ['gold_free_liquor', 'liquid', 0xb8641e, 'H2PtCl6(H2PdCl4)'],

        // ---- 5-6. Platinum and palladium ----
        ['ammonium_hexachloroplatinate', 'dust', 0xe8c020, '(NH4)2PtCl6'],       // the yellow salt
        ['palladium_filtrate', 'liquid', 0xc27a2a, 'H2PdCl4'],
        ['palladium_tetraammine_solution', 'liquid', 0xcfd6e8, '[Pd(NH3)4]Cl2'],
        ['palladium_diammine_dichloride', 'dust', 0xe6d24a, 'Pd(NH3)2Cl2'],      // Pd "yellow salt"

        // ---- 7. Ruthenium and osmium ----
        ['pgm_fusion_cake', 'dust', 0x3a5a46, '(Na2RuO4)(Na2OsO4)(Rh2O3)(IrO2)'],
        ['ruthenate_osmate_liquor', 'liquid', 0x2a6a5a, '(Na2RuO4)(Na2OsO4)'],
        ['platinum_group_tetroxide_vapour', 'gas', 0xd8d070, '(RuO4)(OsO4)'],
        ['ruthenium_tetroxide_vapour', 'gas', 0xd8c040, 'RuO4'],                 // bp 40 C
        ['osmium_tetroxide_vapour', 'gas', 0xe6e6c8, 'OsO4'],                    // bp 130 C
        ['ammonium_hexachlororuthenate', 'dust', 0x7a2a2a, '(NH4)2RuCl6'],
        ['ammonium_hexachloroosmate', 'dust', 0x8a3a2a, '(NH4)2OsCl6'],

        // ---- 8. Iridium and rhodium ----
        ['rhodium_iridium_oxide', 'dust', 0x3a3a3e, '(Rh2O3)(IrO2)'],
        ['rhodium_iridium_chloride_liquor', 'liquid', 0x8a2a3a, '(H2IrCl6)(H3RhCl6)'],
        ['ammonium_hexachloroiridate', 'dust', 0x6a1a1a, '(NH4)2IrCl6'],         // dark red
        ['rhodium_chloride_filtrate', 'liquid', 0xc03a50, 'H3RhCl6'],
        ['sodium_hexanitritorhodate', 'dust', 0xe8e0a0, 'Na3Rh(NO2)6'],
        ['rhodium_trichloride_solution', 'liquid', 0xd04a60, 'RhCl3'],
        ['zinc_chloride', 'dust', 0xf0f0f0, 'ZnCl2']                              // the cementation of rhodium leaves it
    ]
    materials.forEach(entry => {
        const [id, form, color, formula] = entry
        const material = event.create(id)
        if (form === 'dust') material.dust().iconSet('rough')
        else if (form === 'gas') material.gas()
        else material.liquid()
        material.color(color)
        if (formula) material.formula(formula)
    })
})
