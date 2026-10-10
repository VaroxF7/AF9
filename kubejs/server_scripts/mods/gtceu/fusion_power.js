// AF9 - Fusion power: the plasma reactions of GTNH's fusion reactor that GT does not have, and the fuel values that make
// every one of them pay. Spec: docs/fusion-power.md
//
// GT ships eight burnable plasmas (helium, oxygen, nitrogen, argon, iron, tin, nickel, americium). GTNH's reactor fuses
// eleven more, the plasma turbines burn them, and the fuel of each is denser than the one before. Here they come in
// four steps, as the reactors do:
//   Fusion Reactor Mk1 (LuV, 160 MEU to start)   helium from deuterium + helium-3, calcium
//   Fusion Reactor Mk2 (ZPM, 320 MEU)            sulfur, zinc, niobium
//   Fusion Reactor Mk3 (UV, 640 MEU)             silver, bismuth (from the zinc plasma), radon
//   Mega Fusion Reactor (UIV; only 2 energy hatches, 640 MEU)   lead, thorium, plutonium-241: recipes of UHV and UEV
//                                                voltage, which the Mk3 cannot run
// The Mk1-Mk3 recipes run in the Mega Fusion Reactor too, as the base pack makes it do for GT's: half the time on one and
// a half times the power (mega_fusion_reactor.js of the base pack mirrors GT's own recipes only, not KubeJS's, so they
// are made here). Amounts are GTNH's, in GT's units: a nugget is 16 mB, an ingot 144, a half ingot 72.
//
// The fuel values (EU per mB, the plasma generator's duration x 2,048 EU/t) are GTNH's plasma turbine table, which GT's
// own are the same scale of (helium 81,920). Nothing here is a loop: every plasma costs more to make than it was worth
// before it was fused, and pays several times what its reaction draws (docs/fusion-power.md has the table).

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const V = GTValues.V

    // GT 7.5 throws on a fluid that does not exist and KubeJS drops every recipe after the throw: one reaction or fuel
    // with a missing fluid (the base pack's star matter plasma, say) must not take the rest with it, so each is tried alone
    const attempt = (what, make) => {
        try {
            make()
        } catch (e) {
            console.warn(`fusion_power.js: ${what} skipped: ${e}`)
        }
    }

    // the reaction in the Fusion Reactor; the base pack's hook copies it into the Mega Fusion Reactor (half the time, 1.5
    // times the power; seen in the game: af9:mega_fusion_reactor/fusion/<id>), so it is not made twice
    const fusion = (id, a, b, out, duration, eut, start) => {
        attempt(id, () => {
            event.recipes.gtceu.fusion_reactor(`af9:fusion/${id}`)
                .inputFluids(a, b)
                .outputFluids(out)
                .duration(duration)
                .EUt(eut)
                .fusionStartEU(start)
        })
    }
    // a reaction only the Mega Fusion Reactor can run: its voltage is above the Mk3's
    const megaFusion = (id, a, b, out, duration, eut, start) => attempt(id, () => {
        event.recipes.gtceu.mega_fusion_reactor(`af9:mega_fusion/${id}`)
            .inputFluids(a, b)
            .outputFluids(out)
            .duration(duration)
            .EUt(eut)
            .fusionStartEU(start)
    })
    // a plasma turbine fuel: 1 mB lasts `ticks` ticks of an EV generator (2,048 EU/t), so it is worth ticks x 2,048 EU.
    // The element comes back as its fluid where it has one.
    const fuel = (id, plasma, ticks, back) => attempt(`fuel ${id}`, () => {
        const recipe = event.recipes.gtceu.plasma_generator(`af9:plasma/${id}`)
            .inputFluids(Fluid.of(plasma, 1))
            .duration(ticks)
            .EUt(-V[GTValues.EV])
        if (back) recipe.outputFluids(Fluid.of(back, 1))
    })

    // ---- Mk1 (LuV) ----
    // deuterium + helium-3: cheaper to run than deuterium + tritium, the same plasma
    fusion('deuterium_and_helium_3_to_helium_plasma',
        Fluid.of('gtceu:deuterium', 125), Fluid.of('gtceu:helium_3', 125),
        Fluid.of('gtceu:helium_plasma', 125), 16, VA[GTValues.EV], 60000000)
    // magnesium + oxygen: calcium plasma, the first of the new fuels
    fusion('magnesium_and_oxygen_to_calcium_plasma',
        Fluid.of('gtceu:magnesium', 128), Fluid.of('gtceu:oxygen', 128),
        Fluid.of('gtceu:calcium_plasma', 16), 128, VA[GTValues.IV], 120000000)

    // ---- Mk2 (ZPM) ----
    fusion('aluminium_and_lithium_to_sulfur_plasma',
        Fluid.of('gtceu:aluminium', 16), Fluid.of('gtceu:lithium', 16),
        Fluid.of('gtceu:sulfur_plasma', 144), 32, 10240, 240000000)
    fusion('copper_and_tritium_to_zinc_plasma',
        Fluid.of('gtceu:copper', 72), Fluid.of('gtceu:tritium', 250),
        Fluid.of('gtceu:zinc_plasma', 72), 16, 49152, 180000000)
    fusion('cobalt_and_silicon_to_niobium_plasma',
        Fluid.of('gtceu:cobalt', 144), Fluid.of('gtceu:silicon', 144),
        Fluid.of('gtceu:niobium_plasma', 144), 16, 49152, 200000000)

    // ---- Mk3 (UV) ----
    fusion('gold_and_arsenic_to_silver_plasma',
        Fluid.of('gtceu:gold', 144), Fluid.of('gtceu:arsenic', 144),
        Fluid.of('gtceu:silver_plasma', 144), 16, 49152, 350000000)
    // the zinc plasma of the Mk2 is the second input
    fusion('tantalum_and_zinc_plasma_to_bismuth_plasma',
        Fluid.of('gtceu:tantalum', 144), Fluid.of('gtceu:zinc_plasma', 72),
        Fluid.of('gtceu:bismuth_plasma', 144), 16, 98304, 350000000)
    fusion('iridium_and_fluorine_to_radon_plasma',
        Fluid.of('gtceu:iridium', 144), Fluid.of('gtceu:fluorine', 500),
        Fluid.of('gtceu:radon_plasma', 144), 32, 98304, 450000000)

    // ---- Mega Fusion Reactor only (GTNH's Mk IV and V; here UHV and UEV voltage) ----
    // GTNH starts these on 6 GEU; the Mega has two energy hatches, so 640 MEU is all it can hold: the voltage is the gate.
    // lead: platinum + beryllium (GTNH's tellurium has no fluid here)
    megaFusion('platinum_and_beryllium_to_lead_plasma',
        Fluid.of('gtceu:platinum', 576), Fluid.of('gtceu:beryllium', 576),
        Fluid.of('gtceu:lead_plasma', 576), 8, VA[GTValues.UHV], 500000000)
    megaFusion('osmium_and_silicon_to_thorium_plasma',
        Fluid.of('gtceu:osmium', 576), Fluid.of('gtceu:silicon', 576),
        Fluid.of('gtceu:thorium_plasma', 576), 8, Math.floor(VA[GTValues.UEV] / 2), 580000000)
    megaFusion('lutetium_and_vanadium_to_plutonium_241_plasma',
        Fluid.of('gtceu:lutetium', 576), Fluid.of('gtceu:vanadium', 576),
        Fluid.of('gtceu:plutonium_241_plasma', 576), 8, VA[GTValues.UEV], 640000000)

    // ---- the fuels: GTNH's plasma turbine values (EU per mB = ticks x 2,048) ----
    fuel('calcium', 'gtceu:calcium_plasma', 92)                                    // 188,416
    fuel('sulfur', 'gtceu:sulfur_plasma', 83)                                      // 169,984
    fuel('zinc', 'gtceu:zinc_plasma', 110, 'gtceu:zinc')                           // 225,280
    fuel('niobium', 'gtceu:niobium_plasma', 132, 'gtceu:niobium')                  // 270,336
    fuel('silver', 'gtceu:silver_plasma', 138, 'gtceu:silver')                     // 282,624
    fuel('lead', 'gtceu:lead_plasma', 207, 'gtceu:lead')                           // 423,936
    fuel('bismuth', 'gtceu:bismuth_plasma', 208, 'gtceu:bismuth')                  // 425,984
    fuel('radon', 'gtceu:radon_plasma', 220, 'gtceu:radon')                        // 450,560
    fuel('thorium', 'gtceu:thorium_plasma', 230, 'gtceu:thorium')                  // 471,040
    fuel('plutonium_241', 'gtceu:plutonium_241_plasma', 243, 'gtceu:plutonium_241') // 497,664
    // the endgame's own: the star matter the Mega Fusion Reactor makes from helium, nitrogen, oxygen and iron plasma
    // (1,000 mB of it from 1.35 GEU of those). Idontknowium plasma is no fuel: the Plasma Forge makes it on UHV power at
    // 100 A (196 MEU/t), which it could never pay back
    fuel('star_matter', 'gtceu:star_matter_plasma', 800)                           // 1,638,400
})
