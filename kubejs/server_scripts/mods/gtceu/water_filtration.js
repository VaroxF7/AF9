// AF9 - Water Filtration Plant recipes (startup_scripts/gtceu/water_filtration.js).
// Dirty water in, distilled water out, the skimmed debris on the side. The fluid filter is never
// used up; better tiers filter more water per run.

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // ---- The controller: stainless basin, pumps, glass and MV circuits round an MV hull ----
    event.shaped('gtceu:water_filtration_plant', [
        'SPS',
        'GCG',
        'SMS'
    ], {
        S: '#forge:plates/stainless_steel',
        P: 'gtceu:lv_electric_pump',
        G: 'gtceu:tempered_glass',
        C: '#gtceu:circuits/mv',
        M: 'gtceu:mv_machine_hull'
    }).id('af9:water_filtration_plant')

    // ---- LV: river water, 60% clean ----
    event.recipes.gtceu.water_filtration('af9:river_water_filtration')
        .notConsumable('gtceu:fluid_filter')
        .circuit(1)
        .inputFluids(Fluid.of('minecraft:water', 1000))
        .outputFluids(Fluid.of('gtceu:distilled_water', 600))
        .itemOutputs('minecraft:sand', 'minecraft:gravel')
        .duration(200)
        .EUt(VA[GTValues.LV])

    // ---- MV: murky water, 80% clean ----
    event.recipes.gtceu.water_filtration('af9:murky_water_filtration')
        .notConsumable('gtceu:fluid_filter')
        .circuit(2)
        .inputFluids(Fluid.of('minecraft:water', 1000))
        .outputFluids(Fluid.of('gtceu:distilled_water', 800))
        .itemOutputs('minecraft:sand', 'minecraft:clay_ball')
        .duration(200)
        .EUt(VA[GTValues.MV])

    // ---- HV: pond water, more of it ----
    event.recipes.gtceu.water_filtration('af9:pond_water_filtration')
        .notConsumable('gtceu:fluid_filter')
        .circuit(3)
        .inputFluids(Fluid.of('minecraft:water', 2000))
        .outputFluids(Fluid.of('gtceu:distilled_water', 1800))
        .itemOutputs('minecraft:clay_ball', 'minecraft:dirt', 'minecraft:sand')
        .duration(200)
        .EUt(VA[GTValues.HV])

    // ---- EV: brackish water, the full skim ----
    event.recipes.gtceu.water_filtration('af9:brackish_water_filtration')
        .notConsumable('gtceu:fluid_filter')
        .circuit(4)
        .inputFluids(Fluid.of('minecraft:water', 3000))
        .outputFluids(Fluid.of('gtceu:distilled_water', 2800))
        .itemOutputs('minecraft:sand', 'minecraft:gravel', 'minecraft:clay_ball', 'minecraft:dirt')
        .duration(200)
        .EUt(VA[GTValues.EV])

    // ---- MV: desalination. Salt water in, distilled water and salt out ----
    event.recipes.gtceu.water_filtration('af9:salt_water_desalination')
        .notConsumable('gtceu:fluid_filter')
        .circuit(6)
        .inputFluids(Fluid.of('gtceu:salt_water', 1000))
        .outputFluids(Fluid.of('gtceu:distilled_water', 800))
        .itemOutputs('gtceu:salt_dust')
        .duration(200)
        .EUt(VA[GTValues.MV])

    // ---- LuV biosorption (needs the breeding station built and piped): salt water straight to brine ----
    event.recipes.gtceu.water_filtration('af9:luv_brine_biosorption')
        .notConsumable('gtceu:fluid_filter')
        .circuit(9)
        .inputFluids(Fluid.of('gtceu:salt_water', 2000), Fluid.of('gtceu:bacteria_culture', 500))
        .outputFluids(Fluid.of('gtceu:lithium_brine', 1000))
        .itemOutputs('gtceu:salt_dust')
        .duration(200)
        .EUt(VA[GTValues.LuV])

    // ---- ZPM biosorption: salt water straight to concentrate, no ponds ----
    event.recipes.gtceu.water_filtration('af9:zpm_brine_biosorption')
        .notConsumable('gtceu:fluid_filter')
        .circuit(10)
        .inputFluids(Fluid.of('gtceu:salt_water', 2000), Fluid.of('gtceu:enriched_bacteria_culture', 500))
        .outputFluids(Fluid.of('gtceu:concentrated_lithium_brine', 1000))
        .itemOutputs('gtceu:salt_dust')
        .duration(200)
        .EUt(VA[GTValues.ZPM])

    // ---- Wastewater cleaning: the fab's aqueous waste, polished back to wash water ----
    // Mechanical separation only: suspended solids cake out as spent filter cake (burns like coal),
    // unreacted pre-liquid passes through and comes back, dissolved values stay in the wash water,
    // which loops back to washing, never to chemistry. The full treatments
    // (lime neutralisation, PGMEA fractionation) stay the better-paying paths. Nothing is voided:
    // every cleaning run pays its cake.
    // Spent piranha: unreacted piranha passes the bed, the dissolved resist cakes out
    event.recipes.gtceu.water_filtration('af9:spent_piranha_filtration')
        .notConsumable('gtceu:fluid_filter')
        .circuit(7)
        .inputFluids(Fluid.of('gtceu:spent_piranha', 1000))
        .outputFluids(Fluid.of('minecraft:water', 600), Fluid.of('gtceu:piranha_solution', 200))
        .itemOutputs('gtceu:filter_cake_dust')
        .duration(200)
        .EUt(VA[GTValues.MV])

    // Spent resist solvent over the carbon bed: half the PGMEA back, no cleanroom, cake instead
    event.recipes.gtceu.water_filtration('af9:spent_resist_carbon_bed')
        .notConsumable('gtceu:fluid_filter')
        .circuit(8)
        .inputFluids(Fluid.of('gtceu:spent_resist_solvent', 1000))
        .outputFluids(Fluid.of('gtceu:propylene_glycol_methyl_ether_acetate', 300))
        .itemOutputs('gtceu:carbon_dust', 'gtceu:filter_cake_dust')
        .duration(200)
        .EUt(VA[GTValues.HV])
})
