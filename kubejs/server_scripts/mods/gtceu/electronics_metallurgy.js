// AF9 - Electronics metallurgy recipes (materials: af9-core, registry/AF9Materials)
// The zircon -> zirconium / hafnium chain and the zircon sands vein. The alloy dusts of the circuit metals (Al-Si, Kovar,
// Pt-Ir: mixers) and the circuits using them are in circuits_af9.js. Spec: docs/semiconductor-factory.md

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // ---- Zircon -> zirconium and hafnium tetrachloride ----
    // 1. Plasma dissociation: ZrSiO4 -> ZrO2 + SiO2
    event.recipes.gtceu.electric_blast_furnace('af9:zircon_dissociation')
        .itemInputs('6x gtceu:zircon_dust')
        .inputFluids(Fluid.of('gtceu:argon', 100))
        .itemOutputs('3x gtceu:zirconia_dust', '3x gtceu:silicon_dioxide_dust')
        .blastFurnaceTemp(2300)
        .duration(600)
        .EUt(VA[GTValues.HV])

    // 2. Carbochlorination: ZrO2 + 2 C + 2 Cl2 -> ZrCl4 + 2 CO (the CO is flared). The hafnium comes along.
    event.recipes.gtceu.electric_blast_furnace('af9:zirconia_carbochlorination')
        .itemInputs('3x gtceu:zirconia_dust', '2x gtceu:carbon_dust')
        .inputFluids(Fluid.of('gtceu:chlorine', 4000))
        .outputFluids(Fluid.of('gtceu:crude_zirconium_tetrachloride', 1000))
        .blastFurnaceTemp(1300)
        .duration(400)
        .EUt(VA[GTValues.HV])

    // 3. Extractive distillation, the only practical way to split zirconium from hafnium. Real zircon holds ~2 % Hf;
    // this is richer so LUV lithography has enough. The hafnium tetrachloride is a fab precursor (high-k gate), so
    // this runs in the SMC Rectification Column (fab_machines.js), whole: no single-block cuts.
    event.recipes.gtceu.fab_distillation('af9:zirconium_hafnium_separation')
        .inputFluids(Fluid.of('gtceu:crude_zirconium_tetrachloride', 1000))
        .outputFluids(Fluid.of('gtceu:zirconium_tetrachloride', 900), Fluid.of('gtceu:hafnium_tetrachloride', 100))
        .duration(600)
        .EUt(VA[GTValues.HV])
        .cleanroom(CleanroomType.CLEANROOM)

    // 4. Kroll process: ZrCl4 + 2 Mg -> Zr (sponge) + 2 MgCl2. GT's electrolyzer turns the MgCl2 back into Mg + Cl2.
    event.recipes.gtceu.electric_blast_furnace('af9:zirconium_kroll')
        .itemInputs('2x gtceu:magnesium_dust')
        .inputFluids(Fluid.of('gtceu:zirconium_tetrachloride', 1000))
        .itemOutputs('gtceu:zirconium_dust', '6x gtceu:magnesium_chloride_dust')
        .blastFurnaceTemp(1150)
        .duration(400)
        .EUt(VA[GTValues.HV])
})

// Zircon sands vein in the Mining Dimension (where the pack puts every GT vein), stone layer, same form as its other
// veins
GTCEuServerEvents.oreVeins(event => {
    const dike = (material, weight) => new GTDikeBlockDefinition['(com.gregtechceu.gtceu.api.data.chemical.material.Material,int,int,int)'](
        GTMaterials.get(material), weight, 129, 248)
    event.add('af9:zircon_sands_vein', builder => {
        builder.clusterSize(35)
            .weight(30)
            .density(0.6)
            .discardChanceOnAirExposure(0.0)
            .layer('stone')
            .dimensions('allthemodium:mining')
            .biomes('#allthemodium:mining_features/mining_biomes')
            .heightRangeUniform(129, 248)
            .dikeVeinGenerator(generator => generator
                .withBlock(dike('zircon', 5))
                .withBlock(dike('monazite', 2))
                .withBlock(dike('almandine', 1)))
    })
})
