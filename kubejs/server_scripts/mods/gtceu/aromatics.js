// AF9 - Aromatics, GTNH-style: benzene from wood tar and toluene, chlorobenzene both ways,
// and the stepwise methane chlorination down to chloroform (materials: af9-core, registry/AF9Materials).
// Spec: docs/green-chemistry.md
//
//   wood tar                 -distillation tower-> benzene + toluene + phenol + creosote (AF9 topping)
//   toluene + H2            -reactor->          benzene + methane (hydrodealkylation)
//   benzene + Cl2           -reactor->          chlorobenzene + HCl (FeCl3, circuit 1)
//   benzene + 2 Cl2         -reactor->          dichlorobenzene + 2 HCl (FeCl3, circuit 2)
//   chlorobenzene + H2      -reactor->          benzene + HCl (Pd/C: the elevator's chlorobenzene back to benzene)
//   methane -Cl2-> chloromethane -Cl2-> dichloromethane -Cl2-> chloroform (+ HCl each step, free-radical)
//
// The chain ends at chloroform on purpose. GT's own benzene and chlorobenzene chemistry is untouched.

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // ---- AF9 topping: a bucket of wood tar splits four ways ----
    event.recipes.gtceu.distillation_tower('af9:wood_tar_topping')
        .inputFluids(Fluid.of('gtceu:wood_tar', 1000))
        .outputFluids(
            Fluid.of('gtceu:benzene', 400),
            Fluid.of('gtceu:toluene', 200),
            Fluid.of('gtceu:phenol', 100),
            Fluid.of('gtceu:creosote', 300)
        )
        .duration(200)
        .EUt(VA[GTValues.MV])

    // ---- Hydrodealkylation: C7H8 + H2 -> C6H6 + CH4 ----
    event.recipes.gtceu.chemical_reactor('af9:toluene_dealkylation')
        .inputFluids(Fluid.of('gtceu:toluene', 1000), Fluid.of('gtceu:hydrogen', 1000))
        .outputFluids(Fluid.of('gtceu:benzene', 1000), Fluid.of('gtceu:methane', 1000))
        .duration(200)
        .EUt(VA[GTValues.HV])

    // ---- Electrophilic chlorination over ferric chloride: mono (circuit 1) and di (circuit 2) ----
    // The circuits are load-bearing: the mono recipe's inputs are inside the di recipe's (lint R7)
    event.recipes.gtceu.chemical_reactor('af9:chlorobenzene_synthesis')
        .notConsumable('gtceu:iron_iii_chloride_dust')
        .circuit(1)
        .inputFluids(Fluid.of('gtceu:benzene', 1000), Fluid.of('gtceu:chlorine', 1000))
        .outputFluids(Fluid.of('gtceu:chlorobenzene', 1000), Fluid.of('gtceu:hydrochloric_acid', 1000))
        .duration(200)
        .EUt(VA[GTValues.MV])

    event.recipes.gtceu.chemical_reactor('af9:dichlorobenzene_synthesis')
        .notConsumable('gtceu:iron_iii_chloride_dust')
        .circuit(2)
        .inputFluids(Fluid.of('gtceu:benzene', 1000), Fluid.of('gtceu:chlorine', 2000))
        .outputFluids(Fluid.of('gtceu:dichlorobenzene', 1000), Fluid.of('gtceu:hydrochloric_acid', 2000))
        .duration(300)
        .EUt(VA[GTValues.HV])

    // ---- Hydrodechlorination over palladium on carbon: elevator chlorobenzene back to benzene ----
    event.recipes.gtceu.chemical_reactor('af9:chlorobenzene_hydrodechlorination')
        .notConsumable('gtceu:palladium_on_carbon_dust')
        .inputFluids(Fluid.of('gtceu:chlorobenzene', 1000), Fluid.of('gtceu:hydrogen', 1000))
        .outputFluids(Fluid.of('gtceu:benzene', 1000), Fluid.of('gtceu:hydrochloric_acid', 1000))
        .duration(200)
        .EUt(VA[GTValues.HV])

    // ---- Methane chlorination, one chlorine at a time ----
    event.recipes.gtceu.chemical_reactor('af9:chloromethane_synthesis')
        .inputFluids(Fluid.of('gtceu:methane', 1000), Fluid.of('gtceu:chlorine', 1000))
        .outputFluids(Fluid.of('gtceu:chloromethane', 1000), Fluid.of('gtceu:hydrochloric_acid', 1000))
        .duration(160)
        .EUt(VA[GTValues.MV])

    event.recipes.gtceu.chemical_reactor('af9:dichloromethane_synthesis')
        .inputFluids(Fluid.of('gtceu:chloromethane', 1000), Fluid.of('gtceu:chlorine', 1000))
        .outputFluids(Fluid.of('gtceu:dichloromethane', 1000), Fluid.of('gtceu:hydrochloric_acid', 1000))
        .duration(160)
        .EUt(VA[GTValues.MV])

    event.recipes.gtceu.chemical_reactor('af9:chloroform_synthesis')
        .inputFluids(Fluid.of('gtceu:dichloromethane', 1000), Fluid.of('gtceu:chlorine', 1000))
        .outputFluids(Fluid.of('gtceu:chloroform', 1000), Fluid.of('gtceu:hydrochloric_acid', 1000))
        .duration(160)
        .EUt(VA[GTValues.MV])
})
