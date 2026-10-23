// AF9 - PCB Coolant as an askarel, the dielectric of old transformers, instead of GT's "PCB + distilled water" (materials:
// af9-core, registry/AF9Materials (askarel()). Spec: docs/pcb-coolant.md). The first user is the IV HPCA; nothing here
// needs more than an EV Large Chemical Reactor, and every chlorination loop returns its HCl to the epichlorohydrin.
//
//   biphenyl (GT: benzene + O2) + 2 Cl2 (FeCl3)      -reactor->  dichlorobiphenyl + 2 HCl
//   dichlorobiphenyl + 2 Cl2 (FeCl3)                 -LCR->      polychlorinated biphenyl + 2 HCl
//   dichlorobenzene (aromatics.js) + Cl2 (FeCl3)     -reactor->  trichlorobenzene + HCl
//   PCB + trichlorobenzene                           -mixer->    askarel
//   askarel + calcium chloride                       -reactor->  dry askarel                (water would ruin the dielectric)
//   glycerol + HCl (GT)                              -reactor->  epichlorohydrin            (the HCl scavenger)
//   dry askarel + epichlorohydrin                    -mixer->    PCB Coolant
//   calcite + 2 HCl                                  -reactor->  calcium chloride (the desiccant, from the loop's own acid)
ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const gt = event.recipes.gtceu

    // GT's one-step PCB and its water mix are gone (the large reactor copy of the PCB recipe too)
    for (const id of ['gtceu:chemical_reactor/polychlorinated_biphenyl', 'gtceu:large_chemical_reactor/polychlorinated_biphenyl',
        'gtceu:mixer/pcb_coolant']) event.remove({ id: id })

    // ---- the biphenyl is chlorinated in two goes: ferric chloride makes the ring take chlorine ----
    gt.chemical_reactor('af9:pcb_coolant/dichlorobiphenyl')
        .notConsumableFluid(Fluid.of('gtceu:iron_iii_chloride', 100))
        .itemInputs('2x gtceu:biphenyl_dust')
        .inputFluids(Fluid.of('gtceu:chlorine', 2000))
        .outputFluids(Fluid.of('gtceu:dichlorobiphenyl', 1000), Fluid.of('gtceu:hydrochloric_acid', 2000))
        .duration(300)
        .EUt(VA[GTValues.HV])

    gt.large_chemical_reactor('af9:pcb_coolant/polychlorination')
        .notConsumableFluid(Fluid.of('gtceu:iron_iii_chloride', 100))
        .circuit(2)
        .inputFluids(Fluid.of('gtceu:dichlorobiphenyl', 1000), Fluid.of('gtceu:chlorine', 2000))
        .outputFluids(Fluid.of('gtceu:polychlorinated_biphenyl', 1000), Fluid.of('gtceu:hydrochloric_acid', 2000))
        .duration(400)
        .EUt(VA[GTValues.EV])

    // ---- the thinner: askarel was PCB cut with trichlorobenzene, so the ring is chlorinated once more ----
    gt.chemical_reactor('af9:pcb_coolant/trichlorobenzene')
        .notConsumableFluid(Fluid.of('gtceu:iron_iii_chloride', 100))
        .circuit(3)
        .inputFluids(Fluid.of('gtceu:dichlorobenzene', 1000), Fluid.of('gtceu:chlorine', 1000))
        .outputFluids(Fluid.of('gtceu:trichlorobenzene', 1000), Fluid.of('gtceu:hydrochloric_acid', 1000))
        .duration(240)
        .EUt(VA[GTValues.HV])

    gt.mixer('af9:pcb_coolant/askarel')
        .inputFluids(Fluid.of('gtceu:polychlorinated_biphenyl', 600), Fluid.of('gtceu:trichlorobenzene', 400))
        .outputFluids(Fluid.of('gtceu:askarel', 1000))
        .duration(160)
        .EUt(VA[GTValues.HV])

    // ---- a dielectric must be dry: calcium chloride takes the water ----
    gt.chemical_reactor('af9:pcb_coolant/calcium_chloride')
        .itemInputs('gtceu:calcite_dust')
        .inputFluids(Fluid.of('gtceu:hydrochloric_acid', 2000))
        .itemOutputs('gtceu:calcium_chloride_dust')
        .outputFluids(Fluid.of('gtceu:carbon_dioxide', 1000), Fluid.of('minecraft:water', 1000))
        .duration(160)
        .EUt(VA[GTValues.MV])

    gt.chemical_reactor('af9:pcb_coolant/drying')
        .itemInputs('gtceu:calcium_chloride_dust')
        .inputFluids(Fluid.of('gtceu:askarel', 1000))
        .outputFluids(Fluid.of('gtceu:dry_askarel', 1000))
        .duration(200)
        .EUt(VA[GTValues.HV])

    // ---- the stabiliser: an epoxide that mops up the HCl a hot PCB gives off; the glycerol is the biodiesel's ----
    // (GT's own epichlorohydrin_from_glycerol is the recipe; the coolant takes its output)
    gt.mixer('af9:pcb_coolant/blend')
        .inputFluids(Fluid.of('gtceu:dry_askarel', 900), Fluid.of('gtceu:epichlorohydrin', 100))
        .outputFluids(Fluid.of('gtceu:pcb_coolant', 1000))
        .duration(200)
        .EUt(VA[GTValues.HV])
})
