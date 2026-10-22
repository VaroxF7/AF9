// AF9 - Redstone etching (TerraFirmaGreg's idea): the persulfate boards etch with redstone tri-p-toluenesulfonate.
// GT's "persulfate" circuit board recipes (plastic to wetware, the iron(III) chloride ones stay) swap their
// sodium persulfate for it; the large chemical reactor copies of them go, like the persulfate ones.
// Chain: redstone + HNO3 -> redstone nitrate; + salt -> redstone chloride (+ sodium nitrate -> HNO3 again);
// toluene + thionyl chloride + H2SO4 -> p-toluenesulfonic acid; 3 acid + redstone chloride -> the etchant + Cl2.
// Thionyl chloride: gtceu:thionyl_chloride from fab_chemistry.js.

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const gt = event.recipes.gtceu

    gt.chemical_bath('af9:redstone_nitrate')
        .itemInputs('10x minecraft:redstone')
        .inputFluids(Fluid.of('gtceu:nitric_acid', 3000))
        .itemOutputs('10x gtceu:redstone_nitrate_dust')
        .outputFluids(Fluid.of('gtceu:hydrogen', 3000))
        .duration(100)
        .EUt(VA[GTValues.HV])
    gt.chemical_reactor('af9:redstone_chloride')
        .itemInputs('10x gtceu:redstone_nitrate_dust', '6x gtceu:salt_dust')
        .itemOutputs('15x gtceu:sodium_nitrate_dust')
        .outputFluids(Fluid.of('gtceu:redstone_chloride', 1000))
        .duration(100)
        .EUt(VA[GTValues.HV])
    gt.chemical_reactor('af9:sodium_nitrate_to_nitric_acid')
        .itemInputs('5x gtceu:sodium_nitrate_dust')
        .inputFluids(Fluid.of('gtceu:hydrochloric_acid', 1000))
        .itemOutputs('2x gtceu:salt_dust')
        .outputFluids(Fluid.of('gtceu:nitric_acid', 1000))
        .duration(30)
        .EUt(VA[GTValues.HV])
    gt.large_chemical_reactor('af9:p_toluenesulfonic_acid')
        .inputFluids(Fluid.of('gtceu:toluene', 1000), Fluid.of('gtceu:thionyl_chloride', 1000),
            Fluid.of('gtceu:sulfuric_acid', 1000))
        .outputFluids(Fluid.of('gtceu:p_toluenesulfonic_acid', 1000), Fluid.of('gtceu:sulfur_dioxide', 1000),
            Fluid.of('gtceu:hydrochloric_acid', 2000))
        .duration(100)
        .EUt(VA[GTValues.MV])
    gt.chemical_reactor('af9:redstone_tri_p_toluenesulfonate')
        .inputFluids(Fluid.of('gtceu:redstone_chloride', 1000), Fluid.of('gtceu:p_toluenesulfonic_acid', 3000))
        .outputFluids(Fluid.of('gtceu:redstone_tri_p_toluenesulfonate', 1000), Fluid.of('gtceu:chlorine', 3000))
        .duration(100)
        .EUt(VA[GTValues.HV])

    // ---- the persulfate board recipes: same numbers, the new etchant ----
    const boards = ['plastic', 'advanced', 'extreme', 'elite', 'wetware']
    const swaps = []
    boards.forEach(board => {
        const id = `gtceu:chemical_reactor/${board}_circuit_board_persulfate`
        event.forEachRecipe({ id: id }, recipe => {
            const json = JSON.parse(recipe.json.toString())
            const fluids = json.inputs && json.inputs.fluid
            let swapped = false
            if (fluids) fluids.forEach(slot => slot.content.value.forEach(v => {
                if ((v.tag || v.fluid) === 'forge:sodium_persulfate' || v.fluid === 'gtceu:sodium_persulfate') {
                    delete v.tag
                    v.fluid = 'gtceu:redstone_tri_p_toluenesulfonate'
                    swapped = true
                }
            }))
            if (swapped) swaps.push({ id: id, board: board, json: json })
        })
    })
    swaps.forEach(swap => {
        event.remove({ id: swap.id })
        const made = event.custom(swap.json)
        if (made && typeof made.id === 'function') made.id(`af9:chemical_reactor/${swap.board}_circuit_board_redstone_etching`)
        else console.warn(`circuit_etching.js: no recipe object for ${swap.id}`)
    })
    boards.forEach(board => event.remove({ id: `gtceu:large_chemical_reactor/${board}_circuit_board_persulfate` }))
})
