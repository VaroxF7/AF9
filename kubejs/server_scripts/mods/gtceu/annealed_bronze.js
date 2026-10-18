// AF9 - Annealed Bronze: bronze softened by annealing, as annealed copper is. The material (plate, foil): af9-core,
// registry/AF9Materials (annealedBronze()). Janne writes the recipes that use the foil.
//
//   bronze ingot + nitrogen    -EBF 1000 K->    annealed bronze ingot   (inert atmosphere, no scale; slow furnace cool)
//   plate and foil             GT's own bender / hammer recipes from the flags

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    event.recipes.gtceu.electric_blast_furnace('af9:annealed_bronze/anneal')
        .itemInputs('gtceu:bronze_ingot')
        .inputFluids(Fluid.of('gtceu:nitrogen', 1000))
        .itemOutputs('gtceu:annealed_bronze_ingot')
        .blastFurnaceTemp(1000)
        .duration(400)
        .EUt(VA[GTValues.MV])
})
