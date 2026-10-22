// AF9 - Nuclear rework: the molten-salt branch of the FX Reactor. Spec: docs/nuclear-rework.md
//
// The idea is taken from TerraFirmaGreg Modern's nuclear line (a graphite-moderated reactor, a molten-salt (FLiBe)
// coolant loop, a heat exchanger that turns the hot coolant into power, and fuel rods that burn the minor actinides up
// to californium: github.com/TerraFirmaGreg-Team/Modpack-Modern, LGPL-3.0, credited in CREDITS-textures.md). The recipes
// and numbers are AF9's own and build on the FX-1 Reactor of asteroid_fission.js / fission_fuels.js.
//
//   Graphite moderator   graphite + coal tar -> green body -> baked body -> purified billet -> moderator (never used up)
//   FLiBe                lithium fluoride + beryllium fluoride, melted together: the molten-salt coolant
//   Molten-salt cycles   FX Reactor Mk2 (heating coils, Kanthal or better): a rod and FLiBe (and the moderator for the
//                        thermal fuels) in, the spent rod and hot FLiBe out; no water in the core
//   Heat Exchanger       hot FLiBe + distilled water -> supercritical steam and FLiBe again: 1.5 times the steam of the
//                        Mk1 cycle of the same rod; thorium, burning best in salt, makes twice
//   Burner rods          neptunium, americium, curium and californium: the minor actinides of reprocessing burned for
//                        heat, each rung leaving the next one; molten-salt cycles only, no moderator (fast spectrum)

ServerEvents.tags('item', event => {
    event.add('af9:radioactive', ['np', 'am', 'cm', 'cf'].flatMap(b =>
        [`af9:fx_${b}_pellet`, `af9:fx_${b}_rod`, `af9:fx_spent_${b}_rod`]))
})

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const MODERATOR = 'af9:graphite_moderator'

    // ---- The graphite moderator: nuclear-grade graphite, boron purified out with chlorine ----
    event.recipes.gtceu.mixer('af9:graphite_green_body')
        .itemInputs('8x gtceu:graphite_dust')
        .inputFluids(Fluid.of('gtceu:coal_tar', 1000))
        .itemOutputs('4x af9:graphite_green_body')
        .duration(200)
        .EUt(VA[GTValues.HV])
    // the binder is coked out in nitrogen
    event.recipes.gtceu.electric_blast_furnace('af9:graphite_baked_body')
        .itemInputs('4x af9:graphite_green_body')
        .inputFluids(Fluid.of('gtceu:nitrogen', 2000))
        .itemOutputs('4x af9:graphite_baked_body')
        .blastFurnaceTemp(1800)
        .duration(600)
        .EUt(VA[GTValues.HV])
    // graphitised and halogen-purified in one hot pass: boron leaves as a chloride (a neutron poison gone)
    event.recipes.gtceu.electric_blast_furnace('af9:graphite_billet')
        .itemInputs('4x af9:graphite_baked_body')
        .inputFluids(Fluid.of('gtceu:chlorine', 2000))
        .itemOutputs('4x af9:graphite_billet')
        .blastFurnaceTemp(2700)
        .duration(900)
        .EUt(VA[GTValues.HV])
    event.recipes.gtceu.assembler('af9:graphite_moderator')
        .itemInputs('4x af9:graphite_billet', '4x gtceu:titanium_plate')
        .itemOutputs(MODERATOR)
        .duration(400)
        .EUt(VA[GTValues.EV])

    // ---- FLiBe: the molten-salt coolant ----
    // (the lithium chain's own route to LiF is the IV one, lithium_brine.js; this is the direct one for a molten-salt hall)
    event.recipes.gtceu.chemical_reactor('af9:lithium_fluoride_direct')
        .itemInputs('2x gtceu:lithium_dust')
        .inputFluids(Fluid.of('gtceu:fluorine', 1000))
        .itemOutputs('2x gtceu:lithium_fluoride_dust')
        .duration(160)
        .EUt(VA[GTValues.HV])
    event.recipes.gtceu.chemical_reactor('af9:beryllium_fluoride')
        .itemInputs('gtceu:beryllium_dust')
        .inputFluids(Fluid.of('gtceu:fluorine', 2000))
        .itemOutputs('gtceu:beryllium_fluoride_dust')
        .duration(160)
        .EUt(VA[GTValues.HV])
    event.recipes.gtceu.electric_blast_furnace('af9:flibe')
        .itemInputs('2x gtceu:lithium_fluoride_dust', 'gtceu:beryllium_fluoride_dust')
        .outputFluids(Fluid.of('gtceu:flibe', 3000))
        .blastFurnaceTemp(1800)
        .duration(400)
        .EUt(VA[GTValues.HV])

    // ---- Heat Exchanger: hot FLiBe -> steam; the salt goes round again ----
    event.recipes.gtceu.assembler('af9:heat_exchanger')
        .itemInputs('4x gtceu:titanium_plate', '2x gtceu:titanium_normal_fluid_pipe', 'gtceu:hv_machine_hull')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 576))
        .itemOutputs('gtceu:heat_exchanger')
        .duration(400)
        .EUt(VA[GTValues.HV])
    // 1,000 mB of hot salt carry the heat of 92,160 mB of supercritical steam (the Mk1 cycle's 96 mB of water per
    // 1,000 mB of steam, 1.5 times the rod's heat: the salt runs hotter than the sodium-potassium)
    event.recipes.gtceu.heat_exchanger('af9:flibe_heat_exchange')
        .inputFluids(Fluid.of('gtceu:hot_flibe', 1000), Fluid.of('gtceu:distilled_water', 960))
        .outputFluids(Fluid.of('gtceu:supercritical_steam', 92160), Fluid.of('gtceu:flibe', 1000))
        .duration(400)
        .EUt(VA[GTValues.HV])

    // ---- Molten-salt cycles of the thermal fuels: rod + FLiBe + moderator -> spent rod + hot FLiBe ----
    // the same rods and spent rods as the Mk1 cycles, so reprocessing does not change. `hot` is 1.5 times the Mk1 cycle's
    // steam, in thousands of mB of salt; thorium, which burns best in salt, gets three times. Heating coils of Kanthal (2,700 K)
    // or better: the Mk2 shell
    const thermal = [
        { id: 'fuel', rod: 'af9:fx_fuel_rod', spent: 'af9:fx_spent_fuel_rod', hot: 1000, ticks: 1200, eut: VA[GTValues.HV], temp: 2700 },
        { id: 'heu', rod: 'af9:fx_heu_rod', spent: 'af9:fx_spent_heu_rod', hot: 2000, ticks: 1200, eut: VA[GTValues.HV], temp: 2700 },
        { id: 'lep', rod: 'af9:fx_lep_rod', spent: 'af9:fx_spent_lep_rod', hot: 1000, ticks: 600, eut: VA[GTValues.HV], temp: 2700 },
        { id: 'hep', rod: 'af9:fx_hep_rod', spent: 'af9:fx_spent_hep_rod', hot: 2000, ticks: 600, eut: VA[GTValues.HV], temp: 2700 },
        { id: 'mox', rod: 'af9:fx_mox_rod', spent: 'af9:fx_spent_mox_rod', hot: 1500, ticks: 1800, eut: VA[GTValues.HV], temp: 2700 },
        { id: 'mox_2', rod: 'af9:fx_mox_2_rod', spent: 'af9:fx_spent_mox_2_rod', hot: 2000, ticks: 1800, eut: VA[GTValues.HV], temp: 2700 },
        { id: 'mox_3', rod: 'af9:fx_mox_3_rod', spent: 'af9:fx_spent_mox_3_rod', hot: 2500, ticks: 1800, eut: VA[GTValues.EV], temp: 2700 },
        { id: 'mox_4', rod: 'af9:fx_mox_4_rod', spent: 'af9:fx_spent_mox_4_rod', hot: 3000, ticks: 1800, eut: VA[GTValues.EV], temp: 3600 },
        { id: 'thorium', rod: 'af9:fx_thorium_rod', spent: 'af9:fx_spent_thorium_rod', hot: 2000, ticks: 2400, eut: VA[GTValues.HV], temp: 2700 },
    ]
    thermal.forEach(fuel => {
        event.recipes.gtceu.fx1_reactor(`af9:fx1_${fuel.id}_flibe_cycle`)
            .itemInputs(fuel.rod)
            .notConsumable(MODERATOR)
            .inputFluids(Fluid.of('gtceu:flibe', fuel.hot))
            .itemOutputs(fuel.spent)
            .outputFluids(Fluid.of('gtceu:hot_flibe', fuel.hot))
            .blastFurnaceTemp(fuel.temp)
            .duration(fuel.ticks)
            .EUt(fuel.eut)
    })

    // ---- The actinide burner rods: neptunium -> americium -> curium -> californium ----
    // Fast-spectrum fuels: no moderator, only salt, and a hotter shell for each rung (Kanthal, Nichrome, Tungstensteel,
    // HSS-G). Each leaves the next rung behind: a spent rod is decladded straight to dusts (no acid), and what is left of
    // the last rung is vitrified waste. The matrix is the fuel's inert carrier.
    const burners = [
        { id: 'np', name: 'neptunium', dusts: ['4x gtceu:neptunium_dust', '4x gtceu:uranium_dust'], temp: 2700, ticks: 2400, hot: 2000,
            outs: ['2x gtceu:plutonium_dust'], chances: [['gtceu:americium_dust', 5000]] },
        { id: 'am', name: 'americium', dusts: ['4x gtceu:americium_dust', '4x gtceu:thorium_dust'], temp: 3600, ticks: 2400, hot: 3000,
            outs: ['3x gtceu:curium_dust'], chances: [['gtceu:plutonium_dust', 5000]] },
        { id: 'cm', name: 'curium', dusts: ['4x gtceu:curium_dust', '4x gtceu:plutonium_dust'], temp: 4500, ticks: 3000, hot: 5000,
            outs: ['2x gtceu:curium_dust'], chances: [['gtceu:californium_dust', 5000]] },
        { id: 'cf', name: 'californium', dusts: ['2x gtceu:californium_dust', '6x gtceu:plutonium_dust'], temp: 5400, ticks: 3000, hot: 8000,
            outs: ['2x gtceu:vitrified_waste_dust'], chances: [['gtceu:plutonium_241_dust', 2000]] },
    ]
    burners.forEach(fuel => {
        event.recipes.gtceu.electric_blast_furnace(`af9:fx_${fuel.id}_pellets`)
            .itemInputs(fuel.dusts)
            .inputFluids(Fluid.of('gtceu:oxygen', 8000))
            .itemOutputs(`4x af9:fx_${fuel.id}_pellet`)
            .blastFurnaceTemp(1800)
            .duration(400)
            .EUt(VA[GTValues.HV])
        event.recipes.gtceu.assembler(`af9:fx_${fuel.id}_rod`)
            .itemInputs(`4x af9:fx_${fuel.id}_pellet`, 'gtceu:zirconium_ingot')
            .itemOutputs(`af9:fx_${fuel.id}_rod`)
            .duration(200)
            .EUt(VA[GTValues.HV])
        event.recipes.gtceu.fx1_reactor(`af9:fx1_${fuel.id}_flibe_cycle`)
            .itemInputs(`af9:fx_${fuel.id}_rod`)
            .inputFluids(Fluid.of('gtceu:flibe', fuel.hot))
            .itemOutputs(`af9:fx_spent_${fuel.id}_rod`)
            .outputFluids(Fluid.of('gtceu:hot_flibe', fuel.hot))
            .blastFurnaceTemp(fuel.temp)
            .duration(fuel.ticks)
            .EUt(VA[GTValues.EV])
        // decladding: macerator or the Spent Fuel Pool (slow, hands-free), the same dusts
        const declad = recipe => {
            recipe.itemOutputs(fuel.outs.concat(['gtceu:zirconium_dust']))
            fuel.chances.forEach(chance => recipe.chancedOutput(chance[0], chance[1], 0))
        }
        declad(event.recipes.gtceu.macerator(`af9:decladding_spent_${fuel.id}_rod`)
            .itemInputs(`af9:fx_spent_${fuel.id}_rod`)
            .duration(200)
            .EUt(VA[GTValues.HV]))
        declad(event.recipes.gtceu.spent_fuel_pool(`af9:pool_${fuel.id}`)
            .itemInputs(`af9:fx_spent_${fuel.id}_rod`)
            .inputFluids(Fluid.of('minecraft:water', 1000))
            .duration(3200)
            .EUt(VA[GTValues.HV]))
        // Fuel Fabrication Bay: dusts straight to the clad rod
        event.recipes.gtceu.fuel_fabrication(`af9:fabricate_${fuel.id}`)
            .itemInputs(fuel.dusts.concat(['gtceu:zirconium_ingot']))
            .inputFluids(Fluid.of('gtceu:oxygen', 8000))
            .itemOutputs(`af9:fx_${fuel.id}_rod`)
            .duration(600)
            .EUt(VA[GTValues.HV])
    })
})
