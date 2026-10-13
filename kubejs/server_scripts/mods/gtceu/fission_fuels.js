// AF9 - Extended fission fuels: MOX, thorium and breeder blankets for the FX-1 Reactor,
// minor actinides and vitrified waste out of reprocessing. The uranium line stays in
// asteroid_fission.js. Spec: docs/asteroid-fission.md §4, §6.
//
//   MOX:      uranium + plutonium -> pellets -> rods -> hotter, longer FX-1 cycle -> Pu back and more
//   Thorium:  thorium + U-235 driver -> pellets -> rods -> long, gentle FX-1 cycle -> thorium + Pa back
//   Blanket:  depleted uranium assemblies ride a normal cycle and breed extra Pu-239
//   MA:       Np/Am/Pa lock into vitrified glass, permanently safe; nothing is voided by a recipe.

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // ---- MOX pellets: plutonium and uranium mixed oxide, sintered in oxygen ----
    event.recipes.gtceu.electric_blast_furnace('af9:fx_mox_pellets')
        .itemInputs('6x gtceu:uranium_dust', '2x gtceu:plutonium_dust')
        .inputFluids(Fluid.of('gtceu:oxygen', 8000))
        .itemOutputs('4x af9:fx_mox_pellet')
        .blastFurnaceTemp(1800)
        .duration(400)
        .EUt(VA[GTValues.HV])
    // MOX rods: four pellets in a zirconium cladding, like uranium
    event.recipes.gtceu.assembler('af9:fx_mox_rod')
        .itemInputs('4x af9:fx_mox_pellet', 'gtceu:zirconium_ingot')
        .itemOutputs('af9:fx_mox_rod')
        .duration(200)
        .EUt(VA[GTValues.HV])
    // MOX cycle: hotter and longer than uranium (same steam rate, half as long again)
    event.recipes.gtceu.fx1_reactor('af9:fx1_mox_cycle')
        .itemInputs('af9:fx_mox_rod')
        .inputFluids(Fluid.of('gtceu:distilled_water', 640))
        .inputFluids(Fluid.of('gtceu:sodium_potassium', 1000))
        .itemOutputs('af9:fx_spent_mox_rod')
        .outputFluids(Fluid.of('gtceu:supercritical_steam', 92160))
        .outputFluids(Fluid.of('gtceu:hot_sodium_potassium', 1000))
        .duration(1800)
        .EUt(VA[GTValues.EV])

    // ---- Thorium pellets: thorium oxide with a U-235 driver to light it ----
    event.recipes.gtceu.electric_blast_furnace('af9:fx_thorium_pellets')
        .itemInputs('10x gtceu:thorium_dust', '2x gtceu:tiny_uranium_235_dust')
        .inputFluids(Fluid.of('gtceu:oxygen', 8000))
        .itemOutputs('4x af9:fx_thorium_pellet')
        .blastFurnaceTemp(1800)
        .duration(400)
        .EUt(VA[GTValues.HV])
    // Thorium rods: four pellets in a zirconium cladding
    event.recipes.gtceu.assembler('af9:fx_thorium_rod')
        .itemInputs('4x af9:fx_thorium_pellet', 'gtceu:zirconium_ingot')
        .itemOutputs('af9:fx_thorium_rod')
        .duration(200)
        .EUt(VA[GTValues.HV])
    // Thorium cycle: the patient fuel (half the steam rate, twice the time, same total)
    event.recipes.gtceu.fx1_reactor('af9:fx1_thorium_cycle')
        .itemInputs('af9:fx_thorium_rod')
        .inputFluids(Fluid.of('gtceu:distilled_water', 640))
        .inputFluids(Fluid.of('gtceu:sodium_potassium', 1000))
        .itemOutputs('af9:fx_spent_thorium_rod')
        .outputFluids(Fluid.of('gtceu:supercritical_steam', 61440))
        .outputFluids(Fluid.of('gtceu:hot_sodium_potassium', 1000))
        .duration(2400)
        .EUt(VA[GTValues.EV])

    // ---- Breeder blanket: depleted uranium assemblies around a running core ----
    event.recipes.gtceu.assembler('af9:fx_blanket_assembly')
        .itemInputs('8x gtceu:uranium_dust', '2x gtceu:steel_plate')
        .itemOutputs('af9:fx_blanket_assembly')
        .duration(200)
        .EUt(VA[GTValues.HV])
    // Breeder mode: a normal uranium cycle with a blanket riding along (it breeds Pu-239).
    // Heavy water moderates the blanket pins (CANDU-style): a different fluid, so the two FX-1
    // modes never shadow each other.
    event.recipes.gtceu.fx1_reactor('af9:fx1_breeder_cycle')
        .itemInputs('af9:fx_fuel_rod', 'af9:fx_blanket_assembly')
        .inputFluids(Fluid.of('gtceu:heavy_water', 640))
        .inputFluids(Fluid.of('gtceu:sodium_potassium', 1000))
        .itemOutputs('af9:fx_spent_fuel_rod', 'af9:fx_spent_blanket')
        .outputFluids(Fluid.of('gtceu:supercritical_steam', 61440))
        .outputFluids(Fluid.of('gtceu:hot_sodium_potassium', 1000))
        .duration(1200)
        .EUt(VA[GTValues.EV])
    // The bred plutonium out of the blanket (the amplifier of the whole line)
    event.recipes.gtceu.centrifuge('af9:separate_spent_blanket')
        .itemInputs('af9:fx_spent_blanket')
        .itemOutputs('2x gtceu:plutonium_dust', '4x gtceu:uranium_dust', '2x gtceu:steel_dust')
        .duration(600)
        .EUt(VA[GTValues.EV])

    // ---- MOX reprocessing: its own dust and solution (PuO2 needs fluoride to dissolve) ----
    event.recipes.gtceu.macerator('af9:decladding_spent_mox_rod')
        .itemInputs('af9:fx_spent_mox_rod')
        .itemOutputs('4x gtceu:irradiated_mox_dust', 'gtceu:zirconium_dust')
        .duration(200)
        .EUt(VA[GTValues.HV])
    event.recipes.gtceu.chemical_reactor('af9:dissolve_irradiated_mox')
        .itemInputs('4x gtceu:irradiated_mox_dust')
        .inputFluids(Fluid.of('gtceu:nitric_acid', 3000))
        .outputFluids(Fluid.of('gtceu:spent_mox_solution', 3000), Fluid.of('gtceu:fission_off_gas', 800))
        .duration(300)
        .EUt(VA[GTValues.HV])
    // MOX fully recycles: two plutonium back plus minor actinides, acid back
    event.recipes.gtceu.centrifuge('af9:separate_spent_mox')
        .inputFluids(Fluid.of('gtceu:spent_mox_solution', 3000))
        .itemOutputs('2x gtceu:plutonium_dust', '4x gtceu:uranium_dust')
        .chancedOutput('gtceu:plutonium_241_dust', 5000, 0)
        .chancedOutput('gtceu:neptunium_dust', 2000, 0)
        .chancedOutput('gtceu:americium_dust', 800, 0)
        .outputFluids(Fluid.of('gtceu:nitric_acid', 2000))
        .duration(600)
        .EUt(VA[GTValues.EV])

    // ---- Thorium reprocessing: thorium back, protactinium out, driver burned ----
    event.recipes.gtceu.macerator('af9:decladding_spent_thorium_rod')
        .itemInputs('af9:fx_spent_thorium_rod')
        .itemOutputs('6x gtceu:thorium_dust', 'gtceu:zirconium_dust')
        .chancedOutput('gtceu:protactinium_dust', 3000, 0)
        .chancedOutput('gtceu:tiny_uranium_235_dust', 2000, 0)
        .duration(200)
        .EUt(VA[GTValues.HV])

    // ---- Vitrification: minor actinides locked in glass, permanently safe ----
    event.recipes.gtceu.mixer('af9:vitrify_neptunium')
        .itemInputs('gtceu:neptunium_dust', '4x gtceu:silicon_dioxide_dust', '2x gtceu:lead_dust')
        .itemOutputs('4x gtceu:vitrified_waste_dust')
        .duration(200)
        .EUt(VA[GTValues.HV])
    event.recipes.gtceu.mixer('af9:vitrify_americium')
        .itemInputs('gtceu:americium_dust', '4x gtceu:silicon_dioxide_dust', '2x gtceu:lead_dust')
        .itemOutputs('4x gtceu:vitrified_waste_dust')
        .duration(200)
        .EUt(VA[GTValues.HV])
    event.recipes.gtceu.mixer('af9:vitrify_protactinium')
        .itemInputs('gtceu:protactinium_dust', '4x gtceu:silicon_dioxide_dust', '2x gtceu:lead_dust')
        .itemOutputs('4x gtceu:vitrified_waste_dust')
        .duration(200)
        .EUt(VA[GTValues.HV])

    // ---- Vitrification: minor actinides locked in glass, permanently safe ----
    event.recipes.gtceu.mixer('af9:vitrify_neptunium')
        .itemInputs('gtceu:neptunium_dust', '4x gtceu:silicon_dioxide_dust', '2x gtceu:lead_dust')
        .itemOutputs('4x gtceu:vitrified_waste_dust')
        .duration(200)
        .EUt(VA[GTValues.HV])
    event.recipes.gtceu.mixer('af9:vitrify_americium')
        .itemInputs('gtceu:americium_dust', '4x gtceu:silicon_dioxide_dust', '2x gtceu:lead_dust')
        .itemOutputs('4x gtceu:vitrified_waste_dust')
        .duration(200)
        .EUt(VA[GTValues.HV])
    event.recipes.gtceu.mixer('af9:vitrify_protactinium')
        .itemInputs('gtceu:protactinium_dust', '4x gtceu:silicon_dioxide_dust', '2x gtceu:lead_dust')
        .itemOutputs('4x gtceu:vitrified_waste_dust')
        .duration(200)
        .EUt(VA[GTValues.HV])

    // ---- Off-gas cryogenics: dissolver fumes split to krypton and xenon (boule melts burn it) ----
    event.recipes.gtceu.distillation_tower('af9:fission_off_gas_split')
        .inputFluids(Fluid.of('gtceu:fission_off_gas', 1000))
        .outputFluids(Fluid.of('gtceu:krypton', 600), Fluid.of('gtceu:xenon', 200))
        .duration(200)
        .EUt(VA[GTValues.HV])

    // ---- MOX tiers 2-4: more plutonium per pellet, hotter cycles (T3/T4 run in the Mk2) ----
    const moxTiers = [
        { tier: '2', uranium: 4, eut: VA[GTValues.EV], steam: 122880, type: 'fx1_reactor' },
        { tier: '3', uranium: 2, eut: VA[GTValues.EV], steam: 153600, type: 'fx2_reactor' },
        { tier: '4', uranium: 0, eut: VA[GTValues.EV], steam: 184320, type: 'fx2_reactor' },
    ]
    moxTiers.forEach(cfg => {
        const plutonium = 8 - cfg.uranium
        const uranium = cfg.uranium > 0 ? [`${cfg.uranium}x gtceu:uranium_dust`] : []
        // pellets: the tier's mix, sintered in oxygen
        event.recipes.gtceu.electric_blast_furnace(`af9:fx_mox_${cfg.tier}_pellets`)
            .itemInputs(uranium.concat([`${plutonium}x gtceu:plutonium_dust`]))
            .inputFluids(Fluid.of('gtceu:oxygen', 8000))
            .itemOutputs(`4x af9:fx_mox_${cfg.tier}_pellet`)
            .blastFurnaceTemp(1800)
            .duration(400)
            .EUt(VA[GTValues.HV])
        // rods: four pellets in a zirconium cladding
        event.recipes.gtceu.assembler(`af9:fx_mox_${cfg.tier}_rod`)
            .itemInputs(`4x af9:fx_mox_${cfg.tier}_pellet`, 'gtceu:zirconium_ingot')
            .itemOutputs(`af9:fx_mox_${cfg.tier}_rod`)
            .duration(200)
            .EUt(VA[GTValues.HV])
        // the cycle: same steam rate steps, all 1,800 ticks
        event.recipes.gtceu[cfg.type](`af9:fx1_mox_${cfg.tier}_cycle`)
            .itemInputs(`af9:fx_mox_${cfg.tier}_rod`)
            .inputFluids(Fluid.of('gtceu:distilled_water', 640))
            .inputFluids(Fluid.of('gtceu:sodium_potassium', 1000))
            .itemOutputs(`af9:fx_spent_mox_${cfg.tier}_rod`)
            .outputFluids(Fluid.of('gtceu:supercritical_steam', cfg.steam))
            .outputFluids(Fluid.of('gtceu:hot_sodium_potassium', 1000))
            .duration(1800)
            .EUt(cfg.eut)
        // decladding joins the shared MOX stream (reprocessing takes every tier alike)
        event.recipes.gtceu.macerator(`af9:decladding_spent_mox_${cfg.tier}_rod`)
            .itemInputs(`af9:fx_spent_mox_${cfg.tier}_rod`)
            .itemOutputs('4x gtceu:irradiated_mox_dust', 'gtceu:zirconium_dust')
            .duration(200)
            .EUt(VA[GTValues.HV])
    })

    // ---- FX-2 refit: the Mk1 controller turned hotter (runs the fx2_reactor type) ----
    event.recipes.gtceu.assembler('af9:fx2_reactor')
        .itemInputs('gtceu:fx1_reactor', '4x gtceu:tungsten_steel_plate', '2x gtceu:ev_electric_pump',
            '4x #gtceu:circuits/ev')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 1152))
        .itemOutputs('gtceu:fx2_reactor')
        .duration(600)
        .EUt(VA[GTValues.UV])

    // ---- Spent Fuel Pool and Fabrication Bay controllers, stainless like the work they do ----
    event.recipes.gtceu.assembler('af9:spent_fuel_pool')
        .itemInputs('4x gtceu:stainless_steel_plate', '2x gtceu:tempered_glass', 'gtceu:mv_machine_hull')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 576))
        .itemOutputs('gtceu:spent_fuel_pool')
        .duration(400)
        .EUt(VA[GTValues.HV])
    event.recipes.gtceu.assembler('af9:fuel_fabrication_bay')
        .itemInputs('4x gtceu:stainless_steel_plate', '2x gtceu:ev_electric_motor', 'gtceu:mv_machine_hull')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 576))
        .itemOutputs('gtceu:fuel_fabrication_bay')
        .duration(400)
        .EUt(VA[GTValues.HV])

    // ---- Spent Fuel Pool: cooling decladding for every spent rod, the passive bulk path ----
    // same outputs as the macerators, slower and hands-free (water in, nothing out but the dust)
    const poolDeclad = (id, rod, outs, chances) => {
        const recipe = event.recipes.gtceu.spent_fuel_pool(`af9:pool_${id}`)
            .itemInputs(rod)
            .inputFluids(Fluid.of('minecraft:water', 1000))
        outs.forEach(out => recipe.itemOutputs(out))
        ;(chances || []).forEach(chance => recipe.chancedOutput(chance[0], chance[1], 0))
        recipe.duration(3200).EUt(VA[GTValues.HV])
    }
    poolDeclad('uranium', 'af9:fx_spent_fuel_rod', ['4x gtceu:irradiated_fuel_dust', 'gtceu:zirconium_dust'])
    poolDeclad('mox', 'af9:fx_spent_mox_rod', ['4x gtceu:irradiated_mox_dust', 'gtceu:zirconium_dust'])
    poolDeclad('mox_2', 'af9:fx_spent_mox_2_rod', ['4x gtceu:irradiated_mox_dust', 'gtceu:zirconium_dust'])
    poolDeclad('mox_3', 'af9:fx_spent_mox_3_rod', ['4x gtceu:irradiated_mox_dust', 'gtceu:zirconium_dust'])
    poolDeclad('mox_4', 'af9:fx_spent_mox_4_rod', ['4x gtceu:irradiated_mox_dust', 'gtceu:zirconium_dust'])
    poolDeclad('thorium', 'af9:fx_spent_thorium_rod',
        ['6x gtceu:thorium_dust', 'gtceu:zirconium_dust'],
        [['gtceu:protactinium_dust', 3000], ['gtceu:tiny_uranium_235_dust', 2000]])

    // ---- Fuel Fabrication Bay: dusts straight to clad rods and blankets, one passive step ----
    // same totals as pellets plus rods, no EBF/assembler round trip
    const fabricate = (id, inputs, fluid, output) => {
        const recipe = event.recipes.gtceu.fuel_fabrication(`af9:fabricate_${id}`)
            .itemInputs(inputs)
        if (fluid) recipe.inputFluids(fluid)
        recipe.itemOutputs(output).duration(600).EUt(VA[GTValues.HV])
    }
    fabricate('uranium', ['12x gtceu:uranium_dust', '4x gtceu:tiny_uranium_235_dust', 'gtceu:zirconium_ingot'],
        Fluid.of('gtceu:oxygen', 8000), 'af9:fx_fuel_rod')
    fabricate('mox', ['6x gtceu:uranium_dust', '2x gtceu:plutonium_dust', 'gtceu:zirconium_ingot'],
        Fluid.of('gtceu:oxygen', 8000), 'af9:fx_mox_rod')
    fabricate('mox_2', ['4x gtceu:uranium_dust', '4x gtceu:plutonium_dust', 'gtceu:zirconium_ingot'],
        Fluid.of('gtceu:oxygen', 8000), 'af9:fx_mox_2_rod')
    fabricate('mox_3', ['2x gtceu:uranium_dust', '6x gtceu:plutonium_dust', 'gtceu:zirconium_ingot'],
        Fluid.of('gtceu:oxygen', 8000), 'af9:fx_mox_3_rod')
    fabricate('mox_4', ['8x gtceu:plutonium_dust', 'gtceu:zirconium_ingot'],
        Fluid.of('gtceu:oxygen', 8000), 'af9:fx_mox_4_rod')
    fabricate('thorium', ['10x gtceu:thorium_dust', '2x gtceu:tiny_uranium_235_dust', 'gtceu:zirconium_ingot'],
        Fluid.of('gtceu:oxygen', 8000), 'af9:fx_thorium_rod')
    fabricate('blanket', ['8x gtceu:uranium_dust', '2x gtceu:steel_plate'], null, 'af9:fx_blanket_assembly')
})
