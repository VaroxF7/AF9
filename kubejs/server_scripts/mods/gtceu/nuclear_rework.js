// AF9 - Nuclear rework: the molten-salt branch (FLiBe, Heat Exchanger, graphite, burner rods). Spec: docs/nuclear-rework.md
//
// The idea is taken from TerraFirmaGreg Modern's nuclear line (a graphite-moderated reactor, a molten-salt (FLiBe)
// coolant loop, a heat exchanger that turns the hot coolant into power, and fuel rods that burn the minor actinides up
// to californium: github.com/TerraFirmaGreg-Team/Modpack-Modern, LGPL-3.0, credited in CREDITS-textures.md). The recipes
// and numbers are AF9's own and build on the Extreme Reactors reactor (er_bridge.js) and the rods of asteroid_fission.js / fission_fuels.js.
//
//   Graphite moderator   graphite + coal tar -> green body -> baked body -> purified billet -> moderator (never used up)
//   FLiBe                lithium fluoride + beryllium fluoride, melted together: the molten-salt coolant
//   Molten-salt cycles   an Extreme Reactors reactor with FLiBe as coolant (and nuclear graphite as moderator): the hot
//                        FLiBe comes out as a vapour-like fluid (config/extremereactors/modpack_api.json)
//   Heat Exchanger       hot FLiBe + distilled water -> supercritical steam and FLiBe again
//   Burner rods          neptunium, americium, curium and californium: the minor actinides of reprocessing burned for
//                        heat, each rung leaving the next one; fast spectrum, no moderator needed

ServerEvents.tags('item', event => {
    ;['np', 'am', 'cm', 'cf'].forEach(b =>
        event.add('af9:radioactive', [`af9:fx_${b}_pellet`, `af9:fx_${b}_rod`, `af9:fx_spent_${b}_rod`]))
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
    // 1,000 mB of hot salt carry the heat of 92,160 mB of supercritical steam (96 mB of water per 100 mB of steam)
    event.recipes.gtceu.heat_exchanger('af9:flibe_heat_exchange')
        .inputFluids(Fluid.of('gtceu:hot_flibe', 1000), Fluid.of('gtceu:distilled_water', 960))
        .outputFluids(Fluid.of('gtceu:supercritical_steam', 92160), Fluid.of('gtceu:flibe', 1000))
        .duration(400)
        .EUt(VA[GTValues.HV])

    // The molten-salt cycles themselves are Extreme Reactors' (er_bridge.js, config/extremereactors/modpack_api.json): FLiBe
    // goes in as coolant and comes out as hot FLiBe, which the Heat Exchanger above turns into steam.

    // ---- The actinide burner rods: neptunium -> americium -> curium -> californium ----
    // Fast-spectrum fuels, hotter and more reactive each rung (see tools/er/gen_modpack_api.py). Each leaves the next rung behind: a spent rod is decladded straight to dusts (no acid), and what is left of
    // the last rung is vitrified waste. The matrix is the fuel's inert carrier.
    const burners = [
        { id: 'np', name: 'neptunium', dusts: ['4x gtceu:neptunium_dust', '4x gtceu:uranium_dust'],
            outs: ['2x gtceu:plutonium_dust'], chances: [['gtceu:americium_dust', 5000]] },
        { id: 'am', name: 'americium', dusts: ['4x gtceu:americium_dust', '4x gtceu:thorium_dust'],
            outs: ['3x gtceu:curium_dust'], chances: [['gtceu:plutonium_dust', 5000]] },
        { id: 'cm', name: 'curium', dusts: ['4x gtceu:curium_dust', '4x gtceu:plutonium_dust'],
            outs: ['2x gtceu:curium_dust'], chances: [['gtceu:californium_dust', 5000]] },
        { id: 'cf', name: 'californium', dusts: ['2x gtceu:californium_dust', '6x gtceu:plutonium_dust'],
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
